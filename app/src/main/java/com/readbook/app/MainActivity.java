package com.readbook.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final String READBOOK_URL = "https://bolum-okuma.ilkayse7989.chatgpt.site";
    private static final String PRODUCTION_REWARDED_AD_UNIT_ID =
            "ca-app-pub-2195815120748412/9636373902";

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private RewardedAd rewardedAd;
    private boolean isLoadingRewardedAd;
    private boolean showRewardedAdWhenReady;

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
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
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

        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());

        if (savedInstanceState == null) {
            webView.loadUrl(READBOOK_URL);
        } else {
            webView.restoreState(savedInstanceState);
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

    private String getRewardedAdUnitId() {
        return PRODUCTION_REWARDED_AD_UNIT_ID;
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
                        if (showRewardedAdWhenReady) {
                            showRewardedAdWhenReady = false;
                            showRewardedAd();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        isLoadingRewardedAd = false;
                        rewardedAd = null;
                        showRewardedAdWhenReady = false;
                        Log.w(TAG, "Rewarded ad failed to load: " + error.getMessage());
                        notifyWeb("readbook:rewarded-unavailable", "{}" );
                    }
                });
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            showRewardedAdWhenReady = true;
            notifyWeb("readbook:rewarded-loading", "{}" );
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
