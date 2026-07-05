package com.tiffincraft.app.activities.meal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiffincraft.app.activities.common.SearchFilterActivity;
import com.tiffincraft.app.adapters.CookAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookListBinding;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookListActivity extends AppCompatActivity {

    private ActivityCookListBinding binding;
    private CookAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        binding.chipFilter.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));

        setupRecyclerView();
        loadCooks();
    }

    private void setupRecyclerView() {
        binding.rvCooks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CookAdapter(this, new ArrayList<>());
        binding.rvCooks.setAdapter(adapter);
    }

    private void loadCooks() {
        // NOTE: getAllCooks() currently has no params per your ApiService listing.
        // If your backend's GET /api/cook returns { success, cooks: [...] } rather than
        // a single "cook" object, CookProfileResponse needs a List<CookProfile> field —
        // see note below the code block.
        apiService.getAllCooks().enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CookProfile> cooks = response.body().getCooks();
                    if (cooks != null && !cooks.isEmpty()) {
                        adapter.setCooks(cooks);
                    } else {
                        Toast.makeText(CookListActivity.this, "No cooks available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CookListActivity.this, "Failed to load cooks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Toast.makeText(CookListActivity.this, "Network error loading cooks", Toast.LENGTH_SHORT).show();
            }
        });
    }
}