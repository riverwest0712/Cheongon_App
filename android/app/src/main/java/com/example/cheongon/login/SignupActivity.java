package com.example.cheongon.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    EditText etSignupId;
    EditText etSignupEmail;
    EditText etSignupPassword;
    EditText etSignupPasswordCheck;

    Button btnSignup;
    ImageView btnBack;
    private SignupConsent consent;
    private Call<Map<String, Object>> signupCall;

    // 아이디 중복 확인 완료 여부
    boolean isIdChecked = false;

    // 이메일 중복 확인 완료 여부
    boolean isEmailChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etSignupId = findViewById(R.id.etSignupId);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupPasswordCheck = findViewById(R.id.etSignupPasswordCheck);

        btnSignup = findViewById(R.id.btnSignup);
        btnBack = findViewById(R.id.btnBack);
        consent = new SignupConsent(this, btnSignup);
        SocialLoginManager socialLoginManager = new SocialLoginManager(this);
        findViewById(R.id.btnGoogleSignup).setOnClickListener(view -> socialLoginManager.loginWithGoogle());

        // 뒤로가기 버튼
        btnBack.setOnClickListener(view -> finish());

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 아이디 내용 변경 시 기존 중복 확인 결과 무효화
        etSignupId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isIdChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 이메일 내용 변경 시 기존 중복 확인 결과 무효화
        etSignupEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isEmailChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 아이디 입력 후 다른 입력칸으로 이동하면 서버와 통신하여 자동 중복 확인
        etSignupId.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                String userId = etSignupId.getText().toString().trim();
                if (!userId.isEmpty()) {
                    checkId(userId);
                }
            }
        });

        // 이메일 입력 후 다른 입력칸으로 이동하면 서버와 통신하여 자동 중복 확인
        etSignupEmail.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                String email = etSignupEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    checkEmail(email);
                }
            }
        });

        btnSignup.setOnClickListener(view -> {
            if (!consent.isAccepted()) return;
            String userId = etSignupId.getText().toString().trim();
            String email = etSignupEmail.getText().toString().trim();
            String password = etSignupPassword.getText().toString().trim();
            String passwordCheck = etSignupPasswordCheck.getText().toString().trim();

            if (userId.isEmpty()) {
                etSignupId.setError("아이디를 입력해주세요.");
                return;
            }

            // 자동 중복 확인을 통과하지 않은 아이디는 회원가입 불가
            if (!isIdChecked) {
                etSignupId.setError("사용할 수 없거나 아직 확인되지 않은 아이디입니다.");
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "아이디 중복 확인을 완료해주세요.",
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }

            if (email.isEmpty()) {
                etSignupEmail.setError("이메일을 입력해주세요.");
                return;
            }

            // 자동 중복 확인을 통과하지 않은 이메일은 회원가입 불가
            if (!isEmailChecked) {
                etSignupEmail.setError("사용할 수 없거나 아직 확인되지 않은 이메일입니다.");
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "이메일 중복 확인을 완료해주세요.",
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }

            if (password.isEmpty()) {
                etSignupPassword.setError("비밀번호를 입력해주세요.");
                return;
            }

            if (passwordCheck.isEmpty()) {
                etSignupPasswordCheck.setError("비밀번호 확인을 입력해주세요.");
                return;
            }

            if (!password.equals(passwordCheck)) {
                etSignupPasswordCheck.setError("비밀번호가 일치하지 않습니다.");
                return;
            }

            Map<String, Object> user = new HashMap<>();
            user.put("user_id", userId);
            user.put("email", email);
            user.put("password", password);
            consent.addTo(user);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            consent.setSubmitting(true);
            signupCall = apiService.signup(user);
            signupCall.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (isFinishing() || isDestroyed()) return;
                    consent.setSubmitting(false);
                    if (response.isSuccessful()) {
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "회원가입이 완료되었습니다!",
                                Snackbar.LENGTH_SHORT
                        ).show();
                        finish();
                    } else {
                        String errorMessage = "회원가입 실패";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage = response.errorBody().string();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                errorMessage,
                                Snackbar.LENGTH_LONG
                        ).show();
                        if (response.code() == 409) consent.reload();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (call.isCanceled() || isDestroyed()) return;
                    consent.setSubmitting(false);
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "서버 연결 실패: " + t.getMessage(),
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    // 서버에 아이디 중복 요청
    @Override protected void onDestroy() {
        if (consent != null) consent.cancel();
        if (signupCall != null) signupCall.cancel();
        super.onDestroy();
    }

    private void checkId(String userId) {
        isIdChecked = false;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> request = new HashMap<>();
        request.put("user_id", userId);

        apiService.checkIdDuplicate(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    isIdChecked = true;
                    etSignupId.setError(null);
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "사용 가능한 아이디입니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                } else {
                    isIdChecked = false;
                    etSignupId.setError("이미 사용 중인 아이디입니다.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isIdChecked = false;
                etSignupId.setError("아이디 확인 실패");
            }
        });
    }

    // 서버에 이메일 중복 요청
    private void checkEmail(String email) {
        isEmailChecked = false;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        apiService.checkEmailDuplicate(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    isEmailChecked = true;
                    etSignupEmail.setError(null);
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "사용 가능한 이메일입니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                } else {
                    isEmailChecked = false;
                    etSignupEmail.setError("이미 사용 중인 이메일입니다.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isEmailChecked = false;
                etSignupEmail.setError("이메일 확인 실패");
            }
        });
    }

}
