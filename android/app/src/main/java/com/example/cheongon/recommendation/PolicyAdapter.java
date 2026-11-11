package com.example.cheongon.recommendation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PolicyAdapter
        extends RecyclerView.Adapter<PolicyAdapter.PolicyViewHolder> {
    private final List<Recommendation> policyList;

    private List<Bookmark> bookmarkList;

    private int userId;

    // 찜 화면인지 여부
    private boolean bookmarkMode = false;

    // 찜 화면에서 카드 삭제 후 개수 갱신용
    private Runnable onBookmarkRemoved;

    // 🐾 [추가 1] 아이템 클릭 리스너 인터페이스 선언
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Recommendation policy);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // 캘린더 등 북마크 기능이 필요 없는 화면
    public PolicyAdapter(
            List<Recommendation> policyList
    ) {
        this.policyList = policyList;

        this.bookmarkList = null;

        this.userId = -1;

        this.bookmarkMode = false;
    }

    // 추천 결과 화면
    public PolicyAdapter(
            List<Recommendation> policyList,
            List<Bookmark> bookmarkList,
            int userId
    ) {
        this.policyList = policyList;

        this.bookmarkList = bookmarkList;

        this.userId = userId;

        this.bookmarkMode = false;
    }

    // 찜한 정책 화면
    public PolicyAdapter(
            List<Recommendation> policyList,
            List<Bookmark> bookmarkList,
            int userId,
            boolean bookmarkMode,
            Runnable onBookmarkRemoved
    ) {
        this.policyList = policyList;

        this.bookmarkList = bookmarkList;

        this.userId = userId;

        this.bookmarkMode = bookmarkMode;

        this.onBookmarkRemoved = onBookmarkRemoved;
    }

    @NonNull
    @Override
    public PolicyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_recommendation,
                                parent,
                                false
                        );

        return new PolicyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PolicyViewHolder holder,
            int position
    ) {
        Recommendation policy =
                policyList.get(position);
        holder.txtDeadline.setText(PolicyDeadline.label(policy.getEnd_date(), new java.util.Date()));

        // 카테고리
        holder.txtCategory.setText(
                policy.getCategory()
        );

        // 제목
        holder.txtTitle.setText(
                policy.getTitle()
        );

        // 설명
        holder.txtDescription.setText(
                policy.getDescription()
        );

        // 추천 점수
        int score =
                (int) Math.round(
                        policy.getScore()
                );

        holder.txtScore.setText(
                score + "점"
        );

        // 🐾 [추가 2] 아이템 전체(카드)를 눌렀을 때 상세 화면으로 이동하도록 리스너 연결
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(policy);
            }
        });

        // 캘린더 등 북마크 기능 없는 화면
        if (bookmarkList == null
                || userId == -1) {
            holder.imgBookmark.setVisibility(
                    View.GONE
            );

            holder.imgBookmark.setOnClickListener(
                    null
            );

            return;
        }

        holder.imgBookmark.setVisibility(
                View.VISIBLE
        );

        // 현재 정책의 찜 여부 확인
        Bookmark bookmark =
                findBookmark(
                        policy.getId()
                );

        if (bookmark != null) {
            // 찜 상태
            holder.imgBookmark.setImageResource(
                    R.drawable.ic_favorite
            );
        } else {
            // 찜 안 된 상태
            holder.imgBookmark.setImageResource(
                    R.drawable.ic_favorite_border
            );
        }

        // ~~변경 시작~~ : 하트 클릭 시 addBookmark/deleteBookmark 두 개로 분기하던 로직을
        // PolicyDetailActivity와 동일한 /api/bookmark/toggle 하나로 통일
        // (상세페이지 하트 ↔ 목록 하트 ↔ 찜 목록화면이 전부 같은 데이터 소스를 씀)
        holder.imgBookmark.setOnClickListener(view -> {

            boolean currentlyBookmarked =
                    findBookmark(policy.getId()) != null;

            toggleBookmarkApi(holder, policy, currentlyBookmarked); // ~~추가~~
        });
        // ~~변경 끝~~
    }

    // 정책 ID로 찜 찾기 (기존 그대로, 수정 없음)
    private Bookmark findBookmark(
            String policyId
    ) {
        if (bookmarkList == null) {
            return null;
        }

        for (Bookmark bookmark
                : bookmarkList) {
            if (policyId != null
                    && policyId.equals(
                    bookmark.getPolicy_id()
            )) {
                return bookmark;
            }
        }

        return null;
    }

    // ~~변경 시작~~ : 기존 addBookmark(...) / deleteBookmark(...) 두 메서드를
    // 이 메서드 하나로 통합. PolicyDetailActivity의 toggleBookmark()와 동일한 API를 호출하므로
    // 상세페이지에서 찜해도, 목록에서 찜해도 같은 bookmark 테이블 데이터를 공유하게 됨.
    private void toggleBookmarkApi( // ~~추가~~ (기존 addBookmark/deleteBookmark 대체)
                                    PolicyViewHolder holder,
                                    Recommendation policy,
                                    boolean currentlyBookmarked
    ) {
        ApiService apiService =
                ApiClient
                        .getClient()
                        .create(
                                ApiService.class
                        );

        Map<String, Object> request =
                new HashMap<>();

        request.put(
                "users_id",
                userId
        );

        request.put(
                "policy_id",
                policy.getId()
        );

        apiService.toggleBookmark(request)
                .enqueue(
                        new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> call,
                                    Response<Map<String, Object>> response
                            ) {
                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    Map<String, Object> body =
                                            response.body();

                                    boolean nowBookmarked =
                                            Boolean.TRUE.equals(
                                                    body.get("bookmarked")
                                            );

                                    if (nowBookmarked) {
                                        // 찜 추가된 상태
                                        if (findBookmark(policy.getId()) == null) {
                                            // bookmark_id는 toggle 응답에 없으므로 0으로 두되,
                                            // 이후 삭제도 항상 toggle API(policy_id 기준)를 쓰므로 문제 없음
                                            bookmarkList.add(
                                                    new Bookmark(0, policy.getId())
                                            );
                                        }

                                        holder.imgBookmark
                                                .setImageResource(
                                                        R.drawable.ic_favorite
                                                );

                                        Snackbar.make(
                                                holder.itemView,
                                                "♥  찜 목록에 추가했어요",
                                                Snackbar.LENGTH_SHORT
                                        ).show();

                                    } else {
                                        // 찜 해제된 상태
                                        Bookmark existing =
                                                findBookmark(policy.getId());

                                        if (existing != null) {
                                            bookmarkList.remove(existing);
                                        }

                                        if (bookmarkMode) {
                                            // 찜 목록 화면에서는 카드 자체를 삭제
                                            int currentPosition =
                                                    holder.getBindingAdapterPosition();

                                            if (currentPosition
                                                    != RecyclerView.NO_POSITION) {
                                                policyList.remove(
                                                        currentPosition
                                                );

                                                notifyItemRemoved(
                                                        currentPosition
                                                );

                                                if (onBookmarkRemoved != null) {
                                                    onBookmarkRemoved.run();
                                                }
                                            }
                                        } else {
                                            // 추천/목록 화면에서는 빈 하트로 변경
                                            holder.imgBookmark
                                                    .setImageResource(
                                                            R.drawable.ic_favorite_border
                                                    );
                                        }

                                        Snackbar.make(
                                                holder.itemView,
                                                "♥  찜에서 삭제했어요",
                                                Snackbar.LENGTH_SHORT
                                        ).show();
                                    }

                                } else {
                                    Snackbar.make(
                                            holder.itemView,
                                            "찜 처리에 실패했습니다.",
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
                                        holder.itemView,
                                        "서버 연결 실패",
                                        Snackbar.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }
    // ~~변경 끝~~ (기존 addBookmark(...), deleteBookmark(...) 메서드는 삭제합니다)

    @Override
    public int getItemCount() {
        return policyList.size();
    }

    static class PolicyViewHolder
            extends RecyclerView.ViewHolder {
        TextView txtCategory;
        TextView txtDeadline;
        TextView txtTitle;
        TextView txtDescription;
        TextView txtScore;

        ImageView imgBookmark;

        public PolicyViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);
            txtDeadline = itemView.findViewById(R.id.txtDeadline);

            txtCategory =
                    itemView.findViewById(
                            R.id.txtCategory
                    );

            txtTitle =
                    itemView.findViewById(
                            R.id.txtTitle
                    );

            txtDescription =
                    itemView.findViewById(
                            R.id.txtDescription
                    );

            txtScore =
                    itemView.findViewById(
                            R.id.txtScore
                    );

            imgBookmark =
                    itemView.findViewById(
                            R.id.imgBookmark
                    );
        }
    }
}
