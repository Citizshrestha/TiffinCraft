package com.tiffincraft.app.activities.cook;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.utils.CookNotificationPreferences;

/**
 * Lets the cook control which push notifications they receive. Preferences are stored
 * on-device (SharedPreferences) and read by the notification/socket layer before
 * showing a toast, sound or badge for each event type.
 */
public class NotificationPreferencesActivity extends AppCompatActivity {

    public static final String PREFS_NAME = CookNotificationPreferences.PREFS_NAME;

    private static final String[] KEYS = {
            CookNotificationPreferences.NEW_ORDERS,
            CookNotificationPreferences.ORDER_STATUS,
            CookNotificationPreferences.CHAT_MESSAGES,
            CookNotificationPreferences.EARNINGS_SUMMARY,
            CookNotificationPreferences.PROMOTIONS
    };
    private static final String[] TITLES = {
            "New Orders", "Order Status Updates", "Chat Messages", "Weekly Earnings Summary", "Promotions & Offers"
    };
    private static final String[] SUBTITLES = {
            "Get notified instantly when a customer places a new order",
            "Alerts when an order is confirmed, preparing, ready or delivered",
            "Notifications for new chat messages from customers",
            "A weekly digest of your earnings and order count",
            "Occasional tips and TiffinCraft announcements"
    };
    private static final boolean[] DEFAULTS = {true, true, true, true, false};

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preferences);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        buildRows();
    }

    private void buildRows() {
        LinearLayout container = findViewById(R.id.layoutPrefs);
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < KEYS.length; i++) {
            String key = KEYS[i];
            boolean defaultValue = DEFAULTS[i];

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
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textColLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textColLp.rightMargin = (int) (12 * density);
            textCol.setLayoutParams(textColLp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(TITLES[i]);
            tvTitle.setTextColor(0xFF111111);
            tvTitle.setTextSize(14.5f);
            tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);

            TextView tvSubtitle = new TextView(this);
            tvSubtitle.setText(SUBTITLES[i]);
            tvSubtitle.setTextColor(0xFF888888);
            tvSubtitle.setTextSize(12f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = (int) (2 * density);
            tvSubtitle.setLayoutParams(subLp);

            textCol.addView(tvTitle);
            textCol.addView(tvSubtitle);

            SwitchCompat sw = new SwitchCompat(this);
            sw.setChecked(prefs.getBoolean(key, defaultValue));
            int[][] switchStates = {
                    new int[] { android.R.attr.state_checked },
                    new int[] { -android.R.attr.state_checked }
            };
            sw.setThumbTintList(new ColorStateList(switchStates,
                    new int[] { 0xFF43AE50, 0xFF9DA39B }));
            sw.setTrackTintList(new ColorStateList(switchStates,
                    new int[] { 0xFFBFE5C3, 0xFFE0E3DE }));
            sw.setContentDescription(TITLES[i]);
            sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                    CookNotificationPreferences.set(this, key, isChecked));
            row.setOnClickListener(v -> sw.toggle());

            row.addView(textCol);
            row.addView(sw);
            card.addView(row);
            container.addView(card);
        }
    }
}
