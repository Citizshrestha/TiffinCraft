package com.tiffincraft.app.utils;

import android.content.Context;
import android.widget.Toast;

import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiHelper {

    private Context context;
    private ApiService apiService;

    public ApiHelper(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void registerUser(String fullName, String email, String phone, String password, String role) {
        RegisterRequest request = new RegisterRequest(fullName, email, phone, password, role);

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();

                    if (registerResponse.isSuccess()) {
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, registerResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loginUser(String email, String password, final LoginCallback callback) {
        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        RetrofitClient.saveAuthToken(context, loginResponse.getToken());

                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show();

                        if (callback != null) {
                            callback.onLoginSuccess(loginResponse.getUser());
                        }
                    } else {
                        Toast.makeText(context, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onLoginFailed(loginResponse.getMessage());
                        }
                    }
                } else {
                    Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onLoginFailed("Login failed");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onLoginFailed(error);
                }
            }
        });
    }

    public void setupCookProfile(String kitchenName, String foodType, String description, int capacity) {
        String token = RetrofitClient.getAuthToken(context);

        if (token == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        CookProfileRequest request = new CookProfileRequest(kitchenName, foodType, description, capacity);

        apiService.setupCookProfile(token, request).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CookProfileResponse profileResponse = response.body();

                    if (profileResponse.isSuccess()) {
                        Toast.makeText(context, "Profile setup successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, profileResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 403) {
                    Toast.makeText(context, "Access denied. Cook account required.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Profile setup failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addMeal(String name, String description, double price, String category) {
        String token = RetrofitClient.getAuthToken(context);

        if (token == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        MealRequest request = new MealRequest(name, price);
        request.setDescription(description);
        request.setCategory(category);
        request.setIsAvailable(true);

        apiService.addMeal(token, request).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MealResponse mealResponse = response.body();

                    if (mealResponse.isSuccess()) {
                        Toast.makeText(context, "Meal added successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, mealResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to add meal", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getAllMeals(final MealsCallback callback) {
        apiService.getAllMeals().enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MealResponse mealResponse = response.body();

                    if (mealResponse.isSuccess() && mealResponse.getMeals() != null) {
                        if (callback != null) {
                            callback.onMealsLoaded(mealResponse.getMeals());
                        }
                    } else {
                        Toast.makeText(context, "No meals found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Failed to load meals", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onMealsLoadFailed(t.getMessage());
                }
            }
        });
    }

    public void getAllCooks(final CooksCallback callback) {
        apiService.getAllCooks().enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CookProfileResponse profileResponse = response.body();

                    if (profileResponse.isSuccess() && profileResponse.getCooks() != null) {
                        if (callback != null) {
                            callback.onCooksLoaded(profileResponse.getCooks());
                        }
                    } else {
                        Toast.makeText(context, "No cooks found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Failed to load cooks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onCooksLoadFailed(t.getMessage());
                }
            }
        });
    }

    public void logout() {
        String token = RetrofitClient.getAuthToken(context);

        if (token != null) {
            apiService.logout(token).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    RetrofitClient.clearAuthToken(context);
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    RetrofitClient.clearAuthToken(context);
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    public interface LoginCallback {
        void onLoginSuccess(LoginResponse.User user);
        void onLoginFailed(String error);
    }

    public interface MealsCallback {
        void onMealsLoaded(java.util.List<Meal> meals);
        void onMealsLoadFailed(String error);
    }

    public interface CooksCallback {
        void onCooksLoaded(java.util.List<CookProfile> cooks);
        void onCooksLoadFailed(String error);
    }
}
