package com.example.cheongon.mypage;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.TopHeader;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.recommendation.PolicyAdapter;
import com.example.cheongon.recommendation.Recommendation;
import com.example.cheongon.recommendation.PolicyDeadline;
import com.example.cheongon.recommendation.PolicyDetailActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarActivity extends AppCompatActivity {

    private Hamburger hamburgerMenu;

    private PolicyMonthView calendarView;
    private TextView txtSelectedDate;
    private TextView txtPolicyCount;
    private RecyclerView recyclerCalendarPolicy;

    private ApiService apiService;

    // 전체 추천 정책
    private final List<Recommendation> recommendationList =
            new ArrayList<>();

    // 선택한 날짜의 정책
    private final List<Recommendation> selectedPolicyList =
            new ArrayList<>();

    // 기존 추천 정책 Adapter 재사용
    private PolicyAdapter policyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        // 상단 헤더
        new TopHeader(this);

        // 햄버거 메뉴
        hamburgerMenu = new Hamburger(this);

        // 하단 메뉴
        new BottomMenu(this);

        // 뷰 연결
        ImageView btnBack =
                findViewById(R.id.btnBack);

        calendarView =
                findViewById(R.id.calendarView);
        if (savedInstanceState != null) {
            calendarView.setDate(savedInstanceState.getLong("selectedDate", calendarView.getDate()));
        }

        txtSelectedDate =
                findViewById(R.id.txtSelectedDate);

        txtPolicyCount =
                findViewById(R.id.txtPolicyCount);

        recyclerCalendarPolicy =
                findViewById(R.id.recyclerCalendarPolicy);

        // RecyclerView 설정
        recyclerCalendarPolicy.setLayoutManager(
                new LinearLayoutManager(this)
        );

        policyAdapter =
                new PolicyAdapter(selectedPolicyList);

        recyclerCalendarPolicy.setAdapter(policyAdapter);
        policyAdapter.setOnItemClickListener(policy -> {
            Intent intent = new Intent(this, PolicyDetailActivity.class);
            intent.putExtra("policy_id", policy.getId());
            startActivity(intent);
        });

        // Retrofit
        apiService = ApiClient
                .getClient()
                .create(ApiService.class);

        // 뒤로가기
        btnBack.setOnClickListener(view -> finish());

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 로그인 사용자 ID
        SharedPreferences prefs =
                getSharedPreferences(
                        "loginInfo",
                        MODE_PRIVATE
                );

        int usersId =
                prefs.getInt("id", -1);

        if (usersId == -1) {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인이 필요합니다.",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        // 추천 정책 불러오기
        recommendationList.addAll(PolicyCalendarStore.load(this, usersId));
        refreshSelectedDate();
        loadRecommendations(usersId);

        // 날짜 선택
        calendarView.setOnDateChangeListener(
                new PolicyMonthView.OnDateChangeListener() {

                    @Override
                    public void onSelectedDayChange(
                            @NonNull PolicyMonthView view,
                            int year,
                            int month,
                            int dayOfMonth
                    ) {

                        int realMonth = month + 1;

                        txtSelectedDate.setText(
                                String.format(
                                        Locale.KOREA,
                                        "%d월 %d일 정책",
                                        realMonth,
                                        dayOfMonth
                                )
                        );

                        filterPoliciesByDate(
                                year,
                                realMonth,
                                dayOfMonth
                        );
                    }
                }
        );
    }

    // 추천 정책 조회
    private void loadRecommendations(int usersId) {

        apiService
                .getRecommendations(usersId)
                .enqueue(new Callback<List<Recommendation>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<Recommendation>> call,
                            @NonNull Response<List<Recommendation>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            recommendationList.clear();

                            recommendationList.addAll(
                                    response.body()
                            );
                            mergeSavedPolicies(usersId);

                            // 오늘 날짜
                            Calendar today =
                                    Calendar.getInstance();
                            today.setTimeInMillis(calendarView.getDate());

                            int year =
                                    today.get(Calendar.YEAR);

                            int month =
                                    today.get(Calendar.MONTH) + 1;

                            int day =
                                    today.get(Calendar.DAY_OF_MONTH);

                            txtSelectedDate.setText(
                                    String.format(
                                            Locale.KOREA,
                                            "%d월 %d일 정책",
                                            month,
                                            day
                                    )
                            );

                            // 오늘 마감 정책 표시
                            filterPoliciesByDate(
                                    year,
                                    month,
                                    day
                            );

                        } else {

                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "추천 정책을 불러오지 못했습니다.",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Recommendation>> call,
                            @NonNull Throwable t
                    ) {

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "서버 연결에 실패했습니다.",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // 선택 날짜의 정책만 필터링
    private void filterPoliciesByDate(
            int year,
            int month,
            int day
    ) {

        java.util.Set<String> dates = new java.util.HashSet<>();
        for (Recommendation policy : recommendationList) {
            String deadline = PolicyDeadline.date(policy.getEnd_date());
            if (deadline != null) dates.add(deadline);
        }
        calendarView.setPolicyDates(dates);
        selectedPolicyList.clear();

        String selectedDate =
                String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month,
                        day
                );

        for (Recommendation policy
                : recommendationList) {

            String endDate = PolicyDeadline.date(policy.getEnd_date());

            if (endDate == null) {
                continue;
            }

            // 서버 날짜에 시간이 포함된 경우
            if (endDate.length() >= 10) {
                endDate =
                        endDate.substring(0, 10);
            }

            // 선택 날짜와 마감일 비교
            if (selectedDate.equals(endDate)) {

                selectedPolicyList.add(policy);
            }
        }

        // 정책 개수
        txtPolicyCount.setText(
                selectedPolicyList.size() + "개"
        );

        // RecyclerView 갱신
        policyAdapter.notifyDataSetChanged();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putLong("selectedDate", calendarView.getDate());
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        int userId = getSharedPreferences("loginInfo", MODE_PRIVATE).getInt("id", -1);
        if (userId > 0 && policyAdapter != null) {
            mergeSavedPolicies(userId);
            refreshSelectedDate();
            policyAdapter.notifyDataSetChanged();
        }
    }

    private void mergeSavedPolicies(int userId) {
        for (Recommendation saved : PolicyCalendarStore.load(this, userId)) {
            if (saved == null || saved.getId() == null) continue;
            boolean found = false;
            for (Recommendation policy : recommendationList) {
                if (saved.getId().equals(policy.getId())) { found = true; break; }
            }
            if (!found) recommendationList.add(saved);
        }
    }

    private void refreshSelectedDate() {
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(calendarView.getDate());
        int month = selected.get(Calendar.MONTH) + 1;
        int day = selected.get(Calendar.DAY_OF_MONTH);
        txtSelectedDate.setText(String.format(Locale.KOREA, "%d월 %d일 정책", month, day));
        filterPoliciesByDate(selected.get(Calendar.YEAR), month, day);
    }

}
