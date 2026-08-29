package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.CommissionSettlementAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCommissionHistoryBinding;
import com.tiffincraft.app.models.CommissionSettlement;
import com.tiffincraft.app.models.CommissionSettlementsListResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full commission-settlement history for a cook — every month generated so
 * far, not just the current/past-due one shown on CommissionSettlementActivity.
 */
public class CommissionHistoryActivity extends AppCompatActivity {

    private ActivityCommissionHistoryBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private final List<CommissionSettlement> items = new ArrayList<>();
    private CommissionSettlementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommissionHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new CommissionSettlementAdapter(items);
        binding.rvSettlements.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSettlements.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadSettlements);

        loadSettlements();
    }

    private void loadSettlements() {
        binding.progressLoading.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyCommissionSettlements(token).enqueue(new Callback<CommissionSettlementsListResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommissionSettlementsListResponse> call,
                                    @NonNull Response<CommissionSettlementsListResponse> response) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()
                        || response.body().getSettlements() == null) {
                    Toast.makeText(CommissionHistoryActivity.this, "Failed to load settlement history", Toast.LENGTH_SHORT).show();
                    return;
                }

                items.clear();
                items.addAll(response.body().getSettlements());
                adapter.notifyDataSetChanged();
                render();
            }

            @Override
            public void onFailure(@NonNull Call<CommissionSettlementsListResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(CommissionHistoryActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render() {
        boolean isEmpty = items.isEmpty();
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvSettlements.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // amount_paid, not amount_due-of-verified: an admin can record a partial
        // payment against a still-pending settlement, and that money was really
        // paid. Summing due-of-verified would hide it until the month closes.
        double totalPaid = 0;
        for (CommissionSettlement s : items) totalPaid += s.getAmountPaid();
        binding.tvTotalPaidAllTime.setText(CurrencyUtils.formatRupees(totalPaid));
        binding.tvSettlementCount.setText(String.valueOf(items.size()));
    }
}
