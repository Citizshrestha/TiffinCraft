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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

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
    
    // Search and Filter State
    private String currentSearchQuery = "";
    private String currentStatusFilter = "All"; // All, Active, Sold Out
    private String currentDietaryFilter = "All"; // All, Veg, Non-Veg
    
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
        Log.d(TAG, "=== onResume called - refreshing meals ===");
        loadMyMeals();
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
        
        // Search button toggle
        if (binding.btnSearchMeal != null) {
            binding.btnSearchMeal.setOnClickListener(v -> {
                if (binding.etSearchMeal.getVisibility() == View.VISIBLE) {
                    // Close search
                    binding.etSearchMeal.setVisibility(View.GONE);
                    binding.tvTitle.setVisibility(View.VISIBLE);
                    binding.etSearchMeal.setText("");
                    
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(binding.etSearchMeal.getWindowToken(), 0);
                } else {
                    // Open search
                    binding.tvTitle.setVisibility(View.GONE);
                    binding.etSearchMeal.setVisibility(View.VISIBLE);
                    binding.etSearchMeal.requestFocus();
                    
                    // Show keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(binding.etSearchMeal, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
        
        // Search text watcher
        if (binding.etSearchMeal != null) {
            binding.etSearchMeal.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim().toLowerCase();
                    filterMeals();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        // Filter button
        if (binding.btnFilterMeal != null) {
            binding.btnFilterMeal.setOnClickListener(v -> {
                showFilterDialog();
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
                    startActivity(new Intent(this, ManageOrdersActivity.class));
                    finish();
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
        Log.d(TAG, "Token exists: " + (sessionManager.getToken() != null));
        Log.d(TAG, "Token length: " + (sessionManager.getToken() != null ? sessionManager.getToken().length() : 0));
        Log.d(TAG, "User ID: " + sessionManager.getUserId());
        Log.d(TAG, "User Role: " + sessionManager.getRole());
        Log.d(TAG, "API URL: " + RetrofitClient.BASE_URL + "meals/my");
        
        // Show loading state
        if (binding.tvTotalMeals != null) {
            binding.tvTotalMeals.setText("...");
        }
        
        apiService.getMyMeals(token).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                Log.d(TAG, "=== API Response Received ===");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Success: " + response.isSuccessful());
                Log.d(TAG, "Response Message: " + response.message());
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        MealResponse mealResponse = response.body();
                        Log.d(TAG, "Response Body Success: " + mealResponse.isSuccess());
                        Log.d(TAG, "Response Body Message: " + mealResponse.getMessage());
                        
                        if (mealResponse.isSuccess()) {
                            mealsList = mealResponse.getMeals();
                            
                            if (mealsList != null && !mealsList.isEmpty()) {
                                Log.d(TAG, "✅ Loaded " + mealsList.size() + " meals");
                                for (int i = 0; i < Math.min(3, mealsList.size()); i++) {
                                    Meal meal = mealsList.get(i);
                                    Log.d(TAG, "  Meal " + (i+1) + ": " + meal.getName() + " (ID: " + meal.getId() + ")");
                                }
                                updateUIWithMeals();
                            } else {
                                Log.d(TAG, "⚠️ Meals list is empty or null");
                                showNoMealsMessage();
                            }
                        } else {
                            Log.e(TAG, "❌ Response success=false: " + mealResponse.getMessage());
                            Toast.makeText(CookMealActivity.this, 
                                mealResponse.getMessage(), Toast.LENGTH_LONG).show();
                            showNoMealsMessage();
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
                Log.e(TAG, "Cause: " + (t.getCause() != null ? t.getCause().getMessage() : "null"));
                
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
        Log.d(TAG, "=== updateUIWithMeals called ===");
        Log.d(TAG, "Meals list size: " + (mealsList != null ? mealsList.size() : "null"));
        
        // Update stats
        if (binding.tvTotalMeals != null) {
            binding.tvTotalMeals.setText(String.valueOf(mealsList != null ? mealsList.size() : 0));
        }
        
        // Count active meals
        int activeMeals = 0;
        int soldOutMeals = 0;
        
        if (mealsList != null) {
            for (Meal meal : mealsList) {
                if (meal.isAvailable()) {
                    activeMeals++;
                } else {
                    soldOutMeals++;
                }
            }
        }
        
        if (binding.tvActiveMeals != null) {
            binding.tvActiveMeals.setText(String.valueOf(activeMeals));
        }
        
        if (binding.tvSoldOutMeals != null) {
            binding.tvSoldOutMeals.setText(String.valueOf(soldOutMeals));
        }
        
        // Update RecyclerView
        if (mealsList == null || mealsList.isEmpty()) {
            Log.d(TAG, "No meals to display - showing empty state");
            recyclerViewMeals.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
        // Update RecyclerView with currently active filters instead of all meals
        filterMeals();
        Log.d(TAG, "Adapter updated with filtered meals");
        }
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
    
    private void showFilterDialog() {
        BottomSheetDialog filterDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_meal_filter, null);
        filterDialog.setContentView(dialogView);

        ChipGroup chipGroupStatus = dialogView.findViewById(R.id.chipGroupStatus);
        ChipGroup chipGroupDietary = dialogView.findViewById(R.id.chipGroupDietary);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);
        MaterialButton btnResetFilters = dialogView.findViewById(R.id.btnResetFilters);

        if ("Active".equals(currentStatusFilter)) {
            ((Chip) dialogView.findViewById(R.id.chipStatusActive)).setChecked(true);
        } else if ("Sold Out".equals(currentStatusFilter)) {
            ((Chip) dialogView.findViewById(R.id.chipStatusSoldOut)).setChecked(true);
        } else {
            ((Chip) dialogView.findViewById(R.id.chipStatusAll)).setChecked(true);
        }

        if ("Veg".equals(currentDietaryFilter)) {
            ((Chip) dialogView.findViewById(R.id.chipDietaryVeg)).setChecked(true);
        } else if ("Non-Veg".equals(currentDietaryFilter)) {
            ((Chip) dialogView.findViewById(R.id.chipDietaryNonVeg)).setChecked(true);
        } else {
            ((Chip) dialogView.findViewById(R.id.chipDietaryAll)).setChecked(true);
        }

        btnApplyFilters.setOnClickListener(v -> {
            int selectedStatusId = chipGroupStatus.getCheckedChipId();
            if (selectedStatusId == R.id.chipStatusActive) {
                currentStatusFilter = "Active";
            } else if (selectedStatusId == R.id.chipStatusSoldOut) {
                currentStatusFilter = "Sold Out";
            } else {
                currentStatusFilter = "All";
            }

            int selectedDietaryId = chipGroupDietary.getCheckedChipId();
            if (selectedDietaryId == R.id.chipDietaryVeg) {
                currentDietaryFilter = "Veg";
            } else if (selectedDietaryId == R.id.chipDietaryNonVeg) {
                currentDietaryFilter = "Non-Veg";
            } else {
                currentDietaryFilter = "All";
            }

            filterDialog.dismiss();
            filterMeals();
        });

        btnResetFilters.setOnClickListener(v -> {
            ((Chip) dialogView.findViewById(R.id.chipStatusAll)).setChecked(true);
            ((Chip) dialogView.findViewById(R.id.chipDietaryAll)).setChecked(true);
        });

        filterDialog.show();
    }

    private void filterMeals() {
        if (mealsList == null) return;

        List<Meal> filteredList = new ArrayList<>();
        
        for (Meal meal : mealsList) {
            // 1. Search Query Filter
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String mealName = meal.getName() != null ? meal.getName().toLowerCase() : "";
                String mealDesc = meal.getDescription() != null ? meal.getDescription().toLowerCase() : "";
                matchesSearch = mealName.contains(currentSearchQuery) || mealDesc.contains(currentSearchQuery);
            }
            
            // 2. Status Filter
            boolean matchesStatus = true;
            if ("Active".equals(currentStatusFilter)) {
                matchesStatus = meal.isAvailable();
            } else if ("Sold Out".equals(currentStatusFilter)) {
                matchesStatus = !meal.isAvailable();
            }
            
            // 3. Dietary Filter
            boolean matchesDietary = true;
            if ("Veg".equals(currentDietaryFilter)) {
                matchesDietary = meal.isVegetarian();
            } else if ("Non-Veg".equals(currentDietaryFilter)) {
                matchesDietary = !meal.isVegetarian();
            }
            
            if (matchesSearch && matchesStatus && matchesDietary) {
                filteredList.add(meal);
            }
        }
        
        if (filteredList.isEmpty()) {
            recyclerViewMeals.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerViewMeals.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
        
        mealAdapter.updateMeals(filteredList);
    }
    
    // ========== MEAL ADAPTER CALLBACKS ==========
    
    @Override
    public void onEditClick(Meal meal) {
        Intent intent = new Intent(this, EditMealActivity.class);
        intent.putExtra(EditMealActivity.EXTRA_MEAL_ID,          meal.getId());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_NAME,        meal.getName());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_DESCRIPTION, meal.getDescription());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_PRICE,       meal.getPrice());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_CATEGORY,    meal.getCategory());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_IS_VEG,      meal.isVegetarian());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_IS_SPICY,    "hot".equalsIgnoreCase(meal.getSpiceLevel()));
        intent.putExtra(EditMealActivity.EXTRA_MEAL_IS_AVAILABLE,meal.isAvailable());
        intent.putExtra(EditMealActivity.EXTRA_MEAL_IMAGE_URL,   meal.getImageUrl());
        startActivity(intent);
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
        if (meal.isInSubscription()) {
            // Show dialog to remove from subscriptions
            showRemoveFromSubscriptionDialog(meal);
        } else {
            // Add meal to subscription availability
            addMealToSubscription(meal);
        }
    }
    
    private void addMealToSubscription(Meal meal) {
        String token = "Bearer " + sessionManager.getToken();
        
        apiService.addMealToSubscription(token, meal.getId()).enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, 
                                   @NonNull Response<com.tiffincraft.app.models.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookMealActivity.this, 
                        "Meal added to subscription availability", 
                        Toast.LENGTH_SHORT).show();
                    loadMyMeals(); // Refresh the list
                } else {
                    Toast.makeText(CookMealActivity.this, 
                        "Failed to add meal to subscription", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, 
                                  @NonNull Throwable t) {
                Toast.makeText(CookMealActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showRemoveFromSubscriptionDialog(Meal meal) {
        String token = "Bearer " + sessionManager.getToken();
        
        // First, get the list of subscription plans containing this meal
        apiService.getSubscriptionPlansByMeal(token, meal.getId()).enqueue(new Callback<com.tiffincraft.app.models.SubscriptionPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.SubscriptionPlanResponse> call, 
                                   @NonNull Response<com.tiffincraft.app.models.SubscriptionPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.tiffincraft.app.models.SubscriptionPlanResponse planResponse = response.body();
                    
                    if (planResponse.getPlans() == null || planResponse.getPlans().isEmpty()) {
                        new MaterialAlertDialogBuilder(CookMealActivity.this, R.style.RoundedWhiteDialog)
                            .setTitle("Remove from Subscriptions?")
                            .setMessage("This meal will no longer show when you create a subscription plan.")
                            .setPositiveButton("Remove", (dialog, which) -> {
                                removeMealFromSubscriptions(meal);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                        return;
                    }
                    
                    // Build the plan names list for the dialog
                    StringBuilder planNames = new StringBuilder();
                    for (com.tiffincraft.app.models.SubscriptionPlanResponse.Plan plan : planResponse.getPlans()) {
                        if (planNames.length() > 0) {
                            planNames.append("\n");
                        }
                        planNames.append("• ").append(plan.getName());
                    }
                    
                    // Show confirmation dialog
                    new MaterialAlertDialogBuilder(CookMealActivity.this, R.style.RoundedWhiteDialog)
                        .setTitle("Remove from Subscriptions?")
                        .setMessage("Currently in:\n\n" + planNames + 
                                    "\n\nAll these plans will be updated.")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            removeMealFromSubscriptions(meal);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    Toast.makeText(CookMealActivity.this, 
                        "Failed to load subscription plans", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.SubscriptionPlanResponse> call, 
                                  @NonNull Throwable t) {
                Toast.makeText(CookMealActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void removeMealFromSubscriptions(Meal meal) {
        String token = "Bearer " + sessionManager.getToken();
        
        apiService.removeMealFromSubscriptionPlans(token, meal.getId()).enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, 
                                   @NonNull Response<com.tiffincraft.app.models.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookMealActivity.this, 
                        response.body().getMessage(), 
                        Toast.LENGTH_LONG).show();
                    loadMyMeals(); // Refresh the list to update the button state
                } else {
                    String errorMsg = "Failed to remove meal from subscriptions";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(CookMealActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, 
                                  @NonNull Throwable t) {
                Toast.makeText(CookMealActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onDeleteClick(Meal meal) {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(this, R.style.RoundedWhiteDialog)
            .setTitle("Delete \"" + meal.getName() + "\"?")
            .setMessage("This will permanently remove this meal.")
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