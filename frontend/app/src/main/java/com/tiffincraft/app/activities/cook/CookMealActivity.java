package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.MealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookMealBinding;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class CookMealActivity extends AppCompatActivity implements MealAdapter.OnMealClickListener {
    private static final String TAG = "CookMealActivity";
    
    private ActivityCookMealBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private List<Meal> mealsList = new ArrayList<>();
    private MealAdapter mealAdapter;
    private RecyclerView recyclerViewMeals;
    private View emptyStateLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookMealBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        
        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation();
        loadMyMeals();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh meals when returning from Add Meal screen
        // Add a small delay to let backend finish processing
        binding.getRoot().postDelayed(this::loadMyMeals, 500);
    }
    
    private void setupRecyclerView() {
        recyclerViewMeals = binding.recyclerViewMeals;
        emptyStateLayout = binding.emptyStateLayout;
        
        // Setup RecyclerView
        recyclerViewMeals.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMeals.setHasFixedSize(true);
        
        // Setup adapter with listener
        mealAdapter = new MealAdapter(this, mealsList, this);
        recyclerViewMeals.setAdapter(mealAdapter);
    }
    
    private void setupClickListeners() {
        // FAB - Add new meal
        if (binding.fabAddMeal != null) {
            binding.fabAddMeal.setOnClickListener(v -> {
                startActivity(new Intent(this, AddMenuActivity.class));
            });
        }
        
        // Search and filter buttons (placeholder for now)
        if (binding.btnSearchMeal != null) {
            binding.btnSearchMeal.setOnClickListener(v -> {
                Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (binding.btnFilterMeal != null) {
            binding.btnFilterMeal.setOnClickListener(v -> {
                Toast.makeText(this, "Filter coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_meals);
            
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, CookHomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_meals) {
                    return true; // Already on this screen
                } else if (itemId == R.id.nav_orders) {
                    Toast.makeText(this, "Orders screen coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_earnings) {
                    startActivity(new Intent(this, CookEarningsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, CookProfileActivity.class));
                    finish();
                    return true;
                }
                
                return false;
            });
        }
    }
    
    private void loadMyMeals() {
        String token = "Bearer " + sessionManager.getToken();
        
        Log.d(TAG, "=== Loading Meals ===");
        Log.d(TAG, "Token: " + (token != null ? "Present (length: " + token.length() + ")" : "NULL"));
        Log.d(TAG, "API URL: http://192.168.100.115:5000/api/meals/my");
        
        // Show loading state
        if (binding.tvTotalMeals != null) {
            binding.tvTotalMeals.setText("...");
        }
        
        apiService.getMyMeals(token).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Success: " + response.isSuccessful());
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        MealResponse mealResponse = response.body();
                        Log.d(TAG, "Response Body Success: " + mealResponse.isSuccess());
                        
                        if (mealResponse.isSuccess()) {
                            mealsList = mealResponse.getMeals();
                            
                            if (mealsList != null && !mealsList.isEmpty()) {
                                Log.d(TAG, "✅ Loaded " + mealsList.size() + " meals");
                                updateUIWithMeals();
                            } else {
                                Log.d(TAG, "⚠️ Meals list is empty or null");
                                showNoMealsMessage();
                            }
                        } else {
                            Log.e(TAG, "❌ Response success=false: " + mealResponse.getMessage());
                            Toast.makeText(CookMealActivity.this, 
                                mealResponse.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "❌ Response not successful or body is null");
                        Log.e(TAG, "Response code: " + response.code());
                        
                        String errorMessage = "Failed to load meals";
                        if (response.code() == 401) {
                            errorMessage = "Session expired. Please login again.";
                        } else if (response.code() == 403) {
                            errorMessage = "Access denied. Cook account required.";
                        } else if (response.code() == 404) {
                            errorMessage = "Meals endpoint not found. Check backend.";
                        } else if (response.code() >= 500) {
                            errorMessage = "Server error. Check backend logs.";
                        }
                        
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not read error body", e);
                            }
                        }
                        
                        Toast.makeText(CookMealActivity.this, 
                            errorMessage + " (Code: " + response.code() + ")", 
                            Toast.LENGTH_LONG).show();
                            
                        showNoMealsMessage();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Exception in onResponse", e);
                    Toast.makeText(CookMealActivity.this, 
                        "Error parsing response: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    showNoMealsMessage();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network failure", t);
                Log.e(TAG, "Error message: " + t.getMessage());
                Log.e(TAG, "Error class: " + t.getClass().getName());
                
                String errorMessage;
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMessage = "Cannot connect to server.\n\nCheck:\n1. Backend is running\n2. Both on same WiFi\n3. IP: 192.168.100.115";
                } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMessage = "Connection timeout. Backend might be slow or offline.";
                } else if (t.getMessage() != null && t.getMessage().contains("Connection refused")) {
                    errorMessage = "Backend not running on port 5000.\nRun: npm start";
                } else {
                    errorMessage = "Network error: " + t.getMessage();
                }
                
                Toast.makeText(CookMealActivity.this, 
                    errorMessage, 
                    Toast.LENGTH_LONG).show();
                    
                showNoMealsMessage();
            }
        });
    }
    
    private void updateUIWithMeals() {
        // Update stats
        if (binding.tvTotalMeals != null) {
            binding.tvTotalMeals.setText(String.valueOf(mealsList.size()));
        }
        
        // Count active meals
        int activeMeals = 0;
        int soldOutMeals = 0;
        for (Meal meal : mealsList) {
            if (meal.isAvailable()) {
                activeMeals++;
            } else {
                soldOutMeals++;
            }
        }
        
        if (binding.tvActiveMeals != null) {
            binding.tvActiveMeals.setText(String.valueOf(activeMeals));
        }
        
        if (binding.tvSoldOutMeals != null) {
            binding.tvSoldOutMeals.setText(String.valueOf(soldOutMeals));
        }
        
        // Update RecyclerView
        if (mealsList.isEmpty()) {
            recyclerViewMeals.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerViewMeals.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            mealAdapter.updateMeals(mealsList);
        }
        
        Toast.makeText(this, 
            "Loaded " + mealsList.size() + " meals", 
            Toast.LENGTH_SHORT).show();
    }
    
    private void showNoMealsMessage() {
        if (binding.tvTotalMeals != null) {
            binding.tvTotalMeals.setText("0");
        }
        if (binding.tvActiveMeals != null) {
            binding.tvActiveMeals.setText("0");
        }
        if (binding.tvSoldOutMeals != null) {
            binding.tvSoldOutMeals.setText("0");
        }
        
        recyclerViewMeals.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }
    
    // ========== MEAL ADAPTER CALLBACKS ==========
    
    @Override
    public void onEditClick(Meal meal) {
        // TODO: Navigate to Edit Meal Activity
        Toast.makeText(this, 
            "Edit " + meal.getName() + " - Coming soon", 
            Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onAvailabilityToggle(Meal meal, boolean isAvailable) {
        // Update meal availability on backend
        String token = "Bearer " + sessionManager.getToken();
        
        // Create updated meal request with all required fields
        com.tiffincraft.app.models.MealRequest request = new com.tiffincraft.app.models.MealRequest();
        request.setName(meal.getName());
        request.setDescription(meal.getDescription());
        request.setPrice(meal.getPrice());
        request.setCategory(meal.getCategory());
        request.setCuisineType(meal.getCuisineType());
        request.setAvailable(isAvailable);
        request.setVegetarian(meal.isVegetarian());
        request.setVegan(meal.isVegan());
        if (meal.getPreparationTime() != null) {
            request.setPreparationTime(meal.getPreparationTime());
        }
        if (meal.getSpiceLevel() != null) {
            request.setSpiceLevel(meal.getSpiceLevel());
        }
        if (meal.getAllergens() != null) {
            request.setAllergens(meal.getAllergens());
        }
        
        apiService.updateMeal(token, meal.getId(), request).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookMealActivity.this, 
                        isAvailable ? "Meal is now available" : "Meal marked as sold out", 
                        Toast.LENGTH_SHORT).show();
                    loadMyMeals(); // Refresh list
                } else {
                    Toast.makeText(CookMealActivity.this, 
                        "Failed to update availability", 
                        Toast.LENGTH_SHORT).show();
                    loadMyMeals(); // Revert UI
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookMealActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                loadMyMeals(); // Revert UI
            }
        });
    }
    
    @Override
    public void onSubscriptionClick(Meal meal) {
        // Show subscription options dialog
        new MaterialAlertDialogBuilder(this)
            .setTitle("Add to Subscription")
            .setMessage("Add \"" + meal.getName() + "\" to subscription plans?\n\nSubscription features are coming soon!")
            .setPositiveButton("OK", null)
            .show();
    }
    
    @Override
    public void onDeleteClick(Meal meal) {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Meal")
            .setMessage("Are you sure you want to delete \"" + meal.getName() + "\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteMeal(meal);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteMeal(Meal meal) {
        String token = "Bearer " + sessionManager.getToken();
        
        apiService.deleteMeal(token, meal.getId()).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookMealActivity.this, 
                        "Meal deleted successfully", 
                        Toast.LENGTH_SHORT).show();
                    loadMyMeals(); // Refresh list
                } else {
                    Toast.makeText(CookMealActivity.this, 
                        "Failed to delete meal", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookMealActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
