package com.example.cheongon.mypage;

public final class NotificationItem {
    public enum Category {
        POLICY("추천 정책"),
        DEADLINE("마감 안내"),
        COMMUNITY("커뮤니티");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public String id;
    public String targetId;
    public String title;
    public String body;
    public Category category;
    public long createdAt;
    public boolean read;
}
