package com.example.cheongon.jobtool;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InterviewQuestionActivity extends AppCompatActivity {
    private EditText jobRole, careerLevel, competencies, coverLetter, company;
    private Button generateButton;
    private View progressBar, resultCard;
    private TextView resultText;
    private Hamburger hamburger;
    private SharedPreferences loginInfo;
    private Call<InterviewQuestionResponse> pendingCall;
    private boolean purchaseVerified;
    private final SharedPreferences.OnSharedPreferenceChangeListener loginListener = (prefs, key) -> {
        if ("isLoggedIn".equals(key) || key == null) {
            if (!prefs.getBoolean("isLoggedIn", false)) clearSessionResult();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interview_question);
        jobRole = findViewById(R.id.etJobRole);
        careerLevel = findViewById(R.id.etCareerLevel);
        competencies = findViewById(R.id.etRequiredCompetencies);
        coverLetter = findViewById(R.id.etCoverLetter);
        company = findViewById(R.id.etCompanyIndustry);
        generateButton = findViewById(R.id.btnGenerateQuestions);
        progressBar = findViewById(R.id.progressBar);
        resultCard = findViewById(R.id.cardResult);
        resultText = findViewById(R.id.tvInterviewQuestions);
        loginInfo = getSharedPreferences("loginInfo", MODE_PRIVATE);
        loginInfo.registerOnSharedPreferenceChangeListener(loginListener);
        hamburger = new Hamburger(this);
        new TopHeader(this);
        new BottomMenu(this);
        FloatingChatbotHelper.init(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        generateButton.setOnClickListener(v -> generate());
        if (savedInstanceState != null && loggedIn()) {
            CharSequence previousResult = savedInstanceState.getCharSequence("interviewResult");
            if (previousResult != null && previousResult.length() > 0) {
                resultText.setText(previousResult);
                resultCard.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean loggedIn() {
        return loginInfo.getBoolean("isLoggedIn", false);
    }

    private void generate() {
        if (!purchaseVerified) return;
        if (!loggedIn()) {
            showMessage("로그인 후 이용해주세요");
            return;
        }
        if (pendingCall != null) return;
        if (!validate(jobRole, 100, "지원 직무")
                || !validate(careerLevel, 80, "경력 수준")
                || !validate(competencies, 8000, "채용공고·필요 역량")
                || !validate(coverLetter, 12000, "자기소개서·주요 경험")) return;
        if (value(company).length() > 300) {
            company.setError("지원 회사·산업은 300자 이내로 입력해주세요.");
            company.requestFocus();
            return;
        }
        InterviewQuestionRequest request = new InterviewQuestionRequest(value(jobRole),
                value(careerLevel), value(competencies), value(coverLetter), value(company));
        resultCard.setVisibility(View.GONE);
        resultText.setText("");
        setLoading(true);
        pendingCall = ApiClient.getClient().create(ApiService.class)
                .generateInterviewQuestions(JobtoolNavigation.authorization(this), request);
        pendingCall.timeout().timeout(110, TimeUnit.SECONDS);
        pendingCall.enqueue(new Callback<InterviewQuestionResponse>() {
            @Override
            public void onResponse(Call<InterviewQuestionResponse> call, Response<InterviewQuestionResponse> response) {
                if (!canHandle(call)) return;
                pendingCall = null;
                setLoading(false);
                if (response.code() == 401 || response.code() == 403) {
                        JobtoolNavigation.invalidatePurchase();
                    verifyPurchase();
                    return;
                }
                if (!loggedIn()) {
                    showMessage("로그인 후 이용해주세요");
                    return;
                }
                InterviewQuestionResponse body = response.body();
                if (response.isSuccessful() && body != null && body.questions != null
                        && !body.questions.isEmpty() && body.questions.size() <= 20) {
                    renderResult(body);
                } else {
                    String message = "질문을 생성하지 못했어요. 잠시 후 다시 시도해주세요.";
                    try {
                        if (response.errorBody() != null) {
                            message = new JSONObject(response.errorBody().string()).optString("error", message);
                        }
                    } catch (Exception ignored) { }
                    showMessage(message);
                }
            }

            @Override
            public void onFailure(Call<InterviewQuestionResponse> call, Throwable error) {
                if (!canHandle(call)) return;
                pendingCall = null;
                setLoading(false);
                showMessage("서버 연결이 끊겼거나 응답 시간이 초과됐어요. 다시 시도해주세요.");
            }
        });
    }

    private boolean canHandle(Call<InterviewQuestionResponse> call) {
        return pendingCall == call && !call.isCanceled() && !isFinishing() && !isDestroyed();
    }

    private boolean validate(EditText input, int limit, String label) {
        String text = value(input);
        if (text.isEmpty() || text.length() > limit) {
            input.setError(text.isEmpty() ? label + "을(를) 입력해주세요."
                    : label + "은(는) " + limit + "자 이내로 입력해주세요.");
            input.requestFocus();
            return false;
        }
        input.setError(null);
        return true;
    }

    private static String value(EditText input) {
        return input.getText().toString().trim();
    }

    private void renderResult(InterviewQuestionResponse response) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        int index = 1;
        for (InterviewQuestionResponse.Question question : response.questions) {
            text.append(String.valueOf(index++)).append(". [").append(categoryName(question.category))
                    .append("] ").append(question.question == null ? "" : question.question).append("\n");
            text.append("\n");
        }
        resultText.setText(text);
        resultCard.setVisibility(View.VISIBLE);
        resultCard.post(() -> resultCard.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, resultCard.getWidth(), 120), false));
    }

    private String categoryName(String key) {
        if ("role".equals(key)) return "직무";
        if ("competency".equals(key)) return "역량";
        if ("experience".equals(key)) return "경험";
        return "질문";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        generateButton.setEnabled(purchaseVerified && !loading);
        generateButton.setText(loading ? "자료를 찾아 질문을 생성하고 있어요…" : "AI 면접 질문 생성하기");
        for (EditText input : new EditText[]{jobRole, careerLevel, competencies, coverLetter, company}) {
            input.setEnabled(purchaseVerified && !loading);
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void clearSessionResult() {
        purchaseVerified = false;
        if (pendingCall != null) pendingCall.cancel();
        pendingCall = null;
        setLoading(false);
        resultText.setText("");
        resultCard.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) hamburger.updateLoginState();
        if (loginInfo != null && !loggedIn()) clearSessionResult();
        verifyPurchase();
    }

    private void verifyPurchase() {
        purchaseVerified = false;
        setLoading(pendingCall != null);
        JobtoolNavigation.checkAccess(this, () -> {
            purchaseVerified = true;
            setLoading(pendingCall != null);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (loggedIn()) outState.putCharSequence("interviewResult", resultText.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingCall != null) pendingCall.cancel();
        loginInfo.unregisterOnSharedPreferenceChangeListener(loginListener);
        super.onDestroy();
    }
}
