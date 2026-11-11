package com.example.cheongon.community;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cheongon.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MyCommentAdapter extends RecyclerView.Adapter<MyCommentAdapter.ViewHolder> {

    private Context context;
    private List<Comment> commentList;
    private OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onEditClick(Comment comment, int position);
        void onDeleteClick(int commentId, int position);
    }

    public MyCommentAdapter(Context context, List<Comment> commentList, OnCommentClickListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.txtCommentText.setText(comment.getCommentText());
        holder.txtPostTitle.setText("게시글: " + (comment.getPostTitle() != null ? comment.getPostTitle() : "삭제된 게시글"));

        // 한국 시간(KST) 기준 날짜 포맷팅 함수 적용
        String formattedDate = formatKstDate(comment.getCreated_at());
        holder.txtDate.setText(formattedDate);

        // 수정 버튼 클릭
        holder.btnEditComment.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(comment, position);
        });

        // 삭제 버튼 클릭
        holder.btnDeleteComment.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(comment.getCommentId(), position);
        });

        // 댓글 아이템 클릭 시 해당 게시글 상세로 이동 (제목, 내용 등 데이터 함께 전달)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("post_id", comment.getPostId());
            intent.putExtra("title", comment.getPostTitle());       // 게시글 제목 전달
            intent.putExtra("content", comment.getPostContent());   // 게시글 내용 전달
            intent.putExtra("writer", comment.getWriter());         // 작성자 전달
            intent.putExtra("date", comment.getCreated_at());       // 날짜 전달
            intent.putExtra("users_id", comment.getUsersId());      // 게시글 작성자 ID 전달
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // UTC 날짜를 한국 시간(KST)으로 변환해주는 유틸 메서드
    private String formatKstDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";

        // 서버에서 ISO 형식("2026-09-02T15:00:00.000Z" 등)으로 주는 경우
        if (dateStr.contains("T")) {
            try {
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                int tIndex = dateStr.indexOf('T');
                String subStr = dateStr.substring(0, tIndex + 9);
                Date date = utcFormat.parse(subStr);

                SimpleDateFormat kstFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                kstFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                return kstFormat.format(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 단순 "YYYY-MM-DD" 형태인 경우 앞 10자리 반환
        return dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCommentText, txtPostTitle, txtDate;
        Button btnEditComment, btnDeleteComment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCommentText = itemView.findViewById(R.id.txtCommentText);
            txtPostTitle = itemView.findViewById(R.id.txtPostTitle);
            txtDate = itemView.findViewById(R.id.txtDate);
            btnEditComment = itemView.findViewById(R.id.btnEditComment);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}