package com.readbook.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final String READBOOK_URL = "https://bolum-okuma.ilkayse7989.chatgpt.site";

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private Dialog promoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#211B2B"));
        getWindow().setNavigationBarColor(Color.WHITE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString(settings.getUserAgentString() + " ReadBookAndroid/1.0");

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = filePathCallback;
                try {
                    Intent chooserIntent = fileChooserParams.createIntent();
                    chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    chooserIntent.setType("image/*");
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
                    return true;
                } catch (ActivityNotFoundException error) {
                    fileChooserCallback = null;
                    return false;
                }
            }
        });

        webView.addJavascriptInterface(new AdsBridge(), "ReadBookAds");
        setContentView(webView);

        if (savedInstanceState == null) webView.loadUrl(READBOOK_URL);
        else webView.restoreState(savedInstanceState);

        notifyWeb("readbook:rewarded-ready", "{}");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showReadBookPromo() {
        if (promoDialog != null && promoDialog.isShowing()) return;

        promoDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(36), dp(28), dp(28));

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#090611"), Color.parseColor("#251238"), Color.parseColor("#090611")});
        root.setBackground(background);

        TextView team = new TextView(this);
        team.setText("READ BOOK TEAM");
        team.setTextColor(Color.parseColor("#8B5CF6"));
        team.setTextSize(16);
        team.setGravity(Gravity.CENTER);
        team.setLetterSpacing(0.18f);
        root.addView(team, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Koyu Modumuzu\nDenediniz mi?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(30), 0, dp(20));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView phone = new TextView(this);
        phone.setText("☾\n\nReadBook Team\n\nTesadüfen Sen\n\nOkumaya başla\n\n♡   ☾   ✦   ⚡\n\nAna Sayfa   Keşfet   Kitaplığım   Coin   Profil");
        phone.setTextColor(Color.WHITE);
        phone.setTextSize(18);
        phone.setGravity(Gravity.CENTER);
        phone.setPadding(dp(20), dp(24), dp(20), dp(24));
        GradientDrawable phoneBg = new GradientDrawable();
        phoneBg.setColor(Color.parseColor("#15101D"));
        phoneBg.setCornerRadius(dp(28));
        phoneBg.setStroke(dp(2), Color.parseColor("#7047B7"));
        phone.setBackground(phoneBg);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        phoneParams.setMargins(0, dp(8), 0, dp(20));
        root.addView(phone, phoneParams);

        Button close = new Button(this);
        close.setText("5 saniye");
        close.setEnabled(false);
        root.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        promoDialog.setContentView(root);
        promoDialog.setCancelable(false);
        promoDialog.show();

        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished + 999) / 1000;
                close.setText(seconds + " saniye");
            }

            @Override
            public void onFinish() {
                close.setEnabled(true);
                close.setText("✕  Kapat");
                notifyWeb("readbook:reward-earned", "{\"amount\":1,\"type\":\"readbook\"}");
            }
        }.start();

        close.setOnClickListener(v -> {
            promoDialog.dismiss();
            promoDialog = null;
            notifyWeb("readbook:rewarded-closed", "{}");
        });
    }

    private void notifyWeb(String eventName, String detailJson) {
        if (webView == null) return;
        String script = "window.dispatchEvent(new CustomEvent('" + eventName + "',{detail:" + detailJson + "}));";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    private final class AdsBridge {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(MainActivity.this::showReadBookPromo);
        }

        @JavascriptInterface
        public boolean isRewardedAdReady() {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileChooserCallback != null) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                fileChooserCallback.onReceiveValue(result);
                fileChooserCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (promoDialog != null && promoDialog.isShowing()) return;
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
