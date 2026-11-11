package com.example.cheongon.main;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import com.example.cheongon.mypage.CalendarActivity;

import com.example.cheongon.jobtool.JobtoolNavigation;

import com.example.cheongon.R;
import com.example.cheongon.community.BoardActivity;
import com.example.cheongon.recommendation.ConditionInputActivity;
import com.example.cheongon.jobtool.JobToolActivity;

public class BottomMenu {
    private final Activity activity;

    public BottomMenu(Activity activity) {
        this.activity = activity;
        bind(R.id.btnHome, view -> open(Main.class, true));
        bind(R.id.btnPolicy, view -> open(ConditionInputActivity.class, false));
        bind(R.id.btnCommunity, view -> open(BoardActivity.class, false));
        bind(R.id.btnCalendar, view -> {
            if (!(activity instanceof JobToolActivity)) JobtoolNavigation.confirmPayment(activity);
        });
        updateSelection();
    }

    // Derive selection from the destination, so back navigation and drawer links
    // show the same state; a cancelled payment dialog cannot select another tab.
    private void updateSelection() {
        String screen = activity.getClass().getName();
        int selectedId = 0;
        if (activity instanceof Main) selectedId = R.id.btnHome;
        else if (screen.startsWith("com.example.cheongon.recommendation.")
                || activity instanceof CalendarActivity) selectedId = R.id.btnPolicy;
        else if (screen.startsWith("com.example.cheongon.jobtool.")) selectedId = R.id.btnCalendar;
        else if (screen.startsWith("com.example.cheongon.community.")) selectedId = R.id.btnCommunity;

        for (int id : new int[]{R.id.btnHome, R.id.btnPolicy, R.id.btnCalendar, R.id.btnCommunity}) {
            View icon = activity.findViewById(id);
            if (icon == null) continue;
            boolean selected = id == selectedId;
            icon.setSelected(selected);
            if (icon.getParent() instanceof View) ((View) icon.getParent()).setSelected(selected);
        }
    }

    private void bind(int iconId, View.OnClickListener listener) {
        View icon = activity.findViewById(iconId);
        if (icon == null) {
            return;
        }
        icon.setOnClickListener(listener);
        // 아이콘뿐 아니라 메뉴 이름과 주변 영역도 터치할 수 있게 연결합니다.
        if (icon.getParent() instanceof View) {
            ((View) icon.getParent()).setOnClickListener(listener);
        }
    }

    private void open(Class<? extends Activity> destination, boolean finishCurrent) {
        if (destination.isInstance(activity)) {
            return;
        }
        activity.startActivity(new Intent(activity, destination));
        if (finishCurrent) {
            activity.finish();
        }
    }
}
