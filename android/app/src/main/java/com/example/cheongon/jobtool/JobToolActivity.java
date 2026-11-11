package com.example.cheongon.jobtool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;

public class JobToolActivity extends AppCompatActivity {

    private Hamburger hamburgerMenu;
    private ImageView btnBack;
    private CardView cardCoverLetter, cardInterview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_tool);

        // 1. 상하단바 및 메뉴 초기화
        new TopHeader(this);
        hamburgerMenu = new Hamburger(this);
        new BottomMenu(this);

        // 2. 플로팅 챗봇 애니메이션 활성화
        FloatingChatbotHelper.init(this);

        // 3. 뷰 초기화 및 클릭 이벤트 설정
        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);                 // 상단 뒤로가기 버튼
        cardCoverLetter = findViewById(R.id.cardCoverLetter); // 버튼 A (자소서 첨삭)
        cardInterview = findViewById(R.id.cardInterview);     // 버튼 B (면접 질문 생성)
    }

    private void setupListeners() {
        // 뒤로가기 버튼 클릭 시 이전 화면으로 복귀
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 버튼 A 클릭 시 (자소서 첨삭 화면으로 이동)
        cardCoverLetter.setOnClickListener(v -> {
            Intent intent = new Intent(JobToolActivity.this, CoverLetterEditActivity.class);
            startActivity(intent);
        });

        // 버튼 B 클릭 시 (면접 질문 생성 화면으로 이동)
        cardInterview.setOnClickListener(v -> {
            // InterviewQuestionActivity로 이동
            Intent intent = new Intent(JobToolActivity.this, InterviewQuestionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 햄버거 메뉴 로그인 상태 갱신
        if (hamburgerMenu != null) {
            hamburgerMenu.updateLoginState();
        }
    }
}
