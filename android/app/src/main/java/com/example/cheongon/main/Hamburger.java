package com.example.cheongon.main;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cheongon.R;
import com.example.cheongon.community.LikedPostsActivity;
import com.example.cheongon.community.MyCommentsActivity;
import com.example.cheongon.community.MyPostsActivity;
import com.example.cheongon.jobtool.CoverLetterEditActivity;
import com.example.cheongon.jobtool.InterviewQuestionActivity;
import com.google.android.material.snackbar.Snackbar;
import com.example.cheongon.jobtool.JobtoolNavigation;
import com.example.cheongon.login.LoginActivity;
import com.example.cheongon.login.SignupActivity;
import com.example.cheongon.mypage.CalendarActivity;
import com.example.cheongon.mypage.MyPageActivity;
import com.example.cheongon.mypage.ChatbotActivity;
import com.example.cheongon.main.SettingsActivity;
import com.example.cheongon.recommendation.BookmarkActivity;
import com.example.cheongon.recommendation.ConditionInputActivity;
import com.example.cheongon.recommendation.RecommendationActivity;
import com.example.cheongon.community.BoardActivity;

public class Hamburger {
    private final Activity activity;
    private final DrawerLayout drawerLayout;

    private final LinearLayout layoutLoggedOut;
    private final LinearLayout layoutLoggedIn;
    private final LinearLayout layoutLoginMenu;

    private final TextView tvUserName;
    private final TextView btnLogin;
    private final TextView btnSignUp;
    private final TextView btnLogout;
    private final TextView btnMyInfo;

    private final TextView btnMenu;

    // 홈
    private final TextView drawerHome;

    // 홈
    private final TextView drawerChatbot;

    // 정책 찾기
    private final TextView drawerPolicy;
    private final TextView drawerRecommendation;
    private final TextView drawerBookmark;
    private final TextView drawerCalendar;

    // 취업 도구
    private final TextView drawerResume;
    private final TextView drawerInterview;

    // 커뮤니티
    private final TextView drawerCommunity;
    private final TextView drawerMyPosts;
    private final TextView drawerMyComments;
    private final TextView drawerLikedPosts;

    // 마이페이지
//    private final TextView drawerMyPage;
    private final TextView drawerSettings;

    private final SharedPreferences loginPreferences;

    public Hamburger(Activity activity) {
        this.activity = activity;

        drawerLayout =
                activity.findViewById(R.id.drawerLayout);

        layoutLoggedOut =
                activity.findViewById(R.id.layoutLoggedOut);

        layoutLoggedIn =
                activity.findViewById(R.id.layoutLoggedIn);

        layoutLoginMenu =
                activity.findViewById(R.id.layoutLoginMenu);

        tvUserName =
                activity.findViewById(R.id.tvUserName);

        btnLogin =
                activity.findViewById(R.id.btnLogin);

        btnSignUp =
                activity.findViewById(R.id.btnSignUp);

        btnLogout =
                activity.findViewById(R.id.btnLogout);

        btnMyInfo = activity.findViewById(R.id.btnMyInfo);

        btnMenu =
                activity.findViewById(R.id.btnMenu);

        // 홈
        drawerHome =
                activity.findViewById(R.id.drawerHome);

        // 챗봇
        drawerChatbot =
                activity.findViewById(R.id.drawerChatbot);

        // 정책 찾기
        drawerPolicy =
                activity.findViewById(R.id.drawerPolicy);

        drawerRecommendation =
                activity.findViewById(R.id.drawerRecommendation);

        drawerBookmark =
                activity.findViewById(R.id.drawerBookmark);

        drawerCalendar =
                activity.findViewById(R.id.drawerCalendar);

        // 취업 도구
        drawerResume =
                activity.findViewById(R.id.drawerResume);

        drawerInterview =
                activity.findViewById(R.id.drawerInterview);

        // 커뮤니티
        drawerCommunity =
                activity.findViewById(R.id.drawerCommunity);

        drawerMyPosts =
                activity.findViewById(R.id.drawerMyPosts);

        drawerMyComments =
                activity.findViewById(R.id.drawerMyComments);

        drawerLikedPosts =
                activity.findViewById(R.id.drawerLikedPosts);

        // 설정
//        drawerMyPage =
//                activity.findViewById(R.id.drawerMyPage);

        drawerSettings =
                activity.findViewById(R.id.drawerSettings);

        loginPreferences =
                activity.getSharedPreferences(
                        "loginInfo",
                        Activity.MODE_PRIVATE
                );

        setup();
        updateLoginState();
    }

