package com.example.cheongon.main;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Recently viewed policies, stored separately for each signed-in account. */
public final class RecentPolicies {
    private RecentPolicies() {}
    private static String key(Context context) {
        android.content.SharedPreferences login = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
        return login.getBoolean("isLoggedIn", false) ? "user_" + login.getInt("id", -1) : "guest";
    }
    public static JSONArray list(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences("recentPolicies", Context.MODE_PRIVATE).getString(key(context), "[]"));
        } catch (org.json.JSONException e) { return new JSONArray(); }
    }
    public static void record(Context context, String id, String title) {
        if (id == null || title == null) return;
        try {
            JSONArray result = new JSONArray();
            result.put(new JSONObject().put("id", id).put("title", title));
            JSONArray previous = list(context);
            for (int i = 0; i < previous.length() && result.length() < 5; i++) {
                JSONObject item = previous.getJSONObject(i);
                if (!id.equals(item.optString("id"))) result.put(item);
            }
            context.getSharedPreferences("recentPolicies", Context.MODE_PRIVATE).edit().putString(key(context), result.toString()).apply();
        } catch (org.json.JSONException ignored) { }
    }
}
