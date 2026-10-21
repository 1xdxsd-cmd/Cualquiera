package com.streamingthalia.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    // ============================================
    // ⚠️ CAMBIA ESTA URL A LA DE TU SERVIDOR ⚠️
    // ============================================
    private static final String SERVER_URL = "http://144.126.137.93:8081/";

    private static final int BG = Color.parseColor("#0a0a0f");
    private static final int CYAN = Color.parseColor("#00f3ff");
    private static final int VIOLET = Color.parseColor("#8b5cf6");
    private static final int PINK = Color.parseColor("#ff3d9a");

    // Same motion tokens as the web pages: strong ease-out for enter/exit, ease-in-out for loops
    private static final Interpolator EASE_OUT = new PathInterpolator(0.23f, 1f, 0.32f, 1f);
    private static final Interpolator EASE_IN_OUT = new PathInterpolator(0.77f, 0f, 0.175f, 1f);

    // The splash stays at least this long so the logo is seen, and at most this long on a slow network
    private static final long SPLASH_MIN_MS = 900;
    private static final long SPLASH_MAX_MS = 10000;

    private FrameLayout root;
    private WebView webView;
    private View progressBar;
    private FrameLayout splash;
    private View splashGlow;
    private ImageView splashLogo;
    private View splashIndicator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long splashShownAt;
    private boolean splashGone = false;
    private int barColor = BG;
    private ValueAnimator barAnimator;
    private ValueAnimator progressAnimator;
    private ObjectAnimator glowLoop;
    private ObjectAnimator indicatorLoop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(BG);
        webView.setAlpha(0f); // revealed when the splash leaves

        progressBar = buildProgressBar();
        splash = buildSplash();

        root.addView(webView);
        root.addView(progressBar);
        root.addView(splash);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Lets the pages tell the app their theme color, so the system bars follow the admin's theme
        webView.addJavascriptInterface(new Bridge(), "ThaliaApp");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(SERVER_URL) || url.startsWith("http://") && url.contains(SERVER_URL.replace("http://", ""))) {
                    view.loadUrl(url);
                    return true;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // Navigating between pages: dim the old page while the next one loads
                if (splashGone) {
                    webView.animate().alpha(0.55f).setDuration(160).setInterpolator(EASE_OUT).start();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                watchThemeColor(view);
                if (!splashGone) {
                    long wait = Math.max(0, SPLASH_MIN_MS - (SystemClock.uptimeMillis() - splashShownAt));
                    handler.postDelayed(MainActivity.this::hideSplash, wait);
                } else {
                    webView.animate().alpha(1f).setDuration(240).setInterpolator(EASE_OUT).start();
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                animateBars(BG);
                view.loadDataWithBaseURL(null, errorPage(), "text/html", "UTF-8", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                updateProgress(newProgress);
            }
        });

        startSplash();
        webView.loadUrl(SERVER_URL);
    }

    // ───────────────────────── SPLASH ─────────────────────────

    private FrameLayout buildSplash() {
        FrameLayout s = new FrameLayout(this);
        s.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        s.setBackgroundColor(BG);
        s.setClickable(true); // swallow taps while it's up

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        s.addView(col, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // logo with a soft glow that breathes behind it
        FrameLayout stack = new FrameLayout(this);
        col.addView(stack, new LinearLayout.LayoutParams(dp(260), dp(260)));

        splashGlow = new View(this);
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(dp(130));
        glow.setColors(new int[]{withAlpha(PINK, 0.38f), withAlpha(VIOLET, 0.16f), Color.TRANSPARENT});
        splashGlow.setBackground(glow);
        stack.addView(splashGlow, new FrameLayout.LayoutParams(dp(260), dp(260), Gravity.CENTER));

        splashLogo = new ImageView(this);
        splashLogo.setImageResource(R.drawable.splash_logo);
        splashLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable clip = new GradientDrawable();
        clip.setCornerRadius(dp(36));
        clip.setColor(BG);
        splashLogo.setBackground(clip);
        splashLogo.setClipToOutline(true);
        splashLogo.setElevation(dp(12));
        stack.addView(splashLogo, new FrameLayout.LayoutParams(dp(176), dp(171), Gravity.CENTER));

        // thin loading track with a gradient segment sweeping across
        FrameLayout track = new FrameLayout(this);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setCornerRadius(dp(2));
        trackBg.setColor(Color.argb(28, 255, 255, 255));
        track.setBackground(trackBg);
        track.setClipToOutline(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(132), dp(3));
        tp.topMargin = dp(18);
        col.addView(track, tp);

        splashIndicator = new View(this);
        GradientDrawable seg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.TRANSPARENT, CYAN, VIOLET, Color.TRANSPARENT});
        seg.setCornerRadius(dp(2));
        splashIndicator.setBackground(seg);
        track.addView(splashIndicator, new FrameLayout.LayoutParams(dp(56), dp(3)));

        TextView label = new TextView(this);
        label.setText("Cargando");
        label.setTextColor(Color.parseColor("#9090b8"));
        label.setTextSize(12);
        label.setLetterSpacing(0.18f);
        label.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        col.addView(label, lp);

        // starting state for the entrance
        splashLogo.setAlpha(0f);
        splashLogo.setScaleX(0.9f);
        splashLogo.setScaleY(0.9f);
        splashGlow.setAlpha(0f);
        track.setAlpha(0f);
        label.setAlpha(0f);
        track.setTag("fadeIn");
        label.setTag("fadeIn");
        return s;
    }

    private void startSplash() {
        splashShownAt = SystemClock.uptimeMillis();

        // entrance: logo rises out of a slight scale, glow and loader follow
        splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(EASE_OUT).start();
        splashGlow.animate().alpha(1f).setDuration(800).setStartDelay(150).setInterpolator(EASE_OUT).start();
        ViewGroup col = (ViewGroup) splashLogo.getParent().getParent();
        for (int i = 0; i < col.getChildCount(); i++) {
            View v = col.getChildAt(i);
            if ("fadeIn".equals(v.getTag())) {
                v.animate().alpha(1f).setDuration(400).setStartDelay(350).setInterpolator(EASE_OUT).start();
            }
        }

        // the "alive while waiting" loops only run when the system allows animations
        if (animationsEnabled()) {
            glowLoop = ObjectAnimator.ofFloat(splashGlow, View.SCALE_X, 0.92f, 1.08f);
            glowLoop.setDuration(1600);
            glowLoop.setRepeatMode(ValueAnimator.REVERSE);
            glowLoop.setRepeatCount(ValueAnimator.INFINITE);
            glowLoop.setInterpolator(EASE_IN_OUT);
            glowLoop.addUpdateListener(a -> splashGlow.setScaleY((float) a.getAnimatedValue()));
            glowLoop.start();

            indicatorLoop = ObjectAnimator.ofFloat(splashIndicator, View.TRANSLATION_X, -dp(56), dp(132));
            indicatorLoop.setDuration(1100);
            indicatorLoop.setRepeatCount(ValueAnimator.INFINITE);
            indicatorLoop.setInterpolator(EASE_IN_OUT);
            indicatorLoop.start();
        }

        // never keep people on the splash forever
        handler.postDelayed(this::hideSplash, SPLASH_MAX_MS);
    }

    private void hideSplash() {
        if (splashGone) return;
        splashGone = true;
        handler.removeCallbacksAndMessages(null);

        webView.animate().alpha(1f).setDuration(380).setInterpolator(EASE_OUT).start();
        splashLogo.animate().scaleX(1.06f).scaleY(1.06f).setDuration(320).setInterpolator(EASE_OUT).start();
        splash.animate().alpha(0f).setDuration(320).setInterpolator(EASE_OUT)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (glowLoop != null) glowLoop.cancel();
                    if (indicatorLoop != null) indicatorLoop.cancel();
                    root.removeView(splash);
                }
            }).start();
    }

    // ───────────────────────── PROGRESS BAR ─────────────────────────

    private View buildProgressBar() {
        View bar = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{CYAN, VIOLET, PINK});
        bar.setBackground(g);
        bar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP));
        bar.setPivotX(0f);
        bar.setScaleX(0f);
        bar.setAlpha(0f);
        return bar;
    }

    /** Glides the bar to the new value instead of jumping, then fades it away at 100%. */
    private void updateProgress(int newProgress) {
        float target = newProgress / 100f;
        if (progressBar.getAlpha() == 0f && newProgress < 100) {
            progressBar.setScaleX(0f);
            progressBar.animate().cancel();
            progressBar.setAlpha(1f);
        }
        if (progressAnimator != null) progressAnimator.cancel();
        progressAnimator = ValueAnimator.ofFloat(progressBar.getScaleX(), target);
        progressAnimator.setDuration(220);
        progressAnimator.setInterpolator(EASE_OUT);
        progressAnimator.addUpdateListener(a -> progressBar.setScaleX((float) a.getAnimatedValue()));
        if (newProgress >= 100) {
            progressAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.animate().alpha(0f).setStartDelay(120).setDuration(260).setInterpolator(EASE_OUT).start();
                }
            });
        }
        progressAnimator.start();
    }

    // ───────────────────────── SYSTEM BARS FOLLOW THE THEME ─────────────────────────

    /** Reads the page's <meta name="theme-color"> now and every time the theme changes it. */
    private void watchThemeColor(WebView view) {
        view.evaluateJavascript(
            "(function(){if(window.__thBar)return;window.__thBar=1;" +
            "function send(){var m=document.querySelector('meta[name=\"theme-color\"]');" +
            "if(m&&window.ThaliaApp)ThaliaApp.setBarColor(m.content);}" +
            "send();var m=document.querySelector('meta[name=\"theme-color\"]');" +
            "if(m)new MutationObserver(send).observe(m,{attributes:true,attributeFilter:['content']});})();",
            null);
    }

    private class Bridge {
        @JavascriptInterface
        public void setBarColor(final String css) {
            runOnUiThread(() -> {
                try {
                    animateBars(Color.parseColor(css.trim()));
                } catch (Exception ignored) { }
            });
        }
    }

    /** Cross-fades status bar, nav bar and the window background to the page's color. */
    private void animateBars(int target) {
        if (target == barColor) return;
        if (barAnimator != null) barAnimator.cancel();
        barAnimator = ValueAnimator.ofArgb(barColor, target);
        barAnimator.setDuration(450);
        barAnimator.setInterpolator(EASE_OUT);
        barAnimator.addUpdateListener(a -> {
            int c = (int) a.getAnimatedValue();
            barColor = c;
            getWindow().setStatusBarColor(c);
            getWindow().setNavigationBarColor(c);
            root.setBackgroundColor(c);
        });
        barAnimator.start();
        setLightBars(luminance(target) > 0.6f);
    }

    /** Light themes (pastel, rosa chic, neutros) need dark status/nav icons to stay readable. */
    @SuppressWarnings("deprecation")
    private void setLightBars(boolean light) {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = light ? (flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = light ? (flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : (flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        decor.setSystemUiVisibility(flags);
    }

    // ───────────────────────── OFFLINE PAGE ─────────────────────────

    /** Animated offline screen that matches the site: no emoji, pulsing signal, auto-retry countdown. */
    private String errorPage() {
        return "<!DOCTYPE html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "*{margin:0;padding:0;box-sizing:border-box}"
            + "html{-webkit-tap-highlight-color:transparent}"
            + "body{background:#0a0a0f;color:#eef0ff;font-family:system-ui,-apple-system,Roboto,sans-serif;"
            + "display:flex;align-items:center;justify-content:center;min-height:100vh;text-align:center;padding:24px}"
            + ".w{max-width:340px;animation:up .6s cubic-bezier(.16,1,.3,1) both}"
            + ".sig{position:relative;width:96px;height:96px;margin:0 auto 26px;display:grid;place-items:center;color:#ff3d9a}"
            + ".sig svg{width:40px;height:40px;position:relative;z-index:1}"
            + ".sig i{position:absolute;inset:0;border-radius:50%;border:1px solid rgba(255,61,154,.5);animation:ring 2.4s cubic-bezier(.16,1,.3,1) infinite}"
            + ".sig i:nth-child(2){animation-delay:.8s}.sig i:nth-child(3){animation-delay:1.6s}"
            + ".sig::before{content:'';position:absolute;inset:18px;border-radius:50%;background:rgba(255,61,154,.1);border:1px solid rgba(255,61,154,.3)}"
            + ".t{font-size:1.45rem;font-weight:600;letter-spacing:-.02em;margin-bottom:10px}"
            + ".m{color:#9090b8;font-size:.95rem;line-height:1.6;margin-bottom:28px}"
            + ".b{display:inline-flex;align-items:center;gap:10px;padding:15px 28px;border-radius:14px;"
            + "background:linear-gradient(135deg,#00f3ff,#8b5cf6);color:#0a0a0f;font-weight:600;font-size:.95rem;"
            + "text-decoration:none;transition:transform .12s cubic-bezier(.16,1,.3,1);touch-action:manipulation}"
            + ".b:active{transform:scale(.96)}.b svg{width:18px;height:18px}"
            + ".c{margin-top:16px;font-family:monospace;font-size:.78rem;color:#7c7ca6;letter-spacing:.06em}"
            + "@keyframes up{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:none}}"
            + "@keyframes ring{from{transform:scale(.6);opacity:.9}to{transform:scale(1.25);opacity:0}}"
            + "@media (prefers-reduced-motion:reduce){.w,.sig i{animation:none}.sig i{opacity:0}}"
            + "</style></head><body><div class='w'>"
            + "<div class='sig'><i></i><i></i><i></i>"
            + "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<path d='M2 8.82a15 15 0 0 1 4.17-2.65M10.66 5c4.01-.36 8.14.9 11.34 3.76M16.85 11.25a10 10 0 0 1 2.22 1.68M5 13a10 10 0 0 1 5.24-2.76M8.5 16.43a5 5 0 0 1 7 0M12 20h.01M2 2l20 20'/></svg></div>"
            + "<div class='t'>Sin conexión</div>"
            + "<div class='m'>No se puede conectar al servidor.<br>Verifica tu internet y que el servidor esté encendido.</div>"
            + "<a class='b' href='" + SERVER_URL + "'>"
            + "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<path d='M21 12a9 9 0 0 1-15.5 6.2L3 16'/><path d='M3 21v-5h5'/><path d='M3 12a9 9 0 0 1 15.5-6.2L21 8'/><path d='M21 3v5h-5'/></svg>"
            + "Reintentar</a>"
            + "<div class='c' id='c'>Reintentando en 10 s</div>"
            + "</div><script>var n=10,c=document.getElementById('c');"
            + "var t=setInterval(function(){n--;if(n<=0){clearInterval(t);c.textContent='Reintentando…';location.href='" + SERVER_URL + "';}"
            + "else c.textContent='Reintentando en '+n+' s';},1000);</script></body></html>";
    }

    // ───────────────────────── HELPERS ─────────────────────────

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static float luminance(int c) {
        return (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f;
    }

    /** Respects the system "remove animations" setting (Developer options / Accessibility). */
    private static boolean animationsEnabled() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled();
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

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (glowLoop != null) glowLoop.cancel();
        if (indicatorLoop != null) indicatorLoop.cancel();
        super.onDestroy();
    }
}
