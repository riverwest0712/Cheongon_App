package com.example.cheongon.server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiService api = ApiClient
                .getClient().create(ApiService.class);

        api.getPolicies().enqueue(new retrofit2.Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(
                    retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call,
                    retrofit2.Response<java.util.List<java.util.Map<String, Object>>> response
            ) {
                if (response.isSuccessful()) {
                    android.util.Log.d("SERVER_TEST", "연결 성공! 개수: " + response.body().size());
                } else {
                    android.util.Log.d("SERVER_TEST", "응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(
                    retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call,
                    Throwable t
            ) {
                android.util.Log.d("SERVER_TEST", "연결 실패: " + t.getMessage());
            }
        });
    }
}