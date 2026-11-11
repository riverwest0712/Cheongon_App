package com.example.cheongon.main;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cheongon.R;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.mypage.ChatbotActivity;
import com.example.cheongon.login.LoginActivity;
import com.example.cheongon.recommendation.*;
import com.example.cheongon.community.*;
import java.util.*;
import java.text.SimpleDateFormat;
import retrofit2.*;
import org.json.*;

public class Main extends AppCompatActivity {
    private Hamburger hamburgerMenu;
    private final List<Call<?>> requests = new ArrayList<>();
    private int generation;
    private interface Loaded<T> { void accept(T value); }

    // 홈 화면(로고 등)을 다시 눌러서 진입해도 접힘/펼침 상태를 유지하기 위한 static 변수 (기본값: true - 펼침)
    private static boolean recentExpanded = true;
    private static boolean recommendationExpanded = true;
    private static boolean deadlineExpanded = true;
    private static boolean communityExpanded = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        // 저장된 상태나 static 기억 값을 활용하여 섹션 초기화
        setupSection(R.id.recentHeader, R.id.recentContent, R.id.recentArrow, "최근 기록", state, recentExpanded, 1);
        setupSection(R.id.recommendationHeader, R.id.recommendationContent, R.id.recommendationArrow, "오늘의 추천 정책", state, recommendationExpanded, 2);
        setupSection(R.id.deadlineHeader, R.id.deadlineContent, R.id.deadlineArrow, "디데이", state, deadlineExpanded, 3);
        setupSection(R.id.communityHeader, R.id.communityContent, R.id.communityArrow, "커뮤니티", state, communityExpanded, 4);

