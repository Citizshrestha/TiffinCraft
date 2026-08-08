package com.tiffincraft.app.activities.order;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.EpayInitiateResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * eSewa ePay v2 checkout — the fallback payment path (see esewaEpayClient.js
 * on the backend for why this exists alongside Intent Payment). Loads eSewa's
 * REAL hosted checkout page inside a WebView by auto-submitting a signed
 * HTML form built server-side, exactly like a browser-based web integration
 * would — no eSewa SDK involved, deprecated or otherwise.
 *
 * When eSewa redirects back, our own backend (/esewa-epay/return) verifies
 * everything and 302s to tiffincraft://payment-result — this Activity just
 * watches for that scheme and hands off to the existing PaymentResultActivity,
 * which re-verifies via the same server-confirmed status poll used by the
 * Intent Payment flow.
 */
public class EsewaEpayCheckoutActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvOpenInBrowser;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int orderId = -1;
    private String transactionUuid;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esewa_epay_checkout);

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId == -1) {
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> handleBack());

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        tvOpenInBrowser = findViewById(R.id.tvOpenInBrowser);
        tvOpenInBrowser.setVisibility(View.GONE);
        tvOpenInBrowser.setOnClickListener(v -> openInExternalBrowser());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // eSewa's login page redirects to our own http:// LAN return URL after
        // payment; without this, WebView can block that as insecure content
        // once the page itself was loaded over https.
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // WebView's default UA carries the "; wv" marker, which Google's
        // reCAPTCHA treats differently (and sometimes refuses) than a normal
        // mobile browser. eSewa's login gates on reCAPTCHA, so present a
        // plain Chrome UA to keep that widget functional.
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv", ""));

        // CRITICAL: WebView blocks third-party cookies by default, but
        // reCAPTCHA needs its own google.com cookies to issue a token — and
        // eSewa's login fails with a generic "Service is currently
        // unavailable" when that token is missing. Session cookies for
        // eSewa's own domain are needed to stay logged in through the flow.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if ("tiffincraft".equals(url.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });

        startCheckout();
    }

    private void handleBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private void startCheckout() {
        String token = "Bearer " + sessionManager.getToken();
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("order_id", orderId);

        apiService.initiateEpayPayment(token, body).enqueue(new Callback<EpayInitiateResponse>() {
            @Override
            public void onResponse(@NonNull Call<EpayInitiateResponse> call, @NonNull Response<EpayInitiateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getFormUrl() != null && response.body().getFields() != null) {
                    EpayInitiateResponse body = response.body();
                    // Saved now (not just after redirect) so PaymentResultActivity
                    // can find this transaction even if the process is killed
                    // while the user is inside eSewa's hosted checkout.
                    sessionManager.savePendingEsewaTransaction(body.getTransactionUuid(), orderId);
                    transactionUuid = body.getTransactionUuid();
                    tvOpenInBrowser.setVisibility(View.VISIBLE);
                    loadAutoSubmitForm(body.getFormUrl(), body.getFields());
                } else {
                    Toast.makeText(EsewaEpayCheckoutActivity.this,
                            "Could not start eSewa checkout. Please try again.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EpayInitiateResponse> call, @NonNull Throwable t) {
                Toast.makeText(EsewaEpayCheckoutActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Hands the same checkout to the real browser. Chrome supports reCAPTCHA
     * properly (WebView is flaky with it), and eSewa's return redirect still
     * comes back to the app via the tiffincraft:// intent-filter, so the
     * flow completes identically. Backend serves an auto-submitting form at
     * this URL because a browser can't be handed a POST body directly.
     */
    private void openInExternalBrowser() {
        if (transactionUuid == null) return;
        String url = RetrofitClient.BASE_URL + "payments/esewa-epay/checkout/" + transactionUuid;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            // Don't finish() — returning here shows the WebView again, and
            // PaymentResultActivity takes over once the browser redirects back.
        } catch (Exception e) {
            Toast.makeText(this, "No browser available to open the checkout.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Builds a hidden HTML form that submits itself on load — the standard
     * way to perform a real cross-origin POST from a WebView, since WebView
     * has no API to load a URL with a POST body + arbitrary form fields directly.
     */
    private void loadAutoSubmitForm(String formUrl, Map<String, String> fields) {
        StringBuilder inputs = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            inputs.append("<input type=\"hidden\" name=\"")
                    .append(escapeHtml(entry.getKey()))
                    .append("\" value=\"")
                    .append(escapeHtml(entry.getValue()))
                    .append("\">");
        }

        String html = "<!DOCTYPE html><html><body onload=\"document.forms[0].submit()\">"
                + "<form method=\"POST\" action=\"" + escapeHtml(formUrl) + "\">"
                + inputs
                + "</form></body></html>";

        Uri formOrigin = Uri.parse(formUrl);
        String baseUrl = formOrigin.getScheme() + "://" + formOrigin.getHost();
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
