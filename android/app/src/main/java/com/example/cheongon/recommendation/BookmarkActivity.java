package com.example.cheongon.recommendation;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookmarkActivity extends AppCompatActivity {

    private RecyclerView recyclerBookmark;

    private PolicyAdapter policyAdapter;

    // DB에서 가져온 찜 목록
    private final List<Bookmark> bookmarkList =
            new ArrayList<>();

    // 전체 찜 정책
    private final List<Recommendation> allBookmarkedPolicyList =
            new ArrayList<>();

    // 현재 화면에 표시할 정책
    private final List<Recommendation> bookmarkedPolicyList =
            new ArrayList<>();

    private String selectedCategory = "전체";

    private TextView txtCount;
    private TextView txtSort;
    // 카테고리
    private TextView categoryAll;
    private TextView categoryEducation;
    private TextView categoryFinance;
    private TextView categoryHousing;
    private TextView categoryJob;
    private TextView categoryParticipation;

    private int usersId = -1;

    private Hamburger hamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        usersId = getSharedPreferences("loginInfo", MODE_PRIVATE)
                .getInt("id", -1);

        if (usersId <= 0) {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보가 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        new TopHeader(this);
        new BottomMenu(this);
        FloatingChatbotHelper.init(this);

        hamburger = new Hamburger(this);

        initViews();
        setupRecyclerView();
        setupListeners();

        updateCount();

        loadBookmarks();
    }

    private void initViews() {

        recyclerBookmark =
                findViewById(R.id.recyclerBookmark);

        txtCount =
                findViewById(R.id.txtCount);

        txtSort =
                findViewById(R.id.txtSort);

        categoryAll =
                findViewById(R.id.categoryAll);

        categoryEducation =
                findViewById(R.id.categoryEducation);

        categoryFinance =
                findViewById(R.id.categoryFinance);

        categoryHousing =
                findViewById(R.id.categoryHousing);

        categoryJob =
                findViewById(R.id.categoryJob);

        categoryParticipation =
                findViewById(R.id.categoryParticipation);
    }

    private void setupRecyclerView() {

        recyclerBookmark.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // 찜 화면 모드
        policyAdapter =
                new PolicyAdapter(
                        bookmarkedPolicyList,
                        bookmarkList,
                        usersId,
                        true,
                        this::updateCount
                );
        // 찜 목록에서 정책 카드를 눌렀을 때 상세 페이지 이동
        policyAdapter.setOnItemClickListener(policy -> {
            android.content.Intent intent = new android.content.Intent(BookmarkActivity.this, PolicyDetailActivity.class);
            intent.putExtra("policy_id", policy.getId()); // 정책 ID 전달
            startActivity(intent);
        });

        recyclerBookmark.setAdapter(
                policyAdapter
        );
    }

    private void setupListeners() {

        findViewById(R.id.btnBack)
                .setOnClickListener(view -> finish());

        txtSort.setOnClickListener(view ->
                showSortMenu()
        );

        // 카테고리
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

        updateCategoryStyle();
    }

    // 찜 목록 조회
    private void loadBookmarks() {

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.getBookmarks(usersId)
                .enqueue(
                        new Callback<List<Bookmark>>() {

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

                                    Log.d(
                                            "BOOKMARK",
                                            "찜 목록 개수: "
                                                    + bookmarkList.size()
                                    );

                                    // bookmarkList(진짜 찜한 정책)를 그대로 화면에 표시
                                    buildBookmarkedList();

                                } else {

                                    Log.e(
                                            "BOOKMARK",
                                            "찜 목록 조회 실패: "
                                                    + response.code()
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Bookmark>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        "BOOKMARK",
                                        "찜 목록 서버 연결 실패",
                                        t
                                );
                            }
                        }
                );
    }

    // bookmarkList(진짜 찜한 정책)를 화면용 Recommendation 리스트로 변환
    private void buildBookmarkedList() {

        allBookmarkedPolicyList.clear();

        for (Bookmark bookmark : bookmarkList) {

            Recommendation policy =
                    new Recommendation(
                            bookmark.getPolicy_id(),
                            bookmark.getTitle(),
                            bookmark.getDescription(),
                            bookmark.getRegion(),
                            bookmark.getCategory(),
                            bookmark.getEnd_date(),
                            bookmark.getApply_url(),
                            bookmark.getScore()
                    );

            allBookmarkedPolicyList.add(policy);
        }

        applyBookmarkCategoryFilter();


        policyAdapter.notifyDataSetChanged();


        Log.d(
                "BOOKMARK",
                "최종 찜 정책 개수: "
                        + bookmarkedPolicyList.size()
        );
    }

    // ~~추가~~ : 누락되어 있던 개수 텍스트 갱신 메서드 복구
    private void updateCount() { // ~~추가~~

        txtCount.setText(
                "찜한 정책 목록 "
                        + bookmarkedPolicyList.size()
                        + "개"
        );
    }
    // ~~추가 끝~~

    // 정책 카테고리
    private void selectCategory(String category) {

        selectedCategory = category;

        applyBookmarkCategoryFilter();

        updateCategoryStyle();

        policyAdapter.notifyDataSetChanged();
    }
    private void applyBookmarkCategoryFilter() {

        bookmarkedPolicyList.clear();

        if (selectedCategory.equals("전체")) {

            bookmarkedPolicyList.addAll(
                    allBookmarkedPolicyList
            );

        } else {

            for (Recommendation policy :
                    allBookmarkedPolicyList) {

                if (selectedCategory.equals(
                        policy.getCategory()
                )) {

                    bookmarkedPolicyList.add(policy);
                }
            }
        }

        updateCount();
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

    // 정렬 메뉴
    private void showSortMenu() {

        // 메뉴 전체 레이아웃
        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                0,
                dpToPx(4),
                0,
                dpToPx(4)
        );

        // 흰색 배경
        layout.setBackgroundColor(
                Color.WHITE
        );

        // 메뉴 항목
        TextView scoreHigh =
                createSortItem("점수 높은 순");

        TextView scoreLow =
                createSortItem("점수 낮은 순");

        TextView title =
                createSortItem("가나다순");

        layout.addView(scoreHigh);
        layout.addView(scoreLow);
        layout.addView(title);

        // PopupWindow
        final PopupWindow popupWindow =
                new PopupWindow(
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

            txtSort.setText(
                    "점수 높은 순 ▼"
            );

            popupWindow.dismiss();
        });

        // 점수 낮은 순
        scoreLow.setOnClickListener(v -> {

            sortScoreLow();

            txtSort.setText(
                    "점수 낮은 순 ▼"
            );

            popupWindow.dismiss();
        });

        // 가나다순
        title.setOnClickListener(v -> {

            sortTitle();

            txtSort.setText(
                    "가나다순 ▼"
            );

            popupWindow.dismiss();
        });

        // 정렬 버튼 아래에 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(90),
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

    // dp → px 변환
    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // 점수 높은 순
    private void sortScoreHigh() {

        Collections.sort(
                bookmarkedPolicyList,
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

        Collections.sort(
                bookmarkedPolicyList,
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

        Collections.sort(
                bookmarkedPolicyList,
                (a, b) -> {

                    String titleA =
                            a.getTitle() == null
                                    ? ""
                                    : a.getTitle();

                    String titleB =
                            b.getTitle() == null
                                    ? ""
                                    : b.getTitle();

                    return titleA.compareTo(
                            titleB
                    );
                }
        );

        policyAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        // 다른 화면(상세페이지 등)에서 찜을 해제/추가하고 돌아왔을 때
        // 찜 목록이 최신 상태로 보이도록 재조회
        loadBookmarks();
    }
}