    private void setup() {
        // 햄버거 열기
        if (btnMenu != null) {
            btnMenu.setOnClickListener(view ->
                    drawerLayout.openDrawer(
                            GravityCompat.END
                    )
            );
        }

        // 로그인
        btnLogin.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            LoginActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 회원가입
        btnSignUp.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            SignupActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 내 정보 보기
        btnMyInfo.setOnClickListener(view -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
        });

        // 홈
        drawerHome.setOnClickListener(view -> {
            if (!(activity instanceof Main)) {
                activity.startActivity(
                        new Intent(
                                activity,
                                Main.class
                        )
                );
            }

            drawerLayout.closeDrawers();
        });

        // 챗봇
        drawerChatbot.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            ChatbotActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });


        // 조건 설정
        drawerPolicy.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            ConditionInputActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 추천 정책
        drawerRecommendation.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            RecommendationActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 찜한 정책
        drawerBookmark.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            BookmarkActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 정책 캘린더
        drawerCalendar.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            CalendarActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 취업 도구 - 프로 서비스 안내 후 결제 화면 이동
        drawerResume.setOnClickListener(view -> {
            drawerLayout.closeDrawers();
            JobtoolNavigation.confirmPayment(activity, CoverLetterEditActivity.class);
        });

        drawerInterview.setOnClickListener(view -> {
            drawerLayout.closeDrawers();
            JobtoolNavigation.confirmPayment(activity, InterviewQuestionActivity.class);
        });

        // 로그인 프로필
        layoutLoggedIn.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            MyPageActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 회원정보 수정
//        drawerMyPage.setOnClickListener(view -> {
//
//            activity.startActivity(
//                    new Intent(
//                            activity,
//                            MyPageActivity.class
//                    )
//            );
//
//            drawerLayout.closeDrawers();
//        });

        // 설정
        drawerSettings.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            SettingsActivity.class
                    )
            );

            drawerLayout.closeDrawers();
        });

        // 로그아웃
        btnLogout.setOnClickListener(view -> {
            loginPreferences.edit()
                    .clear()
                    .apply();

            updateLoginState();

            drawerLayout.closeDrawers();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        });

        // 커뮤니티 (게시판)
        drawerCommunity.setOnClickListener(view -> {
            if (!(activity instanceof BoardActivity)) {
                activity.startActivity(
                        new Intent(
                                activity,
                                BoardActivity.class
                        )
                );
            }

            drawerLayout.closeDrawers();
        });

        // 내가 쓴 게시글
        drawerMyPosts.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            MyPostsActivity.class
                    )
            );
            drawerLayout.closeDrawers();
        });

        // 내가 쓴 댓글
        drawerMyComments.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            MyCommentsActivity.class
                    )
            );
            drawerLayout.closeDrawers();
        });

        drawerLikedPosts.setOnClickListener(view -> {
            activity.startActivity(
                    new Intent(
                            activity,
                            LikedPostsActivity.class
                    )
            );
            drawerLayout.closeDrawers();
        });
    }

    public void updateLoginState() {
        boolean isLoggedIn =
                loginPreferences.getBoolean(
                        "isLoggedIn",
                        false
                );

        if (isLoggedIn) {
            // 로그인 안내 숨기기
            layoutLoggedOut.setVisibility(
                    View.GONE
            );

            // 프로필 표시
            layoutLoggedIn.setVisibility(
                    View.VISIBLE
            );

            // 로그인 전용 메뉴 표시
            layoutLoginMenu.setVisibility(
                    View.VISIBLE
            );

            String userName =
                    loginPreferences.getString(
                            "userName",
                            "사용자"
                    );

            tvUserName.setText(
                    userName + "님"
            );
        } else {
            // 로그인 안내 표시
            layoutLoggedOut.setVisibility(
                    View.VISIBLE
            );

            // 프로필 숨기기
            layoutLoggedIn.setVisibility(
                    View.GONE
            );

            // 홈 이외 메뉴 전부 숨기기
            layoutLoginMenu.setVisibility(
                    View.GONE
            );
        }
    }
}
