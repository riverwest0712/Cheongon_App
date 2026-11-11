package com.example.cheongon.main;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;

import android.widget.ImageView;

import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;

import android.widget.ScrollView;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout layoutDeleteAccount;
    private Hamburger hamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // 상단 헤더 + 햄버거 메뉴
        new TopHeader(this);
        hamburger = new Hamburger(this);

        // 뒤로가기
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(view -> finish());


        // 정책 알림 -> 휴대폰 설정으로 이동
        LinearLayout layoutNotification =
                findViewById(R.id.layoutNotification);

        layoutNotification.setOnClickListener(view -> {

            Intent intent = new Intent();

            if (android.os.Build.VERSION.SDK_INT
                    >= android.os.Build.VERSION_CODES.O) {

                intent.setAction(
                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                );

                intent.putExtra(
                        android.provider.Settings.EXTRA_APP_PACKAGE,
                        getPackageName()
                );

            } else {

                intent.setAction(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                );

                intent.setData(
                        android.net.Uri.parse(
                                "package:" + getPackageName()
                        )
                );
            }

            startActivity(intent);
        });


        // 회원정보 수정
        TextView btnEditProfile =
                findViewById(R.id.btnEditProfile);

        btnEditProfile.setOnClickListener(view -> {

            Intent intent = new Intent(
                    SettingsActivity.this,
                    EditProfileActivity.class
            );

            startActivity(intent);
        });

        // 이용약관
        TextView tvTerms = findViewById(R.id.tvTerms);

        tvTerms.setOnClickListener(view -> {
            loadSignupTerms(true);
        });


        // 개인정보 처리방침
        TextView tvPrivacy = findViewById(R.id.tvPrivacy);

        tvPrivacy.setOnClickListener(view -> {
            loadSignupTerms(false);
        });


        // 회원탈퇴
        layoutDeleteAccount =
                findViewById(R.id.layoutDeleteAccount);

        layoutDeleteAccount.setOnClickListener(view -> {

            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("회원탈퇴")
                    .setMessage("회원탈퇴 시 작성한 게시글과 댓글은 삭제되지 않으며, 회원님의 개인정보는 즉시 폐기됩니다. 회원탈퇴를 하시겠습니까? ")

                    .setPositiveButton("예", (dialog, which) -> {

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "회원탈퇴 요청 중...",
                                Snackbar.LENGTH_SHORT
                        ).show();

                        deleteAccount();
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                " 👋 회원 탈퇴가 완료되었습니다. 잘 가 안녕 ㅠㅠ",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    })

                    .setNegativeButton("아니오", null)
                    .show();
        });
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }
    }


    private void deleteAccount() {

        // 회원탈퇴 함수 실행
        Snackbar.make(
                findViewById(android.R.id.content),
                "회원탈퇴 처리 중...",
                Snackbar.LENGTH_SHORT
        ).show();


        SharedPreferences loginPreferences =
                getSharedPreferences(
                        "loginInfo",
                        MODE_PRIVATE
                );

        int userId =
                loginPreferences.getInt("id", -1);


        // 사용자 ID 확인
        Snackbar.make(
                findViewById(android.R.id.content),
                "사용자 ID: " + userId,
                Snackbar.LENGTH_SHORT
        ).show();


        if (userId == -1) {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보를 찾을 수 없습니다.",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }


        Map<String, Object> request =
                new HashMap<>();

        request.put("users_id", userId);


        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);


        // 서버 요청 시작
//        Snackbar.make(
//                findViewById(android.R.id.content),
//                "회원탈퇴 성공",
//                Snackbar.LENGTH_SHORT
//        ).show();


        apiService.deleteUser(request)
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        // 서버 응답
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "서버 응답: " + response.code(),
                                Snackbar.LENGTH_LONG
                        ).show();


                        if (response.isSuccessful()
                                && response.body() != null) {

                            Map<String, Object> body =
                                    response.body();

                            Boolean success =
                                    (Boolean) body.get("success");

                            String message =
                                    String.valueOf(
                                            body.get("message")
                                    );


                            // 서버 메시지
                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    message,
                                    Snackbar.LENGTH_SHORT
                            ).show();


                            if (Boolean.TRUE.equals(success)) {

                                loginPreferences.edit()
                                        .clear()
                                        .apply();


                                Intent intent =
                                        new Intent(
                                                SettingsActivity.this,
                                                com.example.cheongon.login.LoginActivity.class
                                        );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);

                                finish();
                            }

                        } else {

                            // 회원탈퇴 실패
                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "회원탈퇴 실패: "
                                            + response.code(),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    }


                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        // 서버 연결 실패
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "서버 연결 실패: "
                                        + t.getMessage(),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });


    }
    private void loadSignupTerms(boolean showTerms) {

        Snackbar.make(
                findViewById(android.R.id.content),
                "약관을 불러오는 중...",
                Snackbar.LENGTH_SHORT
        ).show();

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.getSignupTerms()
                .enqueue(new Callback<Map<String, String>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, String>> call,
                            Response<Map<String, String>> response) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Map<String, String> body = response.body();

                        if (!response.isSuccessful()
                                || body == null) {

                            showTermsError();
                            return;
                        }

                        String title;
                        String content;

                        if (showTerms) {

                            title = "이용약관";
                            content = body.get("terms_text");

                        } else {

                            title = "개인정보 처리방침";
                            content = body.get("privacy_text");
                        }

                        if (content == null
                                || content.trim().isEmpty()) {

                            showTermsError();
                            return;
                        }

                        showTermsDialog(title, content);
                    }


                    @Override
                    public void onFailure(
                            Call<Map<String, String>> call,
                            Throwable t) {

                        if (call.isCanceled()
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showTermsError();
                    }
                });
    }
    private void showTermsDialog(String title, String content) {

        TextView text = new TextView(this);

        int padding =
                (int) (24 * getResources()
                        .getDisplayMetrics().density);

        text.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        text.setTextSize(15);
        text.setTextColor(android.graphics.Color.DKGRAY);
        text.setText(content);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(text);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("확인", null)
                .show();
    }
    private void showTermsError() {

        Snackbar.make(
                findViewById(android.R.id.content),
                "약관을 불러오지 못했습니다. 다시 시도해주세요.",
                Snackbar.LENGTH_LONG
        ).show();
    }
}