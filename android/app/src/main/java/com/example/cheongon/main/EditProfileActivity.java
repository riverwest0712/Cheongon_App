package com.example.cheongon.main;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.google.android.material.snackbar.Snackbar;

import android.widget.ImageView;

import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack;

    private Hamburger hamburger;

    // 아이디 수정
    private EditText etNewId;
    private TextView btnCheckId;
    private TextView btnChangeId;

    // 비밀번호 변경
    private EditText etPasswordId;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private TextView btnChangePassword;

    // Retrofit
    private ApiService apiService;

    // 아이디 중복확인 완료 여부
    private boolean isIdChecked = false;

    // 중복확인한 아이디
    private String checkedId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 상단 헤더 + 햄버거 메뉴
        new TopHeader(this);
        hamburger = new Hamburger(this);

        apiService = ApiClient.getClient().create(ApiService.class);

        // =========================
        // 뒤로가기
        // =========================

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(view -> finish());


        // =========================
        // 아이디 수정
        // =========================

        etNewId = findViewById(R.id.etNewId);
        btnCheckId = findViewById(R.id.btnCheckId);
        btnChangeId = findViewById(R.id.btnChangeId);


        etNewId.addTextChangedListener(new android.text.TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                isIdChecked = false;
                checkedId = "";
            }

            @Override
            public void afterTextChanged(
                    android.text.Editable s) {
            }
        });


        // =========================
        // 아이디 중복확인
        // =========================

        btnCheckId.setOnClickListener(view -> {

            String newId =
                    etNewId.getText().toString().trim();

            if (newId.isEmpty()) {

                showSnackbar("새 아이디를 입력해주세요.");

                return;
            }

            checkId(newId);
        });


        // =========================
        // 아이디 변경
        // =========================

        btnChangeId.setOnClickListener(view -> {

            String newId =
                    etNewId.getText().toString().trim();

            if (newId.isEmpty()) {

                showSnackbar("새 아이디를 입력해주세요.");

                return;
            }

            if (!isIdChecked || !newId.equals(checkedId)) {

                showSnackbar(
                        "아이디 중복확인을 먼저 해주세요."
                );

                return;
            }

            changeId(newId);
        });


        // =========================
        // 비밀번호 변경
        // =========================

        etPasswordId = findViewById(R.id.etPasswordId);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);


        btnChangePassword.setOnClickListener(view -> {

            String userId =
                    etPasswordId.getText().toString().trim();

            String newPassword =
                    etNewPassword.getText().toString();

            String confirmPassword =
                    etConfirmPassword.getText().toString();


            if (userId.isEmpty()) {

                showSnackbar("아이디를 입력해주세요.");

                return;
            }

            if (newPassword.isEmpty()) {

                showSnackbar("새 비밀번호를 입력해주세요.");

                return;
            }

            if (confirmPassword.isEmpty()) {

                showSnackbar(
                        "새 비밀번호를 다시 입력해주세요."
                );

                return;
            }

            if (!newPassword.equals(confirmPassword)) {

                showSnackbar(
                        "비밀번호가 일치하지 않습니다."
                );

                return;
            }

            changePassword(userId, newPassword);
        });
    }


    // =========================
    // Snackbar 공통 함수
    // =========================

    private void showSnackbar(String message) {

        Snackbar.make(
                findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_SHORT
        ).show();
    }


    // =========================
    // 아이디 중복확인 API
    // =========================

    private void checkId(String newId) {

        Map<String, String> request =
                new HashMap<>();

        request.put("user_id", newId);

        apiService.checkIdDuplicate(request)
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

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

                            if (Boolean.TRUE.equals(success)) {

                                isIdChecked = true;
                                checkedId = newId;

                                showSnackbar(message);

                            } else {

                                isIdChecked = false;

                                showSnackbar(message);
                            }

                        } else {

                            isIdChecked = false;

                            showSnackbar(
                                    "아이디 중복확인에 실패했습니다."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        isIdChecked = false;

                        showSnackbar(
                                "서버 연결에 실패했습니다."
                        );
                    }
                });
    }


    // =========================
    // 아이디 변경 API
    // =========================

    private void changeId(String newId) {

        // 현재 로그인한 사용자 정보 가져오기
        android.content.SharedPreferences loginPreferences =
                getSharedPreferences(
                        "loginInfo",
                        MODE_PRIVATE
                );

        int userId =
                loginPreferences.getInt("id", -1);

        if (userId == -1) {

            showSnackbar(
                    "로그인 정보를 찾을 수 없습니다."
            );

            return;
        }

        Map<String, Object> request =
                new HashMap<>();

        request.put("users_id", userId);
        request.put("new_user_id", newId);

        apiService.changeUserId(request)
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

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

                            showSnackbar(message);

                            if (Boolean.TRUE.equals(success)) {

                                // SharedPreferences의 아이디도 변경
                                loginPreferences.edit()
                                        .putString(
                                                "userName",
                                                newId
                                        )
                                        .apply();

                                // 중복확인 상태 초기화
                                isIdChecked = false;
                                checkedId = "";

                                // 입력창 비우기
                                etNewId.setText("");
                            }

                        } else {

                            showSnackbar(
                                    "아이디 변경에 실패했습니다."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        showSnackbar(
                                "서버 연결에 실패했습니다."
                        );
                    }
                });
    }


    // =========================
    // 비밀번호 변경 API
    // =========================

    private void changePassword(
            String userId,
            String newPassword) {

        Map<String, Object> request =
                new HashMap<>();

        request.put("user_id", userId);
        request.put("new_password", newPassword);

        apiService.changePassword(request)
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

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

                            showSnackbar(message);

                            if (Boolean.TRUE.equals(success)) {

                                etNewPassword.setText("");
                                etConfirmPassword.setText("");
                            }

                        } else {

                            showSnackbar(
                                    "비밀번호 변경에 실패했습니다."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        showSnackbar(
                                "서버 연결에 실패했습니다."
                        );
                    }
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
}