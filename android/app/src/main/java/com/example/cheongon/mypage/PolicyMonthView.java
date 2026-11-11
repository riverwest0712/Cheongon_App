package com.example.cheongon.mypage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Month grid whose policy dates remain visible while the whole page scrolls. */
public class PolicyMonthView extends LinearLayout {
    private static final int BLUE = Color.rgb(37, 99, 235);
    private final Calendar month = Calendar.getInstance();
    private final Calendar selected = Calendar.getInstance();
    private final Set<String> policyDates = new HashSet<>();
    private OnDateChangeListener listener;

    public interface OnDateChangeListener {
        void onSelectedDayChange(PolicyMonthView view, int year, int month, int day);
    }
    public PolicyMonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        month.set(Calendar.DAY_OF_MONTH, 1);
        render();
    }
    public long getDate() { return selected.getTimeInMillis(); }
    public void setDate(long date) {
        selected.setTimeInMillis(date);
        month.setTimeInMillis(date);
        month.set(Calendar.DAY_OF_MONTH, 1);
        render();
    }
    public void setOnDateChangeListener(OnDateChangeListener value) { listener = value; }
    public void setPolicyDates(Set<String> dates) {
        policyDates.clear();
        policyDates.addAll(dates);
        render();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String key(Calendar day) {
        return String.format(Locale.US, "%04d-%02d-%02d", day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
    }
    private TextView text(String label) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.rgb(25, 31, 40));
        return view;
    }
    private void render() {
        removeAllViews();
        LinearLayout header = new LinearLayout(getContext());
        TextView previous = text("‹"), next = text("›");
        previous.setTextSize(28); next.setTextSize(28);
        previous.setContentDescription("이전 달"); next.setContentDescription("다음 달");
        TextView title = text(String.format(Locale.KOREA, "%d년 %d월", month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1));
        title.setTypeface(null, Typeface.BOLD);
        header.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        addView(header);
        previous.setOnClickListener(v -> { month.add(Calendar.MONTH, -1); render(); });
        next.setOnClickListener(v -> { month.add(Calendar.MONTH, 1); render(); });
        LinearLayout weekdays = new LinearLayout(getContext());
        for (String label : new String[]{"일", "월", "화", "수", "목", "금", "토"}) {
            weekdays.addView(text(label), new LinearLayout.LayoutParams(0, dp(36), 1));
        }
        addView(weekdays);
        int offset = month.get(Calendar.DAY_OF_WEEK) - 1;
        int count = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int week = 0; week < (offset + count + 6) / 7; week++) {
            LinearLayout row = new LinearLayout(getContext());
            for (int column = 0; column < 7; column++) {
                int day = week * 7 + column - offset + 1;
                LinearLayout cell = new LinearLayout(getContext());
                cell.setOrientation(VERTICAL);
                cell.setGravity(Gravity.CENTER);
                TextView number = text(day > 0 && day <= count ? String.valueOf(day) : "");
                cell.addView(number, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(28)));
                View dot = new View(getContext());
                GradientDrawable dotBackground = new GradientDrawable();
                dotBackground.setShape(GradientDrawable.OVAL);
                dotBackground.setColor(BLUE);
                dot.setBackground(dotBackground);
                dot.setVisibility(View.INVISIBLE);
                cell.addView(dot, new LinearLayout.LayoutParams(dp(5), dp(5)));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(cell, params);
                if (day < 1 || day > count) continue;
                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, day);
                boolean hasPolicy = policyDates.contains(key(date));
                dot.setVisibility(hasPolicy ? View.VISIBLE : View.INVISIBLE);
                boolean isSelected = key(date).equals(key(selected));
                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(dp(22));
                background.setColor(isSelected ? Color.rgb(234, 242, 255) : Color.TRANSPARENT);
                if (isSelected) background.setStroke(dp(2), BLUE);
                cell.setBackground(background);
                cell.setSelected(isSelected);
                cell.setContentDescription(key(date) + (hasPolicy ? ", 마감 정책 있음" : "") + (isSelected ? ", 선택됨" : ""));
                cell.setOnClickListener(v -> {
                    selected.setTimeInMillis(date.getTimeInMillis());
                    render();
                    if (listener != null) listener.onSelectedDayChange(this, date.get(Calendar.YEAR), date.get(Calendar.MONTH), day);
                });
            }
            addView(row);
        }
    }
}
