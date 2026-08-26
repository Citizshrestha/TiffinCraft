package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.adapters.EarningsAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookEarningsBinding;
import com.tiffincraft.app.models.EarningsSummary;
import com.tiffincraft.app.models.EarningsSummaryResponse;
import com.tiffincraft.app.models.EarningsTransaction;
import com.tiffincraft.app.models.MonthlyBreakdown;
import com.tiffincraft.app.models.WeeklyBreakdown;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.views.CommissionBanner;
import com.tiffincraft.app.views.EarningsMarkerView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookEarningsActivity extends AppCompatActivity {

    private static final String TAG = "CookEarningsActivity";
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private ActivityCookEarningsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private CommissionBanner commissionBanner;

    private EarningsAdapter adapter;
    private final List<EarningsTransaction> transactions = new ArrayList<>();

    private EarningsSummary currentSummary;
    private int selectedMonth; // 1-12
    private int selectedYear;

    // Earnings Trend chart range toggle (matches CookHomeActivity's pattern)
    private int trendRange = 0; // 0=Week (7d), 1=Month (30d), 2=Year (12mo)
    private List<WeeklyBreakdown> trendWeekly;
    private List<WeeklyBreakdown> trendDaily30;
    private List<MonthlyBreakdown> trendMonthly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        Calendar now = Calendar.getInstance();
        selectedMonth = now.get(Calendar.MONTH) + 1;
        selectedYear = now.get(Calendar.YEAR);

        setupRecyclerView();
        setupClickListeners();
        setupEarningsTrendChart();
        setupBottomNavigation();
        fetchEarnings();

        // hideWhenNothingDue=false: on Earnings the banner doubles as the cook's
        // permanent route into settlement history, so it must stay visible even
        // when the balance is zero.
        commissionBanner = CommissionBanner.attach(this, R.id.commissionBanner, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the settlement screen after uploading proof must not
        // leave a stale "Pay Now" banner behind.
        if (commissionBanner != null) commissionBanner.refresh();
    }

    private void setupRecyclerView() {
        adapter = new EarningsAdapter(this, transactions, transaction -> {
            Intent intent = new Intent(this, OrderDetailsCookActivity.class);
            intent.putExtra("order_id", transaction.getOrderId());
            startActivity(intent);
        });
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.tvViewAllTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, AllTransactionsActivity.class)));

        binding.btnDownloadStatement.setOnClickListener(v -> exportAndShareStatement());

        binding.btnSelectMonth.setOnClickListener(v -> showMonthPickerDialog());

        binding.tvTrendRange.setOnClickListener(v -> {
            trendRange = (trendRange + 1) % 3;
            renderTrendChart();
        });
    }

    // ==================== Data ====================

    private void fetchEarnings() {
        setLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        Callback<EarningsSummaryResponse> callback = new Callback<EarningsSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<EarningsSummaryResponse> call,
                                   @NonNull Response<EarningsSummaryResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getEarnings() != null) {
                    currentSummary = response.body().getEarnings();
                    bindSummary(currentSummary);
                } else {
                    String message = response.code() == 403
                            ? "Only cook accounts can view earnings."
                            : "Server could not load earnings (code " + response.code() + ").";
                    showErrorWithRetry(message);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EarningsSummaryResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "fetchEarnings failed", t);
                setLoading(false);
                String message = t.getMessage() != null && t.getMessage().contains("Unable to resolve host")
                        ? "No internet connection. Check your network."
                        : "Could not reach the server. Try again.";
                showErrorWithRetry(message);
            }
        };

        if (isCurrentMonthSelected()) {
            apiService.getCookEarningsSummary(token).enqueue(callback);
        } else {
            apiService.getCookEarningsSummaryByMonth(token, selectedMonth, selectedYear).enqueue(callback);
        }
    }

    private boolean isCurrentMonthSelected() {
        Calendar now = Calendar.getInstance();
        return selectedMonth == now.get(Calendar.MONTH) + 1
                && selectedYear == now.get(Calendar.YEAR);
    }

    private void bindSummary(EarningsSummary summary) {
        String monthLabel = MONTH_NAMES[selectedMonth - 1] + " " + selectedYear;
        binding.tvSelectedMonth.setText(monthLabel);
        binding.tvMonthLabel.setText(isCurrentMonthSelected() ? "This Month" : monthLabel);

        int orderCount = summary.getThisMonthOrderCount();
        double monthTotal = summary.getThisMonthTotal();
        double monthCommission = summary.getThisMonthCommission();
        double monthNetTotal = summary.getThisMonthNetTotal();

        // Show what the cook actually keeps (net of platform commission), not gross —
        // gross was misleading here since commission is deducted at monthly settlement.
        binding.tvTotalEarnings.setText(CurrencyUtils.formatRupees(monthNetTotal));
        binding.tvEarningsSubtitle.setText(orderCount == 1
                ? "Total Earnings · 1 order"
                : "Total Earnings · " + orderCount + " orders");

        if (monthCommission > 0) {
            binding.tvCommissionBreakdown.setText("Gross " + CurrencyUtils.formatRupees(monthTotal)
                    + " · Commission −" + CurrencyUtils.formatRupees(monthCommission));
            binding.tvCommissionBreakdown.setVisibility(View.VISIBLE);
        } else {
            binding.tvCommissionBreakdown.setVisibility(View.GONE);
        }

        // Quick-glance strip
        binding.tvTodayQuick.setText(CurrencyUtils.formatRupees(summary.getTodayTotal()));
        binding.tvWeekQuick.setText(CurrencyUtils.formatRupees(summary.getThisWeekTotal()));
        binding.tvOrdersQuick.setText(String.valueOf(orderCount));

        // Trend chart
        trendWeekly = summary.getWeeklyBreakdown();
        trendDaily30 = summary.getDailyBreakdown();
        trendMonthly = summary.getMonthlyBreakdown();
        renderTrendChart();

        // Transactions
        transactions.clear();
        if (summary.getRecentTransactions() != null) {
            transactions.addAll(summary.getRecentTransactions());
        }
        adapter.notifyDataSetChanged();
        bindPaymentSplit(transactions);

        boolean isEmpty = transactions.isEmpty() && monthTotal <= 0;
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.cardTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.tvViewAllTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /** Online vs Cash-on-Delivery split across the currently loaded (recent) transactions. */
    private void bindPaymentSplit(List<EarningsTransaction> list) {
        if (list.isEmpty()) {
            binding.cardPaymentSplit.setVisibility(View.GONE);
            return;
        }

        double onlineTotal = 0;
        double codTotal = 0;
        for (EarningsTransaction t : list) {
            if (t.isOnlinePayment()) onlineTotal += t.getAmount();
            else codTotal += t.getAmount();
        }

        double grandTotal = onlineTotal + codTotal;
        if (grandTotal <= 0) {
            binding.cardPaymentSplit.setVisibility(View.GONE);
            return;
        }

        int onlinePercent = (int) Math.round((onlineTotal / grandTotal) * 100);
        int codPercent = 100 - onlinePercent;

        binding.cardPaymentSplit.setVisibility(View.VISIBLE);
        binding.tvOnlineAmountSplit.setText(CurrencyUtils.formatRupees(onlineTotal) + " · " + onlinePercent + "%");
        binding.tvCodAmountSplit.setText(CurrencyUtils.formatRupees(codTotal) + " · " + codPercent + "%");

        ((android.widget.LinearLayout.LayoutParams) binding.viewOnlineBarFill.getLayoutParams()).weight =
                Math.max(onlinePercent, 1);
        ((android.widget.LinearLayout.LayoutParams) binding.viewCodBarFill.getLayoutParams()).weight =
                Math.max(codPercent, 1);
        binding.viewOnlineBarFill.setVisibility(onlinePercent > 0 ? View.VISIBLE : View.GONE);
        binding.viewCodBarFill.setVisibility(codPercent > 0 ? View.VISIBLE : View.GONE);
        binding.viewOnlineBarFill.requestLayout();
        binding.viewCodBarFill.requestLayout();
    }

    // ==================== Earnings trend chart ====================

    /** One-time visual styling for the Earnings Trend chart (mirrors CookHomeActivity). */
    private void setupEarningsTrendChart() {
        com.github.mikephil.charting.charts.LineChart chart = binding.chartEarningsTrend;

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("No earnings yet");
        chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_hint));
        chart.setExtraBottomOffset(6f);
        chart.setDrawGridBackground(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);

        chart.setHighlightPerTapEnabled(true);
        chart.setHighlightPerDragEnabled(true);
        EarningsMarkerView marker = new EarningsMarkerView(this);
        marker.setChartView(chart);
        chart.setMarker(marker);

        int axisLabelColor = ContextCompat.getColor(this, R.color.text_hint);
        int gridColor = ContextCompat.getColor(this, R.color.divider);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(axisLabelColor);
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
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
                    return "₹" + String.format(Locale.getDefault(), "%.0fk", value / 1000);
                }
                return "₹" + String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        chart.getAxisRight().setEnabled(false);
    }

    /** Re-bind the trend chart for the currently selected range (Week/Month/Year). */
    private void renderTrendChart() {
        com.github.mikephil.charting.charts.LineChart chart = binding.chartEarningsTrend;

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (trendRange == 2) {
            binding.tvTrendRange.setText("Year ▾");
            binding.tvTrendSubtitle.setText("Last 12 months");
            SimpleDateFormat parseFmt = new SimpleDateFormat("yyyy-MM", Locale.US);
            SimpleDateFormat labelFmt = new SimpleDateFormat("MMM", Locale.getDefault());
            if (trendMonthly != null) {
                for (int i = 0; i < trendMonthly.size(); i++) {
                    MonthlyBreakdown m = trendMonthly.get(i);
                    String label = formatTrendLabel(m.getMonth(), parseFmt, labelFmt);
                    labels.add(label);
                    Entry entry = new Entry(i, (float) m.getAmount());
                    entry.setData(label);
                    entries.add(entry);
                }
            }
        } else {
            List<WeeklyBreakdown> days = trendRange == 0 ? trendWeekly : trendDaily30;
            binding.tvTrendRange.setText(trendRange == 0 ? "Week ▾" : "Month ▾");
            binding.tvTrendSubtitle.setText(trendRange == 0 ? "Last 7 days" : "Last 30 days");
            SimpleDateFormat parseFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat labelFmt = new SimpleDateFormat(
                    trendRange == 0 ? "EEE" : "d MMM", Locale.getDefault());
            if (days != null) {
                for (int i = 0; i < days.size(); i++) {
                    WeeklyBreakdown d = days.get(i);
                    String label = formatTrendLabel(d.getDate(), parseFmt, labelFmt);
                    labels.add(label);
                    Entry entry = new Entry(i, (float) d.getAmount());
                    entry.setData(label);
                    entries.add(entry);
                }
            }
        }

        if (entries.isEmpty()) {
            chart.clear();
            chart.invalidate();
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
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(yMax);
        leftAxis.setAxisMinimum(0f);

        // Week (7 pts) is fine showing every label; Month (30 pts) and Year (12 pts)
        // both get thinned to ~6 or every 12 months look crammed/unreadable.
        chart.getXAxis().setLabelCount(
                Math.min(entries.size(), trendRange == 0 ? entries.size() : 6), false);

        int green = ContextCompat.getColor(this, R.color.green_primary);

        LineDataSet dataSet = new LineDataSet(entries, "Earnings");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(green);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(green);
        dataSet.setCircleRadius(trendRange == 1 ? 2.5f : 4f);
        dataSet.setCircleHoleColor(0xFFFFFFFF);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(green);
        dataSet.setFillAlpha(40);
        dataSet.setHighLightColor(green);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(dataSet));
        chart.highlightValue(null);
        chart.animateX(400);
        chart.invalidate();
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

    private String formatTrendLabel(String raw, SimpleDateFormat parseFmt, SimpleDateFormat labelFmt) {
        if (raw == null) return "";
        try {
            Date d = parseFmt.parse(raw);
            if (d != null) return labelFmt.format(d);
        } catch (ParseException ignored) { }
        return raw;
    }

    private void setLoading(boolean loading) {
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.cardTransactions.setVisibility(View.GONE);
            binding.cardPaymentSplit.setVisibility(View.GONE);
        }
    }

    private void showErrorWithRetry(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> fetchEarnings())
                .setAnchorView(binding.bottomNavigation)
                .show();
    }

    // ==================== Month picker ====================

    private void showMonthPickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_picker, null);
        TextView tvYear = dialogView.findViewById(R.id.tvPickerYear);
        GridLayout grid = dialogView.findViewById(R.id.gridMonths);

        final int[] pickerYear = {selectedYear};
        tvYear.setText(String.valueOf(pickerYear[0]));

        AlertDialog dialog = new AlertDialog.Builder(this)
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

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);

            cell.setOnClickListener(v -> {
                selectedMonth = month;
                selectedYear = pickerYear[0];
                dialog.dismiss();
                fetchEarnings();
            });
            grid.addView(cell);
        }

        dialog.show();
    }

    // ==================== Export / share ====================

    private void exportAndShareStatement() {
        if (currentSummary == null) {
            Toast.makeText(this, "Earnings are still loading — try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<EarningsTransaction> list = currentSummary.getRecentTransactions();
        if (list == null || list.isEmpty()) {
            Toast.makeText(this, "No transactions to export for this month.", Toast.LENGTH_SHORT).show();
            return;
        }

        String monthLabel = MONTH_NAMES[selectedMonth - 1] + " " + selectedYear;
        StringBuilder sb = new StringBuilder();
        sb.append("TiffinCraft Earnings Statement — ").append(monthLabel).append("\n");
        sb.append("Total: ").append(CurrencyUtils.formatRupees(currentSummary.getThisMonthTotal()))
          .append(" · ").append(currentSummary.getThisMonthOrderCount()).append(" orders\n");
        sb.append("--------------------------------------\n");
        for (EarningsTransaction t : list) {
            sb.append("Order #").append(t.getOrderId())
              .append(" | ").append(t.getCustomerName())
              .append(" | ").append(CurrencyUtils.formatRupees(t.getAmount()))
              .append(" | ").append(t.isOnlinePayment() ? "Online" : "COD")
              .append(" | ").append(t.getDate() != null ? t.getDate() : "")
              .append("\n");
        }

        try {
            File dir = new File(getCacheDir(), "statements");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new java.io.IOException("Could not create statements directory");
            }
            String fileName = "earnings_" + selectedYear + "_" + selectedMonth + ".txt";
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "TiffinCraft Earnings — " + monthLabel);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share earnings statement"));
        } catch (Exception e) {
            Log.e(TAG, "exportAndShareStatement failed", e);
            Toast.makeText(this, "Could not create the statement file.", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== Navigation ====================

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_earnings);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CookHomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_meals) {
                startActivity(new Intent(this, CookMealActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, ManageOrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_earnings) {
                return true; // Already on this screen
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, CookProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }
}
