package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.adapters.EarningsAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityAllTransactionsBinding;
import com.tiffincraft.app.models.EarningsTransaction;
import com.tiffincraft.app.models.EarningsTransactionsResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllTransactionsActivity extends AppCompatActivity {

    private static final String TAG = "AllTransactionsActivity";
    private static final int PAGE_SIZE = 20;

    private ActivityAllTransactionsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private EarningsAdapter adapter;
    /** Everything loaded from the server so far (across pages). */
    private final List<EarningsTransaction> allLoaded = new ArrayList<>();
    /** What the RecyclerView shows (allLoaded, filtered by the search box). */
    private final List<EarningsTransaction> displayed = new ArrayList<>();

    private int currentPage = 1;
    private boolean hasNextPage = true;
    private boolean isLoading = false;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupSearch();

        binding.swipeRefresh.setColorSchemeColors(0xFF4CAF50);
        binding.swipeRefresh.setOnRefreshListener(this::refresh);

        loadPage(1, true);
    }

    private void setupRecyclerView() {
        adapter = new EarningsAdapter(this, displayed, transaction -> {
            Intent intent = new Intent(this, OrderDetailsCookActivity.class);
            intent.putExtra("order_id", transaction.getOrderId());
            startActivity(intent);
        });
        LinearLayoutManager lm = new LinearLayoutManager(this);
        binding.rvAllTransactions.setLayoutManager(lm);
        binding.rvAllTransactions.setAdapter(adapter);

        // Infinite scroll: fetch next page when close to the bottom.
        // Paused while a search query is active (search filters the loaded list).
        binding.rvAllTransactions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasNextPage || !currentQuery.isEmpty()) return;
                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (firstVisible + visible >= total - 4) {
                    loadPage(currentPage + 1, false);
                }
            }
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s != null ? s.toString().trim() : "";
                applyFilter();
            }
        });
    }

    /** Filter the locally loaded list by order ID or customer name. */
    private void applyFilter() {
        displayed.clear();
        if (currentQuery.isEmpty()) {
            displayed.addAll(allLoaded);
        } else {
            String q = currentQuery.toLowerCase(Locale.getDefault());
            for (EarningsTransaction t : allLoaded) {
                String orderId = String.valueOf(t.getOrderId());
                String name = t.getCustomerName() != null
                        ? t.getCustomerName().toLowerCase(Locale.getDefault()) : "";
                if (orderId.contains(q) || name.contains(q)) {
                    displayed.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void refresh() {
        currentPage = 1;
        hasNextPage = true;
        loadPage(1, false);
    }

    private void loadPage(int page, boolean showSpinner) {
        if (isLoading) return;
        isLoading = true;
        if (showSpinner) binding.progressLoading.setVisibility(View.VISIBLE);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getCookEarningsTransactions(token, page, PAGE_SIZE, "")
                .enqueue(new Callback<EarningsTransactionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<EarningsTransactionsResponse> call,
                                           @NonNull Response<EarningsTransactionsResponse> response) {
                        isLoading = false;
                        binding.progressLoading.setVisibility(View.GONE);
                        binding.swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            List<EarningsTransaction> pageItems = response.body().getTransactions();

                            if (page == 1) allLoaded.clear();
                            if (pageItems != null) allLoaded.addAll(pageItems);

                            currentPage = page;
                            hasNextPage = response.body().getPagination() != null
                                    && response.body().getPagination().isHasNextPage();

                            applyFilter();
                        } else {
                            Toast.makeText(AllTransactionsActivity.this,
                                    "Server could not load transactions (code " + response.code() + ").",
                                    Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<EarningsTransactionsResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "loadPage failed", t);
                        isLoading = false;
                        binding.progressLoading.setVisibility(View.GONE);
                        binding.swipeRefresh.setRefreshing(false);

                        String message = t.getMessage() != null && t.getMessage().contains("Unable to resolve host")
                                ? "No internet connection. Check your network."
                                : "Could not reach the server. Pull down to retry.";
                        Toast.makeText(AllTransactionsActivity.this, message, Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        boolean empty = displayed.isEmpty();
        binding.layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            if (!currentQuery.isEmpty()) {
                binding.tvEmptyTitle.setText("No matches found");
                binding.tvEmptySubtitle.setText("Try a different order ID or customer name.");
            } else {
                binding.tvEmptyTitle.setText("No earnings yet");
                binding.tvEmptySubtitle.setText("Delivered orders will show up here.");
            }
        }
    }
}
