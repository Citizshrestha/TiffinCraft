package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.DashboardResponse;
import com.tiffincraft.app.models.DashboardStats;
import com.tiffincraft.app.models.EarningsSummaryResponse;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ChatPanelManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.SocketManager;

import org.json.JSONObject;

import java.text.DecimalFormat;

import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookHomeActivity extends AppCompatActivity {

    private static final String TAG = "CookHomeActivity";
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private ImageView imgCookProfilePic;
    private TextView tvKitchenName, tvWelcome;
    private TextView tvTodayOrders, tvTodayEarnings, tvActiveSubscriptions, tvAvgRating;
    private TextView tvNotificationBadge, tvEarningsPeriod;
    private TextView tvOverviewTotalEarnings, tvOverviewSubtitle, tvViewAllOrders, tvNoOrdersToday;
    private com.tiffincraft.app.views.SparklineView sparklineHome;
    private android.widget.LinearLayout layoutChartDates;
    private androidx.recyclerview.widget.RecyclerView rvTodayOrders;
    private com.tiffincraft.app.adapters.TodayOrderAdapter todayOrderAdapter;
    private final java.util.List<com.tiffincraft.app.models.Order> todayOrders = new java.util.ArrayList<>();
    private View btnAddMeal, btnManageMeals, btnViewOrders, btnSubscriptions, btnThisWeek;
    private TextView tvOverviewPeriodLabel;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;
    private SocketManager socketManager;
    private ChatPanelManager chatPanelManager;

    // Earnings data storage
    private double todayEarnings = 0;
    private double thisWeekEarnings = 0;
    private double thisMonthEarnings = 0;
    private int currentEarningsPeriod = 1; // 0=Today, 1=This Week, 2=This Month

    // Overview card month selection (defaults to current month)
    private int selectedOverviewMonth;
    private int selectedOverviewYear;

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

            java.util.Calendar now = java.util.Calendar.getInstance();
            selectedOverviewMonth = now.get(java.util.Calendar.MONTH) + 1;
            selectedOverviewYear = now.get(java.util.Calendar.YEAR);

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

            Log.d(TAG, "CookHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disconnect socket when activity is not visible
        if (socketManager != null) {
            socketManager.disconnect();
        }
    }

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
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        tvOverviewTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvOverviewSubtitle = findViewById(R.id.tvOverviewSubtitle);
        sparklineHome = findViewById(R.id.sparklineHome);
        layoutChartDates = findViewById(R.id.layoutChartDates);
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
        btnThisWeek = findViewById(R.id.btnThisWeek);
        tvOverviewPeriodLabel = findViewById(R.id.tvOverviewPeriodLabel);

        btnManageMeals.setOnClickListener(v ->
                startActivity(new Intent(this, CookMealActivity.class)));
        btnViewOrders.setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrdersActivity.class)));
        btnSubscriptions.setOnClickListener(v ->
                startActivity(new Intent(this, CookSubscriptionsActivity.class)));
        btnThisWeek.setOnClickListener(v -> showOverviewMonthPicker());
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

                        // Overview card: figure for the selected month + 7-day chart
                        updateOverviewChart(earnings.getWeeklyBreakdown(), earnings.getThisMonthOrderCount());

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

        if (isCurrentOverviewMonthSelected()) {
            apiService.getCookEarningsSummary(token).enqueue(callback);
        } else {
            apiService.getCookEarningsSummaryByMonth(token, selectedOverviewMonth, selectedOverviewYear)
                    .enqueue(callback);
        }
    }

    private boolean isCurrentOverviewMonthSelected() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        return selectedOverviewMonth == now.get(java.util.Calendar.MONTH) + 1
                && selectedOverviewYear == now.get(java.util.Calendar.YEAR);
    }

    /** Opens the same month/year picker used on the Earnings screen, scoped to the overview card. */
    private void showOverviewMonthPicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_picker, null);
        TextView tvYear = dialogView.findViewById(R.id.tvPickerYear);
        android.widget.GridLayout grid = dialogView.findViewById(R.id.gridMonths);

        final int[] pickerYear = {selectedOverviewYear};
        tvYear.setText(String.valueOf(pickerYear[0]));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select month")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        dialogView.findViewById(R.id.btnPrevYear).setOnClickListener(v -> {
            pickerYear[0]--;
            tvYear.setText(String.valueOf(pickerYear[0]));
        });
        dialogView.findViewById(R.id.btnNextYear).setOnClickListener(v -> {
            pickerYear[0]++;
            tvYear.setText(String.valueOf(pickerYear[0]));
        });

        float density = getResources().getDisplayMetrics().density;
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            TextView cell = new TextView(this);
            cell.setText(MONTH_NAMES[m - 1].substring(0, 3));
            cell.setTextSize(14);
            cell.setTextColor(0xFF111111);
            cell.setGravity(android.view.Gravity.CENTER);
            int pad = (int) (12 * density);
            cell.setPadding(pad, pad, pad, pad);
            cell.setBackgroundResource(android.R.drawable.list_selector_background);

            android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);

            cell.setOnClickListener(v -> {
                selectedOverviewMonth = month;
                selectedOverviewYear = pickerYear[0];
                dialog.dismiss();
                fetchEarningsData();
            });
            grid.addView(cell);
        }

        dialog.show();
    }

    /**
     * Fill the green overview card with the real 7-day earnings line and date labels.
     * Shows the total + order count for whichever month is currently selected via the
     * pill dropdown (defaults to the current month, same figure as the Earnings screen).
     */
    private void updateOverviewChart(java.util.List<com.tiffincraft.app.models.WeeklyBreakdown> weeklyBreakdown,
                                      int monthOrderCount) {
        if (tvOverviewTotalEarnings != null) {
            tvOverviewTotalEarnings.setText(CurrencyUtils.formatRupees(thisMonthEarnings));
        }
        if (tvOverviewSubtitle != null) {
            tvOverviewSubtitle.setText(monthOrderCount == 1
                    ? "Total Earnings · 1 order"
                    : "Total Earnings · " + monthOrderCount + " orders");
        }
        if (tvOverviewPeriodLabel != null) {
            tvOverviewPeriodLabel.setText(isCurrentOverviewMonthSelected()
                    ? "This Month"
                    : MONTH_NAMES[selectedOverviewMonth - 1].substring(0, 3) + " " + selectedOverviewYear);
        }
        if (sparklineHome == null || layoutChartDates == null) return;

        java.util.List<Double> values = new java.util.ArrayList<>();
        layoutChartDates.removeAllViews();

        int count = weeklyBreakdown != null ? weeklyBreakdown.size() : 0;
        java.text.SimpleDateFormat parseFmt =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        java.text.SimpleDateFormat labelFmt =
                new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());

        for (int i = 0; i < count; i++) {
            com.tiffincraft.app.models.WeeklyBreakdown day = weeklyBreakdown.get(i);
            values.add(day.getAmount());

            String label = day.getDate() != null ? day.getDate() : "";
            try {
                java.util.Date d = parseFmt.parse(label);
                if (d != null) label = labelFmt.format(d);
            } catch (java.text.ParseException ignored) { }

            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(0xFFC8E6C9);
            tv.setTextSize(9.5f);
            tv.setGravity(i == 0 ? android.view.Gravity.START
                    : (i == count - 1 ? android.view.Gravity.END : android.view.Gravity.CENTER));
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            layoutChartDates.addView(tv);
        }

        sparklineHome.setValues(values);
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
    }

    private void handleNewOrder(JSONObject data) {
        try {
            int orderId = data.optInt("orderId");
            String customerName = data.optString("mealName", "New Order");
            double totalAmount = data.optDouble("total_amount", 0);

            Log.d(TAG, "🔔 New Order Received! Order #" + orderId);

            // Show toast notification
            Toast.makeText(this,
                    "🔔 New Order: ₹" + String.format("%.0f", totalAmount),
                    Toast.LENGTH_LONG).show();

            // Play notification sound and vibrate
            playNotificationSound();

            // Refresh dashboard data
            fetchDashboardData();
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
