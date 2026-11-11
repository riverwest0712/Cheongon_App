package com.example.cheongon.login;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.R;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Shared, unchecked-by-default consent gate for every signup method. */
final class SignupConsent {
    private final AppCompatActivity activity;
    private final Button submit;
    private final CheckBox terms, privacy;
    private String version;
    private boolean submitting;
    private Call<Map<String, String>> call;

    SignupConsent(AppCompatActivity activity, Button submit) {
        this.activity = activity;
        this.submit = submit;
        terms = activity.findViewById(R.id.agreeTerms);
        privacy = activity.findViewById(R.id.agreePrivacy);
        terms.setSaveEnabled(false);
        privacy.setSaveEnabled(false);
        terms.setOnCheckedChangeListener((button, checked) -> updateButton());
        privacy.setOnCheckedChangeListener((button, checked) -> updateButton());
        reload();
    }

    boolean isAccepted() { return version != null && terms.isChecked() && privacy.isChecked(); }

    void addTo(Map<String, Object> body) {
        body.put("terms_agreed", terms.isChecked());
        body.put("privacy_agreed", privacy.isChecked());
        body.put("terms_version", version);
    }

    void setSubmitting(boolean value) {
        submitting = value;
        terms.setEnabled(!value && version != null);
        privacy.setEnabled(!value && version != null);
        updateButton();
    }

    private void updateButton() {
        boolean enabled = isAccepted() && !submitting;
        submit.setEnabled(enabled);
        submit.setAlpha(enabled ? 1f : 0.45f);
    }

    void reload() {
        if (call != null) call.cancel();
        version = null;
        terms.setChecked(false);
        privacy.setChecked(false);
        setSubmitting(false);
        activity.findViewById(R.id.viewTerms).setEnabled(false);
        activity.findViewById(R.id.viewPrivacy).setEnabled(false);
        call = ApiClient.getClient().create(ApiService.class).getSignupTerms();
        call.enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> request, Response<Map<String, String>> response) {
                if (request.isCanceled() || activity.isFinishing() || activity.isDestroyed()) return;
                Map<String, String> body = response.body();
                if (!response.isSuccessful() || body == null || !hasText(body.get("terms_version"))
                        || !hasText(body.get("terms_text")) || !hasText(body.get("privacy_text"))) {
                    failed(); return;
                }
                version = body.get("terms_version");
                activity.findViewById(R.id.viewTerms).setEnabled(true);
                activity.findViewById(R.id.viewPrivacy).setEnabled(true);
                activity.findViewById(R.id.viewTerms).setOnClickListener(v -> show("이용약관", body.get("terms_text")));
                activity.findViewById(R.id.viewPrivacy).setOnClickListener(v -> show("개인정보 수집·이용 동의", body.get("privacy_text")));
                setSubmitting(false);
            }
            @Override public void onFailure(Call<Map<String, String>> request, Throwable error) {
                if (!request.isCanceled() && !activity.isDestroyed()) failed();
            }
        });
    }

    private boolean hasText(String text) { return text != null && !text.trim().isEmpty(); }

    private void failed() {
        Snackbar.make(
                activity.findViewById(android.R.id.content),
                "약관을 불러오지 못했습니다. 다시 시도해주세요.",
                Snackbar.LENGTH_LONG
        ).show();
        activity.findViewById(R.id.viewTerms).setEnabled(true);
        activity.findViewById(R.id.viewTerms).setOnClickListener(v -> reload());
    }

    private void show(String title, String content) {
        TextView text = new TextView(activity);
        int padding = (int) (24 * activity.getResources().getDisplayMetrics().density);
        text.setPadding(padding, padding, padding, padding);
        text.setTextSize(16);
        text.setText(content);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(text);
        new AlertDialog.Builder(activity).setTitle(title).setView(scroll).setPositiveButton("확인", null).show();
    }

    void cancel() { if (call != null) call.cancel(); }
}