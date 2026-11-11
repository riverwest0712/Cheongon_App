package com.example.cheongon.recommendation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.cheongon.R;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
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

public class ConditionInputActivity extends AppCompatActivity {

    private EditText etAgeInput; // 직접 타이핑하는 나이 입력창
    private SeekBar seekBarAge;

    private Spinner spinnerRegion, spinnerSubRegion, spinnerJob, spinnerFamily, spinnerWeak, spinnerMilitary;
    private EditText etIncome;
    private Button btnMale, btnFemale, btnMatching;
    private ImageView btnBack;

    private SharedPreferences loginPreferences;

    // 성별 선택을 저장할 변수
    private String selectedGender = "";

    private Hamburger hamburger;

    private final ActivityResultLauncher<Intent> incomeCalculatorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int income = result.getData().getIntExtra(
                            IncomeCalculatorActivity.EXTRA_MONTHLY_INCOME, 0);
                    etIncome.setText(String.valueOf(income));
                    etIncome.setError(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_condition_input);

        // 햄버거 메뉴
        hamburger = new Hamburger(this);

        // 상단 헤더
        new TopHeader(this);

        // 하단 메뉴
        new BottomMenu(this);

        // XML 뷰 연결
        etAgeInput = findViewById(R.id.etAgeInput);
        seekBarAge = findViewById(R.id.seekBarAge);

        spinnerRegion = findViewById(R.id.spinnerRegion);
        spinnerSubRegion = findViewById(R.id.spinnerSubRegion);
        spinnerJob = findViewById(R.id.spinnerJob);
        spinnerFamily = findViewById(R.id.spinnerFamily);
        spinnerWeak = findViewById(R.id.spinnerWeak);
        spinnerMilitary = findViewById(R.id.spinnerMilitary);
        etIncome = findViewById(R.id.etIncome);

        btnMale = findViewById(R.id.btnMale);
        btnFemale = findViewById(R.id.btnFemale);
        btnMatching = findViewById(R.id.btnMatching);
        btnBack = findViewById(R.id.btnBack);

        findViewById(R.id.btnOpenIncomeCalculator).setOnClickListener(view ->
                incomeCalculatorLauncher.launch(
                        new Intent(this, IncomeCalculatorActivity.class)));

        loginPreferences = getSharedPreferences("loginInfo", MODE_PRIVATE);

        // 뒤로가기
        btnBack.setOnClickListener(view -> finish());

