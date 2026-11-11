package com.example.cheongon.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.R;
import com.example.cheongon.mypage.NotificationActivity;
import com.example.cheongon.API.NotificationApi;
import com.example.cheongon.mypage.NotificationSession;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopHeader implements DefaultLifecycleObserver {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Call<Map<String, Integer>> request;
    private boolean active;
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            refreshBadge();
            if (active) {
                handler.postDelayed(this, 15000);
            }
        }
    };

    public TopHeader(Activity activity) {
        this.activity = activity;
        NotificationSession.initialize(activity);
        View notification = activity.findViewById(R.id.btnNotification);
        if (notification != null) {
            notification.setOnClickListener(view -> {
                if (!(activity instanceof NotificationActivity)) {
                    activity.startActivity(new Intent(activity, NotificationActivity.class));
                }
            });
        }
        View home = activity.findViewById(R.id.headerHome);
        if (home != null) {
            home.setOnClickListener(view -> {
                if (!(activity instanceof Main)) {
                    activity.startActivity(new Intent(activity, Main.class));
                }
            });
        }
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(this);
        }
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        active = true;
        showCount(0);
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        active = false;
        handler.removeCallbacks(refresh);
        if (request != null) {
            request.cancel();
            request = null;
        }
    }

    public void refreshBadge() {
        String token = NotificationSession.token();
        if (token.isEmpty()) {
            showCount(0);
            return;
        }
        if (request != null) {
            request.cancel();
        }
        request = ApiClient.getClient().create(NotificationApi.class).unread();
        request.enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(Call<Map<String, Integer>> call,
                    Response<Map<String, Integer>> response) {
                if (call.isCanceled() || !token.equals(NotificationSession.token())) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    Integer count = response.body().get("count");
                    showCount(count == null ? 0 : count);
                } else {
                    showCount(0);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Integer>> call, Throwable error) {
                if (!call.isCanceled() && token.equals(NotificationSession.token())) showCount(0);
            }
        });
    }

    public void updateUnreadCount(int count) {
        if (request != null) request.cancel();
        showCount(count);
    }

    private void showCount(int count) {
        View badge = activity.findViewById(R.id.notificationBadge);
        if (badge != null) {
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
        View button = activity.findViewById(R.id.btnNotification);
        if (button != null) {
            button.setContentDescription(count > 0 ? "알림, 읽지 않은 알림 " + count + "개" : "알림");
        }
    }
}
