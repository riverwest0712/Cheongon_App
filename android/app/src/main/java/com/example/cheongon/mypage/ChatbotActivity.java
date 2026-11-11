package com.example.cheongon.mypage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.R;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.BottomMenu;

import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.jobtool.JobtoolNavigation;
import com.example.cheongon.login.LoginActivity;
import com.example.cheongon.main.TopHeader;
import com.example.cheongon.recommendation.PolicyDetailActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {
    // 대화 기록은 loginInfo에 저장해서 로그아웃·회원탈퇴 때 같이 지워지게 한다.
    private static final String CHAT_HISTORY_KEY = "chatHistory";
    private static final int MAX_SAVED_MESSAGES = 50;
    private static final int MAX_SENT_HISTORY = 10;

    private ImageView btnBack;

    // 챗봇 화면 요소들
    private ScrollView chatScrollView;
    private LinearLayout chatWindow;
    private EditText userInput;
    private Button btnSend;

    private Hamburger hamburger;

    // 저장된 대화 [{role: "user"|"assistant", content, policies: [{policy_id, policy_name}]}]
    private JSONArray chatHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // 햄버거 메뉴와 하단 메뉴 모두 활성화
        hamburger = new Hamburger(this);
        new BottomMenu(this);

        new TopHeader(this);

        initViews();
        setupListeners();

        // 챗봇 첫 시작 인사말 (저장하지 않고 매번 보여준다)
        addAiMessage("안녕! 나는 청온의 도우미 청냥이야 🐾\n궁금한 거 물어봐줘!");

        // 화면을 나갔다 와도 이전 대화를 다시 보여준다
        chatHistory = loadChatHistory();
        for (int i = 0; i < chatHistory.length(); i++) {
            JSONObject item = chatHistory.optJSONObject(i);
            if (item == null) continue;
            if ("user".equals(item.optString("role"))) {
                addUserMessage(item.optString("content"));
            } else {
                addAiMessage(item.optString("content"));
                addPolicyCards(item.optJSONArray("policies"));
            }
        }
    }

    // 화면에 다시 돌아올 때 햄버거 메뉴 로그인 정보 새로고침
    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) {
            hamburger.updateLoginState();
        }
    }

    private void initViews() {
        // 상단 뒤로가기
        btnBack = findViewById(R.id.btnBack);

        // 챗봇 영역 뷰
        chatScrollView = findViewById(R.id.chatScrollView);
        chatWindow = findViewById(R.id.chatWindow);
        userInput = findViewById(R.id.userInput);
        btnSend = findViewById(R.id.btnSend);
    }

    private void setupListeners() {
        // 뒤로가기
        btnBack.setOnClickListener(view -> finish());

        // 메시지 전송 버튼
        btnSend.setOnClickListener(view -> sendMessage());
    }

    private void sendMessage() {
        String text = userInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 챗봇은 로그인한 사용자만 쓸 수 있다
        String authorization = JobtoolNavigation.authorization(this);
        if (authorization.equals("Bearer ")) {
            showLoginPrompt();
            return;
        }

        // 1. 서버에 보낼 이전 대화 (이번 질문을 저장하기 전에 만든다)
        Map<String, Object> request = new HashMap<>();
        request.put("message", text);
        request.put("history", recentHistory());

        // 2. 유저 메시지 화면에 추가하고 저장
        addUserMessage(text);
        saveMessage("user", text, null);
        userInput.setText("");

        // 3. "생각 중..." 로딩 메시지 추가
        View loadingView = addAiMessage("생각 중...");

        // 4. 서버(Node.js /api/chat)로 요청 보내기
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.sendChatMessage(authorization, request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // "생각 중..." 메시지 삭제
                chatWindow.removeView(loadingView);

                if (response.code() == 401) {
                    addAiMessage("로그인이 만료됐다냥! 다시 로그인하고 물어봐줘 🐾");
                    showLoginPrompt();
                    return;
                }

                if (response.code() == 429) {
                    addAiMessage("질문이 너무 많아서 잠깐 쉬어야 한다냥. 조금 있다가 다시 물어봐줘!");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();

                    try {
                        Map<String, Object> data = (Map<String, Object>) body.get("data");
                        String reply = (String) data.get("reply");

                        if (reply != null && !reply.isEmpty()) {
                            JSONArray policies = toPolicyCards(data.get("policies"));
                            saveMessage("assistant", reply, policies);
                            if (isDestroyed()) return;
                            addAiMessage(reply);
                            addPolicyCards(policies);
                        } else {
                            addAiMessage("청냥이가 답장을 받아오지 못했다냥! 🐾");
                        }
                    } catch (Exception e) {
                        // 🔍 안드로이드 Logcat에 데이터 파싱 에러 출력
                        android.util.Log.e("ChatbotError", "데이터 파싱 실패: " + e.getMessage());
                        addAiMessage("응답을 처리하는 중에 문제가 생겼다냥 🐾");
                    }
                } else {
                    // 🔍 서버가 200 외의 에러 코드(400, 500 등)를 보냈을 때의 로그
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "정보 없음";
                        android.util.Log.e("ChatbotError", "서버 응답 에러 [코드 " + response.code() + "]: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    addAiMessage("미안하다냥! 서버 연결에 문제가 생겼다냥 🐾");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // "생각 중..." 메시지 삭제
                chatWindow.removeView(loadingView);

                // 🔍 아예 인터넷 연결이나 주소(IP)가 틀려서 통신 자체를 실패했을 때의 로그
                android.util.Log.e("ChatbotError", "서버 통신 실패 (네트워크/주소 오류): " + t.getMessage());
                t.printStackTrace();

                addAiMessage("서버 통신에 실패했다냥 🐾");
            }
        });
    }

    private void showLoginPrompt() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("로그인 필요")
                .setMessage("청냥이와 대화하려면 로그인이 필요해요.\n로그인 화면으로 이동하시겠습니까?")
                .setPositiveButton("예", (dialog, which) ->
                        startActivity(new Intent(this, LoginActivity.class)
                                .putExtra("forceReauthenticate", true)))
                .setNegativeButton("아니오", null)
                .show();
    }

    private JSONArray loadChatHistory() {
        String saved = getSharedPreferences("loginInfo", MODE_PRIVATE).getString(CHAT_HISTORY_KEY, "[]");
        try {
            return new JSONArray(saved);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void saveMessage(String role, String content, JSONArray policies) {
        try {
            JSONObject item = new JSONObject();
            item.put("role", role);
            item.put("content", content);
            if (policies != null) item.put("policies", policies);
            chatHistory.put(item);
        } catch (JSONException e) {
            return;
        }

        // 오래된 대화부터 지워서 최근 50개만 남긴다
        while (chatHistory.length() > MAX_SAVED_MESSAGES) {
            chatHistory.remove(0);
        }

        SharedPreferences preferences = getSharedPreferences("loginInfo", MODE_PRIVATE);
        // 로그아웃 상태에서 응답이 늦게 오면 저장하지 않는다
        if (!preferences.getBoolean("isLoggedIn", false)) return;
        preferences.edit().putString(CHAT_HISTORY_KEY, chatHistory.toString()).apply();
    }

    // 서버가 이어지는 질문을 이해할 수 있게 최근 대화 10개를 보낸다
    private List<Map<String, String>> recentHistory() {
        List<Map<String, String>> history = new ArrayList<>();
        for (int i = Math.max(0, chatHistory.length() - MAX_SENT_HISTORY); i < chatHistory.length(); i++) {
            JSONObject item = chatHistory.optJSONObject(i);
            if (item == null) continue;
            Map<String, String> message = new HashMap<>();
            message.put("role", item.optString("role"));
            message.put("content", item.optString("content"));
            history.add(message);
        }
        return history;
    }

    // 서버 응답의 정책 목록에서 카드에 필요한 id와 이름만 남긴다
    private JSONArray toPolicyCards(Object policies) throws JSONException {
        JSONArray cards = new JSONArray();
        if (!(policies instanceof List)) return cards;
        for (Object policy : (List<?>) policies) {
            if (!(policy instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) policy;
            Object id = map.get("policy_id");
            Object name = map.get("policy_name");
            if (id == null || name == null) continue;
            cards.put(new JSONObject()
                    .put("policy_id", String.valueOf(id))
                    .put("policy_name", String.valueOf(name)));
        }
        return cards;
    }

    // 답변 아래에 정책 카드를 붙이고, 누르면 정책 상세로 이동
    private void addPolicyCards(JSONArray policies) {
        if (policies == null) return;
        for (int i = 0; i < policies.length(); i++) {
            JSONObject policy = policies.optJSONObject(i);
            if (policy == null) continue;
            String policyId = policy.optString("policy_id");

            TextView card = (TextView) LayoutInflater.from(this)
                    .inflate(R.layout.item_chat_policy, chatWindow, false);
            card.setText("📄 " + policy.optString("policy_name") + "  ›");
            card.setOnClickListener(view -> startActivity(
                    new Intent(this, PolicyDetailActivity.class).putExtra("policy_id", policyId)));
            chatWindow.addView(card);
        }
        scrollToBottom();
    }

    // 유저 말풍선 뷰 생성
    private void addUserMessage(String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_chat_user, chatWindow, false);
        TextView tvUserMessage = view.findViewById(R.id.tvUserMessage);
        tvUserMessage.setText(message);
        chatWindow.addView(view);
        scrollToBottom();
    }

    // 청냥이 말풍선 뷰 생성 (나중에 지울 수 있도록 View 객체 반환)
    private View addAiMessage(String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_chat_ai, chatWindow, false);
        TextView tvAiMessage = view.findViewById(R.id.tvAiMessage);
        tvAiMessage.setText(message);
        chatWindow.addView(view);
        scrollToBottom();
        return view;
    }

    // 메시지가 추가될 때마다 스크롤을 맨 아래로 이동
    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
