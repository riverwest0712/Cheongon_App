package com.example.cheongon.jobtool;

import android.os.Bundle;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.login.LoginActivity;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.HttpUrl;

/** Opens Toss test checkout in the browser and refreshes access when returning to the app. */
public class JobtoolPaymentActivity extends AppCompatActivity {
    private Hamburger hamburger;
    private Button paymentButton;
    private boolean purchased;
    private boolean creatingOrder;
    private Call<Map<String, Object>> orderRequest;
    private Call<Map<String, Object>> accessRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobtool_payment);
        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);
        findViewById(R.id.btnPaymentBack).setOnClickListener(view -> finish());
        paymentButton = findViewById(R.id.btnProPayment);
        paymentButton.setOnClickListener(view -> {
            if (purchased) openTools();
            else startPayment();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) hamburger.updateLoginState();
        refreshPurchase();
    }

    private void startPayment() {
        if (creatingOrder) return;
        String token = JobtoolNavigation.authorization(this);
        if (token.equals("Bearer ")) {
            requestLogin();
            return;
        }
        paymentButton.setEnabled(false);
        creatingOrder = true;
        paymentButton.setText("결제창 여는 중…");
        orderRequest = ApiClient.getClient().create(ApiService.class).createJobtoolPaymentOrder(token);
        orderRequest.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                creatingOrder = false;
                if (!active(token)) return;
                resetButton();
                if (response.code() == 401) {
                    requestLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    showMessage("결제창을 열지 못했어요", response.code() == 503
                            ? "토스 테스트 키와 결제 DB 설정이 준비되지 않았거나 서버에 연결할 수 없습니다."
                            : "기존 결제 내역을 확인한 뒤 다시 시도해주세요.");
                    return;
                }
                if (Boolean.TRUE.equals(response.body().get("purchased"))) {
                    purchased = true;
                    JobtoolNavigation.rememberPurchase(token, true);
                    resetButton();
                    openTools();
                    return;
                }
                Object path = response.body().get("checkout_path");
                if (!(path instanceof String)
                        || !((String) path).matches("/payments/jobtool/checkout/[a-f0-9]{64}")) {
                    showMessage("결제 오류", "올바른 결제 주소를 받지 못했습니다.");
                    return;
                }
                HttpUrl url = ApiClient.getClient().baseUrl().resolve((String) path);
                if (url == null) {
                    showMessage("결제 오류", "결제 주소를 확인해주세요.");
                    return;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
                } catch (ActivityNotFoundException error) {
                    showMessage("브라우저 필요", "결제를 진행할 웹 브라우저를 설치해주세요.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                creatingOrder = false;
                if (!active(token) || call.isCanceled()) return;
                resetButton();
                showMessage("서버 연결 실패", "인터넷 연결을 확인하고 다시 시도해주세요.");
            }
        });
    }

    private void refreshPurchase() {
        purchased = false;
        if (accessRequest != null) accessRequest.cancel();
        String token = JobtoolNavigation.authorization(this);
        resetButton();
        if (token.equals("Bearer ")) return;
        accessRequest = ApiClient.getClient().create(ApiService.class).getJobtoolAccess(token);
        accessRequest.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!active(token) || call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().get("purchased") instanceof Boolean) {
                    purchased = Boolean.TRUE.equals(response.body().get("purchased"));
                    JobtoolNavigation.rememberPurchase(token, purchased);
                    resetButton();
                    if (purchased) openTools();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                // Creating an order rechecks access on the server, so a failed refresh cannot grant access.
            }
        });
    }

    private boolean active(String token) {
        return !isFinishing() && !isDestroyed() && token.equals(JobtoolNavigation.authorization(this));
    }

    private void resetButton() {
        paymentButton.setEnabled(!creatingOrder);
        paymentButton.setText(creatingOrder ? "결제창 여는 중…"
                : purchased ? "취업 도구 이용하기" : "990원 결제하기");
    }

    private void requestLogin() {
        new AlertDialog.Builder(this)
                .setTitle("로그인 필요")
                .setMessage("구매할 계정을 확인하려면 로그인해주세요.")
                .setPositiveButton("로그인", (dialog, which) -> startActivity(
                        new Intent(this, LoginActivity.class).putExtra("forceReauthenticate", true)))
                .setNegativeButton("취소", null)
                .show();
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private void openTools() {
        startActivity(new Intent(this, JobToolActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (orderRequest != null) orderRequest.cancel();
        if (accessRequest != null) accessRequest.cancel();
        super.onDestroy();
    }
}
