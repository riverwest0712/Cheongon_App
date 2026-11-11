package com.example.cheongon.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cheongon.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.txtWriter.setText(comment.getWriter() != null ? comment.getWriter() : "사용자");

        // 한국 시간(KST) 기준 날짜 포맷팅 함수 적용
        String formattedDate = formatKstDate(comment.getCreated_at());
        holder.txtDate.setText(formattedDate);

        holder.txtContent.setText(comment.getComment_text());
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    // UTC 날짜를 한국 시간(KST)으로 변환해주는 유틸 메서드
    private String formatKstDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";

        // 서버에서 ISO 형식("2026-09-02T15:00:00.000Z" 등)으로 주는 경우
        if (dateStr.contains("T")) {
            try {
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                // 밀리초 및 타임존 표기 제거 후 파싱
                int tIndex = dateStr.indexOf('T');
                String subStr = dateStr.substring(0, tIndex + 9); // 연-월-일T시:분:초 까지만 추출
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
        TextView txtWriter, txtDate, txtContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtWriter = itemView.findViewById(R.id.txtCommentWriter);
            txtDate = itemView.findViewById(R.id.txtCommentDate);
            txtContent = itemView.findViewById(R.id.txtCommentContent);
        }
    }
}