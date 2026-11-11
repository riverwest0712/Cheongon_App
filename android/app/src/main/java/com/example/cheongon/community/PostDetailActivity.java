package com.example.cheongon.community;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ImageView btnBack;
    private Hamburger hamburger;

    // 게시글 본문 뷰
    private TextView txtDetailTitle, txtDetailWriter, txtDetailDate, txtDetailContent;
    private Button btnDeletePost;
    private Button btnEditPost;

    // 좋아요 관련 뷰
    private LinearLayout layoutLike;
    private ImageView imgLike;
    private TextView txtLikeCount;
    private boolean isLiked = false; // 현재 좋아요 여부 상태

    // 댓글 관련 뷰 및 어댑터
    private RecyclerView recyclerComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private EditText edtCommentInput;
    private Button btnSubmitComment;
    private TextView txtCommentCount;

    private int postId = -1;
    private static final int EDIT_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnBack = findViewById(R.id.btnBack);

        txtDetailTitle = findViewById(R.id.txtDetailTitle);
        txtDetailWriter = findViewById(R.id.txtDetailWriter);
        txtDetailDate = findViewById(R.id.txtDetailDate);
        txtDetailContent = findViewById(R.id.txtDetailContent);
        btnDeletePost = findViewById(R.id.btnDeletePost);
        btnEditPost = findViewById(R.id.btnEditPost);

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        // 좋아요 뷰 초기화 바인딩
        layoutLike = findViewById(R.id.layoutLike);
        imgLike = findViewById(R.id.imgLike);
        txtLikeCount = findViewById(R.id.txtLikeCount);

        if (imgLike != null) {
            imgLike.setOnClickListener(v -> toggleLike());
        }

        txtCommentCount = findViewById(R.id.txtCommentCount);
        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList);
        recyclerComments.setAdapter(commentAdapter);

        edtCommentInput = findViewById(R.id.edtCommentInput);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);

        // 뒤로가기 버튼 클릭 시 setResult(RESULT_OK) 전달 후 종료
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                setResult(RESULT_OK);
                finish();
            });
        }

        loadPostDataFromIntent();

        if (postId != -1) {
            fetchComments(postId);
            fetchLikeStatusAndCount(postId);
        }

        btnSubmitComment.setOnClickListener(v -> submitComment());

        // 좋아요 영역 클릭 시 토글 실행
        if (layoutLike != null) {
            layoutLike.setOnClickListener(v -> toggleLike());
        }
    }

    // 하드웨어 뒤로가기 버튼을 눌렀을 때도 목록에 변경 신호 전달
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void loadPostDataFromIntent() {
        postId = getIntent().getIntExtra("post_id", -1);
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        int postUsersId = getIntent().getIntExtra("users_id", -1);
        String writer = getIntent().getStringExtra("writer");
        String date = getIntent().getStringExtra("date");

        if (title != null) txtDetailTitle.setText(title);
        if (content != null) txtDetailContent.setText(content);

        if (writer != null && !writer.isEmpty()) {
            txtDetailWriter.setText(writer);
        } else if (postUsersId != -1) {
            txtDetailWriter.setText("작성자 (ID: " + postUsersId + ")");
        } else {
            txtDetailWriter.setText("작성자 정보 없음");
        }

        // 날짜 문자열에서 초 단위 시간 부분을 제외하고 0000년-00월-00일만 표시
        if (date != null) {
            if (date.length() >= 10) {
                txtDetailDate.setText(date.substring(0, 10));
            } else {
                txtDetailDate.setText(date);
            }
        }

        SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int currentUserId = prefs.getInt("id", -1);

        if (currentUserId != -1 && currentUserId == postUsersId) {
            if (btnDeletePost != null) {
                btnDeletePost.setVisibility(View.VISIBLE);
                btnDeletePost.setOnClickListener(v -> deletePostFromServer(postId));
            }
            if (btnEditPost != null) {
                btnEditPost.setVisibility(View.VISIBLE);
                btnEditPost.setOnClickListener(v -> {
                    Intent intent = new Intent(PostDetailActivity.this, PostWriteActivity.class);
                    intent.putExtra("post_id", postId);
                    intent.putExtra("title", txtDetailTitle.getText().toString());
                    intent.putExtra("content", txtDetailContent.getText().toString());
                    startActivityForResult(intent, EDIT_REQUEST_CODE);
                });
            }
        } else {
            if (btnDeletePost != null) btnDeletePost.setVisibility(View.GONE);
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String updatedTitle = data.getStringExtra("updated_title");
            String updatedContent = data.getStringExtra("updated_content");

            if (updatedTitle != null) {
                txtDetailTitle.setText(updatedTitle);
            }
            if (updatedContent != null) {
                txtDetailContent.setText(updatedContent);
            }
        }
    }

    // 서버에서 좋아요 상태와 개수 불러오기
    private void fetchLikeStatusAndCount(int postId) {
        SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int currentUserId = prefs.getInt("id", -1);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getLikeStatus(postId, currentUserId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    // liked 또는 isLiked 키 모두 대응
                    String key = result.containsKey("liked") ? "liked" : (result.containsKey("isLiked") ? "isLiked" : null);
                    if (key != null) {
                        Object likedObj = result.get(key);
                        if (likedObj instanceof Boolean) {
                            isLiked = (Boolean) likedObj;
                        } else if (likedObj instanceof Number) {
                            isLiked = (((Number) likedObj).intValue() == 1);
                        } else if (likedObj instanceof String) {
                            String strVal = ((String) likedObj).toLowerCase();
                            isLiked = strVal.equals("true") || strVal.equals("1");
                        }
                    }

                    if (result.containsKey("likeCount")) {
                        Object countObj = result.get("likeCount");
                        parseAndSetLikeCount(countObj);
                    }

                    updateLikeUI();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PostDetailActivity", "좋아요 정보 조회 실패: " + t.getMessage());
            }
        });
    }

    // 좋아요 토글 요청
    private void toggleLike() {
        SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int currentUserId = prefs.getInt("id", -1);

        if (currentUserId == -1) {
            Snackbar.make(findViewById(android.R.id.content), "로그인 후 이용해주세요.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> likeData = new HashMap<>();
        likeData.put("post_id", postId);
        likeData.put("users_id", currentUserId);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.toggleLike(likeData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    String key = result.containsKey("liked") ? "liked" : (result.containsKey("isLiked") ? "isLiked" : null);
                    if (key != null) {
                        Object likedObj = result.get(key);
                        if (likedObj instanceof Boolean) {
                            isLiked = (Boolean) likedObj;
                        } else if (likedObj instanceof Number) {
                            isLiked = (((Number) likedObj).intValue() == 1);
                        } else if (likedObj instanceof String) {
                            String strVal = ((String) likedObj).toLowerCase();
                            isLiked = strVal.equals("true") || strVal.equals("1");
                        }
                    } else {
                        isLiked = !isLiked;
                    }

                    if (result.containsKey("likeCount")) {
                        parseAndSetLikeCount(result.get("likeCount"));
                    } else {
                        fallbackLocalLikeCountCalculation();
                    }

                    updateLikeUI();

                    // 상세 화면에서 좋아요가 변경되었음을 알림
                    setResult(RESULT_OK);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "좋아요 처리 실패", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "네트워크 오류 발생", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void parseAndSetLikeCount(Object countObj) {
        if (countObj instanceof Number) {
            txtLikeCount.setText(String.valueOf(((Number) countObj).intValue()));
        } else if (countObj != null) {
            String strVal = countObj.toString().trim();
            if (!strVal.isEmpty()) {
                try {
                    int count = Integer.parseInt(strVal);
                    txtLikeCount.setText(String.valueOf(count));
                } catch (NumberFormatException e) {
                    fallbackLocalLikeCountCalculation();
                }
            } else {
                fallbackLocalLikeCountCalculation();
            }
        } else {
            fallbackLocalLikeCountCalculation();
        }
    }

    private void fallbackLocalLikeCountCalculation() {
        String currentText = txtLikeCount.getText().toString().trim();
        int currentCount = 0;
        try {
            if (!currentText.isEmpty()) {
                currentCount = Integer.parseInt(currentText);
            }
        } catch (NumberFormatException ignored) {}

        int newCount = isLiked ? currentCount + 1 : Math.max(0, currentCount - 1);
        txtLikeCount.setText(String.valueOf(newCount));
    }

    private void updateLikeUI() {
        if (imgLike != null) {
            if (isLiked) {
                // 채워진 하트 아이콘으로 변경
                imgLike.setImageResource(R.drawable.ic_favorite);
                // 하트 색상을 빨간색으로 변경 (#FF0000)
                androidx.core.widget.ImageViewCompat.setImageTintList(
                        imgLike,
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF0000"))
                );
            } else {
                // 테두리 하트 아이콘으로 변경
                imgLike.setImageResource(R.drawable.ic_favorite_border);
                // 하트 색상을 회색(또는 기본 아이콘 색상)으로 변경 (#757575)
                androidx.core.widget.ImageViewCompat.setImageTintList(
                        imgLike,
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3F7BFF"))
                );
            }
        }

        // 옆의 숫자 텍스트뷰에 최신 좋아요 개수 반영
        if (txtLikeCount != null) {
            // 이미 parseAndSetLikeCount나 fallbackLocalLikeCountCalculation에서 숫자가 세팅되지만,
            // UI 동기화를 위해 현재 변수 값을 한 번 더 안전하게 매핑해줍니다.
        }
    }
    private void deletePostFromServer(int postId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.deletePost(postId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(findViewById(android.R.id.content), "게시글이 삭제되었습니다.", Snackbar.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "게시글 삭제 실패", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "네트워크 오류 발생", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchComments(int postId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getComments(postId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Log.d("COMMENT_TEST", "서버에서 받은 댓글 수: " + response.body().size());

                    commentList.clear();
                    commentList.addAll(response.body());

                    commentAdapter.notifyDataSetChanged();

                    txtCommentCount.setText(
                            String.valueOf(commentAdapter.getItemCount())
                    );
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Log.e("PostDetailActivity", "댓글 불러오기 실패: " + t.getMessage());
            }
        });
    }

    private void submitComment() {
        String commentText = edtCommentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "댓글 내용을 입력해주세요.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("loginInfo", MODE_PRIVATE);
        int currentUserId = prefs.getInt("id", -1);

        if (currentUserId == -1) {
            Snackbar.make(findViewById(android.R.id.content), "로그인 정보가 없습니다. 다시 로그인해주세요.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul")); // 한국 시간 적용
        String currentDate = sdf.format(new Date());

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("post_id", postId);
        commentData.put("users_id", currentUserId);
        commentData.put("comment_text", commentText);
        commentData.put("created_at", currentDate);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.createComment(commentData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Snackbar.make(findViewById(android.R.id.content), "댓글이 등록되었습니다.", Snackbar.LENGTH_SHORT).show();
                    edtCommentInput.setText("");
                    fetchComments(postId);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "댓글 등록 실패", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "네트워크 오류", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        // 화면으로 다시 돌아올 때마다 좋아요 상태와 개수를 서버에서 새로고침
        if (postId != -1) {
            fetchLikeStatusAndCount(postId);
        }
    }
}