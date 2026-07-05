package com.tiffincraft.app.api;

import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.CustomerProfileRequest;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.DashboardResponse;
import com.tiffincraft.app.models.ForgotPasswordRequest;
import com.tiffincraft.app.models.GoogleLoginRequest;
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.MealRequest;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.OtpRequest;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.ResendOtpRequest;
import com.tiffincraft.app.models.ResetPasswordRequest;
import com.tiffincraft.app.models.UpdateOrderStatusRequest;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.UpdateCartItemRequest;
import com.tiffincraft.app.models.CheckoutRequest;
import com.tiffincraft.app.models.CheckoutResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
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

    @PUT("cook/profile/complete")
    Call<CookProfileResponse> updateCookCompleteProfile(
            @Header("Authorization") String token,
            @Body CookProfileRequest request
    );

    @GET("cook/profile")
    Call<CookProfileResponse> getCookProfile(
            @Header("Authorization") String token
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

    @GET("meals/my")
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

    @Multipart
    @POST("meals/{mealId}/image")
    Call<MealResponse> uploadMealImage(
            @Header("Authorization") String token,
            @Path("mealId") int mealId,
            @Part MultipartBody.Part mealImage
    );

    // ====== NEW CLOUDINARY UPLOAD ENDPOINTS ======
    
    // Upload meal image to Cloudinary (use this for new meal images)
    @Multipart
    @POST("upload/meal-image")
    Call<UploadResponse> uploadMealImageCloudinary(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );

    // Upload profile image to Cloudinary (for both cook and customer)
    @Multipart
    @POST("upload/profile-image")
    Call<UploadResponse> uploadProfileImageCloudinary(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );

    // Upload document to Cloudinary (certificates, licenses)
    @Multipart
    @POST("upload/document")
    Call<UploadResponse> uploadDocumentCloudinary(
            @Header("Authorization") String token,
            @Part MultipartBody.Part document
    );

    // Delete image from Cloudinary
    @DELETE("upload/image")
    Call<RegisterResponse> deleteImageCloudinary(
            @Header("Authorization") String token,
            @Body com.google.gson.JsonObject requestBody
    );

    // ====== LEGACY UPLOAD ENDPOINTS (DEPRECATED) ======
    
    // Upload cook profile image (OLD - consider migrating to Cloudinary)
    @Multipart
    @POST("cook/profile/image")
    Call<UploadResponse> uploadCookProfileImage(
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part profile_image
    );

    // Upload customer profile image (OLD - consider migrating to Cloudinary)
    @Multipart
    @POST("auth/profile/image")
    Call<UploadResponse> uploadCustomerProfileImage(
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part profile_image
    );

    // Customer profile endpoints
    @GET("auth/profile")
    Call<CustomerProfileResponse> getCustomerProfile(
            @Header("Authorization") String token
    );

    @PUT("auth/profile")
    Call<CustomerProfileResponse> updateCustomerProfile(
            @Header("Authorization") String token,
            @Body CustomerProfileRequest request
    );

    // Customer dashboard endpoints
    @GET("customer/dashboard")
    Call<com.tiffincraft.app.models.CustomerDashboardResponse> getCustomerDashboard(
            @Header("Authorization") String token
    );

    @GET("customer/notifications")
    Call<com.tiffincraft.app.models.NotificationResponse> getCustomerNotifications(
            @Header("Authorization") String token
    );

    @PUT("customer/notifications/{id}/read")
    Call<RegisterResponse> markCustomerNotificationAsRead(
            @Header("Authorization") String token,
            @Path("id") int notificationId
    );

    @PUT("customer/notifications/read-all")
    Call<RegisterResponse> markAllNotificationsAsRead(
            @Header("Authorization") String token
    );

    // Get cook dashboard statistics
    @GET("cook/dashboard")
    Call<DashboardResponse> getCookDashboard(
            @Header("Authorization") String authToken
    );

    // Notifications endpoints
    @GET("notifications")
    Call<NotificationResponse> getNotifications(
            @Header("Authorization") String token
    );

    @GET("notifications/unread-count")
    Call<NotificationResponse> getUnreadNotificationCount(
            @Header("Authorization") String token
    );

    @PUT("notifications/{id}/read")
    Call<NotificationResponse> markNotificationAsRead(
            @Header("Authorization") String token,
            @Path("id") int notificationId
    );

    // Cook profile management endpoints
    @PUT("cook/profile/holiday-mode")
    Call<CookProfileResponse> updateHolidayMode(
            @Header("Authorization") String token,
            @Body com.google.gson.JsonObject requestBody
    );

    @PUT("cook/profile/operating-hours")
    Call<CookProfileResponse> updateOperatingHours(
            @Header("Authorization") String token,
            @Body com.google.gson.JsonObject requestBody
    );

    @PUT("cook/profile/bank-details")
    Call<CookProfileResponse> updateBankDetails(
            @Header("Authorization") String token,
            @Body com.google.gson.JsonObject requestBody
    );

    // Cook Reviews endpoints
    @GET("reviews/cook/my")
    Call<com.tiffincraft.app.models.ReviewResponse> getMyCookReviews(
            @Header("Authorization") String token
    );

    @PUT("reviews/{reviewId}/reply")
    Call<RegisterResponse> replyToReview(
            @Header("Authorization") String token,
            @Path("reviewId") int reviewId,
            @Body com.google.gson.JsonObject requestBody
    );

    // Cook Orders endpoints
    @GET("orders/cook/my")
    Call<OrderResponse> getCookOrders(
            @Header("Authorization") String token
    );

    @GET("orders/cook/my")
    Call<OrderResponse> getCookOrdersByStatus(
            @Header("Authorization") String token,
            @Query("status") String status
    );

    @PUT("orders/{orderId}/status")
    Call<OrderResponse> updateOrderStatus(
            @Header("Authorization") String token,
            @Path("orderId") int orderId,
            @Body UpdateOrderStatusRequest request
    );

    // Payment verification endpoints
    @POST("orders/{orderId}/payment-screenshot")
    Call<OrderResponse> uploadPaymentScreenshot(
            @Header("Authorization") String token,
            @Path("orderId") int orderId,
            @Body com.google.gson.JsonObject requestBody
    );

    @PUT("orders/{orderId}/verify-payment")
    Call<OrderResponse> verifyPayment(
            @Header("Authorization") String token,
            @Path("orderId") int orderId,
            @Body com.google.gson.JsonObject requestBody
    );

    // Favorites endpoints
    @GET("favorites")
    Call<com.tiffincraft.app.models.FavoriteResponse> getFavorites(
            @Header("Authorization") String token
    );

    @POST("favorites")
    Call<com.tiffincraft.app.models.FavoriteResponse> addToFavorites(
            @Header("Authorization") String token,
            @Body com.google.gson.JsonObject requestBody
    );

    @DELETE("favorites/{cookId}")
    Call<com.tiffincraft.app.models.FavoriteResponse> removeFromFavorites(
            @Header("Authorization") String token,
            @Path("cookId") int cookId
    );

    @GET("favorites/check/{cookId}")
    Call<com.tiffincraft.app.models.FavoriteResponse> checkFavoriteStatus(
            @Header("Authorization") String token,
            @Path("cookId") int cookId
    );

    @GET("cart")
Call<CartResponse> getCart(@Header("Authorization") String token);

@POST("cart")
Call<CartResponse> addToCart(@Header("Authorization") String token, @Body AddToCartRequest request);

@PUT("cart/{cartItemId}")
Call<CartResponse> updateCartItem(@Header("Authorization") String token, @Path("cartItemId") int cartItemId, @Body UpdateCartItemRequest request);

@DELETE("cart/{cartItemId}")
Call<CartResponse> removeCartItem(@Header("Authorization") String token, @Path("cartItemId") int cartItemId);

@DELETE("cart")
Call<CartResponse> clearCart(@Header("Authorization") String token);

@POST("cart/checkout")
Call<CheckoutResponse> checkoutCart(@Header("Authorization") String token, @Body CheckoutRequest request);
}
