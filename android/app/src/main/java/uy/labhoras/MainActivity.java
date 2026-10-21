package uy.labhoras;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Contenedor nativo de la app web (carpeta /web, copiada a assets).
 * Los datos se guardan en el localStorage del WebView, que persiste entre aperturas.
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/index.html";

    private WebView web;
    private FrameLayout root;
    private float insetTopDp = 0f;
    private float insetBottomDp = 0f;
    private boolean pageReady = false;
    private boolean pendingPunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setDecorFitsSystemWindows(false);

        int bg = getColor(R.color.bg);
        root = new FrameLayout(this);
        root.setBackgroundColor(bg);

        web = new WebView(this);
        web.setBackgroundColor(bg);
        web.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        applyBarAppearance();
        setupInsets();
        setupWebView();
        setupBack();

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            pendingPunch = isPunch(getIntent());
            web.loadUrl(START_URL);
        }
    }

    /* ---------- Reloj de marcar: atajo, botón rápido y notificación ---------- */

    private static boolean isPunch(Intent i) {
        return i != null && Clock.ACTION_PUNCH.equals(i.getAction());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!isPunch(intent)) return;
        if (pageReady) triggerPunch();
        else pendingPunch = true;
    }

    private void triggerPunch() {
        if (web != null) web.evaluateJavascript("window.__labhorasPunch && window.__labhorasPunch()", null);
    }

    private void sendAuthResult(String id, boolean ok, String msg) {
        if (web == null) return;
        web.evaluateJavascript("window.__labhorasAuthResult && window.__labhorasAuthResult("
                + JSONObject.quote(id) + "," + ok + "," + JSONObject.quote(msg) + ")", null);
    }

    /** Huella (o PIN/patrón del teléfono como alternativa) con el diálogo del sistema. */
    private void authenticate(String title, String subtitle, String id) {
        try {
            showPrompt(title, subtitle, id);
        } catch (Throwable t) {
            // Nunca cerrar la app por un problema con el lector: se informa en pantalla.
            sendAuthResult(id, false, "No se pudo usar el lector de huella (" + t.getClass().getSimpleName() + ").");
        }
    }

    private void showPrompt(String title, String subtitle, String id) {
        int allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        BiometricManager bm = getSystemService(BiometricManager.class);
        if (bm == null || bm.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            sendAuthResult(id, false, "Configurá una huella o un bloqueo de pantalla en el teléfono para poder marcar.");
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowed)
                .build();
        prompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                sendAuthResult(id, true, "");
            }

            @Override
            public void onAuthenticationError(int code, CharSequence err) {
                boolean cancelled = code == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        || code == BiometricPrompt.BIOMETRIC_ERROR_CANCELED;
                sendAuthResult(id, false, cancelled ? "Marcación cancelada" : String.valueOf(err));
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private boolean isNight() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyBarAppearance() {
        WindowInsetsController c = getWindow().getInsetsController();
        if (c == null) return;
        int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        c.setSystemBarsAppearance(isNight() ? 0 : mask, mask);
    }

    /** Pasa los márgenes de las barras del sistema a la página y deja lugar al teclado. */
    private void setupInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            boolean imeVisible = insets.isVisible(WindowInsets.Type.ime());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, 0, bars.right, imeVisible ? ime.bottom : 0);
            float d = getResources().getDisplayMetrics().density;
            insetTopDp = bars.top / d;
            insetBottomDp = imeVisible ? 0f : bars.bottom / d;
            pushInsets();
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
    }

    private void pushInsets() {
        if (web == null) return;
        String js = String.format(Locale.US,
                "(function(s){s.setProperty('--inset-top','%.1fpx');s.setProperty('--inset-bottom','%.1fpx');})(document.documentElement.style);",
                insetTopDp, insetBottomDp);
        web.evaluateJavascript(js, null);
    }

    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                pushInsets();
                if (pendingPunch) {
                    pendingPunch = false;
                    triggerPunch();
                }
            }
        });

        web.addJavascriptInterface(new Bridge(), "LabHoras");
    }

    /** Atrás: primero cierra una hoja abierta en la página; si no hay, sale. */
    private void setupBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
    }

    private void handleBack() {
        web.evaluateJavascript("window.__labhorasBack ? window.__labhorasBack() : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.onBackPressed();
        } else {
            handleBack();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Funciones nativas disponibles para la página como window.LabHoras. */
    private class Bridge {
        @JavascriptInterface
        public void share(String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, getString(R.string.share_title)));
            });
        }

        @JavascriptInterface
        public void authenticate(String title, String subtitle, String id) {
            runOnUiThread(() -> MainActivity.this.authenticate(title, subtitle, id));
        }

        /** startMillis: hora de entrada en milisegundos, o "0" si no hay entrada abierta. */
        @JavascriptInterface
        public void setClockState(String startMillis) {
            long start;
            try {
                start = Long.parseLong(startMillis);
            } catch (NumberFormatException e) {
                start = 0L;
            }
            final long s = start;
            runOnUiThread(() -> {
                try {
                    Clock.setState(getApplicationContext(), s);
                } catch (Throwable ignored) {
                    // La notificación es opcional; la marca ya quedó guardada en la página.
                }
            });
        }

        @JavascriptInterface
        public void requestNotifications() {
            runOnUiThread(() -> {
                try {
                    requestNotificationPermission();
                } catch (Throwable ignored) {
                    // Sin permiso solo se pierde el aviso.
                }
            });
        }

        @JavascriptInterface
        public boolean copy(String text) {
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