        // 1. SeekBar를 움직일 때 EditText 숫자가 바뀌도록 연동
        seekBarAge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int age = 19 + progress;
                    etAgeInput.setText(String.valueOf(age));
                    etAgeInput.setSelection(etAgeInput.getText().length()); // 커서를 맨 뒤로
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 2. EditText에 숫자를 직접 입력할 때 실시간 검증 및 제한
        etAgeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    etAgeInput.setError("나이를 입력해주세요.");
                    return;
                }

                try {
                    int age = Integer.parseInt(s.toString());

                    if (age < 19) {
                        etAgeInput.setError("19세 이상만 입력 가능합니다.");
                    } else if (age > 39) {
                        etAgeInput.setError("39세 이하만 입력 가능합니다.");
                    } else {
                        etAgeInput.setError(null); // 범위 안이면 에러 초기화
                        int progress = age - 19;
                        if (seekBarAge.getProgress() != progress) {
                            seekBarAge.setProgress(progress);
                        }
                    }
                } catch (NumberFormatException e) {
                    etAgeInput.setError("올바른 숫자를 입력해주세요.");
                }
            }
        });

        // 초기 기본값 설정 (예: 29세)
        etAgeInput.setText("29");

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 모든 스피너 항목 설정
        setSpinner(spinnerRegion, new String[]{"거주지를 선택해주세요", "서울특별시", "경기도", "인천광역시", "부산광역시", "대구광역시", "광주광역시", "대전광역시", "울산광역시", "세종특별자치시", "강원특별자치도", "충청북도", "충청남도", "전북특별자치도", "전라남도", "경상북도", "경상남도", "제주특별자치도"});
        setSpinner(spinnerJob, new String[]{"재직 상태를 선택해주세요", "재직자", "미취업자", "학생", "자영업자", "프리랜서", "기타"});
        setSpinner(spinnerFamily, new String[]{"가구 특성을 선택해주세요", "1인 가구", "다인가구", "한부모 가구", "다문화 가구", "해당 없음"});
        setSpinner(spinnerWeak, new String[]{"취약계층 여부를 선택해주세요", "해당 없음", "기초생활수급자", "차상위계층", "장애인", "기타"});
        setSpinner(spinnerMilitary, new String[]{"병역 상태를 선택해주세요", "해당 없음", "미필", "복무 중", "군필", "면제"});

        // 성별 선택 버튼
        btnMale.setOnClickListener(view -> {
            selectedGender = "남자";
            btnMale.setBackgroundResource(R.drawable.logo_background);
            btnMale.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            btnFemale.setBackgroundResource(R.drawable.edittext_background);
            btnFemale.setTextColor(android.graphics.Color.parseColor("#8D8D8D"));
        });

        btnFemale.setOnClickListener(view -> {
            selectedGender = "여자";
            btnFemale.setBackgroundResource(R.drawable.logo_background);
            btnFemale.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            btnMale.setBackgroundResource(R.drawable.edittext_background);
            btnMale.setTextColor(android.graphics.Color.parseColor("#8D8D8D"));
        });

        // 매칭 결과 확인 (서버 전송)
        btnMatching.setOnClickListener(view -> {
            int userId = loginPreferences.getInt("id", -1);

            if (userId == -1) {
                Snackbar.make(findViewById(android.R.id.content), "로그인 정보가 없습니다. 다시 로그인해주세요.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (selectedGender.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "성별을 선택해주세요.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // 나이 입력값 검증
            String ageStr = etAgeInput.getText().toString().trim();
            if (ageStr.isEmpty()) {
                etAgeInput.setError("나이를 입력해주세요.");
                return;
            }

            int age = 29;
            try {
                age = Integer.parseInt(ageStr);
                if (age < 19 || age > 39) {
                    etAgeInput.setError("19세부터 39세 사이로 입력해주세요.");
                    Snackbar.make(findViewById(android.R.id.content), "나이는 19세부터 39세 사이만 가능합니다.", Snackbar.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                etAgeInput.setError("올바른 숫자를 입력해주세요.");
                return;
            }

            // 소득 입력값 검증
            String incomeStr = etIncome.getText().toString().trim();
            if (incomeStr.isEmpty()) {
                etIncome.setError("가구 연소득을 입력해주세요.");
                return;
            }

            int incomeValue = 0;
            try {
                incomeValue = Integer.parseInt(incomeStr);
            } catch (NumberFormatException e) {
                etIncome.setError("올바른 숫자를 입력해주세요.");
                return;
            }

            // 현재 화면의 선택값 가져오기
            String region = spinnerRegion.getSelectedItem().toString();
            String job = spinnerJob.getSelectedItem().toString();
            String family = spinnerFamily.getSelectedItem().toString();
            String weak = spinnerWeak.getSelectedItem().toString();
            String military = spinnerMilitary.getSelectedItem().toString();

            // 서버로 보낼 데이터를 Map에 담기
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("users_id", userId);
            conditions.put("age", age);
            conditions.put("gender", selectedGender);
            conditions.put("region", region.contains("선택") ? "무관" : region);
            conditions.put("employment_status", job.contains("선택") ? "무관" : job);
            conditions.put("household_type", family.contains("선택") ? "무관" : family);
            conditions.put("vulnerable_status", weak.contains("선택") ? "무관" : weak);
            conditions.put("military_status", military.contains("선택") ? "무관" : military);
            conditions.put("income", incomeValue);

            // API 호출
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.saveUserConditions(conditions).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Snackbar.make(findViewById(android.R.id.content), "조건이 성공적으로 저장되었습니다!", Snackbar.LENGTH_SHORT).show();

                        // 추천 결과 화면으로 이동
                        Intent intent = new Intent(ConditionInputActivity.this, RecommendationActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = "조건 저장 실패";
                        try {
                            if (response.errorBody() != null) {
                                errorMsg = response.errorBody().string();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Snackbar.make(findViewById(android.R.id.content), "DB 거절 사유: " + errorMsg, Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Snackbar.make(findViewById(android.R.id.content), "서버 연결 실패: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) {
            hamburger.updateLoginState();
        }
    }

    // Spinner 공통 세팅 함수
    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(android.graphics.Color.parseColor("#191F28"));
                tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(android.graphics.Color.parseColor("#191F28"));
                tv.setBackgroundColor(android.graphics.Color.parseColor("#F8F9FC"));
                tv.setPadding(16, 24, 16, 24);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // 1. 화면의 빈 곳을 누르면 키패드를 숨기고 포커스를 해제하는 메서드
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == android.view.MotionEvent.ACTION_DOWN)) {
            android.graphics.Rect rect = new android.graphics.Rect();
            view.getGlobalVisibleRect(rect);
            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();

            // 현재 포커스된 뷰(EditText 등) 바깥을 터치했을 경우
            if (!rect.contains(x, y)) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                view.clearFocus(); // 포커스(깜빡이 커서) 해제
            }
        }
        return super.dispatchTouchEvent(ev);
    }

}