        hamburgerMenu = new Hamburger(this);
        new TopHeader(this);
        new BottomMenu(this);
        FloatingChatbotHelper.init(this);
        findViewById(R.id.bannerLayout).setOnClickListener(v -> open(ChatbotActivity.class));
    }

    private void setupSection(int headerId, int contentId, int arrowId, String title, Bundle state, boolean memoryExpanded, int sectionIndex) {
        View header = findViewById(headerId);
        View content = findViewById(contentId);
        View arrow = findViewById(arrowId);

        // Bundle state가 있으면 우선하고, 없으면 static 기억값(memoryExpanded)을 적용
        boolean expanded = state != null ? state.getBoolean("section_" + contentId, memoryExpanded) : memoryExpanded;

        setSectionExpanded(header, content, arrow, title, expanded, sectionIndex);

        header.setOnClickListener(v -> {
            boolean nextState = content.getVisibility() != View.VISIBLE;
            setSectionExpanded(header, content, arrow, title, nextState, sectionIndex);
        });
    }

    private void setSectionExpanded(View header, View content, View arrow, String title, boolean expanded, int sectionIndex) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        arrow.setRotation(expanded ? 180f : 0f);
        header.setContentDescription(title + (expanded ? ", 펼쳐짐, 접기" : ", 접힘, 펼치기"));

        // 사용자가 누를 때마다 static 변수 값도 동기화해서 기억
        switch (sectionIndex) {
            case 1: recentExpanded = expanded; break;
            case 2: recommendationExpanded = expanded; break;
            case 3: deadlineExpanded = expanded; break;
            case 4: communityExpanded = expanded; break;
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        for (int id : new int[]{R.id.recentContent, R.id.recommendationContent, R.id.deadlineContent, R.id.communityContent}) {
            state.putBoolean("section_" + id, findViewById(id).getVisibility() == View.VISIBLE);
        }
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        hamburgerMenu.updateLoginState();
        loadWidgets();
    }

    @Override protected void onPause() {
        generation++;
        for (Call<?> call : requests) call.cancel();
        requests.clear();
        super.onPause();
    }

    private void open(Class<?> screen) { startActivity(new Intent(this, screen)); }

    private void policy(String id) {
        startActivity(new Intent(this, PolicyDetailActivity.class).putExtra("policy_id", id));
    }

    private void clear(int id) { ((LinearLayout) findViewById(id)).removeAllViews(); }

    private void row(int id, String text, Runnable action) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setTextColor(Color.parseColor(action == null ? "#64748B" : "#4C1D95"));
        int padding = Math.round(12 * getResources().getDisplayMetrics().density);
        view.setPadding(0, padding, 0, padding);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        if (action != null) {
            view.setMinHeight(padding * 4);
            view.setOnClickListener(v -> action.run());
            view.setFocusable(true);
        }
        ((LinearLayout) findViewById(id)).addView(view);
    }

    private <T> void fetch(Call<T> call, int container, Loaded<T> loaded) {
        requests.add(call);
        int version = generation;
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> response) {
                if (c.isCanceled() || version != generation) return;
                clear(container);
                if (response.isSuccessful() && response.body() != null) loaded.accept(response.body());
                else row(container, "불러오지 못했어요. 눌러서 다시 시도", () -> loadWidgets());
            }
            @Override public void onFailure(Call<T> c, Throwable error) {
                if (c.isCanceled() || version != generation) return;
                clear(container);
                row(container, "연결을 확인하고 눌러서 다시 시도", () -> loadWidgets());
            }
        });
    }

    private void loadWidgets() {
        generation++;
        for (Call<?> call : requests) call.cancel();
        requests.clear();
        for (int id : new int[]{R.id.recentContent, R.id.recommendationContent, R.id.deadlineContent, R.id.communityContent}) clear(id);
        JSONArray recent = RecentPolicies.list(this);
        for (int i = 0; i < Math.min(3, recent.length()); i++) {
            JSONObject item = recent.optJSONObject(i);
            if (item != null) row(R.id.recentContent, item.optString("title"), () -> policy(item.optString("id")));
        }
        if (recent.length() == 0) row(R.id.recentContent, "최근 본 정책이 없어요. 정책을 둘러보세요.", () -> open(ConditionInputActivity.class));
        android.content.SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int user = prefs.getBoolean("isLoggedIn", false) ? prefs.getInt("id", -1) : -1;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        if (user > 0) {
            row(R.id.recommendationContent, "추천 정책을 불러오는 중이에요.", null);
            fetch(api.getRecommendations(user), R.id.recommendationContent, policies -> {
                for (int i = 0; i < Math.min(3, policies.size()); i++) {
                    Recommendation p = policies.get(i);
                    row(R.id.recommendationContent, p.getTitle(), () -> policy(p.getId()));
                }
                row(R.id.recommendationContent, policies.isEmpty() ? "조건을 설정하고 추천받기 →" : "추천 정책 더보기 →",
                        () -> open(policies.isEmpty() ? ConditionInputActivity.class : RecommendationActivity.class));
            });
            row(R.id.deadlineContent, "찜한 정책의 마감일을 확인하고 있어요.", null);
            fetch(api.getBookmarks(user), R.id.deadlineContent, bookmarks -> {
                List<Bookmark> upcoming = new ArrayList<>();
                for (Bookmark b : bookmarks) if (days(b.getEnd_date()) >= 0) upcoming.add(b);
                Collections.sort(upcoming, (a,b) -> Long.compare(days(a.getEnd_date()), days(b.getEnd_date())));
                for (int i = 0; i < Math.min(3, upcoming.size()); i++) {
                    Bookmark b = upcoming.get(i);
                    long remaining = days(b.getEnd_date());
                    row(R.id.deadlineContent, (remaining == 0 ? "D-DAY" : "D-" + remaining) + "  " + b.getTitle(), () -> policy(b.getPolicy_id()));
                }
                if (upcoming.isEmpty()) row(R.id.deadlineContent, "마감 예정인 찜한 정책이 없어요.", null);
                row(R.id.deadlineContent, "찜한 정책 더보기 →", () -> open(BookmarkActivity.class));
            });
        } else {
            row(R.id.recommendationContent, "로그인하고 맞춤 정책 추천받기 →", () -> open(LoginActivity.class));
            row(R.id.deadlineContent, "로그인하고 찜한 정책 마감일 확인하기 →", () -> open(LoginActivity.class));
        }
        row(R.id.communityContent, "커뮤니티 소식을 불러오는 중이에요.", null);
        fetch(api.getPosts(user), R.id.communityContent, posts -> {
            for (int i = 0; i < Math.min(3, posts.size()); i++) {
                Post p = posts.get(i);
                row(R.id.communityContent, p.getTitle() + "\n좋아요 " + p.getLikeCount() + " · 댓글 " + p.getCommentCount(), () -> {
                    Intent intent = new Intent(this, PostDetailActivity.class);
                    intent.putExtra("post_id", p.getPost_id()).putExtra("title", p.getTitle()).putExtra("content", p.getContent())
                            .putExtra("users_id", p.getUsers_id()).putExtra("writer", p.getWriter()).putExtra("date", p.getDate());
                    startActivity(intent);
                });
            }
            if (posts.isEmpty()) row(R.id.communityContent, "아직 게시글이 없어요.", null);
            row(R.id.communityContent, "커뮤니티 더보기 →", () -> open(BoardActivity.class));
        });
    }

    private long days(String value) {
        value = PolicyDeadline.date(value);
        if (value == null) return -1;
        SimpleDateFormat format = new SimpleDateFormat(value.contains("-") ? "yyyy-MM-dd" : "yyyyMMdd", Locale.KOREA);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        format.setLenient(false);
        try {
            Date end = format.parse(value);
            Date today = format.parse(format.format(new Date()));
            return (end.getTime() - today.getTime()) / 86400000L;
        } catch (java.text.ParseException error) { return -1; }
    }
}