package com.readbook.app;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends Activity {
    private static final String TAG = "ReadBookAds";
    private static final String READBOOK_URL = "https://bolum-okuma.ilkayse7989.chatgpt.site";
    private static final String PRODUCTION_REWARDED_AD_UNIT_ID =
            "ca-app-pub-2195815120748412/9636373902";
    private static final String TEST_REWARDED_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/5224354917";

    private WebView webView;
    private RewardedAd rewardedAd;
    private boolean isLoadingRewardedAd;

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
        webView.addJavascriptInterface(new AdsBridge(), "ReadBookAds");
        setContentView(webView);

        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());

        if (savedInstanceState == null) {
            webView.loadUrl(READBOOK_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private String getRewardedAdUnitId() {
        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return isDebuggable ? TEST_REWARDED_AD_UNIT_ID : PRODUCTION_REWARDED_AD_UNIT_ID;
    }

    private void loadRewardedAd() {
        if (isLoadingRewardedAd || rewardedAd != null) {
            return;
        }

        isLoadingRewardedAd = true;
        RewardedAd.load(
                this,
                getRewardedAdUnitId(),
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        isLoadingRewardedAd = false;
                        rewardedAd = ad;
                        notifyWeb("readbook:rewarded-ready", "{}" );
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        isLoadingRewardedAd = false;
                        rewardedAd = null;
                        Log.w(TAG, "Rewarded ad failed to load: " + error.getMessage());
                        notifyWeb("readbook:rewarded-unavailable", "{}" );
                    }
                });
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            notifyWeb("readbook:rewarded-unavailable", "{}" );
            loadRewardedAd();
            return;
        }

        RewardedAd adToShow = rewardedAd;
        rewardedAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyWeb("readbook:rewarded-closed", "{}" );
                loadRewardedAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.w(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                notifyWeb("readbook:rewarded-unavailable", "{}" );
                loadRewardedAd();
            }
        });

        adToShow.show(this, rewardItem -> notifyRewardEarned(rewardItem));
    }

    private void notifyRewardEarned(RewardItem rewardItem) {
        String rewardType = rewardItem.getType().replace("\\", "\\\\").replace("\"", "\\\"");
        String detail = "{\"amount\":" + rewardItem.getAmount()
                + ",\"type\":\"" + rewardType + "\"}";
        notifyWeb("readbook:reward-earned", detail);
    }

    private void notifyWeb(String eventName, String detailJson) {
        if (webView == null) {
            return;
        }
        String script = "window.dispatchEvent(new CustomEvent('" + eventName
                + "',{detail:" + detailJson + "}));";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    private final class AdsBridge {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(MainActivity.this::showRewardedAd);
        }

        @JavascriptInterface
        public boolean isRewardedAdReady() {
            return rewardedAd != null;
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
