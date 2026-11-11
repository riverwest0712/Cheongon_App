package com.example.cheongon.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.R;
import com.example.cheongon.main.FloatingChatbotHelper;
import com.google.android.material.snackbar.Snackbar;

public class FindPasswordActivity extends AppCompatActivity {

    EditText etFindId;
    Button btnFindPassword;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_find_password);

        etFindId = findViewById(R.id.etFindId);
        btnFindPassword = findViewById(R.id.btnFindPassword);
        btnBack = findViewById(R.id.btnBack);

        // 뒤로가기 버튼
        btnBack.setOnClickListener(view -> {
            finish();
        });

        // 챗봇 플로팅 버튼 동작 활성화
        FloatingChatbotHelper.init(this);

        btnFindPassword.setOnClickListener(view -> {

            String id = etFindId.getText().toString().trim();

            if (id.isEmpty()) {
                etFindId.setError("아이디를 입력해주세요.");
                return;
            }

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "비밀번호 찾기 기능을 연결할 예정입니다.",
                    Snackbar.LENGTH_SHORT
            ).show();
        });
    }

}