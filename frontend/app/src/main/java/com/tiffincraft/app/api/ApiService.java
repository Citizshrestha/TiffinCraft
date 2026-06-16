package com.tiffincraft.app.api;

import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.MealRequest;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Health check endpoint
    @GET("../health")
    Call<ResponseBody> healthCheck();

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/logout")
    Call<RegisterResponse> logout(@Header("Authorization") String token);

    @GET("auth/me")
    Call<LoginResponse> getCurrentUser(@Header("Authorization") String token);


    @POST("cook/profile")
    Call<CookProfileResponse> setupCookProfile(
            @Header("Authorization") String token,
            @Body CookProfileRequest request
    );

    @GET("cook/profile")
    Call<CookProfileResponse> getMyCookProfile(
            @Header("Authorization") String token
    );

    @PUT("cook/profile")
    Call<CookProfileResponse> updateCookProfile(
            @Header("Authorization") String token,
            @Body CookProfileRequest request
    );

    @GET("cook")
    Call<CookProfileResponse> getAllCooks();

    @GET("cook/{cookId}")
    Call<CookProfileResponse> getCookById(@Path("cookId") int cookId);


    @GET("meals")
    Call<MealResponse> getAllMeals(
            @Query("category") String category,
            @Query("cuisine_type") String cuisineType,
            @Query("is_vegetarian") String isVegetarian,
            @Query("is_vegan") String isVegan,
            @Query("max_price") String maxPrice
    );

    @GET("meals")
    Call<MealResponse> getAllMeals();

    @GET("meals/cook/{cookId}")
    Call<MealResponse> getMealsByCook(@Path("cookId") int cookId);

    @GET("meals/{mealId}")
    Call<MealResponse> getMealById(@Path("mealId") int mealId);

    @POST("meals")
    Call<MealResponse> addMeal(
            @Header("Authorization") String token,
            @Body MealRequest request
    );

    @GET("meals/my/list")
    Call<MealResponse> getMyMeals(
            @Header("Authorization") String token
    );

    @PUT("meals/{mealId}")
    Call<MealResponse> updateMeal(
            @Header("Authorization") String token,
            @Path("mealId") int mealId,
            @Body MealRequest request
    );

    @DELETE("meals/{mealId}")
    Call<MealResponse> deleteMeal(
            @Header("Authorization") String token,
            @Path("mealId") int mealId
    );
}
