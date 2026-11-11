package com.example.cheongon.community;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LikedPostsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtCount, txtSort, txtEmpty;
    private RecyclerView recyclerLikedPosts;
    private Hamburger hamburger;

    private PostAdapter postAdapter;
    private List<Post> likedPostList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_posts);

        // 상단, 하단, 햄버거 메뉴
        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        // 뷰 초기화
        btnBack = findViewById(R.id.btnBack);
        txtCount = findViewById(R.id.txtCount);
        txtSort = findViewById(R.id.txtSort);
        txtEmpty = findViewById(R.id.txtEmpty);
        recyclerLikedPosts = findViewById(R.id.recyclerLikedPosts);

        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // 챗봇
        FloatingChatbotHelper.init(this);

        // 정렬 버튼
        txtSort.setOnClickListener(v -> showSortDialog());

        // 리사이클러뷰
        recyclerLikedPosts.setLayoutManager(new LinearLayoutManager(this));

        postAdapter = new PostAdapter(likedPostList);
        recyclerLikedPosts.setAdapter(postAdapter);

        // 게시글 클릭
        postAdapter.setOnItemClickListener(post -> {
            Intent intent = new Intent(
                    LikedPostsActivity.this,
                    PostDetailActivity.class
            );

            intent.putExtra("post_id", post.getPost_id());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("users_id", post.getUsers_id());
            intent.putExtra("writer", post.getWriter());
            intent.putExtra("date", post.getDate());

            startActivity(intent);
        });

        // 좋아요 클릭
        postAdapter.setOnLikeClickListener((postId, position) -> {

            SharedPreferences prefs =
                    getSharedPreferences("loginInfo", MODE_PRIVATE);

            int currentUserId = prefs.getInt("id", -1);

            if (currentUserId == -1) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "로그인 후 이용해주세요.",
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }

            Map<String, Object> likeData = new HashMap<>();

            likeData.put("post_id", postId);
            likeData.put("users_id", currentUserId);

            ApiService apiService =
                    ApiClient.getClient().create(ApiService.class);

            apiService.toggleLike(likeData)
                    .enqueue(new Callback<Map<String, Object>>() {

                        @Override
                        public void onResponse(
                                Call<Map<String, Object>> call,
                                Response<Map<String, Object>> response
                        ) {

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                Map<String, Object> result =
                                        response.body();

                                // 서버 응답의 liked / isLiked 값 확인
                                boolean isLiked = false;

                                String key = null;

                                if (result.containsKey("liked")) {
                                    key = "liked";
                                } else if (result.containsKey("isLiked")) {
                                    key = "isLiked";
                                }

                                if (key != null) {

                                    Object likedObj = result.get(key);

                                    if (likedObj instanceof Boolean) {

                                        isLiked = (Boolean) likedObj;

                                    } else if (likedObj instanceof Number) {

                                        isLiked =
                                                ((Number) likedObj).intValue() == 1;

                                    } else if (likedObj instanceof String) {

                                        String strVal =
                                                ((String) likedObj).toLowerCase();

                                        isLiked =
                                                strVal.equals("true")
                                                        || strVal.equals("1");
                                    }
                                }

                                // 좋아요 취소 → 목록에서 삭제
                                if (!isLiked) {

                                    if (position >= 0
                                            && position < likedPostList.size()) {

                                        likedPostList.remove(position);

                                        postAdapter.notifyItemRemoved(position);

                                        postAdapter.notifyItemRangeChanged(
                                                position,
                                                likedPostList.size()
                                        );

                                        updateEmptyAndCountState();

                                        Snackbar.make(
                                                findViewById(android.R.id.content),
                                                "좋아요 목록에서 제외되었습니다.",
                                                Snackbar.LENGTH_SHORT
                                        ).show();
                                    }

                                } else {

                                    postAdapter.notifyItemChanged(position);
                                }

                            } else {

                                Snackbar.make(
                                        findViewById(android.R.id.content),
                                        "좋아요 처리 실패",
                                        Snackbar.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<Map<String, Object>> call,
                                Throwable t
                        ) {

                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "네트워크 오류 발생",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 햄버거 로그인 상태 갱신
        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        // 좋아요 목록 불러오기
        loadLikedPosts();
    }

    // 좋아요 목록 불러오기
    private void loadLikedPosts() {

        SharedPreferences prefs =
                getSharedPreferences("loginInfo", MODE_PRIVATE);

        int currentUserId = prefs.getInt("id", -1);

        if (currentUserId == -1) {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보가 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();

            showEmptyState();
            return;
        }

        ApiService apiService =
                ApiClient.getClient().create(ApiService.class);

        apiService.getLikedPosts(currentUserId)
                .enqueue(new Callback<List<Post>>() {

                    @Override
                    public void onResponse(
                            Call<List<Post>> call,
                            Response<List<Post>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            likedPostList.clear();

                            likedPostList.addAll(response.body());

                            postAdapter.notifyDataSetChanged();

                            updateEmptyAndCountState();

                        } else {

                            showEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Post>> call,
                            Throwable t
                    ) {

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "데이터를 불러오지 못했습니다.",
                                Snackbar.LENGTH_SHORT
                        ).show();

                        showEmptyState();
                    }
                });
    }

    // 정렬 메뉴
    private void showSortDialog() {

        // 전체 메뉴를 담을 레이아웃
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 4, 0, 4);
        layout.setBackgroundColor(Color.WHITE);

        // 최신순
        TextView latest = createSortItem("최신순");

        // 좋아요순
        TextView popular = createSortItem("좋아요순");

        // 가나다순
        TextView title = createSortItem("가나다순");

        layout.addView(latest);
        layout.addView(popular);
        layout.addView(title);

        // PopupWindow 생성
        final PopupWindow popupWindow = new PopupWindow(
                layout,
                dpToPx(130),
                dpToPx(144),
                true
        );

        // 흰색 배경
        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.WHITE)
        );

        // 그림자
        popupWindow.setElevation(dpToPx(8));

        // 최신순 클릭
        latest.setOnClickListener(v -> {
            sortLatest();
            popupWindow.dismiss();
        });

        // 좋아요순 클릭
        popular.setOnClickListener(v -> {
            sortPopular();
            popupWindow.dismiss();
        });

        // 가나다순 클릭
        title.setOnClickListener(v -> {
            sortTitle();
            popupWindow.dismiss();
        });

        // 정렬 버튼 아래쪽에 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(80),
                dpToPx(4)
        );
    }

    // 정렬 메뉴 항목 생성
    private TextView createSortItem(String text) {

        TextView item = new TextView(this);

        item.setText(text);
        item.setTextSize(14);
        item.setTextColor(Color.DKGRAY);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(
                dpToPx(16),
                0,
                dpToPx(16),
                0
        );

        item.setBackgroundResource(
                android.R.drawable.list_selector_background
        );

        item.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(44)
                )
        );

        return item;
    }

    // dp → px 변환
    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // 최신순
    private void sortLatest() {

        likedPostList.sort((post1, post2) -> {

            String date1 = post1.getDate();
            String date2 = post2.getDate();

            if (date1 == null) {
                date1 = "";
            }

            if (date2 == null) {
                date2 = "";
            }

            return date2.compareTo(date1);
        });

        postAdapter.notifyDataSetChanged();
        txtSort.setText("최신순 ▼");
    }

    // 좋아요순
    private void sortPopular() {

        likedPostList.sort((post1, post2) ->
                Integer.compare(
                        post2.getLikeCount(),
                        post1.getLikeCount()
                )
        );

        postAdapter.notifyDataSetChanged();
        txtSort.setText("좋아요순 ▼");
    }

    // 가나다순
    private void sortTitle() {

        likedPostList.sort((post1, post2) -> {

            String title1 = post1.getTitle();
            String title2 = post2.getTitle();

            if (title1 == null || title1.trim().isEmpty()) {
                title1 = "힣";
            }

            if (title2 == null || title2.trim().isEmpty()) {
                title2 = "힣";
            }

            boolean korean1 = isKorean(title1.charAt(0));
            boolean korean2 = isKorean(title2.charAt(0));

            // 한글을 먼저
            if (korean1 && !korean2) {
                return -1;
            }

            if (!korean1 && korean2) {
                return 1;
            }

            return title1.compareToIgnoreCase(title2);
        });

        postAdapter.notifyDataSetChanged();
        txtSort.setText("가나다순 ▼");
    }

    // 첫 글자가 한글인지 확인
    private boolean isKorean(char c) {

        return (c >= '\uAC00' && c <= '\uD7A3')
                || (c >= '\u3131' && c <= '\u314E')
                || (c >= '\u314F' && c <= '\u3163');
    }

    // 개수 및 빈 화면 상태
    private void updateEmptyAndCountState() {

        int count = likedPostList.size();

        txtCount.setText("내가 누른 좋아요 " + count + "개");

        if (count == 0) {

            txtEmpty.setVisibility(View.VISIBLE);
            recyclerLikedPosts.setVisibility(View.GONE);

        } else {

            txtEmpty.setVisibility(View.GONE);
            recyclerLikedPosts.setVisibility(View.VISIBLE);
        }
    }

    // 빈 화면
    private void showEmptyState() {

        likedPostList.clear();

        postAdapter.notifyDataSetChanged();

        updateEmptyAndCountState();
    }
}