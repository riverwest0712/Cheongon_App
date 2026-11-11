package com.example.cheongon.jobtool;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoverLetterEditActivity extends AppCompatActivity {

    private EditText etCoverLetter;
    private Button btnRequestReview;
    private ImageView btnBack;
    private ProgressBar progressBar;
    private CardView cardResult;
    private TextView tvAiResult;

    private Hamburger hamburger;
    private boolean purchaseVerified;
    private boolean reviewInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover_letter_edit);

        // 공통 컴포넌트 초기화
        hamburger = new Hamburger(this);
        new TopHeader(this);
        new BottomMenu(this);
        FloatingChatbotHelper.init(this);

        // 뷰 연결
        etCoverLetter = findViewById(R.id.etCoverLetter);
        btnRequestReview = findViewById(R.id.btnRequestReview);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        cardResult = findViewById(R.id.cardResult);
        tvAiResult = findViewById(R.id.tvAiResult);

        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // AI 첨삭 요청 버튼 클릭
        btnRequestReview.setOnClickListener(v -> {
            if (!purchaseVerified || reviewInFlight) return;
            String content = etCoverLetter.getText().toString().trim();

            if (content.isEmpty()) {
                etCoverLetter.setError("자소서 내용을 입력해주세요.");
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "내용을 입력해주세요.",
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }

            // 로딩 시작 및 결과창 숨기기
            progressBar.setVisibility(View.VISIBLE);
            cardResult.setVisibility(View.GONE);
            btnRequestReview.setEnabled(false);
            reviewInFlight = true;

            // 서버로 보낼 데이터 준비
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("content", content);

            // Retrofit API 호출
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.requestAiCoverLetterReview(JobtoolNavigation.authorization(this), requestBody).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (isFinishing() || isDestroyed()) return;
                    reviewInFlight = false;
                    progressBar.setVisibility(View.GONE);
                    btnRequestReview.setEnabled(purchaseVerified);
                    if (response.code() == 401 || response.code() == 403) {
                        JobtoolNavigation.invalidatePurchase();
                        verifyPurchase();
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> res = response.body();
                        String feedback = (String) res.get("feedback"); // 서버에서 보내주는 응답 키값

                        if (feedback != null) {
                            tvAiResult.setText(feedback);
                            cardResult.setVisibility(View.VISIBLE); // 결과 카드 띄우기
                        } else {
                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "AI 피드백을 받아오지 못했습니다.",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "서버 응답 오류가 발생했습니다.",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (isFinishing() || isDestroyed()) return;
                    reviewInFlight = false;
                    progressBar.setVisibility(View.GONE);
                    btnRequestReview.setEnabled(purchaseVerified);
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "서버 연결 실패: " + t.getMessage(),
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyPurchase();
        if (hamburger != null) {
            hamburger.updateLoginState();
        }
    }

    private void verifyPurchase() {
        purchaseVerified = false;
        btnRequestReview.setEnabled(false);
        etCoverLetter.setEnabled(false);
        JobtoolNavigation.checkAccess(this, () -> {
            purchaseVerified = true;
            etCoverLetter.setEnabled(true);
            btnRequestReview.setEnabled(!reviewInFlight);
        });
    }
}
