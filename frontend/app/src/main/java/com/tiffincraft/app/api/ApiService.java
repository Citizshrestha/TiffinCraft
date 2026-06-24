package com.tiffincraft.app.api;

import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.FacebookLoginRequest;
import com.tiffincraft.app.models.ForgotPasswordRequest;
import com.tiffincraft.app.models.GoogleLoginRequest;
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.MealRequest;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.OtpRequest;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.ResendOtpRequest;
import com.tiffincraft.app.models.ResetPasswordRequest;

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

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/verify-otp")
    Call<LoginResponse> verifyOtp(@Body OtpRequest request);

    @POST("auth/resend-otp")
    Call<RegisterResponse> resendOtp(@Body ResendOtpRequest request);

    @POST("auth/forgot-password")
    Call<RegisterResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/reset-password")
    Call<RegisterResponse> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/google/verify")
    Call<LoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @POST("auth/facebook/verify")
    Call<LoginResponse> facebookLogin(@Body FacebookLoginRequest request);

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
