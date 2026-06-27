package com.tiffincraft.app.activities.meal;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiffincraft.app.activities.common.SearchFilterActivity;
import com.tiffincraft.app.adapters.CookAdapter;
import com.tiffincraft.app.databinding.ActivityCookListBinding;

import java.util.ArrayList;
import java.util.List;

public class CookListActivity extends AppCompatActivity {

    private ActivityCookListBinding binding;
    private CookAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        binding.chipFilter.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        binding.rvCooks.setLayoutManager(new LinearLayoutManager(this));
        
        List<String> cooks = new ArrayList<>();
        cooks.add("Anita\'s Kitchen");
        cooks.add("Shalini Meals");
        cooks.add("Good Food Hub");
        cooks.add("Maa Ki Rasoi");
        cooks.add("The Healthy Tiffin");
        cooks.add("Homestyle Deli");

        adapter = new CookAdapter(this, cooks);
        binding.rvCooks.setAdapter(adapter);
    }
}
