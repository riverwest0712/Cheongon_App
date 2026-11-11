package com.example.cheongon.mypage;

import android.content.Context;
import com.example.cheongon.recommendation.Recommendation;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PolicyCalendarStore {
    private PolicyCalendarStore() { }
    public static List<Recommendation> load(Context context, int userId) {
        String json = context.getSharedPreferences("policyCalendar", Context.MODE_PRIVATE)
                .getString("user_" + userId, "[]");
        try {
            Recommendation[] items = new Gson().fromJson(json, Recommendation[].class);
            return items == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(items));
        } catch (RuntimeException error) { return new ArrayList<>(); }
    }
    public static void save(Context context, int userId, Recommendation policy) {
        List<Recommendation> items = load(context, userId);
        items.removeIf(item -> item == null || policy.getId().equals(item.getId()));
        items.add(policy);
        context.getSharedPreferences("policyCalendar", Context.MODE_PRIVATE).edit()
                .putString("user_" + userId, new Gson().toJson(items)).apply();
    }
}
