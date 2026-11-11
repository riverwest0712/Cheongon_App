package com.example.cheongon.community;

public class Comment {
    private int comment_id;
    private String post_title;
    private int post_id;
    private int users_id;
    private String comment_text;
    private String created_at;
    private String post_content;
    private int post_users_id;
    private String writer;

    public Comment() {}
    public int getPostUsersId() {
        return post_users_id;
    }
    public void setPostUsersId(int post_users_id) {
        this.post_users_id = post_users_id;
    }

    public int getComment_id() { return comment_id; }
    public void setComment_id(int comment_id) { this.comment_id = comment_id; }
    public int getCommentId() { return comment_id; }

    public String getPost_title() { return post_title; }
    public void setPost_title(String post_title) { this.post_title = post_title; }
    public String getPostTitle() { return post_title; }

    public int getPost_id() { return post_id; }
    public void setPost_id(int post_id) { this.post_id = post_id; }
    public int getPostId() { return post_id; }

    public int getUsers_id() { return users_id; }
    public void setUsers_id(int users_id) { this.users_id = users_id; }
    public int getUsersId() { return users_id; }

    public String getComment_text() { return comment_text; }
    public void setComment_text(String comment_text) { this.comment_text = comment_text; }
    public String getCommentText() { return comment_text; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getPost_content() { return post_content; }
    public void setPost_content(String post_content) { this.post_content = post_content; }
    public String getPostContent() { return post_content; }

    public String getWriter() { return writer; }
    public void setWriter(String writer) { this.writer = writer; }
}