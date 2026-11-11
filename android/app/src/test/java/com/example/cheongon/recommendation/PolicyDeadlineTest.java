package com.example.cheongon.recommendation;

import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class PolicyDeadlineTest {
    @Test public void parsesSupportedDatesAndRejectsInvalidDates() {
        assertEquals("2026-09-15", PolicyDeadline.date("20260915"));
        assertEquals("2026-09-15", PolicyDeadline.date("2026-09-15T00:00:00.000Z"));
        assertNull(PolicyDeadline.date("20260230"));
        assertNull(PolicyDeadline.date("상시"));
    }
    @Test public void deadlineIncludesTodayAndUsesKoreanCalendarDay() throws Exception {
        SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        java.util.Date now = utc.parse("2026-09-14 15:01");
        assertEquals("D-Day", PolicyDeadline.label("20260915", now));
        assertEquals("D-1", PolicyDeadline.label("2026-09-16", now));
        assertEquals("마감", PolicyDeadline.label("2026-09-14", now));
        assertEquals("상시", PolicyDeadline.label(null, now));
        assertEquals("마감일 미정", PolicyDeadline.label("날짜 미정", now));
    }
}
