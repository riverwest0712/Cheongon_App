package com.example.cheongon.jobtool;

import android.app.Activity;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import com.example.cheongon.API.ApiClient;
import com.example.cheongon.API.ApiService;
import com.example.cheongon.login.LoginActivity;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public final class JobtoolNavigation {
    private static final Set<Activity> checking = Collections.newSetFromMap(new WeakHashMap<>());
    private static String checkedToken;
    private static Boolean checkedPurchase;

    static void rememberPurchase(String token, boolean purchased) {
        checkedToken = token;
        checkedPurchase = purchased;
    }

    static void invalidatePurchase() {
        checkedToken = null;
        checkedPurchase = null;
    }

    private JobtoolNavigation() { }

    public static String authorization(Activity activity) {
        android.content.SharedPreferences preferences = activity.getSharedPreferences("loginInfo", 0);
        if (!preferences.getBoolean("isLoggedIn", false)) return "Bearer ";
        String token = preferences.getString("notificationToken", "");
        if (token.isEmpty()) token = preferences.getString("accessToken", "");
        return "Bearer " + token;
    }

    public static void confirmPayment(Activity activity) {
        confirmPayment(activity, JobToolActivity.class);
    }

    public static void confirmPayment(Activity activity, Class<? extends Activity> destination) {
        checkAccess(activity, () -> {
            if (!destination.isInstance(activity)) {
                activity.startActivity(new Intent(activity, destination));
            }
        });
    }

    public static void checkAccess(Activity activity, Runnable onPurchased) {
        if (activity.isFinishing() || activity.isDestroyed() || checking.contains(activity)) return;

        String token = authorization(activity);
        if (!token.equals(checkedToken)) invalidatePurchase();
        if (token.equals("Bearer ")) {
            if (activity.getSharedPreferences("loginInfo", 0).getBoolean("isLoggedIn", false)) {
                showLogin(activity);
            } else {
                showPurchasePrompt(activity);
            }
            return;
        }

        if (checkedPurchase != null) {
            if (checkedPurchase) onPurchased.run();
            else showPurchasePrompt(activity);
            return;
        }

        checking.add(activity);
        Call<Map<String, Object>> request = ApiClient.getClient()
                .create(ApiService.class)
                .getJobtoolAccess(token);
        AlertDialog loading = new AlertDialog.Builder(activity)
                .setMessage("구매 여부를 확인하고 있어요.")
                .setNegativeButton("취소", (dialog, which) -> {
                    request.cancel();
                    checking.remove(activity);
                    closeUnpaidTool(activity);
                })
                .create();
        loading.setOnCancelListener(dialog -> {
            request.cancel();
            checking.remove(activity);
            closeUnpaidTool(activity);
        });
        loading.show();

        request.enqueue(new Callback<Map<String, Object>>() {
            private boolean complete() {
                checking.remove(activity);
                if (!activity.isDestroyed()) loading.dismiss();
                return !request.isCanceled() && !activity.isFinishing() && !activity.isDestroyed()
                        && token.equals(authorization(activity));
            }

            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!complete()) return;
                if (response.code() == 401) {
                    showLogin(activity);
                } else if (response.isSuccessful() && response.body() != null
                        && response.body().get("purchased") instanceof Boolean) {
                    rememberPurchase(token, Boolean.TRUE.equals(response.body().get("purchased")));
                    if (Boolean.TRUE.equals(response.body().get("purchased"))) {
                        onPurchased.run();
                    } else {
                        showPurchasePrompt(activity);
                    }
                } else {
                    showError(activity, onPurchased);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                if (complete()) showError(activity, onPurchased);
            }
        });
    }

    private static void showPurchasePrompt(Activity activity) {
        if (activity instanceof JobtoolPaymentActivity) return;
        new AlertDialog.Builder(activity)
                .setTitle("프로 서비스")
                .setMessage("취업 도구는 프로 서비스입니다.\n구매 페이지로 이동하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> openPurchasePage(activity))
                .setNegativeButton("아니오", (dialog, which) -> closeUnpaidTool(activity))
                .setOnCancelListener(dialog -> closeUnpaidTool(activity))
                .show();
    }

    private static void showLogin(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("구매 내역 확인")
                .setMessage("구매 내역 확인에는 로그인이 필요해요.\n구매 페이지로 이동하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> openPurchasePage(activity))
                .setNeutralButton("다시 로그인", (dialog, which) -> {
                    activity.startActivity(new Intent(activity, LoginActivity.class)
                            .putExtra("forceReauthenticate", true));
                    closeUnpaidTool(activity);
                })
                .setNegativeButton("아니오", (dialog, which) -> closeUnpaidTool(activity))
                .setOnCancelListener(dialog -> closeUnpaidTool(activity))
                .show();
    }

    private static void showError(Activity activity, Runnable onPurchased) {
        new AlertDialog.Builder(activity)
                .setTitle("구매 확인 실패")
                .setMessage("구매 내역을 확인하지 못했어요.\n구매 페이지로 이동하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> openPurchasePage(activity))
                .setNeutralButton("다시 확인", (dialog, which) -> checkAccess(activity, onPurchased))
                .setNegativeButton("아니오", (dialog, which) -> closeUnpaidTool(activity))
                .setOnCancelListener(dialog -> closeUnpaidTool(activity))
                .show();
    }

    private static void openPurchasePage(Activity activity) {
        if (!(activity instanceof JobtoolPaymentActivity)) {
            activity.startActivity(new Intent(activity, JobtoolPaymentActivity.class));
        }
        closeUnpaidTool(activity);
    }

    private static void closeUnpaidTool(Activity activity) {
        if (activity instanceof CoverLetterEditActivity || activity instanceof InterviewQuestionActivity) {
            activity.finish();
        }
    }
}
