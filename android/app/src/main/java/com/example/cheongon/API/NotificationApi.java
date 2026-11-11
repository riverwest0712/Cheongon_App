package com.example.cheongon.API;

import com.example.cheongon.mypage.NotificationItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationApi {
    @GET("api/notifications")
    Call<List<NotificationItem>> list();

    @GET("api/notifications/unread")
    Call<Map<String, Integer>> unread();

    @PUT("api/notifications/read-all")
    Call<Map<String, Object>> readAll();

    @PUT("api/notifications/{id}/read")
    Call<Map<String, Object>> read(@Path("id") String id);

    @DELETE("api/notifications/{id}")
    Call<Map<String, Object>> delete(@Path("id") String id);
}
