package com.example.cheongon.recommendation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PolicyDetailActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView tvCategory;
    private TextView tvPolicyTitle;
    private TextView tvBenefitAmount;
    private TextView tvOrganization;
    private TextView tvApplyPeriod;
    private TextView tvApplyLink;

    private TextView tvSupportContent;
    private TextView tvDescription;
    private TextView tvAgeRange;
    private TextView tvIncomeCondition;
    private TextView tvEmploymentCondition;
    private TextView tvRegion;
    private TextView tvApplyCondition;
    private TextView tvApplicationMethod;

    private LinearLayout layoutBenefit;

    private LinearLayout layoutLike;
    private ImageView imgLike;
    private TextView txtLikeCount;

    private boolean isBookmarked = false;
    private int usersId = -1;

    private String policyId;
    private Hamburger hamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy_detail);

        hamburger = new Hamburger(this);
        new com.example.cheongon.main.TopHeader(this);
        new BottomMenu(this);
        FloatingChatbotHelper.init(this);

        usersId = getSharedPreferences("loginInfo", MODE_PRIVATE)
                .getInt("id", -1);

        policyId = getIntent().getStringExtra("policy_id");

        if (policyId == null) {
            policyId = getIntent().getStringExtra("POLICY_ID");
        }

        if (policyId == null) {
            int intId = getIntent().getIntExtra("policy_id", -1);
            if (intId != -1) {
                policyId = String.valueOf(intId);
            }
        }

        initViews();
        setupListeners();

        if (policyId != null && !policyId.equals("-1")) {
            loadPolicyData();
            loadBookmarkStatus();
        } else {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "정책 정보를 찾을 수 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        tvCategory = findViewById(R.id.tvCategory);
        tvPolicyTitle = findViewById(R.id.tvPolicyTitle);
        tvBenefitAmount = findViewById(R.id.tvBenefitAmount);
        tvOrganization = findViewById(R.id.tvOrganization);
        tvApplyPeriod = findViewById(R.id.tvApplyPeriod);
        tvApplyLink = findViewById(R.id.tvApplyLink);

        tvSupportContent = findViewById(R.id.tvSupportContent);
        tvDescription = findViewById(R.id.tvDescription);

        tvAgeRange = findViewById(R.id.tvAgeRange);
        tvIncomeCondition = findViewById(R.id.tvIncomeCondition);
        tvEmploymentCondition = findViewById(R.id.tvEmploymentCondition);
        tvRegion = findViewById(R.id.tvRegion);
        tvApplyCondition = findViewById(R.id.tvApplyCondition);

        tvApplicationMethod = findViewById(R.id.tvApplicationMethod);
        layoutBenefit = findViewById(R.id.layoutBenefit);

        layoutLike = findViewById(R.id.layoutLike);
        imgLike = findViewById(R.id.imgLike);
        txtLikeCount = findViewById(R.id.txtLikeCount);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> finish());
        }

        if (layoutLike != null) {
            layoutLike.setOnClickListener(v -> toggleBookmark());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        /*
         * 중요:
         * 목록 화면에서 하트를 누른 뒤 상세 화면으로 들어오거나,
         * 다른 화면에서 돌아왔을 때 상세 하트 아이콘을 최신 상태로 다시 맞춘다.
         */
        if (policyId != null && !policyId.equals("-1")) {
            loadBookmarkStatus();
        }
    }

    private void loadPolicyData() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<Map<String, Object>> call =
                apiService.getPolicyDetail(policyId, usersId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resBody = response.body();

                    Map<String, Object> policy =
                            (Map<String, Object>) resBody.get("policy");

                    if (policy != null) {
                        bindPolicyData(policy);

                        if (resBody.get("bookmarkCount") != null) {
                            isBookmarked = parseBoolean(resBody.get("bookmarked"));
                            int count = parseInt(resBody.get("bookmarkCount"));
                            updateLikeUI(count);
                        }
                    }

                } else {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "정책 정보를 불러오지 못했습니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "서버 통신 실패: " + t.getMessage(),
                        Snackbar.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void bindPolicyData(Map<String, Object> policy) {
        String title = getStringValue(policy, "policy_name");

        if (title.isEmpty()) {
            title = getStringValue(policy, "title");
        }

        com.example.cheongon.main.RecentPolicies.record(
                PolicyDetailActivity.this,
                policyId,
                title
        );

        tvPolicyTitle.setText(!title.isEmpty() ? title : "-");

        String category = getStringValue(policy, "category");
        String categorySub = getStringValue(policy, "category_sub");

        String categoryText = !category.isEmpty() ? category : "청년정책";

        if (!categorySub.isEmpty()) {
            categoryText += " > " + categorySub;
        }

        tvCategory.setText(categoryText);

        String benefit = getStringValue(policy, "benefit_amount");

        if (!benefit.isEmpty() && !benefit.equals("-")) {
            layoutBenefit.setVisibility(View.VISIBLE);
            tvBenefitAmount.setText(benefit);
        } else {
            layoutBenefit.setVisibility(View.GONE);
        }

        String org = getStringValue(policy, "organization");
        tvOrganization.setText("지원 기관: " + (!org.isEmpty() ? org : "-"));

        String startDate = getStringValue(policy, "start_date");
        String endDate = getStringValue(policy, "end_date");
        String period = getStringValue(policy, "period");

        if (!period.isEmpty() && !period.equals("-")) {
            tvApplyPeriod.setText("신청 기간: " + period);
        } else if (!startDate.isEmpty() || !endDate.isEmpty()) {
            String periodStr =
                    (!startDate.isEmpty() ? startDate : "") +
                            " ~ " +
                            (!endDate.isEmpty() ? endDate : "상시");
            tvApplyPeriod.setText("신청 기간: " + periodStr);
        } else {
            tvApplyPeriod.setText("신청 기간: 상시 신청");
        }

        String support = getStringValue(policy, "support_content");
        tvSupportContent.setText(
                !support.isEmpty() ? support : "지원 내용 정보가 없습니다."
        );

        String desc = getStringValue(policy, "description");
        tvDescription.setText(
                !desc.isEmpty() ? desc : "상세 개요가 없습니다."
        );

        Object minAge = policy.get("min_age");
        Object maxAge = policy.get("max_age");

        if (minAge != null || maxAge != null) {
            tvAgeRange.setText(
                    "• 연령 조건: " +
                            (minAge != null ? parseInt(minAge) : 0) +
                            "세 ~ " +
                            (maxAge != null ? parseInt(maxAge) : "제한없음") +
                            "세"
            );
        } else {
            tvAgeRange.setText("• 연령 조건: 제한 없음");
        }

        String income = getStringValue(policy, "income_condition");

        if (income.isEmpty()) {
            Object minInc = policy.get("min_income");
            Object maxInc = policy.get("max_income");

            if (minInc != null || maxInc != null) {
                income = "소득 기준 충족자";
            }
        }

        tvIncomeCondition.setText(
                "• 소득 조건: " + (!income.isEmpty() ? income : "제한 없음")
        );

        String employment = getStringValue(policy, "employment_condition");

        tvEmploymentCondition.setText(
                "• 취업 상태: " +
                        (!employment.isEmpty() ? employment : "제한 없음")
        );

        String region = getStringValue(policy, "region");

        tvRegion.setText(
                "• 거주 지역: " + (!region.isEmpty() ? region : "전국")
        );

        String applyCondition = getStringValue(policy, "apply_condition");

        tvApplyCondition.setText(
                !applyCondition.isEmpty()
                        ? applyCondition
                        : "추가 신청 자격 요건이 없습니다."
        );

        String appMethod = getStringValue(policy, "application_method");

        tvApplicationMethod.setText(
                !appMethod.isEmpty() ? appMethod : "온라인 또는 방문 접수"
        );

        String applyUrl = getStringValue(policy, "apply_url");

        if (!applyUrl.isEmpty()) {
            if (!applyUrl.startsWith("http://")
                    && !applyUrl.startsWith("https://")) {
                applyUrl = "https://" + applyUrl;
            }

            final String finalUrl = applyUrl;

            tvApplyLink.setText("신청 링크: 바로가기 →");

            tvApplyLink.setOnClickListener(v -> {
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(finalUrl)
                );
                startActivity(intent);
            });

        } else {
            tvApplyLink.setText("신청 링크: 링크 없음");
            tvApplyLink.setOnClickListener(null);
        }
    }

    private void loadBookmarkStatus() {
        if (policyId == null || policyId.equals("-1")) {
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getBookmarkStatus(policyId, usersId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> body = response.body();

                            isBookmarked = parseBoolean(body.get("bookmarked"));
                            int count = parseInt(body.get("count"));

                            updateLikeUI(count);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t
                    ) {
                        // 실패해도 상세 화면 자체는 유지
                    }
                });
    }

    private void toggleBookmark() {
        if (usersId <= 0) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인이 필요합니다.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        if (policyId == null || policyId.equals("-1")) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "정책 정보를 찾을 수 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, Object> req = new HashMap<>();
        req.put("users_id", usersId);
        req.put("policy_id", policyId);

        apiService.toggleBookmark(req)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> body = response.body();

                            isBookmarked = parseBoolean(body.get("bookmarked"));
                            int count = parseInt(body.get("count"));

                            updateLikeUI(count);

                        } else {
                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "찜 처리에 실패했습니다.",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t
                    ) {
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "통신 실패: " + t.getMessage(),
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void updateLikeUI(int count) {
        if (imgLike != null) {
            imgLike.setImageResource(
                    isBookmarked
                            ? R.drawable.ic_favorite
                            : R.drawable.ic_favorite_border
            );
        }

        if (txtLikeCount != null) {
            txtLikeCount.setText(String.valueOf(count));
        }
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.equalsIgnoreCase("true") || str.equals("1");
        }

        return false;
    }

    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
    }
}
