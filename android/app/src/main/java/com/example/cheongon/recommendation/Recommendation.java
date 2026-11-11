package com.example.cheongon.recommendation;

public class Recommendation {

    private String id;
    private String title;
    private String description;
    private String region;
    private String category;
    private String end_date;
    private String apply_url;
    private String deadlineText;
    private double score;
    private String reason;

    // 서버 JSON 파싱(Gson/Retrofit)은 기본 생성자를 쓰므로 유지하되,
    // 찜 목록(Bookmark)에서 Recommendation을 직접 만들 수 있도록 생성자를 추가
    public Recommendation() {
        // 기본 생성자 (Retrofit/Gson 파싱용, 유지)
    } // ~~추가~~

    public Recommendation(
            String id,
            String title,
            String description,
            String region,
            String category,
            String end_date,
            String apply_url,
            double score
    ) { // ~~추가~~
        this.id = id;
        this.title = title;
        this.description = description;
        this.region = region;
        this.category = category;
        this.end_date = end_date;
        this.apply_url = apply_url;
        this.score = score;
    }

    public String getId() {
        return id;
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

    public String getDeadlineText() {
        return deadlineText;
    }

    public double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}