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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    private EarningsAdapter adapter;
    private final List<EarningsTransaction> transactions = new ArrayList<>();

    private EarningsSummary currentSummary;
    private int selectedMonth; // 1-12
    private int selectedYear;

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
        setupBottomNavigation();
        fetchEarnings();
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

        binding.tvTotalEarnings.setText(CurrencyUtils.formatRupees(monthTotal));
        binding.tvEarningsSubtitle.setText(orderCount == 1
                ? "Total Earnings · 1 order"
                : "Total Earnings · " + orderCount + " orders");

        // Transactions
        transactions.clear();
        if (summary.getRecentTransactions() != null) {
            transactions.addAll(summary.getRecentTransactions());
        }
        adapter.notifyDataSetChanged();

        boolean isEmpty = transactions.isEmpty() && monthTotal <= 0;
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.tvViewAllTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.layoutEmptyState.setVisibility(View.GONE);
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
