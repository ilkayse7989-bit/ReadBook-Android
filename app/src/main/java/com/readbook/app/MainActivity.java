package com.readbook.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String READBOOK_URL = "https://bolum-okuma.ilkayse7989.chatgpt.site";
    private WebView webView;

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
        setContentView(webView);

        if (savedInstanceState == null) {
            webView.loadUrl(READBOOK_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
