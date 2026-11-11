package com.example.cheongon.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cheongon.R;
import java.util.List;
import android.util.Log;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<Post> postList;
    private OnItemClickListener itemClickListener;
    private OnLikeClickListener likeClickListener;

    // 아이템 클릭 리스너 인터페이스
    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    // 좋아요 클릭 리스너 인터페이스 정의
    public interface OnLikeClickListener {
        void onLikeClick(int postId, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    // 좋아요 리스너 등록 메서드 추가
    public void setOnLikeClickListener(OnLikeClickListener listener) {
        this.likeClickListener = listener;
    }

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.txtTitle.setText(post.getTitle());
        holder.txtDescription.setText(post.getContent());
        // 날짜 문자열에서 초 단위 시간 부분을 제외하고 0000년-00월-00일만 표시
        String fullDate = post.getDate();
        if (fullDate != null && fullDate.length() >= 10) {
            holder.txtDate.setText(fullDate.substring(0, 10));
        } else {
            holder.txtDate.setText(fullDate);
        }
        holder.txtLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.txtCommentCount.setText(
                String.valueOf(post.getCommentCount())
        );

        // 작성자(Writer) 뷰가 있다면 추가로 바인딩 (Post 모델에 getWriter()가 있다고 가정)
        if (holder.txtWriter != null && post.getWriter() != null) {
            holder.txtWriter.setText(post.getWriter());
        }

        // 좋아요 상태(isLiked)에 따라 하트 아이콘 채우기/비우기 처리
        if (post.isLiked()) {
            holder.imgLike.setImageResource(R.drawable.ic_favorite); // 채워진 하트
            // 빨간색 틴트 적용
            androidx.core.widget.ImageViewCompat.setImageTintList(
                    holder.imgLike,
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF0000"))
            );
        } else {
            holder.imgLike.setImageResource(R.drawable.ic_favorite_border); // 테두리 하트
            // 회색(또는 기존 기본 색상) 틴트 적용
            androidx.core.widget.ImageViewCompat.setImageTintList(
                    holder.imgLike,
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3F7BFF"))
            );
        }
        // 전체 카드 클릭 이벤트
        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (itemClickListener != null && adapterPos != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClick(postList.get(adapterPos));
            }
        });

        // 좋아요 영역 클릭 이벤트 (로그 추가 버전)
        holder.layoutLike.setOnClickListener(v -> {
            Log.d("LikeClickTest", "하트 영역 클릭됨! post_id: " + post.getId()); // 로그 출력 추가
            int adapterPos = holder.getAdapterPosition();
            if (likeClickListener != null && adapterPos != RecyclerView.NO_POSITION) {
                likeClickListener.onLikeClick(post.getId(), adapterPos);
            } else {
                Log.d("LikeClickTest", "likeClickListener가 null이거나 포지션이 유효하지 않음");
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDescription, txtDate, txtLikeCount, txtCommentCount, txtWriter;
        LinearLayout layoutLike;
        ImageView imgLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtLikeCount = itemView.findViewById(R.id.txtLikeCount);
            txtCommentCount = itemView.findViewById(R.id.txtCommentCount);
            txtWriter = itemView.findViewById(R.id.txtWriter);

            // 좋아요 관련 뷰 바인딩
            layoutLike = itemView.findViewById(R.id.layoutLike);
            imgLike = itemView.findViewById(R.id.imgLike);
        }
    }
}