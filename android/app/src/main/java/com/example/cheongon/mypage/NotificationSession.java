package com.example.cheongon.mypage;

import android.content.Context;
import android.content.SharedPreferences;

public final class NotificationSession {
    private static SharedPreferences preferences;

    private NotificationSession() {
    }

    public static void initialize(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
    }

    public static String token() {
        if (preferences == null || !preferences.getBoolean("isLoggedIn", false)) {
            return "";
        }
        return preferences.getString("notificationToken", "");
    }
}
