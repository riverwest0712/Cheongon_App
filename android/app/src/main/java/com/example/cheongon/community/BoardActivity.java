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

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BoardActivity extends AppCompatActivity {

    private RecyclerView recyclerBoard;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    private Hamburger hamburger;
    private TextView txtCount;
    private TextView txtSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        // 상단, 하단, 햄버거 메뉴 세팅
        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        // 뒤로가기 버튼
        ImageView btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(view -> finish());
        }

        // 글쓰기 버튼
        FloatingActionButton fabWrite = findViewById(R.id.fabWrite);

        if (fabWrite != null) {
            fabWrite.setOnClickListener(v -> {
                Intent intent =
                        new Intent(BoardActivity.this, PostWriteActivity.class);

                startActivity(intent);
            });
        }

        // 게시글 개수 / 정렬
        txtCount = findViewById(R.id.txtCount);
        txtSort = findViewById(R.id.txtSort);

        // RecyclerView
        recyclerBoard = findViewById(R.id.recyclerBoard);

        recyclerBoard.setLayoutManager(
                new LinearLayoutManager(this)
        );

        postAdapter = new PostAdapter(postList);
        recyclerBoard.setAdapter(postAdapter);

        // 게시글 상세 이동
        postAdapter.setOnItemClickListener(post -> {

            Intent intent =
                    new Intent(BoardActivity.this, PostDetailActivity.class);

            intent.putExtra("post_id", post.getPost_id());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("users_id", post.getUsers_id());
            intent.putExtra("writer", post.getWriter());
            intent.putExtra("date", post.getDate());

            startActivityForResult(intent, 200);
        });

        // 좋아요 버튼 클릭
        postAdapter.setOnLikeClickListener((postId, position) -> {

            Log.d(
                    "LikeClickTest",
                    "BoardActivity 리스너 탄다! postId: " + postId
            );

            SharedPreferences prefs =
                    getSharedPreferences("loginInfo", MODE_PRIVATE);

            int currentUserId =
                    prefs.getInt("id", -1);

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

            likeData.put("post_id", postId);
            likeData.put("users_id", currentUserId);

            ApiService apiService =
                    ApiClient.getClient().create(ApiService.class);

            apiService.toggleLike(likeData)
                    .enqueue(new Callback<Map<String, Object>>() {

                        @Override
                        public void onResponse(
                                Call<Map<String, Object>> call,
                                Response<Map<String, Object>> response) {

                            Log.d(
                                    "LikeClickTest",
                                    "서버 응답 코드: " + response.code()
                            );

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                Map<String, Object> result =
                                        response.body();

                                Log.d(
                                        "LikeClickTest",
                                        "서버 응답 데이터 성공: "
                                                + result.toString()
                                );

                                Post post =
                                        postList.get(position);

                                // 좋아요 상태 반영
                                if (result.containsKey("liked")) {

                                    Object likedObj =
                                            result.get("liked");

                                    boolean isLiked = false;

                                    if (likedObj instanceof Boolean) {

                                        isLiked =
                                                (Boolean) likedObj;

                                    } else if (likedObj instanceof Number) {

                                        isLiked =
                                                ((Number) likedObj).intValue() == 1;

                                    } else if (likedObj != null) {

                                        String likedString =
                                                likedObj.toString().trim();

                                        isLiked =
                                                likedString.equals("1")
                                                        || likedString.equalsIgnoreCase("true");
                                    }

                                    boolean oldIsLiked =
                                            post.isLiked();

                                    post.setLiked(isLiked);

                                    // 좋아요 개수 변경
                                    if (isLiked && !oldIsLiked) {

                                        post.setLikeCount(
                                                post.getLikeCount() + 1
                                        );

                                    } else if (!isLiked && oldIsLiked) {

                                        post.setLikeCount(
                                                Math.max(
                                                        0,
                                                        post.getLikeCount() - 1
                                                )
                                        );
                                    }
                                }

                                // 서버에서 likeCount를 보내는 경우
                                if (result.containsKey("likeCount")) {

                                    Object countObj =
                                            result.get("likeCount");

                                    if (countObj instanceof Number) {

                                        post.setLikeCount(
                                                ((Number) countObj).intValue()
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

                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                }

                                // 해당 게시글만 갱신
                                postAdapter.notifyItemChanged(position);

                            } else {

                                Log.e(
                                        "LikeClickTest",
                                        "서버 응답 실패 또는 바디 없음"
                                );

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
                                Throwable t) {

                            Log.e(
                                    "LikeClickTest",
                                    "네트워크 통신 오류: "
                                            + t.getMessage()
                            );

                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "네트워크 오류 발생",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        // 정렬 버튼 클릭
        if (txtSort != null) {

            txtSort.setOnClickListener(
                    view -> showSortDialog()
            );
        }
    }

    // =========================================================
    // 게시글 불러오기
    // =========================================================

    private void loadPostsFromServer() {

        SharedPreferences prefs =
                getSharedPreferences("loginInfo", MODE_PRIVATE);

        int currentUserId =
                prefs.getInt("id", -1);

        ApiService apiService =
                ApiClient.getClient().create(ApiService.class);

        apiService.getPosts(currentUserId)
                .enqueue(new Callback<List<Post>>() {

                    @Override
                    public void onResponse(
                            Call<List<Post>> call,
                            Response<List<Post>> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            postList.clear();
                            postList.addAll(response.body());

                            postAdapter.notifyDataSetChanged();

                            if (txtCount != null) {

                                txtCount.setText(
                                        "전체 게시글 "
                                                + postList.size()
                                                + "개"
                                );
                            }

                        } else {

                            Log.e(
                                    "BoardActivity",
                                    "서버 응답 실패 코드: "
                                            + response.code()
                            );

                            Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "게시판을 불러올 수 없습니다.",
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Post>> call,
                            Throwable t) {

                        Log.e(
                                "BoardActivity",
                                "통신/파싱 에러 원인: "
                                        + t.getMessage()
                        );

                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "게시판을 불러올 수 없습니다.",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // =========================================================
    // 정렬 메뉴
    // =========================================================

    private void showSortDialog() {

        // 메뉴 전체를 담을 레이아웃
        LinearLayout menuLayout =
                new LinearLayout(this);

        menuLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        menuLayout.setBackgroundColor(
                Color.WHITE
        );

        // 약간의 그림자
        menuLayout.setElevation(
                8f
        );

        // 메뉴 생성
        addSortItem(
                menuLayout,
                "최신순",
                () -> {
                    sortLatest();

                    if (txtSort != null) {
                        txtSort.setText("최신순 ▼");
                    }
                }
        );

        addSortItem(
                menuLayout,
                "좋아요순",
                () -> {
                    sortPopular();

                    if (txtSort != null) {
                        txtSort.setText("좋아요순 ▼");
                    }
                }
        );

        addSortItem(
                menuLayout,
                "가나다순",
                () -> {
                    sortTitle();

                    if (txtSort != null) {
                        txtSort.setText("가나다순 ▼");
                    }
                }
        );

        // 팝업창
        PopupWindow popupWindow =
                new PopupWindow(
                        menuLayout,
                        dpToPx(120),
                        dpToPx(144),
                        true
                );

        // 흰색 배경
        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.WHITE)
        );

        // 바깥을 누르면 닫힘
        popupWindow.setOutsideTouchable(true);

        // 그림자
        popupWindow.setElevation(8f);

        // 정렬 버튼 아래쪽에 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(70),
                -dpToPx(4)
        );
    }

    // =========================================================
    // 정렬 메뉴 항목 생성
    // =========================================================

    private void addSortItem(
            LinearLayout parent,
            String text,
            Runnable action
    ) {

        TextView item =
                new TextView(this);

        item.setText(text);

        item.setTextColor(
                Color.parseColor("#333333")
        );

        item.setTextSize(14);

        item.setGravity(
                Gravity.CENTER_VERTICAL
        );

        item.setPadding(
                dpToPx(16),
                0,
                dpToPx(16),
                0
        );

        item.setBackgroundColor(
                Color.WHITE
        );

        item.setClickable(true);

        item.setFocusable(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(48)
                );

        parent.addView(
                item,
                params
        );

        item.setOnClickListener(v -> {

            action.run();

            // 클릭 후 메뉴 닫기
            View root = parent.getRootView();

            if (root != null) {
                // PopupWindow는 부모에서 직접 닫을 수 없으므로
                // 클릭 후 부모를 숨김 처리하지 않고
                // 터치 이벤트로 닫히도록 구성
            }
        });
    }

    // =========================================================
    // dp → px 변환
    // =========================================================

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }

    // =========================================================
    // 최신순
    // =========================================================

    private void sortLatest() {

        postList.sort((a, b) -> {

            if (b.getDate() == null
                    || a.getDate() == null) {

                return 0;
            }

            return b.getDate()
                    .compareTo(a.getDate());
        });

        postAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // 좋아요순
    // =========================================================

    private void sortPopular() {

        postList.sort(
                (a, b) ->
                        Integer.compare(
                                b.getLikeCount(),
                                a.getLikeCount()
                        )
        );

        postAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // 가나다순
    // =========================================================

    private void sortTitle() {

        postList.sort((a, b) -> {

            String titleA =
                    a.getTitle() != null
                            ? a.getTitle()
                            : "";

            String titleB =
                    b.getTitle() != null
                            ? b.getTitle()
                            : "";

            if (titleA.isEmpty()
                    && titleB.isEmpty()) {

                return 0;
            }

            if (titleA.isEmpty()) {
                return 1;
            }

            if (titleB.isEmpty()) {
                return -1;
            }

            boolean isAHangul =
                    isKorean(titleA.charAt(0));

            boolean isBHangul =
                    isKorean(titleB.charAt(0));

            // 한글 우선
            if (isAHangul
                    && !isBHangul) {

                return -1;
            }

            if (!isAHangul
                    && isBHangul) {

                return 1;
            }

            // 가나다순 / ABC순
            return titleA.compareToIgnoreCase(
                    titleB
            );
        });

        postAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // 한글인지 확인
    // =========================================================

    private boolean isKorean(char c) {

        return c >= '\uAC00'
                && c <= '\uD7A3';
    }

    // =========================================================
    // 상세 화면에서 돌아왔을 때
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == 200
                && resultCode == RESULT_OK) {

            loadPostsFromServer();
        }
    }

    // =========================================================
    // 화면 다시 보일 때
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        loadPostsFromServer();
    }
}