package com.tiffincraft.app.activities.cook;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lets a cook set the hours their kitchen accepts orders, per day of the week. */
public class OperatingHoursActivity extends AppCompatActivity {

    private static final String TAG = "OperatingHoursActivity";
    private static final String[] DAYS = {
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    };
    private static final String[] DAY_LABELS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private ApiService apiService;
    private SessionManager sessionManager;
    private LinearLayout layoutDays;

    private final Map<String, TextView> openTimeViews = new LinkedHashMap<>();
    private final Map<String, TextView> closeTimeViews = new LinkedHashMap<>();
    private final Map<String, SwitchCompat> daySwitches = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operating_hours);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        layoutDays = findViewById(R.id.layoutDays);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveHours).setOnClickListener(v -> saveHours());

        buildDayRows();
        loadCurrentHours();
    }

    private void buildDayRows() {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];

            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.topMargin = (int) (10 * density);
            card.setLayoutParams(cardLp);
            card.setRadius(16 * density);
            card.setCardElevation(0);
            card.setStrokeColor(0xFFEFEFEF);
            card.setStrokeWidth((int) density);
            card.setCardBackgroundColor(0xFFFFFFFF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvDay = new TextView(this);
            tvDay.setText(DAY_LABELS[i]);
            tvDay.setTextColor(0xFF111111);
            tvDay.setTextSize(14.5f);
            tvDay.setTypeface(tvDay.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams dayLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvDay.setLayoutParams(dayLp);

            SwitchCompat switchOpen = new SwitchCompat(this);
            switchOpen.setChecked(true);
            switchOpen.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            switchOpen.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFFC8E6C9));

            topRow.addView(tvDay);
            topRow.addView(switchOpen);

            LinearLayout timesRow = new LinearLayout(this);
            timesRow.setOrientation(LinearLayout.HORIZONTAL);
            timesRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams timesRowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timesRowLp.topMargin = (int) (8 * density);
            timesRow.setLayoutParams(timesRowLp);

            TextView tvOpenTime = makeTimeChip(density, "09:00 AM");
            TextView tvDash = new TextView(this);
            tvDash.setText("  to  ");
            tvDash.setTextColor(0xFF999999);
            tvDash.setTextSize(13f);
            TextView tvCloseTime = makeTimeChip(density, "09:00 PM");

            timesRow.addView(tvOpenTime);
            timesRow.addView(tvDash);
            timesRow.addView(tvCloseTime);

            row.addView(topRow);
            row.addView(timesRow);
            card.addView(row);
            layoutDays.addView(card);

            tvOpenTime.setOnClickListener(v -> pickTime(tvOpenTime));
            tvCloseTime.setOnClickListener(v -> pickTime(tvCloseTime));

            switchOpen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                float alpha = isChecked ? 1f : 0.4f;
                timesRow.setAlpha(alpha);
                tvOpenTime.setEnabled(isChecked);
                tvCloseTime.setEnabled(isChecked);
            });

            openTimeViews.put(day, tvOpenTime);
            closeTimeViews.put(day, tvCloseTime);
            daySwitches.put(day, switchOpen);
        }
    }

    private TextView makeTimeChip(float density, String defaultText) {
        TextView tv = new TextView(this);
        tv.setText(defaultText);
        tv.setTextColor(0xFF111111);
        tv.setTextSize(13.5f);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.pill_gray_outline);
        int padH = (int) (14 * density);
        int padV = (int) (8 * density);
        tv.setPadding(padH, padV, padH, padV);
        tv.setClickable(true);
        tv.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void pickTime(TextView target) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        String current = target.getText().toString();
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
            java.util.Date d = fmt.parse(current);
            Calendar parsed = Calendar.getInstance();
            parsed.setTime(d);
            hour = parsed.get(Calendar.HOUR_OF_DAY);
            minute = parsed.get(Calendar.MINUTE);
        } catch (Exception ignored) { }

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, selectedHour);
            c.set(Calendar.MINUTE, selectedMinute);
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
            target.setText(fmt.format(c.getTime()));
        }, hour, minute, false).show();
    }

    private void loadCurrentHours() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMyCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call, @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CookProfile profile = response.body().getProfile();
                    if (profile != null && profile.getOperatingHours() != null) {
                        applyHours(profile.getOperatingHours());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load operating hours", t);
            }
        });
    }

    private void applyHours(Map<String, CookProfile.DayHours> hours) {
        java.text.SimpleDateFormat in24 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        java.text.SimpleDateFormat out12 = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);

        for (String day : DAYS) {
            CookProfile.DayHours dh = hours.get(day);
            if (dh == null) continue;

            SwitchCompat sw = daySwitches.get(day);
            if (sw != null) sw.setChecked(dh.isOpen());

            try {
                if (dh.getOpen() != null) {
                    openTimeViews.get(day).setText(out12.format(in24.parse(dh.getOpen())));
                }
                if (dh.getClose() != null) {
                    closeTimeViews.get(day).setText(out12.format(in24.parse(dh.getClose())));
                }
            } catch (Exception ignored) { }
        }
    }

    private void saveHours() {
        java.text.SimpleDateFormat in12 = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
        java.text.SimpleDateFormat out24 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);

        JsonObject hoursObj = new JsonObject();
        try {
            for (String day : DAYS) {
                JsonObject dayObj = new JsonObject();
                String openTime = out24.format(in12.parse(openTimeViews.get(day).getText().toString()));
                String closeTime = out24.format(in12.parse(closeTimeViews.get(day).getText().toString()));
                dayObj.addProperty("open", openTime);
                dayObj.addProperty("close", closeTime);
                dayObj.addProperty("isOpen", daySwitches.get(day).isChecked());
                hoursObj.add(day, dayObj);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.add("operating_hours", hoursObj);

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateOperatingHours(token, body).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call, @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OperatingHoursActivity.this, "Operating hours saved", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(OperatingHoursActivity.this, "Failed to save operating hours", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                Toast.makeText(OperatingHoursActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
