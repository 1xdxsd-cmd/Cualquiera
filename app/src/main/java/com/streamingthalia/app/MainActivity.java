package com.streamingthalia.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.graphics.Color;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;

    // ============================================
    // ⚠️ CAMBIA ESTA URL A LA DE TU SERVIDOR ⚠️
    // ============================================
    private static final String SERVER_URL = "http://TU_IP_AQUI:8081";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.parseColor("#0a0a0f"));
        getWindow().setNavigationBarColor(Color.parseColor("#0a0a0f"));

        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.parseColor("#0a0a0f"));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 6);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(100);

        webView = new WebView(this);
        FrameLayout.LayoutParams wvParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(wvParams);
        webView.setBackgroundColor(Color.parseColor("#0a0a0f"));

        layout.addView(webView);
        layout.addView(progressBar);
        setContentView(layout);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(SERVER_URL) || url.startsWith("http://") && url.contains(SERVER_URL.replace("http://", ""))) {
                    view.loadUrl(url);
                    return true;
                }
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String html = "<!DOCTYPE html><html><head>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<style>"
                    + "*{margin:0;padding:0;box-sizing:border-box}"
                    + "body{background:#0a0a0f;color:#fff;font-family:monospace;display:flex;"
                    + "align-items:center;justify-content:center;min-height:100vh;text-align:center;padding:20px}"
                    + ".i{font-size:4rem;margin-bottom:20px}"
                    + ".t{font-size:1.5rem;color:#ff006e;margin-bottom:15px}"
                    + ".m{color:#a0a0b0;font-size:.9rem;margin-bottom:25px}"
                    + ".b{display:inline-block;padding:15px 30px;background:linear-gradient(135deg,#00f3ff,#8b5cf6);"
                    + "color:#0a0a0f;border-radius:50px;font-weight:700;text-decoration:none;font-size:.9rem}"
                    + "</style></head><body><div>"
                    + "<div class='i'>📡</div>"
                    + "<div class='t'>Sin Conexión</div>"
                    + "<div class='m'>No se puede conectar al servidor.<br>Verifica tu internet y que el servidor esté encendido.</div>"
                    + "<a class='b' href='" + SERVER_URL + "'>🔄 Reintentar</a>"
                    + "</div></body></html>";
                view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) progressBar.setVisibility(View.GONE);
            }
        });

        webView.loadUrl(SERVER_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }
}
