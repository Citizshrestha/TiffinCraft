package com.tiffincraft.app.activities.customer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ApplyReferralResponse;
import com.tiffincraft.app.models.ReferralInfoResponse;
import com.tiffincraft.app.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Refer & Earn: shows the user's referral code, share/copy actions, credit
 * balance, referral history, and (until used once) an apply-a-code box.
 */
public class ReferEarnActivity extends AppCompatActivity {

    private TextView tvReferralCode, tvTotalReferred, tvCreditsEarned,
            tvReferSubtitle, tvApplyHint, tvStep3, tvReferralsHeader;
    private MaterialCardView cardApplyCode, cardReferralsList;
    private LinearLayout layoutReferralsList;
    private EditText etReferralCode;
    private MaterialButton btnCopyCode, btnShareCode, btnApplyCode;
    private ProgressBar progressBar;
    private View scrollView;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String myCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refer_earn);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.forest_deep));

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        initViews();
        loadReferralInfo();
    }

    private void initViews() {
        tvReferralCode     = findViewById(R.id.tvReferralCode);
        tvTotalReferred    = findViewById(R.id.tvTotalReferred);
        tvCreditsEarned    = findViewById(R.id.tvCreditsEarned);
        tvReferSubtitle    = findViewById(R.id.tvReferSubtitle);
        tvApplyHint        = findViewById(R.id.tvApplyHint);
        tvStep3            = findViewById(R.id.tvStep3);
        tvReferralsHeader  = findViewById(R.id.tvReferralsHeader);
        cardApplyCode      = findViewById(R.id.cardApplyCode);
        cardReferralsList  = findViewById(R.id.cardReferralsList);
        layoutReferralsList = findViewById(R.id.layoutReferralsList);
        etReferralCode     = findViewById(R.id.etReferralCode);
        btnCopyCode        = findViewById(R.id.btnCopyCode);
        btnShareCode       = findViewById(R.id.btnShareCode);
        btnApplyCode       = findViewById(R.id.btnApplyCode);
        progressBar        = findViewById(R.id.progressBar);
        scrollView         = findViewById(R.id.scrollView);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnCopyCode.setOnClickListener(v -> copyCode());
        btnShareCode.setOnClickListener(v -> shareCode());
        btnApplyCode.setOnClickListener(v -> applyCode());
    }

    private void loadReferralInfo() {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyReferralInfo(token).enqueue(new Callback<ReferralInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReferralInfoResponse> call,
                                   @NonNull Response<ReferralInfoResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    populate(response.body());
                } else {
                    Toast.makeText(ReferEarnActivity.this,
                            "Could not load referral details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReferralInfoResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ReferEarnActivity.this,
                        "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populate(ReferralInfoResponse info) {
        myCode = info.getCode() != null ? info.getCode() : "";
        tvReferralCode.setText(myCode.isEmpty() ? "--------" : myCode);
        tvTotalReferred.setText(String.valueOf(info.getTotalReferred()));
        tvCreditsEarned.setText(formatMoney(info.getCredits()));

        String referrer = formatMoney(info.getReferrerReward());
        String referred = formatMoney(info.getReferredReward());
        tvReferSubtitle.setText("You get a " + referrer + " discount, they get a " + referred + " discount on joining");
        tvApplyHint.setText("Enter a friend's code and get a " + referred + " discount");
        tvStep3.setText("You get a " + referrer + " discount, they get a " + referred + " discount — instantly");

        cardApplyCode.setVisibility(info.hasApplied() ? View.GONE : View.VISIBLE);

        layoutReferralsList.removeAllViews();
        boolean hasReferrals = info.getReferrals() != null && !info.getReferrals().isEmpty();
        tvReferralsHeader.setVisibility(hasReferrals ? View.VISIBLE : View.GONE);
        cardReferralsList.setVisibility(hasReferrals ? View.VISIBLE : View.GONE);
        if (hasReferrals) {
            for (int i = 0; i < info.getReferrals().size(); i++) {
                addReferralRow(info.getReferrals().get(i), i < info.getReferrals().size() - 1);
            }
        }
    }

    private void addReferralRow(ReferralInfoResponse.ReferralEntry entry, boolean withDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(16);
        row.setPadding(pad, dp(12), pad, dp(12));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(this);
        name.setText(entry.getFullName() != null ? entry.getFullName() : "A friend");
        name.setTextColor(0xFF222222);
        name.setTextSize(14f);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        left.addView(name);

        TextView date = new TextView(this);
        date.setText(formatDate(entry.getCreatedAt()));
        date.setTextColor(0xFF999999);
        date.setTextSize(11.5f);
        left.addView(date);

        TextView reward = new TextView(this);
        reward.setText("+" + formatMoney(entry.getReward()));
        reward.setTextColor(0xFF4CAF50);
        reward.setTextSize(14f);
        reward.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(left);
        row.addView(reward);
        layoutReferralsList.addView(row);

        if (withDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            lp.setMarginStart(dp(16));
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFFF0F0F0);
            layoutReferralsList.addView(divider);
        }
    }

    private void copyCode() {
        if (myCode.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("TiffinCraft referral code", myCode));
        Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
    }

    private void shareCode() {
        if (myCode.isEmpty()) return;
        String text = "Craving homemade food? 🍱 Join me on TiffinCraft and use my referral code "
                + myCode + " to get a ₹50 discount on your first order!";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share your referral code"));
    }

    private void applyCode() {
        String code = etReferralCode.getText().toString().trim().toUpperCase(Locale.US);
        if (TextUtils.isEmpty(code)) {
            etReferralCode.setError("Enter a referral code");
            return;
        }

        btnApplyCode.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("code", code);

        apiService.applyReferralCode(token, body).enqueue(new Callback<ApplyReferralResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApplyReferralResponse> call,
                                   @NonNull Response<ApplyReferralResponse> response) {
                btnApplyCode.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ReferEarnActivity.this,
                            response.body().getMessage(), Toast.LENGTH_LONG).show();
                    etReferralCode.setText("");
                    loadReferralInfo(); // refresh credits + hide the apply card
                } else {
                    String message = "Could not apply the code.";
                    try {
                        if (response.errorBody() != null) {
                            com.google.gson.JsonObject err = new com.google.gson.JsonParser()
                                    .parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) message = err.get("message").getAsString();
                        }
                    } catch (Exception ignored) { }
                    Toast.makeText(ReferEarnActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApplyReferralResponse> call, @NonNull Throwable t) {
                btnApplyCode.setEnabled(true);
                Toast.makeText(ReferEarnActivity.this,
                        "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String formatMoney(double amount) {
        if (amount == Math.floor(amount)) {
            return "₹" + (long) amount;
        }
        return String.format(Locale.US, "₹%.2f", amount);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            Date d = in.parse(iso.substring(0, 10));
            return d != null ? out.format(d) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
