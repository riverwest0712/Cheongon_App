package com.example.cheongon.recommendation;

public class Bookmark {

    private int bookmark_id;
    private String policy_id;
    private String title;
    private String description;
    private String region;
    private String category;
    private String end_date;
    private String apply_url;

    private double score;

    public int getBookmark_id() {
        return bookmark_id;
    }

    public Bookmark(
            int bookmark_id,
            String policy_id
    ) {
        this.bookmark_id = bookmark_id;
        this.policy_id = policy_id;
    }

    public double getScore() {
        return score;
    }

    public String getPolicy_id() {
        return policy_id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRegion() {
        return region;
    }

    public String getCategory() {
        return category;
    }

    public String getEnd_date() {
        return end_date;
    }

    public String getApply_url() {
        return apply_url;
    }
}