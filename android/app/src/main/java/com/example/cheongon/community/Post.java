package com.example.cheongon.community;

import com.google.gson.annotations.SerializedName;

public class Post {
    private int post_id;
    private String title;
    private String content;
    private String writer;
    private String date;
    private int likeCount;
    private int commentCount;
    private int users_id;

    @SerializedName(value = "liked", alternate = {"isLiked"})
    private Object liked;

    public Post() {}

    // Getter
    public int getPost_id() { return post_id; }

    // 어댑터에서 호출하는 getId() 추가 (post_id를 반환하도록 연결)
    public int getId() {
        return post_id;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getWriter() { return writer; }
    public String getDate() { return date; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public int getUsers_id() { return users_id; }

    // 서버에서 주는 데이터 형식(Boolean, Number 1/0, String)에 상관없이 안전하게 판별
    public boolean isLiked() {
        if (liked instanceof Boolean) {
            return (Boolean) liked;
        } else if (liked instanceof Number) {
            return (((Number) liked).intValue() == 1);
        } else if (liked instanceof String) {
            String val = ((String) liked).toLowerCase();
            return val.equals("true") || val.equals("1");
        }
        return false;
    }

    // Setter
    public void setPost_id(int post_id) { this.post_id = post_id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setWriter(String writer) { this.writer = writer; }
    public void setDate(String date) { this.date = date; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setUsers_id(int users_id) { this.users_id = users_id; }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}