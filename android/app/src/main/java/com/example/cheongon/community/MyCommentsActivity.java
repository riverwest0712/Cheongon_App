package com.example.cheongon.community;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.cheongon.main.FloatingChatbotHelper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyCommentsActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView txtCount;
    private TextView txtSort;
    private TextView txtEmpty;

    private Hamburger hamburger;

    private RecyclerView recyclerMyComments;
    private MyCommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();

    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_comments);

        // 챗봇 플로팅 버튼
        FloatingChatbotHelper.init(this);

        // 공통 UI
        new TopHeader(this);
        new BottomMenu(this);
        hamburger = new Hamburger(this);

        // 뒤로가기 버튼
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 개수 / 정렬 / 빈 화면
        txtCount = findViewById(R.id.txtCount);
        txtSort = findViewById(R.id.txtSort);
        txtEmpty = findViewById(R.id.txtEmpty);

        // 정렬 버튼
        if (txtSort != null) {
            txtSort.setOnClickListener(v -> showSortDialog());
        }

        // RecyclerView 설정
        recyclerMyComments = findViewById(R.id.recyclerMyComments);

        recyclerMyComments.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // 어댑터 생성
        commentAdapter = new MyCommentAdapter(
                this,
                commentList,
                new MyCommentAdapter.OnCommentClickListener() {

                    @Override
                    public void onEditClick(
                            Comment comment,
                            int position
                    ) {
                        showEditCommentDialog(
                                comment,
                                position
                        );
                    }

                    @Override
                    public void onDeleteClick(
                            int commentId,
                            int position
                    ) {
                        deleteCommentFromServer(
                                commentId,
                                position
                        );
                    }
                }
        );

        recyclerMyComments.setAdapter(commentAdapter);

        // 로그인된 유저 ID 가져오기
        SharedPreferences prefs =
                getSharedPreferences(
                        "loginInfo",
                        MODE_PRIVATE
                );

        currentUserId = prefs.getInt(
                "id",
                -1
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hamburger != null) {
            hamburger.updateLoginState();
        }

        if (currentUserId != -1) {

            fetchMyComments(currentUserId);

        } else {

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "로그인 정보가 없습니다.",
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // 정렬 메뉴
    // =========================================================

    private void showSortDialog() {

        // 메뉴 전체를 담는 레이아웃
        LinearLayout menuLayout =
                new LinearLayout(this);

        menuLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        // 흰색 배경
        menuLayout.setBackgroundColor(
                Color.WHITE
        );

        // 정렬 메뉴 항목
        TextView latestItem =
                createSortItem("최신순");

        TextView titleItem =
                createSortItem("가나다순");

        menuLayout.addView(latestItem);
        menuLayout.addView(titleItem);

        // PopupWindow 생성
        final PopupWindow popupWindow =
                new PopupWindow(
                        menuLayout,
                        dpToPx(120),
                        dpToPx(96),
                        true
                );

        // 팝업 배경 흰색
        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.WHITE)
        );

        // 바깥 영역 클릭 시 닫기
        popupWindow.setOutsideTouchable(true);

        // 그림자
        popupWindow.setElevation(
                dpToPx(6)
        );

        // 최신순
        latestItem.setOnClickListener(v -> {

            sortLatest();

            if (txtSort != null) {
                txtSort.setText("최신순 ▼");
            }

            popupWindow.dismiss();
        });

        // 가나다순
        titleItem.setOnClickListener(v -> {

            sortTitle();

            if (txtSort != null) {
                txtSort.setText("가나다순 ▼");
            }

            popupWindow.dismiss();
        });

        // 정렬 버튼 아래에 표시
        popupWindow.showAsDropDown(
                txtSort,
                -dpToPx(70),
                -dpToPx(4)
        );
    }

    // =========================================================
    // 정렬 메뉴 항목 생성
    // =========================================================

    private TextView createSortItem(String text) {

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

        // 흰색 배경
        item.setBackgroundColor(
                Color.WHITE
        );

        item.setClickable(true);
        item.setFocusable(true);

        item.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(48)
                )
        );

        return item;
    }

    // =========================================================
    // dp → px 변환
    // =========================================================

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }

    // =========================================================
    // 최신순
    // =========================================================

    private void sortLatest() {

        commentList.sort((a, b) ->
                Integer.compare(
                        b.getComment_id(),
                        a.getComment_id()
                )
        );

        commentAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // 가나다순
    // =========================================================

    private void sortTitle() {

        commentList.sort((a, b) -> {

            String textA =
                    a.getComment_text();

            String textB =
                    b.getComment_text();

            // null 처리
            if (textA == null) {
                textA = "";
            }

            if (textB == null) {
                textB = "";
            }

            textA = textA.trim();
            textB = textB.trim();

            // 빈 댓글은 마지막으로
            if (textA.isEmpty()
                    && !textB.isEmpty()) {

                return 1;
            }

            if (!textA.isEmpty()
                    && textB.isEmpty()) {

                return -1;
            }

            if (textA.isEmpty()
                    && textB.isEmpty()) {

                return 0;
            }

            // 한글 우선 정렬
            boolean koreanA =
                    isKorean(
                            textA.charAt(0)
                    );

            boolean koreanB =
                    isKorean(
                            textB.charAt(0)
                    );

            if (koreanA && !koreanB) {
                return -1;
            }

            if (!koreanA && koreanB) {
                return 1;
            }

            // 가나다순
            return textA.compareToIgnoreCase(
                    textB
            );
        });

        commentAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // 한글인지 확인
    // =========================================================

    private boolean isKorean(char c) {

        return (c >= '\uAC00'
                && c <= '\uD7A3')
                || (c >= '\u3131'
                && c <= '\u314E')
                || (c >= '\u314F'
                && c <= '\u3163');
    }

    // =========================================================
    // 개수 및 빈 화면 처리
    // =========================================================

    private void updateUI() {

        if (txtCount != null) {

            txtCount.setText(
                    "내 댓글 "
                            + commentList.size()
                            + "개"
            );
        }

        if (txtEmpty != null) {

            if (commentList.isEmpty()) {

                txtEmpty.setVisibility(
                        View.VISIBLE
                );

                recyclerMyComments.setVisibility(
                        View.GONE
                );

            } else {

                txtEmpty.setVisibility(
                        View.GONE
                );

                recyclerMyComments.setVisibility(
                        View.VISIBLE
                );
            }
        }
    }

    // =========================================================
    // 내가 쓴 댓글 목록 불러오기
    // =========================================================

    private void fetchMyComments(int userId) {

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.getMyComments(userId)
                .enqueue(
                        new Callback<List<Comment>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Comment>> call,
                                    Response<List<Comment>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    commentList.clear();

                                    commentList.addAll(
                                            response.body()
                                    );

                                    commentAdapter
                                            .notifyDataSetChanged();

                                    updateUI();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Comment>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        "MyCommentsActivity",
                                        "내 댓글 조회 실패: "
                                                + t.getMessage()
                                );
                            }
                        }
                );
    }

    // =========================================================
    // 댓글 수정 다이얼로그
    // =========================================================

    private void showEditCommentDialog(
            Comment comment,
            int position
    ) {

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.dialog_edit_comment,
                                null
                        );

        EditText edtInput =
                dialogView.findViewById(
                        R.id.edtEditInput
                );

        Button btnCancel =
                dialogView.findViewById(
                        R.id.btnCancel
                );

        Button btnConfirm =
                dialogView.findViewById(
                        R.id.btnConfirm
                );

        // 기존 댓글 내용
        edtInput.setText(
                comment.getComment_text()
        );

        edtInput.setSelection(
                edtInput.getText().length()
        );

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setView(dialogView);

        AlertDialog dialog =
                builder.create();

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );
        }

        // 취소
        btnCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        // 수정
        btnConfirm.setOnClickListener(v -> {

            String newText =
                    edtInput.getText()
                            .toString()
                            .trim();

            if (newText.isEmpty()) {

                Snackbar.make(
                        findViewById(
                                android.R.id.content
                        ),
                        "내용을 입력해주세요.",
                        Snackbar.LENGTH_SHORT
                ).show();

                return;
            }

            updateCommentOnServer(
                    comment.getComment_id(),
                    newText,
                    position
            );

            dialog.dismiss();
        });

        dialog.show();
    }

    // =========================================================
    // 댓글 수정 서버 요청
    // =========================================================

    private void updateCommentOnServer(
            int commentId,
            String newText,
            int position
    ) {

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        Map<String, Object> updateData =
                new HashMap<>();

        updateData.put(
                "comment_text",
                newText
        );

        apiService.updateComment(
                        commentId,
                        updateData
                )
                .enqueue(
                        new Callback<Map<String, Object>>() {

                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> call,
                                    Response<Map<String, Object>> response
                            ) {

                                if (response.isSuccessful()) {

                                    Snackbar.make(
                                            findViewById(
                                                    android.R.id.content
                                            ),
                                            "댓글이 수정되었습니다.",
                                            Snackbar.LENGTH_SHORT
                                    ).show();

                                    commentList
                                            .get(position)
                                            .setComment_text(
                                                    newText
                                            );

                                    commentAdapter
                                            .notifyItemChanged(
                                                    position
                                            );

                                } else {

                                    Snackbar.make(
                                            findViewById(
                                                    android.R.id.content
                                            ),
                                            "댓글 수정 실패",
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
                                        "네트워크 오류",
                                        Snackbar.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // 댓글 삭제 서버 요청
    // =========================================================

    private void deleteCommentFromServer(
            int commentId,
            int position
    ) {

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.deleteComment(commentId)
                .enqueue(
                        new Callback<Map<String, Object>>() {

                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> call,
                                    Response<Map<String, Object>> response
                            ) {

                                if (response.isSuccessful()) {

                                    Snackbar.make(
                                            findViewById(
                                                    android.R.id.content
                                            ),
                                            "댓글이 삭제되었습니다.",
                                            Snackbar.LENGTH_SHORT
                                    ).show();

                                    commentList.remove(
                                            position
                                    );

                                    commentAdapter
                                            .notifyItemRemoved(
                                                    position
                                            );

                                    updateUI();

                                } else {

                                    Snackbar.make(
                                            findViewById(
                                                    android.R.id.content
                                            ),
                                            "댓글 삭제 실패",
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
                                        "네트워크 오류",
                                        Snackbar.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }
}