package com.example.cheongon.community;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostWriteActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView btnSubmit;
    private ImageView btnBack;
    private Hamburger hamburger;

    private boolean isEditMode = false;
    private int postId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_write);

        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // 상세 화면에서 수정 버튼을 통해 넘어왔는지 확인
        if (getIntent().hasExtra("post_id")) {
            isEditMode = true;
            postId = getIntent().getIntExtra("post_id", -1);
            String title = getIntent().getStringExtra("title");
            String content = getIntent().getStringExtra("content");

            // 기존 제목과 내용 채워넣기
            if (title != null) etTitle.setText(title);
            if (content != null) etContent.setText(content);
            if (btnSubmit != null) btnSubmit.setText("수정");
        }

        // 등록 또는 수정 분기 처리
        btnSubmit.setOnClickListener(v -> {
            if (isEditMode) {
                updatePost();
            } else {
                savePost();
            }
        });
    }

    private void savePost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "제목을 입력해주세요.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        if (content.isEmpty()) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "내용을 입력해주세요.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int currentUserId = prefs.getInt("id", -1);

        if (currentUserId == -1) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보가 없습니다. 다시 로그인해주세요.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        String currentDate = sdf.format(new Date());

        Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        postData.put("content", content);
        postData.put("users_id", currentUserId);
        postData.put("date", currentDate);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createPost(postData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "게시글이 등록되었습니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                    finish();
                } else {
                    Log.e("PostWriteActivity", "등록 실패 HTTP Status Code: " + response.code());
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "게시글 등록에 실패했습니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PostWriteActivity", "통신 에러: " + t.getMessage());
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "네트워크 오류가 발생했습니다.",
                        Snackbar.LENGTH_SHORT
                ).show();
            }
        });
    }

    // 서버로 게시글 수정 요청 보내는 메서드 추가
    private void updatePost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "제목을 입력해주세요.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        if (content.isEmpty()) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "내용을 입력해주세요.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", title);
        updateData.put("content", content);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updatePost(postId, updateData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "게시글이 수정되었습니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();

                    // 수정된 데이터를 담아서 이전 화면(PostDetailActivity)으로 전달
                    android.content.Intent resultIntent = new android.content.Intent();
                    resultIntent.putExtra("updated_title", title);
                    resultIntent.putExtra("updated_content", content);
                    setResult(RESULT_OK, resultIntent);

                    finish(); // 수정 완료 후 상세 화면으로 복귀
                } else {
                    Log.e("PostWriteActivity", "수정 실패 HTTP Status Code: " + response.code());
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "게시글 수정에 실패했습니다.",
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PostWriteActivity", "통신 에러: " + t.getMessage());
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "네트워크 오류가 발생했습니다.",
                        Snackbar.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) {
            hamburger.updateLoginState();
        }
    }
}