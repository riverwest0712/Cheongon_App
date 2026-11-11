package com.example.cheongon.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.R;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Main;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etId;
    EditText etPassword;
    Button btnLogin;
    TextView tvSignUp;
    TextView tvFindPassword;
    ImageView btnBack;
    SocialLoginManager socialLoginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.cheongon.mypage.NotificationSession.initialize(this);
        if (getSharedPreferences("loginInfo", MODE_PRIVATE).getBoolean("isLoggedIn", false)
                && !getIntent().getBooleanExtra("forceReauthenticate", false)) {
            startActivity(new Intent(this, Main.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        etId = findViewById(R.id.etId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvFindPassword = findViewById(R.id.tvFindPassword);
        btnBack = findViewById(R.id.btnBack);
        socialLoginManager = new SocialLoginManager(this);

        findViewById(R.id.btnGoogleLogin).setOnClickListener(view -> socialLoginManager.loginWithGoogle());

        // 뒤로가기 버튼
        btnBack.setOnClickListener(view -> finish());

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 로그인 버튼 클릭 리스너
        btnLogin.setOnClickListener(view -> {
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (id.isEmpty()) {
                etId.setError("아이디를 입력해주세요.");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("비밀번호를 입력해주세요.");
                return;
            }

            // 🐾 서버로 보낼 로그인 데이터 구성 (서버의 user_id 키값에 맞춤)[cite: 10]
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Map<String, String> loginData = new HashMap<>();
            loginData.put("user_id", id); // 서버가 user_id를 받으므로 일치시킴[cite: 10]
            loginData.put("password", password);

            apiService.login(loginData).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {

                        // 1. 서버가 보낸 전체 데이터 받기
                        Map<String, Object> body = response.body();

                        // 2. 그 안에서 "user" 정보만 쏙 빼내기
                        Map<String, Object> user = (Map<String, Object>) body.get("user");

                        if (user != null) {
                            // 3. user 안에서 id와 user_id(name), email 꺼내기
                            // (Gson은 숫자를 Double로 변환하므로 int로 형변환 필요)
                            int userId = ((Double) user.get("id")).intValue();
                            String userName = (String) user.get("user_id"); // 서버가 보내준 user_id (name)[cite: 10]
                            String email = (String) user.get("email");       // 서버가 보내준 email[cite: 10]

                            // 4. 안드로이드 수첩(SharedPreferences)에 로그인 정보 안전하게 저장
                            SharedPreferences loginPreferences = getSharedPreferences("loginInfo", MODE_PRIVATE);
                            SharedPreferences.Editor editor = loginPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putBoolean("isMockSocialLogin", false);
                            editor.putString("authProvider", "local");
                            editor.putString("notificationToken",
                                    user.get("notificationToken") instanceof String
                                            ? (String) user.get("notificationToken") : "");
                            editor.putString("accessToken", body.get("access_token") instanceof String
                                    ? (String) body.get("access_token") : "");
                            editor.putInt("id", userId);
                            editor.putString("userName", userName);
                            editor.putString("email", email);
                            editor.apply();

                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "청온에 오신 것을 환영합니다!",
                                    Snackbar.LENGTH_SHORT
                            ).show();

                            // 메인 화면으로 이동
                            Intent intent = new Intent(LoginActivity.this, Main.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "아이디 또는 비밀번호가 올바르지 않습니다.",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "서버 연결에 실패했습니다: " + t.getMessage(),
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            });
        });

        // 회원가입 화면으로 이동
        tvSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 비밀번호 찾기 화면으로 이동
        tvFindPassword.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
            startActivity(intent);
        });
    }

    // SharedPreferences에 사용자 정보 저장 보조 메서드
    private void saveLoginInfo(int userId, String userName, String email) {
        SharedPreferences loginPreferences = getSharedPreferences("loginInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = loginPreferences.edit();

        editor.putBoolean("isLoggedIn", true);
        editor.putInt("userId", userId);
        editor.putString("userName", userName);
        editor.putString("email", email);
        editor.apply();
    }

}
