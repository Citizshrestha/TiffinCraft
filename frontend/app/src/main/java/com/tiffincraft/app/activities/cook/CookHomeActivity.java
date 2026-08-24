package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.DashboardResponse;
import com.tiffincraft.app.models.DashboardStats;
import com.tiffincraft.app.models.EarningsSummaryResponse;
import com.tiffincraft.app.models.MonthlyBreakdown;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ChatPanelManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.SocketManager;
import com.tiffincraft.app.views.EarningsMarkerView;

import org.json.JSONObject;

import java.text.DecimalFormat;

import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookHomeActivity extends AppCompatActivity {

    private static final String TAG = "CookHomeActivity";

    private ImageView imgCookProfilePic;
    private TextView tvKitchenName, tvWelcome;
    private TextView tvTodayOrders, tvTodayEarnings, tvActiveSubscriptions, tvAvgRating;
    private TextView tvNotificationBadge, tvEarningsPeriod;
    private TextView tvViewAllOrders, tvNoOrdersToday;
    private LineChart chartEarningsTrend;
    private androidx.recyclerview.widget.RecyclerView rvTodayOrders;
    private com.tiffincraft.app.adapters.TodayOrderAdapter todayOrderAdapter;
    private final java.util.List<com.tiffincraft.app.models.Order> todayOrders = new java.util.ArrayList<>();
    private View btnAddMeal, btnManageMeals, btnViewOrders, btnSubscriptions, btnCombos;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;
    private SocketManager socketManager;
    private ChatPanelManager chatPanelManager;

    // Earnings data storage
    private double todayEarnings = 0;
    private double thisWeekEarnings = 0;
    private double thisMonthEarnings = 0;
    private int currentEarningsPeriod = 1; // 0=Today, 1=This Week, 2=This Month

    // Earnings Trend chart range toggle
    private TextView tvTrendRange, tvTrendSubtitle;
    private int trendRange = 2; // 0=Week (7d), 1=Month (30d), 2=Year (12mo)
    private java.util.List<com.tiffincraft.app.models.WeeklyBreakdown> trendWeekly;
    private java.util.List<com.tiffincraft.app.models.WeeklyBreakdown> trendDaily30;
    private java.util.List<MonthlyBreakdown> trendMonthly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_cook_home);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            sessionManager = new SessionManager(this);
            socketManager = SocketManager.getInstance(this);

            initViews();
            loadUserData();
            setupListeners();
            setupBottomNavigation();
            setupSocketListeners();
            applyEntranceAnimations();
            setupBackPressHandler();

            // Click on notification bell
            findViewById(R.id.btnNotifications).setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
            });

            // Chat: layout already has its own fabChat button, so attach the
            // panel without inflating a second floating button.
            chatPanelManager = ChatPanelManager.attach(this, false);
            findViewById(R.id.fabChat).setOnClickListener(v -> chatPanelManager.open());

            // Check if launched via FCM notification with approval intent
            if (getIntent().getBooleanExtra("show_approval_dialog", false)) {
                getIntent().removeExtra("show_approval_dialog");
                // Post to the end of the queue so all views are laid out
                findViewById(android.R.id.content).post(() -> showApprovalDialog(true));
            }

            requestNotificationPermissionIfNeeded();

            Log.d(TAG, "CookHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    /** Android 13+ requires runtime consent before chat notifications can be shown. */
    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                   != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 9001);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        fetchUnreadNotifications();

        // Refresh stats, earnings and today's orders with real data every time we return here
        fetchDashboardData();
        fetchEarningsData();
        fetchTodayOrders();

        // Connect socket and join cook room
        connectSocket();
        // Re-claim the order events for this screen (SocketManager keeps one
        // listener per event; another screen may have taken them over).
        setupSocketListeners();

        if (chatPanelManager != null) {
            chatPanelManager.refreshUnreadBadge();
        }
    }

    // Note: no socket disconnect in onPause — SocketManager is an app-wide
    // singleton, so disconnecting here killed real-time chat/order updates for
    // every other screen. The socket now lives until logout.

    private void initViews() {
        imgCookProfilePic = findViewById(R.id.imgCookProfilePic);
        tvKitchenName = findViewById(R.id.tvKitchenName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTodayOrders = findViewById(R.id.tvTodayOrders);
        tvTodayEarnings = findViewById(R.id.tvTodayEarnings);
        tvActiveSubscriptions = findViewById(R.id.tvActiveSubscriptions);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvEarningsPeriod = findViewById(R.id.tvEarningsPeriod);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        View fabAddTodayMeal = findViewById(R.id.fabAddTodayMeal);
        if (fabAddTodayMeal != null) {
            fabAddTodayMeal.setOnClickListener(v ->
                    startActivity(new Intent(CookHomeActivity.this, AddMenuActivity.class)));
        }
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        chartEarningsTrend = findViewById(R.id.chartEarningsTrend);
        tvTrendRange = findViewById(R.id.tvTrendRange);
        tvTrendSubtitle = findViewById(R.id.tvTrendSubtitle);
        if (tvTrendRange != null) {
            tvTrendRange.setOnClickListener(v -> {
                trendRange = (trendRange + 1) % 3;
                renderTrendChart();
            });
        }
        setupEarningsTrendChart();
        tvViewAllOrders = findViewById(R.id.tvViewAllOrders);
        tvNoOrdersToday = findViewById(R.id.tvNoOrdersToday);
        rvTodayOrders = findViewById(R.id.rvTodayOrders);

        todayOrderAdapter = new com.tiffincraft.app.adapters.TodayOrderAdapter(this, todayOrders, order -> {
            Intent intent = new Intent(this, com.tiffincraft.app.activities.order.OrderDetailsCookActivity.class);
            intent.putExtra("order_id", order.getId());
            startActivity(intent);
        });
        rvTodayOrders.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvTodayOrders.setAdapter(todayOrderAdapter);

        tvViewAllOrders.setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrdersActivity.class)));

        btnManageMeals = findViewById(R.id.btnManageMeals);
        btnViewOrders = findViewById(R.id.btnViewOrders);
        btnSubscriptions = findViewById(R.id.btnSubscriptions);
        btnCombos = findViewById(R.id.btnCombos);

        btnManageMeals.setOnClickListener(v ->
                startActivity(new Intent(this, CookMealActivity.class)));
        btnViewOrders.setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrdersActivity.class)));
        btnSubscriptions.setOnClickListener(v ->
                startActivity(new Intent(this, CookSubscriptionsActivity.class)));
        btnCombos.setOnClickListener(v ->
                startActivity(new Intent(this, CookCombosActivity.class)));
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvKitchenName.setText(fullName);
            tvWelcome.setText("Hello, " + fullName.split(" ")[0] + "! 👋");
        } else {
            tvKitchenName.setText("Home Cook");
            tvWelcome.setText("Hello! 👋");
        }

        // Load profile picture using same method as CookProfileActivity
        String profileImageUrl = sessionManager.getProfileImage();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            String fullUrl = getFullImageUrl(profileImageUrl);
            if (fullUrl != null) {
                Log.d(TAG, "Loading profile image: " + fullUrl);

                GlideUrl glideUrl = new GlideUrl(fullUrl, new LazyHeaders.Builder()
                    .addHeader("Bypass-Tunnel-Reminder", "true")
                    .build());

                Glide.with(this)
                    .load(glideUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgCookProfilePic);
            } else {
                Glide.with(this)
                    .load(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgCookProfilePic);
            }
        } else {
            // Default avatar
            Glide.with(this)
                .load(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(imgCookProfilePic);
        }

        // Fetch real dashboard data from backend
        fetchDashboardData();
        fetchEarningsData();
    }

    /**
     * Build full URL from relative path returned by backend
     */
    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return RetrofitClient.SERVER_URL + imageUrl;
    }

    /**
     * Fetch real dashboard statistics from backend API
     */
    private void fetchDashboardData() {
        String token = "Bearer " + sessionManager.getToken();

        Log.d(TAG, "=== Fetching Dashboard Data ===");
        Log.d(TAG, "Token exists: " + (sessionManager.getToken() != null));
        Log.d(TAG, "User ID: " + sessionManager.getUserId());
        Log.d(TAG, "User Role: " + sessionManager.getRole());

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getCookDashboard(token).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call,
                                   @NonNull Response<DashboardResponse> response) {
                Log.d(TAG, "=== Dashboard Response Received ===");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {

                    DashboardStats stats = response.body().getDashboard();
                    Log.d(TAG, "✅ Dashboard data received");
                    if (stats != null) {
                        Log.d(TAG, "  - Today orders: " + (stats.getTodayOrders() != null ? stats.getTodayOrders().getCount() : "null"));
                        Log.d(TAG, "  - Today earnings: ₹" + (stats.getTodayEarnings() != null ? stats.getTodayEarnings().getAmount() : "null"));
                        Log.d(TAG, "  - Active orders: " + (stats.getActiveOrders() != null ? stats.getActiveOrders().getCount() : "null"));
                        Log.d(TAG, "  - Average rating: " + (stats.getAverageRating() != null ? stats.getAverageRating().getRating() : "null"));
                    }
                    updateDashboardUI(stats);

                } else {
                    Log.e(TAG, "❌ Failed to fetch dashboard: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                    }
                    // Show default values on error
                    setDefaultDashboardValues();
                    Toast.makeText(CookHomeActivity.this,
                            "Could not load dashboard: Code " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Dashboard API call failed", t);
                Log.e(TAG, "Error message: " + t.getMessage());
                Log.e(TAG, "Error class: " + t.getClass().getName());
                Log.e(TAG, "Cause: " + (t.getCause() != null ? t.getCause().getMessage() : "null"));

                // Show default values on error
                setDefaultDashboardValues();

                String errorMessage = "Could not load dashboard";
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMessage = "Cannot connect to server";
                } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMessage = "Connection timeout";
                }

                Toast.makeText(CookHomeActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Update UI with real dashboard statistics
     */
    private void updateDashboardUI(DashboardStats stats) {
        if (stats == null) {
            setDefaultDashboardValues();
            return;
        }

        // Update today's orders
        if (stats.getTodayOrders() != null) {
            tvTodayOrders.setText(String.valueOf(stats.getTodayOrders().getCount()));
            bindChangeChip(R.id.tvOrdersChange, stats.getTodayOrders().getChangePercentage(), "%");
            bindVsLabel(R.id.tvOrdersVsLabel, stats.getTodayOrders().getVsLabel());
        }

        // Update today's earnings
        if (stats.getTodayEarnings() != null) {
            DecimalFormat formatter = new DecimalFormat("#,##0");
            String earnings = "₹" + formatter.format(stats.getTodayEarnings().getAmount());
            tvTodayEarnings.setText(earnings);
            bindChangeChip(R.id.tvEarningsChange, stats.getTodayEarnings().getChangePercentage(), "%");
            bindVsLabel(R.id.tvEarningsVsLabel, stats.getTodayEarnings().getVsLabel());
        }

        // Update active subscriptions (using active orders count)
        if (stats.getActiveOrders() != null) {
            tvActiveSubscriptions.setText(String.valueOf(stats.getActiveOrders().getCount()));
            bindChangeChip(R.id.tvSubscriptionsChange, stats.getActiveOrders().getChangePercentage(), "%");
            bindVsLabel(R.id.tvSubscriptionsVsLabel, stats.getActiveOrders().getVsLabel());
        }

        // Update average rating
        if (stats.getAverageRating() != null) {
            DecimalFormat ratingFormatter = new DecimalFormat("0.0");
            String rating = ratingFormatter.format(stats.getAverageRating().getRating());
            tvAvgRating.setText(rating);
            bindChangeChip(R.id.tvRatingChange, stats.getAverageRating().getChangeValue(), "");
            bindVsLabel(R.id.tvRatingVsLabel, stats.getAverageRating().getVsLabel());
        }

        Log.d(TAG, "Dashboard UI updated with real data");
    }

    /** Show a real "↑ 12.5%" / "↓ 3%" change chip, green for up, red for down, hidden when flat. */
    private void bindChangeChip(int viewId, double change, String suffix) {
        TextView chip = findViewById(viewId);
        if (chip == null) return;
        DecimalFormat fmt = new DecimalFormat("0.#");
        if (change > 0) {
            chip.setText("↑ " + fmt.format(change) + suffix);
            chip.setTextColor(0xFF43A047);
            chip.setVisibility(View.VISIBLE);
        } else if (change < 0) {
            chip.setText("↓ " + fmt.format(Math.abs(change)) + suffix);
            chip.setTextColor(0xFFE53935);
            chip.setVisibility(View.VISIBLE);
        } else {
            chip.setVisibility(View.GONE);
        }
    }

    private void bindVsLabel(int viewId, String label) {
        TextView tv = findViewById(viewId);
        if (tv != null && label != null && !label.isEmpty()) {
            tv.setText(label);
        }
    }

    /**
     * Set default dashboard values (fallback)
     */
    private void setDefaultDashboardValues() {
        tvTodayOrders.setText("0");
        tvTodayEarnings.setText("₹0");
        tvActiveSubscriptions.setText("0");
        tvAvgRating.setText("0.0");
    }

    private void setupListeners() {
        btnAddMeal.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                })
                .start();
            startActivity(new Intent(CookHomeActivity.this, AddMenuActivity.class));
        });

        // Earnings card tapped → go to earnings detail
        View tvEarningsCard = tvTodayEarnings != null ? (View) tvTodayEarnings.getParent().getParent() : null;
        if (tvEarningsCard != null) {
            tvEarningsCard.setOnClickListener(v ->
                startActivity(new Intent(CookHomeActivity.this, CookEarningsActivity.class))
            );
        }

        // Earnings period toggle
        if (tvEarningsPeriod != null) {
            tvEarningsPeriod.setOnClickListener(v -> toggleEarningsPeriod());
        }
    }

    /**
     * Toggle between Today, This Week, This Month earnings display
     */
    private void toggleEarningsPeriod() {
        currentEarningsPeriod = (currentEarningsPeriod + 1) % 3;
        updateEarningsDisplay();
    }

    /**
     * Update earnings display based on current period selection
     */
    private void updateEarningsDisplay() {
        String earnings;
        String periodLabel;

        switch (currentEarningsPeriod) {
            case 0: // Today
                earnings = CurrencyUtils.formatRupees(todayEarnings);
                periodLabel = "Today";
                break;
            case 1: // This Week
                earnings = CurrencyUtils.formatRupees(thisWeekEarnings);
                periodLabel = "This Week";
                break;
            case 2: // This Month
                earnings = CurrencyUtils.formatRupees(thisMonthEarnings);
                periodLabel = "This Month";
                break;
            default:
                earnings = CurrencyUtils.formatRupees(0);
                periodLabel = "This Week";
        }

        tvTodayEarnings.setText(earnings);
        if (tvEarningsPeriod != null) {
            tvEarningsPeriod.setText(periodLabel);
        }
    }

    /**
     * Fetch earnings data from backend API
     */
    private void fetchEarningsData() {
        String token = "Bearer " + sessionManager.getToken();

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Callback<EarningsSummaryResponse> callback = new Callback<EarningsSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<EarningsSummaryResponse> call,
                                   @NonNull Response<EarningsSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.tiffincraft.app.models.EarningsSummary earnings = response.body().getEarnings();
                    if (earnings != null) {
                        todayEarnings = earnings.getTodayTotal();
                        thisWeekEarnings = earnings.getThisWeekTotal();
                        thisMonthEarnings = earnings.getThisMonthTotal();

                        // Update display with current period
                        updateEarningsDisplay();

                        // Earnings Trend card: store all ranges, render current one
                        trendWeekly = earnings.getWeeklyBreakdown();
                        trendDaily30 = earnings.getDailyBreakdown();
                        trendMonthly = earnings.getMonthlyBreakdown();
                        renderTrendChart();

                        Log.d(TAG, "✅ Earnings data fetched: Today=₹" + todayEarnings +
                              ", Week=₹" + thisWeekEarnings + ", Month=₹" + thisMonthEarnings);
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch earnings: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<EarningsSummaryResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Earnings API call failed", t);
                com.google.android.material.snackbar.Snackbar
                        .make(findViewById(android.R.id.content),
                                "Could not load earnings.",
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> fetchEarningsData())
                        .setAnchorView(bottomNavigation)
                        .show();
            }
        };

        apiService.getCookEarningsSummary(token).enqueue(callback);
    }

    /** One-time visual styling for the Earnings Trend chart (white card, dashed grid, green line). */
    private void setupEarningsTrendChart() {
        if (chartEarningsTrend == null) return;

        chartEarningsTrend.getDescription().setEnabled(false);
        chartEarningsTrend.getLegend().setEnabled(false);
        chartEarningsTrend.setNoDataText("No earnings yet");
        chartEarningsTrend.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_hint));
        chartEarningsTrend.setExtraBottomOffset(6f);
        chartEarningsTrend.setDrawGridBackground(false);
        chartEarningsTrend.setDoubleTapToZoomEnabled(false);
        chartEarningsTrend.setScaleEnabled(false);
        chartEarningsTrend.setPinchZoom(false);

        // Tap-and-drag shows the tooltip, like hovering on the point
        chartEarningsTrend.setHighlightPerTapEnabled(true);
        chartEarningsTrend.setHighlightPerDragEnabled(true);
        EarningsMarkerView marker = new EarningsMarkerView(this);
        // Without this the marker can't see the chart bounds, so its edge
        // clamping never runs and the tooltip clips on the last data point.
        marker.setChartView(chartEarningsTrend);
        chartEarningsTrend.setMarker(marker);

        int axisLabelColor = ContextCompat.getColor(this, R.color.text_hint);
        int gridColor = ContextCompat.getColor(this, R.color.divider);

        XAxis xAxis = chartEarningsTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(axisLabelColor);
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chartEarningsTrend.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(axisLabelColor);
        leftAxis.setTextSize(11f);
        leftAxis.setGridColor(gridColor);
        leftAxis.enableGridDashedLine(6f, 6f, 0f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setSpaceTop(20f);
        leftAxis.setLabelCount(5, true); // force exactly 5 labels for clean intervals
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "₹0";
                // Format with nice round numbers
                if (value >= 1000) {
                    return "₹" + String.format(java.util.Locale.getDefault(), "%.0fk", value / 1000);
                }
                return "₹" + String.format(java.util.Locale.getDefault(), "%.0f", value);
            }
        });

        chartEarningsTrend.getAxisRight().setEnabled(false);
    }

    /** Re-bind the trend chart for the currently selected range (Week/Month/Year). */
    private void renderTrendChart() {
        if (chartEarningsTrend == null) return;

        java.util.List<Entry> entries = new java.util.ArrayList<>();
        java.util.List<String> monthLabels = new java.util.ArrayList<>();

        if (trendRange == 2) {
            // Year: last 12 months, "Jul" labels
            if (tvTrendRange != null) tvTrendRange.setText("Year ▾");
            if (tvTrendSubtitle != null) tvTrendSubtitle.setText("Last 12 months");
            java.text.SimpleDateFormat parseFmt = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US);
            java.text.SimpleDateFormat labelFmt = new java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault());
            if (trendMonthly != null) {
                for (int i = 0; i < trendMonthly.size(); i++) {
                    MonthlyBreakdown m = trendMonthly.get(i);
                    String label = formatTrendLabel(m.getMonth(), parseFmt, labelFmt);
                    monthLabels.add(label);
                    Entry entry = new Entry(i, (float) m.getAmount());
                    entry.setData(label);
                    entries.add(entry);
                }
            }
        } else {
            // Week: last 7 days ("Mon"); Month: last 30 days ("12 Jul")
            java.util.List<com.tiffincraft.app.models.WeeklyBreakdown> days =
                    trendRange == 0 ? trendWeekly : trendDaily30;
            if (tvTrendRange != null) tvTrendRange.setText(trendRange == 0 ? "Week ▾" : "Month ▾");
            if (tvTrendSubtitle != null) tvTrendSubtitle.setText(trendRange == 0 ? "Last 7 days" : "Last 30 days");
            java.text.SimpleDateFormat parseFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.text.SimpleDateFormat labelFmt = new java.text.SimpleDateFormat(
                    trendRange == 0 ? "EEE" : "d MMM", java.util.Locale.getDefault());
            if (days != null) {
                for (int i = 0; i < days.size(); i++) {
                    com.tiffincraft.app.models.WeeklyBreakdown d = days.get(i);
                    String label = formatTrendLabel(d.getDate(), parseFmt, labelFmt);
                    monthLabels.add(label);
                    Entry entry = new Entry(i, (float) d.getAmount());
                    entry.setData(label);
                    entries.add(entry);
                }
            }
        }

        if (entries.isEmpty()) {
            chartEarningsTrend.clear();
            chartEarningsTrend.invalidate();
            return;
        }

        // Calculate max value and set appropriate Y-axis maximum
        float maxValue = 0;
        for (Entry entry : entries) {
            if (entry.getY() > maxValue) {
                maxValue = entry.getY();
            }
        }

        // Set a nice round maximum for better Y-axis labels
        float yMax = calculateNiceMaximum(maxValue);
        YAxis leftAxis = chartEarningsTrend.getAxisLeft();
        leftAxis.setAxisMaximum(yMax);
        leftAxis.setAxisMinimum(0f);

        // Dense 30-point view: thin out X labels so they don't overlap
        chartEarningsTrend.getXAxis().setLabelCount(
                Math.min(entries.size(), trendRange == 1 ? 6 : entries.size()), false);

        int green = ContextCompat.getColor(this, R.color.green_primary);

        LineDataSet dataSet = new LineDataSet(entries, "Earnings");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(green);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(green);
        dataSet.setCircleRadius(trendRange == 1 ? 2.5f : 4f); // smaller dots on the dense 30-day view
        dataSet.setCircleHoleColor(0xFFFFFFFF);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(green);
        dataSet.setFillAlpha(40);
        dataSet.setHighLightColor(green);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        chartEarningsTrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        chartEarningsTrend.setData(new LineData(dataSet));
        chartEarningsTrend.highlightValue(null);
        chartEarningsTrend.animateX(400);
        chartEarningsTrend.invalidate();
    }

    /**
     * Calculate a nice round maximum for Y-axis based on the actual max value.
     * Returns values like 5, 10, 20, 50, 100, 200, 500, 1000, etc.
     */
    private float calculateNiceMaximum(float maxValue) {
        if (maxValue <= 0) return 10; // default minimum scale

        // Round up to next nice number
        float magnitude = (float) Math.pow(10, Math.floor(Math.log10(maxValue)));
        float normalized = maxValue / magnitude;

        float niceMax;
        if (normalized <= 1) niceMax = 1;
        else if (normalized <= 2) niceMax = 2;
        else if (normalized <= 5) niceMax = 5;
        else niceMax = 10;

        float result = niceMax * magnitude;
        
        // Ensure we have some headroom above the max data point
        if (result <= maxValue) {
            result = result * 1.25f;
        }
        
        return result;
    }

    private String formatTrendLabel(String raw, java.text.SimpleDateFormat parseFmt,
                                    java.text.SimpleDateFormat labelFmt) {
        if (raw == null) return "";
        try {
            java.util.Date d = parseFmt.parse(raw);
            if (d != null) return labelFmt.format(d);
        } catch (java.text.ParseException ignored) { }
        return raw;
    }

    /** Load the cook's real orders and show the ones placed today. */
    private void fetchTodayOrders() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getCookOrders(token).enqueue(new Callback<com.tiffincraft.app.models.OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.OrderResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getOrders() != null) {

                    java.text.SimpleDateFormat dayFmt =
                            new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
                    String todayKey = dayFmt.format(new java.util.Date());

                    todayOrders.clear();
                    for (com.tiffincraft.app.models.Order order : response.body().getOrders()) {
                        java.util.Date created =
                                com.tiffincraft.app.models.ChatMessage.parseServerDate(order.getCreatedAt());
                        if (created != null && todayKey.equals(dayFmt.format(created))) {
                            todayOrders.add(order);
                        }
                        if (todayOrders.size() >= 5) break;
                    }
                    todayOrderAdapter.notifyDataSetChanged();

                    boolean empty = todayOrders.isEmpty();
                    tvNoOrdersToday.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvTodayOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
                } else {
                    Log.e(TAG, "❌ Failed to fetch today's orders: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.OrderResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "❌ Today's orders API call failed", t);
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_meals) {
                startActivity(new Intent(CookHomeActivity.this, CookMealActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(CookHomeActivity.this, ManageOrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_earnings) {
                startActivity(new Intent(CookHomeActivity.this, CookEarningsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(CookHomeActivity.this, CookProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void applyEntranceAnimations() {
        // Animation removed as layout doesn't have appBarLayout
        // The activity loads without entrance animations
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show exit confirmation or minimize app
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    private void fetchUnreadNotifications() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getUnreadNotificationCount(token).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int count = response.body().getUnreadCount();
                    if (count > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching unread count", t);
            }
        });
    }

    // ========== Socket.IO Real-time Updates ==========

    private void connectSocket() {
        if (socketManager != null) {
            socketManager.connect();

            // Join cook room
            String userIdStr = sessionManager.getUserId();
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    int cookId = Integer.parseInt(userIdStr);
                    socketManager.joinCookRoom(cookId);
                    Log.d(TAG, "Joined cook room: " + cookId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid user ID format: " + userIdStr);
                }
            }
        }
    }

    private void setupSocketListeners() {
        if (socketManager == null) return;

        // Listen for new orders
        socketManager.onNewOrder(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            JSONObject data = (JSONObject) args[0];
                            handleNewOrder(data);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling new order event", e);
                    }
                });
            }
        });

        // Listen for cancelled orders
        socketManager.onOrderCancelled(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            JSONObject data = (JSONObject) args[0];
                            handleOrderCancelled(data);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling order cancelled event", e);
                    }
                });
            }
        });

        // Listen for cook approval/rejection from admin
        socketManager.onCookApprovalUpdate(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            JSONObject data = (JSONObject) args[0];
                            boolean approved = data.optBoolean("approved", false);
                            showApprovalDialog(approved);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling cook approval update", e);
                    }
                });
            }
        });
    }

    /**
     * Show a celebratory or informational dialog when admin approves/rejects the kitchen.
     */
    private void showApprovalDialog(boolean approved) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (approved) {
            builder.setTitle("🎉 Kitchen Approved!")
                    .setMessage("Congratulations! Your kitchen has been approved. You can now start receiving orders and serving customers. 🚀")
                    .setPositiveButton("Get Cooking!", (dialog, which) -> {
                        dialog.dismiss();
                        fetchDashboardData();
                    });
        } else {
            builder.setTitle("Application Update")
                    .setMessage("Your TiffinCraft kitchen application needs some changes. Please check your profile and contact support for details.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }

        builder.show();
    }

    private void handleNewOrder(JSONObject data) {
        try {
            int orderId = data.optInt("orderId");
            String customerName = data.optString("mealName", "New Order");
            double totalAmount = data.optDouble("total_amount", 0);

            Log.d(TAG, "🔔 New Order Received! Order #" + orderId);

            // Play notification sound (RingtoneManager) and vibrate
            playNotificationSound();

            // Show in-app Snackbar banner with order details
            com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar
                    .make(findViewById(android.R.id.content),
                            "New Order #" + orderId + " — ₹" + String.format("%.0f", totalAmount),
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("View", v -> {
                        Intent intent = new Intent(this,
                                com.tiffincraft.app.activities.order.OrderDetailsCookActivity.class);
                        intent.putExtra("order_id", orderId);
                        startActivity(intent);
                    })
                    .setAnchorView(bottomNavigation);
            snackbar.show();

            // Increment notification badge
            TextView badge = findViewById(R.id.tvNotificationBadge);
            if (badge != null) {
                String currentText = badge.getVisibility() == View.VISIBLE
                        ? badge.getText().toString() : "0";
                try {
                    int newCount = Integer.parseInt(currentText) + 1;
                    badge.setVisibility(View.VISIBLE);
                    badge.setText(newCount > 99 ? "99+" : String.valueOf(newCount));
                } catch (NumberFormatException ignored) { }
            }

            // Refresh all data from server (source of truth)
            fetchDashboardData();
            fetchTodayOrders();
            fetchUnreadNotifications();

        } catch (Exception e) {
            Log.e(TAG, "Error processing new order", e);
        }
    }

    private void handleOrderCancelled(JSONObject data) {
        try {
            int orderId = data.optInt("orderId");
            String message = data.optString("message", "Order cancelled");

            Log.d(TAG, "Order #" + orderId + " cancelled");

            Toast.makeText(this, "Order #" + orderId + " was cancelled", Toast.LENGTH_SHORT).show();

            // Refresh dashboard
            fetchDashboardData();

        } catch (Exception e) {
            Log.e(TAG, "Error processing order cancellation", e);
        }
    }

    private void playNotificationSound() {
        try {
            // Vibrate
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
            }

            // Play default notification sound
            android.media.RingtoneManager.getRingtone(
                this,
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            ).play();

        } catch (Exception e) {
            Log.e(TAG, "Error playing notification", e);
        }
    }
}
