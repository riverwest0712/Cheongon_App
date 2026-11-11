package com.example.cheongon.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoogleSignupActivity extends AppCompatActivity {
    private String token;
    private SignupConsent consent;
    private EditText nickname;
    private Button submit;
    private Call<Map<String, Object>> signupCall;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_google_signup);
        token = getIntent().getStringExtra("id_token");
        if (token == null || token.isEmpty()) { finish(); return; }
        ((TextView) findViewById(R.id.googleEmail)).setText(getIntent().getStringExtra("email"));
        nickname = findViewById(R.id.googleNickname);
        submit = findViewById(R.id.completeGoogleSignup);
        findViewById(R.id.googleSignupBack).setOnClickListener(v -> finish());
        consent = new SignupConsent(this, submit);
        submit.setOnClickListener(v -> complete());
    }

    private void complete() {
        String name = nickname.getText().toString().trim();
        if (!name.matches("[가-힣a-zA-Z0-9_]{2,20}")) {
            nickname.setError("한글, 영문, 숫자, 밑줄 2~20자로 입력해주세요.");
            return;
        }
        if (!consent.isAccepted()) return;
        consent.setSubmitting(true);
        Map<String, Object> body = new HashMap<>();
        body.put("provider", "google");
        body.put("id_token", token);
        body.put("name", name);
        consent.addTo(body);
        signupCall = ApiClient.getClient().create(ApiService.class).completeSocialSignup(body);
        signupCall.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isFinishing() || isDestroyed()) return;
                consent.setSubmitting(false);
                if (response.isSuccessful() && response.body() != null) {
                    new SocialLoginManager(GoogleSignupActivity.this).saveSession(response.body(), false);
                    return;
                }
                String error = "가입을 완료하지 못했습니다. 다시 시도해주세요.";
                String code = "";
                try {
                    if (response.errorBody() != null) {
                        JSONObject body = new JSONObject(response.errorBody().string());
                        error = body.optString("message", error);
                        code = body.optString("code", "");
                    }
                } catch (Exception ignored) { }
                if ("NICKNAME_TAKEN".equals(code)) {
                    nickname.setError(error);
                    nickname.requestFocus();
                    return;
                }
                message(error);
                if (response.code() == 401) finish();
                if (response.code() == 409 && "TERMS_CHANGED".equals(code)) consent.reload();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                if (!call.isCanceled() && !isDestroyed()) {
                    consent.setSubmitting(false);
                    message("서버 연결에 실패했습니다. 다시 시도해주세요.");
                }
            }
        });
    }

    private void message(String text) { Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show(); }

    @Override protected void onDestroy() {
        if (consent != null) consent.cancel();
        if (signupCall != null) signupCall.cancel();
        super.onDestroy();
    }

}
