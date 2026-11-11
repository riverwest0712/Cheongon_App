package com.example.cheongon.recommendation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtSort;
    private TextView txtCount;
    private RecyclerView recyclerPolicy;

    private PolicyAdapter policyAdapter;
    private TextView categoryAll;
    private TextView categoryEducation;
    private TextView categoryFinance;
    private TextView categoryHousing;
    private TextView categoryJob;
    private TextView categoryParticipation;

    private final List<Recommendation> recommendationList =
            new ArrayList<>();

    private final List<Recommendation> allRecommendationList =
            new ArrayList<>();

    private String selectedCategory = "전체";

    private final List<Bookmark> bookmarkList =
            new ArrayList<>();

    private SharedPreferences loginPreferences;

    private Hamburger hamburger;

    private int userId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        // 공통 메뉴
        hamburger = new Hamburger(this);
        new TopHeader(this);
        new BottomMenu(this);

        // 뷰 연결
        btnBack = findViewById(R.id.btnBack);
        txtSort = findViewById(R.id.txtSort);
        txtCount = findViewById(R.id.txtCount);
        recyclerPolicy = findViewById(R.id.recyclerPolicy);

        // 정책 카테고리
        categoryAll = findViewById(R.id.categoryAll);
        categoryEducation = findViewById(R.id.categoryEducation);
        categoryFinance = findViewById(R.id.categoryFinance);
        categoryHousing = findViewById(R.id.categoryHousing);
        categoryJob = findViewById(R.id.categoryJob);
        categoryParticipation = findViewById(R.id.categoryParticipation);

        setupCategoryListeners();

        // 로그인 정보
        loginPreferences =
                getSharedPreferences("loginInfo", MODE_PRIVATE);

        userId = loginPreferences.getInt("id", -1);

        // RecyclerView 설정
        recyclerPolicy.setLayoutManager(
                new LinearLayoutManager(this)
        );

        policyAdapter = new PolicyAdapter(
                recommendationList,
                bookmarkList,
                userId
        );

        recyclerPolicy.setAdapter(policyAdapter);

        // 아이템 클릭 시 정책 상세 화면으로 이동
        policyAdapter.setOnItemClickListener(recommendation -> {

            Intent intent = new Intent(
                    RecommendationActivity.this,
                    PolicyDetailActivity.class
            );

            // 숫자를 문자열로 안전하게 변환해서 전달
            intent.putExtra(
                    "policy_id",
                    String.valueOf(recommendation.getId())
            );

            startActivity(intent);
        });

        // 뒤로가기
        btnBack.setOnClickListener(view -> finish());

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 정렬
        txtSort.setOnClickListener(view ->
                showSortDialog()
        );

        if (userId == -1) {

            Snackbar.make(
                    recyclerPolicy,
                    "로그인 정보가 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        // 추천 결과 조회
        loadRecommendations();

        // 현재 찜 상태 조회
        loadBookmarks();
    }

    // 추천 결과 조회
    private void loadRecommendations() {

        ApiService apiService =
                ApiClient.getClient().create(ApiService.class);

        apiService.getRecommendations(userId)
                .enqueue(new Callback<List<Recommendation>>() {

                    @Override
                    public void onResponse(
                            Call<List<Recommendation>> call,
                            Response<List<Recommendation>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            allRecommendationList.clear();

                            allRecommendationList.addAll(
                                    response.body()
                            );

                            recommendationList.clear();

                            recommendationList.addAll(
                                    allRecommendationList
                            );

                            sortScoreHigh();
                            updateCount();

                            txtSort.setText("점수 높은 순 ▼");

                        } else {

                            Snackbar.make(
                                    recyclerPolicy,
                                    "추천 정책을 불러오지 못했습니다.",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Recommendation>> call,
                            Throwable t
                    ) {

                        Snackbar.make(
                                recyclerPolicy,
                                "추천 정책 서버 연결 실패",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // 현재 찜 목록 조회
    private void loadBookmarks() {

        ApiService apiService =
                ApiClient.getClient().create(ApiService.class);

        apiService.getBookmarks(userId)
                .enqueue(new Callback<List<Bookmark>>() {

                    @Override
                    public void onResponse(
                            Call<List<Bookmark>> call,
                            Response<List<Bookmark>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            bookmarkList.clear();

                            bookmarkList.addAll(
                                    response.body()
                            );

                            policyAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Bookmark>> call,
                            Throwable t
                    ) {
                        // 찜 조회 실패 시 추천 목록은 그대로 표시
                    }
                });
    }
    // 정책 카테고리 선택
    private void setupCategoryListeners() {

        categoryAll.setOnClickListener(v ->
                selectCategory("전체")
        );

        categoryEducation.setOnClickListener(v ->
                selectCategory("교육･직업훈련")
        );

        categoryFinance.setOnClickListener(v ->
                selectCategory("금융･복지･문화")
        );

        categoryHousing.setOnClickListener(v ->
                selectCategory("주거지원")
        );

        categoryJob.setOnClickListener(v ->
                selectCategory("일자리")
        );

        categoryParticipation.setOnClickListener(v ->
                selectCategory("참여･권리")
        );
    }
    private void selectCategory(String category) {

        selectedCategory = category;

        recommendationList.clear();

        if (category.equals("전체")) {

            recommendationList.addAll(
                    allRecommendationList
            );

        } else {

            for (Recommendation policy :
                    allRecommendationList) {

                if (category.equals(
                        policy.getCategory()
                )) {

                    recommendationList.add(policy);
                }
            }
        }

        updateCount();

        policyAdapter.notifyDataSetChanged();

        updateCategoryStyle();
    }
    private void updateCategoryStyle() {

        TextView[] categories = {
                categoryAll,
                categoryEducation,
                categoryFinance,
                categoryHousing,
                categoryJob,
                categoryParticipation
        };

        String[] names = {
                "전체",
                "교육･직업훈련",
                "금융･복지･문화",
                "주거지원",
                "일자리",
                "참여·권리"
        };

        for (int i = 0; i < categories.length; i++) {

            if (names[i].equals(selectedCategory)) {

                categories[i].setBackgroundResource(
                        R.drawable.bg_category2_selected
                );

                categories[i].setTextColor(
                        Color.WHITE
                );

            } else {

                categories[i].setBackgroundResource(
                        R.drawable.bg_category2
                );

                categories[i].setTextColor(
                        Color.parseColor("#4E5968")
                );
            }
        }
    }

    // 정렬 선택창
    private void showSortDialog() {

        // 전체 메뉴 레이아웃
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setPadding(
                0,
                dpToPx(4),
                0,
                dpToPx(4)
        );

        // 흰색 배경
        layout.setBackgroundColor(Color.WHITE);

        // 메뉴 항목
        TextView scoreHigh = createSortItem("점수 높은 순");
        TextView scoreLow = createSortItem("점수 낮은 순");
        TextView title = createSortItem("가나다순");

        layout.addView(scoreHigh);
        layout.addView(scoreLow);
        layout.addView(title);

        // PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                layout,
                dpToPx(140),
                dpToPx(144),
                true
        );

        // 흰색 직사각형 배경
        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.WHITE)
        );

        // 그림자
        popupWindow.setElevation(
                dpToPx(8)
        );

        // 점수 높은 순
        scoreHigh.setOnClickListener(v -> {

            sortScoreHigh();

            txtSort.setText("점수 높은 순 ▼");

            popupWindow.dismiss();
        });

        // 점수 낮은 순
        scoreLow.setOnClickListener(v -> {

            sortScoreLow();

            txtSort.setText("점수 낮은 순 ▼");

            popupWindow.dismiss();
        });

        // 가나다순
        title.setOnClickListener(v -> {

            sortTitle();

            txtSort.setText("가나다순 ▼");

            popupWindow.dismiss();
        });

        // 정렬 버튼 아래에 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(40),
                dpToPx(4)
        );
    }

    // 정렬 메뉴 항목 생성
    private TextView createSortItem(String text) {

        TextView item = new TextView(this);

        item.setText(text);
        item.setTextSize(14);
        item.setTextColor(Color.DKGRAY);

        item.setGravity(
                Gravity.CENTER_VERTICAL
        );

        item.setPadding(
                dpToPx(16),
                0,
                dpToPx(16),
                0
        );

        item.setBackgroundResource(
                android.R.drawable.list_selector_background
        );

        item.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(44)
                )
        );

        return item;
    }

    // dp → px
    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // 점수 높은 순
    private void sortScoreHigh() {

        recommendationList.sort(
                (a, b) ->
                        Double.compare(
                                b.getScore(),
                                a.getScore()
                        )
        );

        policyAdapter.notifyDataSetChanged();
    }

    // 점수 낮은 순
    private void sortScoreLow() {

        recommendationList.sort(
                (a, b) ->
                        Double.compare(
                                a.getScore(),
                                b.getScore()
                        )
        );

        policyAdapter.notifyDataSetChanged();
    }

    // 가나다순
    private void sortTitle() {

        recommendationList.sort(
                Comparator.comparing(
                        Recommendation::getTitle,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
        );

        policyAdapter.notifyDataSetChanged();
    }

    // 추천 정책 개수 표시
    private void updateCount() {

        txtCount.setText(
                "추천 정책 목록 "
                        + recommendationList.size()
                        + "개"
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        // 북마크 화면에서 찜 해제 후 돌아왔을 때 상태 갱신
        if (userId != -1 && policyAdapter != null) {
            loadBookmarks();
        }
    }
}