package com.example.cheongon.main;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.example.cheongon.R;
import com.example.cheongon.mypage.ChatbotActivity;

public class FloatingChatbotHelper {

    public static void init(Activity activity) {
        View btnFloatingChatbot = activity.findViewById(R.id.btnFloatingChatbot);
        if (btnFloatingChatbot != null) {
            btnFloatingChatbot.setOnClickListener(view -> {
                Intent intent = new Intent(activity, ChatbotActivity.class);
                activity.startActivity(intent);
            });
        }
    }
}