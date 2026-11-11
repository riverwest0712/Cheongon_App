package com.example.cheongon.community;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPostsActivity extends AppCompatActivity {

    private RecyclerView recyclerMyPosts;
    private PostAdapter postAdapter;

    private List<Post> myPostList = new ArrayList<>();

    private Hamburger hamburger;

    private TextView txtCount;
    private TextView txtSort;
    private TextView txtEmpty;

    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        // 챗봇 플로팅 버튼
        FloatingChatbotHelper.init(this);

        // 뒤로가기
        ImageView btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 뷰 연결
        txtCount = findViewById(R.id.txtCount);
        txtSort = findViewById(R.id.txtSort);
        txtEmpty = findViewById(R.id.txtEmpty);

        recyclerMyPosts = findViewById(R.id.recyclerMyPosts);

        recyclerMyPosts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // 어댑터
        postAdapter = new PostAdapter(myPostList);

        recyclerMyPosts.setAdapter(postAdapter);

        // 로그인 정보
        SharedPreferences prefs =
                getSharedPreferences(
                        "loginInfo",
                        MODE_PRIVATE
                );

        currentUserId =
                prefs.getInt("id", -1);

        // 게시글 클릭
        postAdapter.setOnItemClickListener(post -> {

            Intent intent =
                    new Intent(
                            MyPostsActivity.this,
                            PostDetailActivity.class
                    );

            intent.putExtra(
                    "post_id",
                    post.getPost_id()
            );

            intent.putExtra(
                    "title",
                    post.getTitle()
            );

            intent.putExtra(
                    "content",
                    post.getContent()
            );

            intent.putExtra(
                    "users_id",
                    post.getUsers_id()
            );

            intent.putExtra(
                    "writer",
                    post.getWriter()
            );

            intent.putExtra(
                    "date",
                    post.getDate()
            );

            startActivity(intent);
        });

        // 좋아요 버튼 클릭
        postAdapter.setOnLikeClickListener(
                (postId, position) -> {

                    if (currentUserId == -1) {

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "로그인 후 이용해주세요.",
                                Snackbar.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    Map<String, Object> likeData =
                            new HashMap<>();

                    likeData.put(
                            "post_id",
                            postId
                    );

                    likeData.put(
                            "users_id",
                            currentUserId
                    );

                    ApiService apiService =
                            ApiClient.getClient()
                                    .create(ApiService.class);

                    apiService.toggleLike(likeData)
                            .enqueue(
                                    new Callback<Map<String, Object>>() {

                                        @Override
                                        public void onResponse(
                                                Call<Map<String, Object>> call,
                                                Response<Map<String, Object>> response
                                        ) {

                                            if (response.isSuccessful()
                                                    && response.body() != null) {

                                                Map<String, Object> result =
                                                        response.body();

                                                Post post =
                                                        myPostList.get(position);

                                                // 좋아요 상태 반영
                                                if (result.containsKey("liked")) {

                                                    Object likedObj =
                                                            result.get("liked");

                                                    boolean isLiked;

                                                    if (likedObj instanceof Boolean) {

                                                        isLiked =
                                                                (Boolean) likedObj;

                                                    } else if (likedObj instanceof Number) {

                                                        isLiked =
                                                                ((Number) likedObj)
                                                                        .intValue() == 1;

                                                    } else {

                                                        isLiked =
                                                                "true".equalsIgnoreCase(
                                                                        String.valueOf(
                                                                                likedObj
                                                                        )
                                                                )
                                                                        || "1".equals(
                                                                        String.valueOf(
                                                                                likedObj
                                                                        )
                                                                );
                                                    }

                                                    boolean oldIsLiked =
                                                            post.isLiked();

                                                    post.setLiked(isLiked);

                                                    // 좋아요 개수 즉시 반영
                                                    if (isLiked
                                                            && !oldIsLiked) {

                                                        post.setLikeCount(
                                                                post.getLikeCount()
                                                                        + 1
                                                        );

                                                    } else if (!isLiked
                                                            && oldIsLiked) {

                                                        post.setLikeCount(
                                                                Math.max(
                                                                        0,
                                                                        post.getLikeCount()
                                                                                - 1
                                                                )
                                                        );
                                                    }
                                                }

                                                // 서버에서 받은 좋아요 개수
                                                if (result.containsKey(
                                                        "likeCount"
                                                )) {

                                                    Object countObj =
                                                            result.get(
                                                                    "likeCount"
                                                            );

                                                    if (countObj instanceof Number) {

                                                        post.setLikeCount(
                                                                ((Number) countObj)
                                                                        .intValue()
                                                        );

                                                    } else if (countObj != null) {

                                                        try {

                                                            post.setLikeCount(
                                                                    Integer.parseInt(
                                                                            countObj
                                                                                    .toString()
                                                                                    .trim()
                                                                    )
                                                            );

                                                        } catch (
                                                                NumberFormatException ignored
                                                        ) {
                                                        }
                                                    }
                                                }

                                                postAdapter.notifyItemChanged(
                                                        position
                                                );

                                            } else {

                                                Snackbar.make(
                                                        findViewById(
                                                                android.R.id.content
                                                        ),
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
                                                    findViewById(
                                                            android.R.id.content
                                                    ),
                                                    "네트워크 오류가 발생했습니다.",
                                                    Snackbar.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );
                }
        );

        // 정렬 버튼
        txtSort.setOnClickListener(
                v -> showSortMenu()
        );
    }

    // 내 게시글 조회
    private void loadMyPosts() {

        if (currentUserId == -1) {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보가 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.getMyPosts(currentUserId)
                .enqueue(
                        new Callback<List<Post>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Post>> call,
                                    Response<List<Post>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    myPostList.clear();

                                    myPostList.addAll(
                                            response.body()
                                    );

                                    postAdapter.notifyDataSetChanged();

                                    if (txtCount != null) {

                                        txtCount.setText(
                                                "내 게시글 "
                                                        + myPostList.size()
                                                        + "개"
                                        );
                                    }

                                    // 게시글이 없을 때
                                    if (myPostList.isEmpty()) {

                                        if (txtEmpty != null) {

                                            txtEmpty.setVisibility(
                                                    View.VISIBLE
                                            );
                                        }

                                        recyclerMyPosts.setVisibility(
                                                View.GONE
                                        );

                                    } else {

                                        if (txtEmpty != null) {

                                            txtEmpty.setVisibility(
                                                    View.GONE
                                            );
                                        }

                                        recyclerMyPosts.setVisibility(
                                                View.VISIBLE
                                        );
                                    }

                                } else {

                                    Snackbar.make(
                                            findViewById(
                                                    android.R.id.content
                                            ),
                                            "내 게시글을 불러오지 못했습니다.",
                                            Snackbar.LENGTH_SHORT
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Post>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        "MyPostsActivity",
                                        "통신 에러: "
                                                + t.getMessage()
                                );

                                Snackbar.make(
                                        findViewById(
                                                android.R.id.content
                                        ),
                                        "네트워크 오류가 발생했습니다.",
                                        Snackbar.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // 정렬 메뉴
    private void showSortMenu() {

        // 전체 메뉴
        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                0,
                dpToPx(4),
                0,
                dpToPx(4)
        );

        // 흰색 배경
        layout.setBackgroundColor(
                Color.WHITE
        );

        // 메뉴 항목
        TextView latest =
                createSortItem("최신순");

        TextView popular =
                createSortItem("좋아요순");

        TextView title =
                createSortItem("가나다순");

        layout.addView(latest);
        layout.addView(popular);
        layout.addView(title);

        // PopupWindow
        final PopupWindow popupWindow =
                new PopupWindow(
                        layout,
                        dpToPx(140),
                        dpToPx(144),
                        true
                );

        // 흰색 직사각형 배경
        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.WHITE)
        );

        // 그림자
        popupWindow.setElevation(
                dpToPx(8)
        );

        // 최신순
        latest.setOnClickListener(v -> {

            sortLatest();

            txtSort.setText(
                    "최신순 ▼"
            );

            popupWindow.dismiss();
        });

        // 좋아요순
        popular.setOnClickListener(v -> {

            sortPopular();

            txtSort.setText(
                    "좋아요순 ▼"
            );

            popupWindow.dismiss();
        });

        // 가나다순
        title.setOnClickListener(v -> {

            sortTitle();

            txtSort.setText(
                    "가나다순 ▼"
            );

            popupWindow.dismiss();
        });

        // 정렬 버튼 아래 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(90),
                dpToPx(4)
        );
    }

    // 정렬 메뉴 항목
    private TextView createSortItem(String text) {

        TextView item =
                new TextView(this);

        item.setText(text);

        item.setTextSize(14);

        item.setTextColor(
                Color.DKGRAY
        );

        item.setGravity(
                Gravity.CENTER_VERTICAL
        );

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

    // 최신순
    private void sortLatest() {

        myPostList.sort(
                (post1, post2) -> {

                    String date1 =
                            post1.getDate();

                    String date2 =
                            post2.getDate();

                    if (date1 == null) {
                        date1 = "";
                    }

                    if (date2 == null) {
                        date2 = "";
                    }

                    // 최신 날짜가 먼저
                    return date2.compareTo(
                            date1
                    );
                }
        );

        postAdapter.notifyDataSetChanged();
    }

    // 좋아요순
    private void sortPopular() {

        myPostList.sort(
                (post1, post2) ->
                        Integer.compare(
                                post2.getLikeCount(),
                                post1.getLikeCount()
                        )
        );

        postAdapter.notifyDataSetChanged();
    }

    // 가나다순
    private void sortTitle() {

        myPostList.sort(
                (post1, post2) -> {

                    String title1 =
                            post1.getTitle();

                    String title2 =
                            post2.getTitle();

                    if (title1 == null
                            || title1.trim().isEmpty()) {

                        title1 = "힣";
                    }

                    if (title2 == null
                            || title2.trim().isEmpty()) {

                        title2 = "힣";
                    }

                    boolean korean1 =
                            isKorean(
                                    title1.charAt(0)
                            );

                    boolean korean2 =
                            isKorean(
                                    title2.charAt(0)
                            );

                    // 한글 먼저
                    if (korean1 && !korean2) {
                        return -1;
                    }

                    if (!korean1 && korean2) {
                        return 1;
                    }

                    return title1.compareToIgnoreCase(
                            title2
                    );
                }
        );

        postAdapter.notifyDataSetChanged();
    }

    // 첫 글자가 한글인지 확인
    private boolean isKorean(char c) {

        return (c >= '\uAC00'
                && c <= '\uD7A3')
                || (c >= '\u3131'
                && c <= '\u314E')
                || (c >= '\u314F'
                && c <= '\u3163');
    }

    // dp → px
    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        loadMyPosts();
    }
}