package com.tiffincraft.app.api;

import com.google.gson.JsonObject;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.ChatContactsResponse;
import com.tiffincraft.app.models.ChatConversationsResponse;
import com.tiffincraft.app.models.ChatMessagesResponse;
import com.tiffincraft.app.models.ChangePasswordResponse;
import com.tiffincraft.app.models.CheckoutRequest;
import com.tiffincraft.app.models.CheckoutResponse;
import com.tiffincraft.app.models.CookEarningsTotalsResponse;
import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.CreateConversationRequest;
import com.tiffincraft.app.models.CreateConversationResponse;
import com.tiffincraft.app.models.CustomerDashboardResponse;
import com.tiffincraft.app.models.CustomerProfileRequest;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.DashboardResponse;
import com.tiffincraft.app.models.DeleteChatMessagesRequest;
import com.tiffincraft.app.models.EarningsSummaryResponse;
import com.tiffincraft.app.models.EarningsTransactionsResponse;
import com.tiffincraft.app.models.EditChatMessageRequest;
import com.tiffincraft.app.models.FavoriteResponse;
import com.tiffincraft.app.models.ForgotPasswordRequest;
import com.tiffincraft.app.models.GoogleLoginRequest;
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.MealRequest;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.OtpRequest;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.ResendOtpRequest;
import com.tiffincraft.app.models.ResetPasswordRequest;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.models.SendChatMessageRequest;
import com.tiffincraft.app.models.SendChatMessageResponse;
import com.tiffincraft.app.models.UpdateCartItemRequest;
import com.tiffincraft.app.models.UpdateOrderStatusRequest;
import com.tiffincraft.app.models.UploadResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // AUTH
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("auth/google")
    Call<LoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @POST("auth/verify-otp")
    Call<LoginResponse> verifyOtp(@Body OtpRequest request);

    @POST("auth/resend-otp")
    Call<RegisterResponse> resendOtp(@Body ResendOtpRequest request);

    @POST("auth/forgot-password")
    Call<RegisterResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/reset-password")
    Call<RegisterResponse> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/logout")
    Call<RegisterResponse> logout(@Header("Authorization") String token);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // CART
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("cart")
    Call<CartResponse> getCart(@Header("Authorization") String token);

    @POST("cart/add")
    Call<CartResponse> addToCart(@Header("Authorization") String token,
                                 @Body AddToCartRequest request);

    @PUT("cart/item/{cartItemId}")
    Call<CartResponse> updateCartItem(@Header("Authorization") String token,
                                      @Path("cartItemId") int cartItemId,
                                      @Body UpdateCartItemRequest request);

    @DELETE("cart/item/{cartItemId}")
    Call<CartResponse> removeCartItem(@Header("Authorization") String token,
                                      @Path("cartItemId") int cartItemId);

    @DELETE("cart")
    Call<CartResponse> clearCart(@Header("Authorization") String token);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // CUSTOMER DASHBOARD & PROFILE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("customer/dashboard")
    Call<CustomerDashboardResponse> getCustomerDashboard(@Header("Authorization") String token);

    @GET("customer/profile")
    Call<CustomerProfileResponse> getCustomerProfile(@Header("Authorization") String token);

    @PUT("customer/profile")
    Call<CustomerProfileResponse> updateCustomerProfile(@Header("Authorization") String token,
                                                        @Body CustomerProfileRequest body);

    @Multipart
    @POST("customer/profile/image")
    Call<UploadResponse> uploadCustomerProfileImage(@Header("Authorization") String token,
                                                    @Part MultipartBody.Part profile_image);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // FAVORITES
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("favorites")
    Call<FavoriteResponse> getFavorites(@Header("Authorization") String token);

    @POST("favorites/add")
    Call<FavoriteResponse> addToFavorites(@Header("Authorization") String token,
                                          @Body JsonObject body);

    @DELETE("favorites/{cookId}")
    Call<FavoriteResponse> removeFromFavorites(@Header("Authorization") String token,
                                               @Path("cookId") int cookId);

    @GET("favorites/check/{cookId}")
    Call<FavoriteResponse> checkFavoriteStatus(@Header("Authorization") String token,
                                               @Path("cookId") int cookId);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // COOK DASHBOARD & PROFILE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("cook/dashboard")
    Call<DashboardResponse> getCookDashboard(@Header("Authorization") String token);

    @GET("cook/profile")
    Call<CookProfileResponse> getCookProfile(@Header("Authorization") String token);

    @GET("cook/profile")
    Call<CookProfileResponse> getMyCookProfile(@Header("Authorization") String token);

    /** Sets up the cook profile (initial creation). Maps to POST /cook/profile */
    @POST("cook/profile")
    Call<CookProfileResponse> setupCookProfile(@Header("Authorization") String token,
                                               @Body CookProfileRequest request);

    @PUT("cook/profile")
    Call<CookProfileResponse> updateCookProfile(@Header("Authorization") String token,
                                                @Body JsonObject body);

    @PUT("cook/profile/complete")
    Call<CookProfileResponse> updateCookCompleteProfile(@Header("Authorization") String token,
                                                        @Body CookProfileRequest body);

    @Multipart
    @POST("cook/profile/image")
    Call<UploadResponse> uploadCookProfileImage(@Header("Authorization") String token,
                                                @Part MultipartBody.Part profile_image);

    @GET("cook")
    Call<CookProfileResponse> getAllCooks();

    @GET("cook/{cookId}")
    Call<CookProfileResponse> getCookById(@Path("cookId") int cookId);

    @GET("orders/cook/my")
    Call<OrderResponse> getCookOrders(@Header("Authorization") String token);

    @PUT("orders/{orderId}/status")
    Call<OrderResponse> updateOrderStatus(@Header("Authorization") String token,
                                          @Path("orderId") int orderId,
                                          @Body UpdateOrderStatusRequest request);

    @PUT("cook/profile/holiday-mode")
    Call<CookProfileResponse> updateHolidayMode(@Header("Authorization") String token,
                                                @Body JsonObject body);

    @PUT("cook/profile/bank-details")
    Call<CookProfileResponse> updateBankDetails(@Header("Authorization") String token,
                                                @Body JsonObject body);

    @PUT("cook/profile/operating-hours")
    Call<CookProfileResponse> updateOperatingHours(@Header("Authorization") String token,
                                                    @Body JsonObject body);

    @PUT("auth/change-password")
    Call<ChangePasswordResponse> changePassword(@Header("Authorization") String token,
                                                 @Body JsonObject body);

    @Multipart
    @POST("upload/document")
    Call<UploadResponse> uploadDocumentCloudinary(@Header("Authorization") String token,
                                                  @Part MultipartBody.Part image);

    /** Uploads to cook/&lt;cookName&gt;/Bank Details/&lt;Esewa|Khalti|Bank&gt;/ in Cloudinary. */
    @Multipart
    @POST("upload/bank-qr")
    Call<UploadResponse> uploadBankQr(@Header("Authorization") String token,
                                      @Part("qrType") okhttp3.RequestBody qrType,
                                      @Part MultipartBody.Part document);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // MEALS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("meals/cook/{cookId}")
    Call<MealResponse> getMealsByCook(@Path("cookId") int cookId);

    @GET("meals/my")
    Call<MealResponse> getMyMeals(@Header("Authorization") String token);

    /** Returns all meals. Maps to GET /meals */
    @GET("meals")
    Call<MealResponse> getAllMeals();

    @POST("meals")
    Call<MealResponse> addMeal(@Header("Authorization") String token,
                               @Body MealRequest request);

    @PUT("meals/{mealId}")
    Call<MealResponse> updateMeal(@Header("Authorization") String token,
                                  @Path("mealId") int mealId,
                                  @Body MealRequest request);

    @DELETE("meals/{mealId}")
    Call<MealResponse> deleteMeal(@Header("Authorization") String token,
                                  @Path("mealId") int mealId);

    @Multipart
    @POST("upload/meal-image")
    Call<UploadResponse> uploadMealImageCloudinary(@Header("Authorization") String token,
                                                   @Part MultipartBody.Part image);

    @Multipart
    @POST("upload/chat-media")
    Call<UploadResponse> uploadChatMediaCloudinary(@Header("Authorization") String token,
                                                   @Part MultipartBody.Part media);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // ORDERS (Customer)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @POST("orders")
    Call<CheckoutResponse> placeOrder(@Header("Authorization") String token,
                                      @Body CheckoutRequest request);

    @GET("orders")
    Call<OrderResponse> getCustomerOrders(@Header("Authorization") String token);

    @GET("orders/{orderId}")
    Call<OrderResponse> getOrderDetails(@Header("Authorization") String token,
                                        @Path("orderId") int orderId);

    // ═══════════════════════════════════════════════════════════
    // EARNINGS (Cook)
    // ═══════════════════════════════════════════════════════════

    /** All-time totals (total orders, total earned) for the profile stats row. */
    @GET("orders/cook/earnings")
    Call<CookEarningsTotalsResponse> getCookEarningsTotals(@Header("Authorization") String token);

    @GET("orders/cook/earnings/summary")
    Call<EarningsSummaryResponse> getCookEarningsSummary(@Header("Authorization") String token);

    @GET("orders/cook/earnings/summary")
    Call<EarningsSummaryResponse> getCookEarningsSummaryByMonth(@Header("Authorization") String token,
                                                                 @Query("month") int month,
                                                                 @Query("year") int year);

    @GET("orders/cook/earnings/transactions")
    Call<EarningsTransactionsResponse> getCookEarningsTransactions(@Header("Authorization") String token,
                                                                    @Query("page") int page,
                                                                    @Query("limit") int limit,
                                                                    @Query("search") String search);


    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // NOTIFICATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("notifications")
    Call<NotificationResponse> getNotifications(@Header("Authorization") String token);

    @GET("notifications/unread-count")
    Call<NotificationResponse> getUnreadNotificationCount(@Header("Authorization") String token);

    @PUT("notifications/{notificationId}/read")
    Call<NotificationResponse> markNotificationAsRead(@Header("Authorization") String token,
                                                      @Path("notificationId") int notificationId);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // REVIEWS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("reviews/my")
    Call<ReviewResponse> getMyCookReviews(@Header("Authorization") String token);

    @PUT("reviews/{reviewId}/reply")
    Call<RegisterResponse> replyToReview(@Header("Authorization") String token,
                                         @Path("reviewId") int reviewId,
                                         @Body JsonObject body);

    @POST("reviews")
    Call<ReviewResponse> submitReview(@Header("Authorization") String token,
                                      @Body JsonObject body);

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // CHAT
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @GET("chat/conversations")
    Call<ChatConversationsResponse> getChatConversations(@Header("Authorization") String token);

    @POST("chat/conversations")
    Call<CreateConversationResponse> createChatConversation(@Header("Authorization") String token,
                                                            @Body CreateConversationRequest request);

    @GET("chat/contacts")
    Call<ChatContactsResponse> getChatContacts(@Header("Authorization") String token);

    @GET("chat/conversations/{conversationId}/messages")
    Call<ChatMessagesResponse> getChatMessages(@Header("Authorization") String token,
                                               @Path("conversationId") int conversationId,
                                               @Query("before_id") String before,
                                               @Query("limit") int limit);

    @POST("chat/conversations/{conversationId}/messages")
    Call<SendChatMessageResponse> sendChatMessage(@Header("Authorization") String token,
                                                  @Path("conversationId") int conversationId,
                                                  @Body SendChatMessageRequest request);

    @PUT("chat/conversations/{conversationId}/messages/{messageId}")
    Call<SendChatMessageResponse> editChatMessage(@Header("Authorization") String token,
                                                  @Path("conversationId") int conversationId,
                                                  @Path("messageId") int messageId,
                                                  @Body EditChatMessageRequest request);

    @DELETE("chat/conversations/{conversationId}/messages/{messageId}")
    Call<RegisterResponse> deleteChatMessage(@Header("Authorization") String token,
                                             @Path("conversationId") int conversationId,
                                             @Path("messageId") int messageId);

    // Plain @DELETE rejects a @Body at runtime ("Non-body HTTP method cannot
    // contain @Body"), so use @HTTP with hasBody = true for the bulk delete.
    @HTTP(method = "DELETE", path = "chat/conversations/{conversationId}/messages", hasBody = true)
    Call<RegisterResponse> deleteChatMessages(@Header("Authorization") String token,
                                              @Path("conversationId") int conversationId,
                                              @Body DeleteChatMessagesRequest request);

    @PUT("chat/conversations/{conversationId}/read")
    Call<RegisterResponse> markChatConversationRead(@Header("Authorization") String token,
                                                    @Path("conversationId") int conversationId);
}
