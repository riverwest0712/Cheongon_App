package com.example.cheongon.mypage;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.NotificationApi;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.community.Post;
import com.example.cheongon.community.PostDetailActivity;
import com.example.cheongon.login.LoginActivity;
import com.example.cheongon.recommendation.PolicyDetailActivity;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {
    private final List<NotificationItem> items = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationItem.Category selected;
    private NotificationApi api;
    private Hamburger hamburger;
    private TopHeader header;
    private Call<List<NotificationItem>> loadRequest;
    private Call<Map<String, Object>> mutation;
    private boolean busy;
    private boolean active;
    private int loadVersion;
    private String status = "알림을 불러오는 중이에요.";
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (!busy) {
                load();
            }
            if (active) {
                handler.postDelayed(this, 15000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.notification);
        NotificationSession.initialize(this);
        api = ApiClient.getClient().create(NotificationApi.class);
        hamburger = new Hamburger(this);
        header = new TopHeader(this);
        new BottomMenu(this);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        findViewById(R.id.markAllRead).setOnClickListener(view ->
                change(api.readAll(), "모든 알림을 읽음으로 표시했어요.", () -> {
                    for (NotificationItem item : items) item.read = true;
                }));
        findViewById(R.id.notificationRetry).setOnClickListener(view -> {
            if (NotificationSession.token().isEmpty()) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                load(true);
            }
        });
        addFilter("전체", null);
        for (NotificationItem.Category category : NotificationItem.Category.values()) {
            addFilter(category.label, category);
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hamburger.updateLoginState();
        active = true;
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;
        handler.removeCallbacks(refresh);
        if (loadRequest != null) {
            loadRequest.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        if (mutation != null) {
            mutation.cancel();
        }
        super.onDestroy();
    }

    private void load() {
        load(false);
    }

    private void load(boolean force) {
        String token = NotificationSession.token();
        if (token.isEmpty()) {
            items.clear();
            status = "알림을 확인하려면 다시 로그인해주세요.";
            render();
            return;
        }
        if (!force && loadRequest != null && !loadRequest.isExecuted()) {
            return;
        }
        if (loadRequest != null) {
            loadRequest.cancel();
        }
        loadRequest = api.list();
        int version = ++loadVersion;
        loadRequest.enqueue(new Callback<List<NotificationItem>>() {
            @Override
            public void onResponse(Call<List<NotificationItem>> call,
                    Response<List<NotificationItem>> response) {
                if (call.isCanceled() || version != loadVersion || !token.equals(NotificationSession.token())) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    items.clear();
                    for (NotificationItem item : response.body()) {
                        if (item.category != null) {
                            items.add(item);
                        }
                    }
                    status = "";
                } else {
                    status = response.code() == 401 ? "다시 로그인해주세요." : "알림을 불러오지 못했어요. 다시 시도해주세요.";
                    if (response.code() == 401) {
                        getSharedPreferences("loginInfo", MODE_PRIVATE).edit()
                                .remove("notificationToken").apply();
                        items.clear();
                    }
                }
                render();
            }

            @Override
            public void onFailure(Call<List<NotificationItem>> call, Throwable error) {
                if (!call.isCanceled() && version == loadVersion) {
                    status = "서버 연결을 확인하고 다시 시도해주세요.";
                    render();
                }
            }
        });
    }

    private void change(Call<Map<String, Object>> request, String message, Runnable onSuccess) {
        if (busy) {
            return;
        }
        busy = true;
        if (loadRequest != null) {
            loadRequest.cancel();
        }
        mutation = request;
        String token = NotificationSession.token();
        render();
        request.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {
                if (isDestroyed() || call.isCanceled()) {
                    return;
                }
                busy = false;
                if (!token.equals(NotificationSession.token())) {
                    items.clear();
                    load();
                    return;
                }
                if (response.isSuccessful()) {
                    onSuccess.run();
                    status = "";
                }
                showMessage(response.isSuccessful() ? message : "처리하지 못했어요. 다시 시도해주세요.");
                render();
                load(true);
                header.refreshBadge();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                if (!isDestroyed() && !call.isCanceled()) {
                    busy = false;
                    showMessage("서버 연결을 확인해주세요. 화면을 새로고침합니다.");
                    load(true);
                    render();
                }
            }
        });
    }

    private void addFilter(String label, NotificationItem.Category category) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
        chip.setChipBackgroundColor(new ColorStateList(states,
                new int[]{Color.parseColor("#2563EB"), Color.WHITE}));
        chip.setTextColor(new ColorStateList(states,
                new int[]{Color.WHITE, Color.parseColor("#64748B")}));
        chip.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                selected = category;
                render();
            }
        });
        ((ChipGroup) findViewById(R.id.notificationFilters)).addView(chip);
        if (category == null) {
            chip.setChecked(true);
        }
    }

    private void render() {
        LinearLayout list = findViewById(R.id.notificationList);
        list.removeAllViews();
        int unread = 0;
        int visible = 0;
        for (NotificationItem item : items) {
            if (!item.read) {
                unread++;
            }
            if (selected != null && selected != item.category) {
                continue;
            }
            visible++;
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_notification, list, false);
            ((TextView) card.findViewById(R.id.notificationCategory)).setText(item.category.label);
            ((TextView) card.findViewById(R.id.notificationTitle)).setText(item.title);
            ((TextView) card.findViewById(R.id.notificationBody)).setText(item.body);
            ((TextView) card.findViewById(R.id.notificationTime)).setText(
                    new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(item.createdAt));
            card.findViewById(R.id.notificationUnread).setVisibility(item.read ? View.GONE : View.VISIBLE);
            card.setCardBackgroundColor(Color.parseColor(item.read ? "#FFFFFF" : "#EFF4FF"));
            card.setEnabled(!busy);
            card.setOnClickListener(view -> {
                if (!item.read) {
                    change(api.read(item.id), "읽음으로 표시했어요.", () -> item.read = true);
                }
                openTarget(item);
            });
            View delete = card.findViewById(R.id.notificationDelete);
            delete.setEnabled(!busy);
            delete.setContentDescription(item.title + " 알림 삭제");
            delete.setOnClickListener(view -> change(api.delete(item.id), "알림을 삭제했어요.", () -> items.remove(item)));
            list.addView(card);
        }
        findViewById(R.id.notificationEmpty).setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.notificationEmptyText)).setText(
                status.isEmpty() ? "표시할 알림이 없어요." : status);
        ((TextView) findViewById(R.id.notificationSummary)).setText(status.isEmpty()
                ? "읽지 않은 알림 " + unread + "개" : status);
        if (status.isEmpty() && header != null) header.updateUnreadCount(unread);
        View markAll = findViewById(R.id.markAllRead);
        markAll.setEnabled(!busy && unread > 0);
        markAll.setAlpha(!busy && unread > 0 ? 1f : 0.4f);
    }

    private void openTarget(NotificationItem item) {
        if (item.targetId == null) return;
        if (item.category == NotificationItem.Category.COMMUNITY) {
            int postId;
            try { postId = Integer.parseInt(item.targetId); }
            catch (NumberFormatException error) { return; }
            ApiClient.getClient().create(ApiService.class).getPostDetail(postId).enqueue(new Callback<Post>() {
                @Override public void onResponse(Call<Post> call, Response<Post> response) {
                    Post post = response.body();
                    if (!response.isSuccessful() || post == null) {
                        showMessage("게시글을 찾을 수 없어요.");
                        return;
                    }
                    Intent intent = new Intent(NotificationActivity.this, PostDetailActivity.class);
                    intent.putExtra("post_id", post.getPost_id());
                    intent.putExtra("title", post.getTitle());
                    intent.putExtra("content", post.getContent());
                    intent.putExtra("users_id", post.getUsers_id());
                    intent.putExtra("writer", post.getWriter());
                    intent.putExtra("date", post.getDate());
                    startActivity(intent);
                }
                @Override public void onFailure(Call<Post> call, Throwable error) {
                    showMessage("게시글을 불러오지 못했어요.");
                }
            });
        } else {
            Intent intent = new Intent(this, PolicyDetailActivity.class);
            intent.putExtra("policy_id", item.targetId);
            startActivity(intent);
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.notificationRoot), message, Snackbar.LENGTH_SHORT)
                .setAnchorView(findViewById(R.id.bottomMenu)).show();
    }
}
