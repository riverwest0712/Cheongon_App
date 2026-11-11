package com.example.cheongon.recommendation;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class PolicyDeadline {
    private PolicyDeadline() { }

    public static String date(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.matches("\\d{8}")) {
            value = value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6);
        } else if (value.matches("\\d{4}-\\d{2}-\\d{2}([T ].*)?")) {
            value = value.substring(0, 10);
        } else return null;
        SimpleDateFormat format = format();
        ParsePosition position = new ParsePosition(0);
        return format.parse(value, position) != null && position.getIndex() == 10 ? value : null;
    }

    private static SimpleDateFormat format() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return format;
    }

    public static String label(String raw, Date now) {
        String end = date(raw);
        if (end == null) return raw == null || raw.trim().isEmpty() || raw.contains("상시") ? "상시" : "마감일 미정";
        SimpleDateFormat format = format();
        long days = (format.parse(end, new ParsePosition(0)).getTime()
                - format.parse(format.format(now), new ParsePosition(0)).getTime()) / 86400000L;
        return days < 0 ? "마감" : days == 0 ? "D-Day" : "D-" + days;
    }
}
