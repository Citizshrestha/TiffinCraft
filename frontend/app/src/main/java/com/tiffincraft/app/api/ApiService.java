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
import com.tiffincraft.app.models.ChatConversationsResponse;
import com.tiffincraft.app.models.ChatMessagesResponse;
import com.tiffincraft.app.models.ChatContactsResponse;
import com.tiffincraft.app.models.CreateConversationRequest;
import com.tiffincraft.app.models.CreateConversationResponse;
import com.tiffincraft.app.models.SendChatMessageRequest;
import com.tiffincraft.app.models.SendChatMessageResponse;
import com.tiffincraft.app.models.EditChatMessageRequest;
import com.tiffincraft.app.models.DeleteChatMessagesRequest;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.models.EarningsSummaryResponse;
import com.tiffincraft.app.models.EarningsTransactionsResponse;
import com.tiffincraft.app.models.CookEarningsTotalsResponse;
import com.tiffincraft.app.models.ChangePasswordResponse;
import com.tiffincraft.app.models.ChatUnreadCountResponse;
import com.tiffincraft.app.models.CustomerDetailsResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.models.SubscriptionPlanRequest;
import com.tiffincraft.app.models.SubscriptionPlanResponse;
import com.tiffincraft.app.models.CreateCustomerSubscriptionRequest;
import com.tiffincraft.app.models.ReferralInfoResponse;
import com.tiffincraft.app.models.ApplyReferralResponse;
import com.tiffincraft.app.models.EsewaInitiateResponse;
import com.tiffincraft.app.models.EsewaStatusResponse;
import com.tiffincraft.app.models.EpayInitiateResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    Call<CookProfileResponse> setupCookProfile(@Header("Authorization") String token, @Body CookProfileRequest request);

    @GET("cook/profile")
    Call<CookProfileResponse> getMyCookProfile(@Header("Authorization") String token);

    @PUT("cook/profile")
    Call<CookProfileResponse> updateCookProfile(@Header("Authorization") String token, @Body CookProfileRequest request);

    @PUT("cook/profile/complete")
    Call<CookProfileResponse> updateCookCompleteProfile(@Header("Authorization") String token, @Body CookProfileRequest request);

    @GET("cook")
    Call<CookProfileResponse> getAllCooks();

    @GET("cook")
    Call<CookProfileResponse> getAllCooks(@Query("search") String search);

    @GET("cook/{cookId}")
    Call<CookProfileResponse> getCookById(@Path("cookId") int cookId);

    @GET("cook/profile")
    Call<CookProfileResponse> getCookProfile(@Header("Authorization") String token);

    @GET("cook/nearby")
    Call<CookProfileResponse> getNearbyCooks(@Header("Authorization") String token,
                                              @Query("lat") double lat,
                                              @Query("lng") double lng,
                                              @Query("radius_km") double radiusKm);

    @GET("meals")
    Call<MealResponse> getAllMeals(@Query("category") String category, @Query("cuisine_type") String cuisineType, @Query("is_vegetarian") String isVegetarian, @Query("is_vegan") String isVegan, @Query("max_price") String maxPrice);

    @GET("meals")
    Call<MealResponse> getAllMeals();

    @GET("meals/cook/{cookId}")
    Call<MealResponse> getMealsByCook(@Path("cookId") int cookId);

    @GET("meals/{mealId}")
    Call<MealResponse> getMealById(@Path("mealId") int mealId);

    @POST("meals")
    Call<MealResponse> addMeal(@Header("Authorization") String token, @Body MealRequest request);

    @GET("meals/my")
    Call<MealResponse> getMyMeals(@Header("Authorization") String token);

    @PUT("meals/{mealId}")
    Call<MealResponse> updateMeal(@Header("Authorization") String token, @Path("mealId") int mealId, @Body MealRequest request);

    @DELETE("meals/{mealId}")
    Call<MealResponse> deleteMeal(@Header("Authorization") String token, @Path("mealId") int mealId);

    @Multipart
    @POST("meals/{mealId}/image")
    Call<MealResponse> uploadMealImage(@Header("Authorization") String token, @Path("mealId") int mealId, @Part MultipartBody.Part mealImage);

    @Multipart
    @POST("upload/meal-image")
    Call<UploadResponse> uploadMealImageCloudinary(@Header("Authorization") String token, @Part MultipartBody.Part image);

    @Multipart
    @POST("upload/profile-image")
    Call<UploadResponse> uploadProfileImageCloudinary(@Header("Authorization") String token, @Part MultipartBody.Part image);

    @Multipart
    @POST("upload/document")
    Call<UploadResponse> uploadDocumentCloudinary(@Header("Authorization") String token, @Part MultipartBody.Part document);

    @DELETE("upload/image")
    Call<RegisterResponse> deleteImageCloudinary(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @Multipart
    @POST("cook/profile/image")
    Call<UploadResponse> uploadCookProfileImage(@Header("Authorization") String authToken, @Part MultipartBody.Part profile_image);

    @Multipart
    @POST("auth/profile/image")
    Call<UploadResponse> uploadCustomerProfileImage(@Header("Authorization") String authToken, @Part MultipartBody.Part profile_image);

    @GET("auth/profile")
    Call<CustomerProfileResponse> getCustomerProfile(@Header("Authorization") String token);

    @PUT("auth/profile")
    Call<CustomerProfileResponse> updateCustomerProfile(@Header("Authorization") String token, @Body CustomerProfileRequest request);

    @GET("customer/dashboard")
    Call<com.tiffincraft.app.models.CustomerDashboardResponse> getCustomerDashboard(@Header("Authorization") String token);

    @GET("customer/notifications")
    Call<com.tiffincraft.app.models.NotificationResponse> getCustomerNotifications(@Header("Authorization") String token);

    @PUT("customer/notifications/{id}/read")
    Call<RegisterResponse> markCustomerNotificationAsRead(@Header("Authorization") String token, @Path("id") int notificationId);

    @PUT("customer/notifications/read-all")
    Call<RegisterResponse> markAllNotificationsAsRead(@Header("Authorization") String token);

    @GET("cook/dashboard")
    Call<DashboardResponse> getCookDashboard(@Header("Authorization") String authToken);

    @GET("notifications")
    Call<NotificationResponse> getNotifications(@Header("Authorization") String token);

    @GET("notifications/unread-count")
    Call<NotificationResponse> getUnreadNotificationCount(@Header("Authorization") String token);

    @PUT("notifications/{id}/read")
    Call<NotificationResponse> markNotificationAsRead(@Header("Authorization") String token, @Path("id") int notificationId);

    @PUT("cook/profile/holiday-mode")
    Call<CookProfileResponse> updateHolidayMode(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @PUT("cook/profile/operating-hours")
    Call<CookProfileResponse> updateOperatingHours(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @PUT("cook/profile/bank-details")
    Call<CookProfileResponse> updateBankDetails(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @GET("reviews/cook/my")
    Call<com.tiffincraft.app.models.ReviewResponse> getMyCookReviews(@Header("Authorization") String token);

    /** Public — reviews for a cook's profile page. No auth required. */
    @GET("reviews/cook/{cookId}")
    Call<ReviewResponse> getCookReviews(@Path("cookId") int cookId);

    @POST("reviews")
    Call<ReviewResponse> submitReview(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    /** Customer edits their own review. */
    @PUT("reviews/{reviewId}")
    Call<ReviewResponse> updateReview(@Header("Authorization") String token, @Path("reviewId") int reviewId, @Body com.google.gson.JsonObject requestBody);

    /** Customer deletes their own review. */
    @DELETE("reviews/{reviewId}")
    Call<RegisterResponse> deleteReview(@Header("Authorization") String token, @Path("reviewId") int reviewId);

    @PUT("reviews/{reviewId}/reply")
    Call<RegisterResponse> replyToReview(@Header("Authorization") String token, @Path("reviewId") int reviewId, @Body com.google.gson.JsonObject requestBody);

    @GET("orders/cook/my")
    Call<OrderResponse> getCookOrders(@Header("Authorization") String token);

    @GET("orders/cook/my")
    Call<OrderResponse> getCookOrdersByStatus(@Header("Authorization") String token, @Query("status") String status);

    @GET("orders/customer/my")
    Call<OrderResponse> getCustomerOrders(@Header("Authorization") String token);

    @GET("orders/{orderId}")
    Call<OrderResponse> getOrderDetails(@Header("Authorization") String token, @Path("orderId") int orderId);

    @PUT("orders/{orderId}/cancel")
    Call<RegisterResponse> cancelOrder(@Header("Authorization") String token, @Path("orderId") int orderId, @Body com.google.gson.JsonObject requestBody);

    @PUT("orders/{orderId}/status")
    Call<OrderResponse> updateOrderStatus(@Header("Authorization") String token, @Path("orderId") int orderId, @Body UpdateOrderStatusRequest request);

    @POST("orders/{orderId}/payment-screenshot")
    Call<RegisterResponse> uploadPaymentScreenshot(@Header("Authorization") String token, @Path("orderId") int orderId, @Body com.google.gson.JsonObject requestBody);

    @PUT("orders/{orderId}/verify-payment")
    Call<RegisterResponse> verifyPayment(@Header("Authorization") String token, @Path("orderId") int orderId, @Body com.google.gson.JsonObject requestBody);

    @DELETE("orders/{orderId}")
    Call<RegisterResponse> deleteOrder(@Header("Authorization") String token, @Path("orderId") int orderId);

    @GET("orders/cook/earnings")
    Call<CookEarningsTotalsResponse> getCookEarningsTotals(@Header("Authorization") String token);

    @GET("orders/cook/earnings/summary")
    Call<EarningsSummaryResponse> getCookEarningsSummary(@Header("Authorization") String token);

    @GET("orders/cook/earnings/summary")
    Call<EarningsSummaryResponse> getCookEarningsSummaryByMonth(@Header("Authorization") String token, @Query("month") int month, @Query("year") int year);

    @GET("orders/cook/earnings/transactions")
    Call<EarningsTransactionsResponse> getCookEarningsTransactions(@Header("Authorization") String token, @Query("page") int page, @Query("limit") int limit, @Query("search") String search);

    // ── Commission settlement (cook pays platform commission — see commissionController.js) ──
    @GET("commission/settlements/current")
    Call<com.tiffincraft.app.models.CommissionSettlementCurrentResponse> getCurrentCommissionSettlement(@Header("Authorization") String token);

    @GET("commission/settlements/mine")
    Call<com.tiffincraft.app.models.CommissionSettlementsListResponse> getMyCommissionSettlements(@Header("Authorization") String token);

    @PUT("commission/settlements/{id}/screenshot")
    Call<RegisterResponse> uploadCommissionScreenshot(@Header("Authorization") String token, @Path("id") int settlementId, @Body com.google.gson.JsonObject requestBody);

    /** Turns the open month's accrual into a payable bill (cook taps "Pay Now" before month close). */
    @POST("commission/settlements/settle-now")
    Call<RegisterResponse> settleCommissionNow(@Header("Authorization") String token);

    @GET("commission/admin-qr")
    Call<com.tiffincraft.app.models.AdminQrResponse> getAdminQr(@Header("Authorization") String token);

    @GET("favorites")
    Call<com.tiffincraft.app.models.FavoriteResponse> getFavorites(@Header("Authorization") String token);

    @POST("favorites")
    Call<com.tiffincraft.app.models.FavoriteResponse> addToFavorites(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @DELETE("favorites/{cookId}")
    Call<com.tiffincraft.app.models.FavoriteResponse> removeFromFavorites(@Header("Authorization") String token, @Path("cookId") int cookId);

    @GET("favorites/check/{cookId}")
    Call<com.tiffincraft.app.models.FavoriteResponse> checkFavoriteStatus(@Header("Authorization") String token, @Path("cookId") int cookId);

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

    @GET("chat/conversations")
    Call<ChatConversationsResponse> getChatConversations(@Header("Authorization") String token, @Query("search") String search);

    @GET("chat/unread-count")
    Call<ChatUnreadCountResponse> getChatUnreadCount(@Header("Authorization") String token);

    @POST("chat/conversations")
    Call<CreateConversationResponse> createChatConversation(@Header("Authorization") String token, @Body CreateConversationRequest request);

    @GET("chat/conversations/{conversationId}/messages")
    Call<ChatMessagesResponse> getChatMessages(@Header("Authorization") String token, @Path("conversationId") int conversationId, @Query("before_id") Integer beforeId, @Query("limit") Integer limit);

    @POST("chat/conversations/{conversationId}/messages")
    Call<SendChatMessageResponse> sendChatMessage(@Header("Authorization") String token, @Path("conversationId") int conversationId, @Body SendChatMessageRequest request);

    @PUT("chat/conversations/{conversationId}/read")
    Call<RegisterResponse> markChatConversationRead(@Header("Authorization") String token, @Path("conversationId") int conversationId);

    @GET("chat/contacts")
    Call<ChatContactsResponse> getChatContacts(@Header("Authorization") String token, @Query("search") String search);

    @PUT("chat/conversations/{conversationId}/messages/{messageId}")
    Call<SendChatMessageResponse> editChatMessage(@Header("Authorization") String token, @Path("conversationId") int conversationId, @Path("messageId") int messageId, @Body EditChatMessageRequest request);

    @POST("chat/conversations/{conversationId}/messages/delete")
    Call<RegisterResponse> deleteChatMessages(@Header("Authorization") String token, @Path("conversationId") int conversationId, @Body DeleteChatMessagesRequest request);

    @Multipart
    @POST("upload/chat-media")
    Call<UploadResponse> uploadChatMediaCloudinary(@Header("Authorization") String token, @Part MultipartBody.Part media);

    /** Cook's payment QR upload — backend reads the type from a "qrType" text field. */
    @Multipart
    @POST("upload/bank-qr")
    Call<UploadResponse> uploadBankQr(@Header("Authorization") String token, @Part("qrType") RequestBody qrType, @Part MultipartBody.Part document);

    @PUT("auth/change-password")
    Call<ChangePasswordResponse> changePassword(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @DELETE("auth/account")
    Call<RegisterResponse> deleteAccount(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    /** Cook-only — read-only view of a customer's profile (e.g. from a chat header). */
    @GET("customer/{customerId}")
    Call<CustomerDetailsResponse> getCustomerById(@Header("Authorization") String token, @Path("customerId") int customerId);

    @GET("subscriptions/customer/my")
    Call<SubscriptionResponse> getMySubscriptions(@Header("Authorization") String token);

    @POST("subscriptions")
    Call<com.tiffincraft.app.models.CreateSubscriptionResponse> createSubscription(@Header("Authorization") String token, @Body CreateCustomerSubscriptionRequest request);

    /**
     * Payment-first subscribe. Body is {cook_id, plan_id, delivery_address} —
     * deliberately NO amount and NO customer_id: the backend charges the plan
     * price it has stored and takes the customer from the JWT. Creates the
     * subscription as pending_payment and returns signed eSewa ePay form
     * fields; the row is only activated after the backend verifies the payment
     * with eSewa. Rate-limited per user.
     */
    @POST("subscriptions/initiate")
    Call<EpayInitiateResponse> initiateSubscriptionPayment(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @PUT("subscriptions/{id}/pause")
    Call<RegisterResponse> pauseSubscription(@Header("Authorization") String token, @Path("id") int subscriptionId);

    @PUT("subscriptions/{id}/resume")
    Call<RegisterResponse> resumeSubscription(@Header("Authorization") String token, @Path("id") int subscriptionId);

    /**
     * Cancel a subscription — and find out what it costs.
     *
     * Returns SubscriptionActionResponse, not a bare acknowledgement, because the
     * reply carries the money decision: `amount_owed` (the full plan amount once
     * the cook had confirmed the payment, 0 before that) and `refund_due`. The
     * customer has to be told that from the response, not from an audit log.
     */
    @DELETE("subscriptions/{id}")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> cancelSubscription(@Header("Authorization") String token, @Path("id") int subscriptionId);

    // ==================== Subscription Plans (cook-authored) ====================

    @POST("subscription-plans")
    Call<SubscriptionPlanResponse> createSubscriptionPlan(@Header("Authorization") String token, @Body SubscriptionPlanRequest request);

    @GET("subscription-plans/my")
    Call<SubscriptionPlanResponse> getMySubscriptionPlans(@Header("Authorization") String token);

    /** Public — powers the "Subscription Plans" section on a cook's profile page. */
    @GET("subscription-plans/cook/{cookId}")
    Call<SubscriptionPlanResponse> getSubscriptionPlansByCook(@Path("cookId") int cookId);

    @GET("subscription-plans/{id}")
    Call<SubscriptionPlanResponse> getSubscriptionPlanById(@Path("id") int planId);

    @PUT("subscription-plans/{id}")
    Call<SubscriptionPlanResponse> updateSubscriptionPlan(@Header("Authorization") String token, @Path("id") int planId, @Body SubscriptionPlanRequest request);

    @DELETE("subscription-plans/{id}")
    Call<RegisterResponse> deleteSubscriptionPlan(@Header("Authorization") String token, @Path("id") int planId);

    @GET("subscriptions/cook/my")
    Call<com.tiffincraft.app.models.CookSubscribersResponse> getCookSubscribers(@Header("Authorization") String token);

    @PUT("subscriptions/{id}/screenshot")
    Call<RegisterResponse> uploadSubscriptionScreenshot(@Header("Authorization") String token, @Path("id") int subscriptionId, @Body com.google.gson.JsonObject requestBody);

    @PUT("subscriptions/{id}/verify-payment")
    Call<RegisterResponse> verifySubscriptionPayment(@Header("Authorization") String token, @Path("id") int subscriptionId, @Body com.google.gson.JsonObject requestBody);

    // ==================== Per-day delivery schedule ====================
    //
    // Note the path prefix on the cook endpoints: the server mounts these under
    // "/api/cook" (SINGULAR). "cooks/..." 404s.

    /**
     * The next ~14 delivery days for one subscription, each with its real logged
     * status and whether it can still be changed. Served to the owning customer
     * AND to the cook of that subscription; anyone else gets 403.
     *
     * Replaces the flat "Active" label — that showed a single on/off flag and a
     * raw ISO timestamp for the next delivery.
     */
    @GET("subscriptions/{id}/calendar")
    Call<com.tiffincraft.app.models.SubscriptionCalendarResponse> getSubscriptionCalendar(
            @Header("Authorization") String token, @Path("id") int subscriptionId);

    /**
     * Customer skips one day. Body is {date: 'YYYY-MM-DD', reason?}.
     *
     * Free, and it makes the subscription one day LONGER: the day is skipped
     * before it arrives, so that meal moves to the end and end_date shifts out by
     * one. The response carries the new end_date and an `extended` flag. Rejected
     * with a specific message (not a generic
     * failure) once that date's cutoff has passed.
     */
    @POST("subscriptions/{id}/skip-day")
    Call<com.tiffincraft.app.models.DayActionResponse> skipSubscriptionDay(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /**
     * Cook states that one day's meal has left the kitchen. Body is
     * {date: 'YYYY-MM-DD', note?}.
     *
     * TODAY ONLY — the server refuses a future date (nothing has been sent yet)
     * and a past one (already settled by the nightly reconcile, and that verdict
     * is what a dispute rests on). Moves the day to 'sent', which is NOT the end
     * of it: the customer still confirms receipt, and only that writes
     * 'delivered'.
     *
     * Fires a notification and a chat card to the customer from the same backend
     * action, so the two can never disagree about what was announced.
     */
    @POST("subscriptions/{id}/mark-sent")
    Call<com.tiffincraft.app.models.DayActionResponse> markSubscriptionDaySent(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /**
     * Customer confirms one day's meal arrived. Body is {date, note?}.
     *
     * The only path by which a live day becomes 'delivered' on someone's say-so —
     * every other route is the cron inferring it. Accepted only for a day the cook
     * already marked 'sent', which is what makes the pair a handshake rather than
     * two independent flags. Notifies the cook on both channels.
     */
    @POST("subscriptions/{id}/mark-received")
    Call<com.tiffincraft.app.models.DayActionResponse> markSubscriptionDayReceived(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /** Everyone the cook is cooking for on `date` (defaults to today in Nepal Time). */
    @GET("cook/today-deliveries")
    Call<com.tiffincraft.app.models.TodayDeliveriesResponse> getTodayDeliveries(
            @Header("Authorization") String token, @Query("date") String date);

    /**
     * Cook closes the kitchen for a whole date. Body is {date, reason?}.
     *
     * BULK — every one of this cook's active subscribers loses that day, in a
     * single transaction. Nobody is charged for it and nobody's end date moves.
     * Days already delivered, or already skipped by the customer, are left
     * untouched and reported back separately.
     */
    @POST("cook/daily-availability")
    Call<com.tiffincraft.app.models.DayActionResponse> setCookDailyUnavailability(
            @Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    /** Cook reopens a date they had closed, restoring the affected subscriber days. */
    @DELETE("cook/daily-availability/{date}")
    Call<com.tiffincraft.app.models.DayActionResponse> clearCookDailyUnavailability(
            @Header("Authorization") String token, @Path("date") String date);

    // ==================== Request → accept → pay → active flow ====================
    //
    // The cook's accept/reject gate happens BEFORE any money is asked for, which
    // is what separates these from the older pay-first routes above
    // (createSubscription / initiateSubscriptionPayment). Every action here fires
    // the in-app notification, the FCM push, and the chat message from one
    // backend call, so the three can never disagree.

    /**
     * Customer subscribes. Body is {plan_id, delivery_address, start_date, note?}.
     *
     * Creates the row in 'requested' — NOT active, and no payment asked for yet.
     * `start_date` must be 'YYYY-MM-DD'; the server validates it against Nepal
     * Time, so a device with a wrong clock gets a 400 rather than a bad window.
     */
    @POST("subscriptions/request")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> createSubscriptionRequest(
            @Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    /**
     * The cook's Subscription Requests inbox.
     *
     * `filter` is one of pending | requested | awaiting_payment |
     * awaiting_proof_check | all; pass null for the server default ('pending',
     * which mixes new requests and payment proofs because both are decisions
     * blocked on the cook). `counts` in the response always describes the whole
     * inbox, not the filtered slice.
     */
    @GET("subscriptions/cook/requests")
    Call<com.tiffincraft.app.models.SubscriptionRequestsResponse> getCookSubscriptionRequests(
            @Header("Authorization") String token, @Query("filter") String filter);

    /**
     * Cook accepts or rejects a request. Body is {action: 'accept'|'reject', note?}.
     *
     * Guarded server-side on the row still being in 'requested', so two taps (or
     * two devices) can't both decide it — the second gets a 409.
     */
    @PUT("subscriptions/{id}/respond")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> respondToSubscriptionRequest(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /**
     * Customer uploads the payment screenshot. Multipart, part name "proof".
     *
     * Multipart rather than a URL because the server hashes the bytes it actually
     * received (SHA-256) and rejects an image already used for another
     * subscription. Only allowed once the cook has accepted, and again after a
     * rejected attempt.
     */
    @Multipart
    @POST("subscriptions/{id}/payment-proof")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> submitPaymentProof(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Part MultipartBody.Part proof);

    /**
     * Cook verifies or rejects the screenshot. Body is
     * {action: 'verify'|'reject', reason?}.
     *
     * MANUAL, TRUST-BASED: the cook is judging an image by eye and nothing here
     * proves money moved. Rejecting keeps the image on the record for a later
     * dispute and puts the subscription back into a re-uploadable state; it does
     * not delete anything.
     */
    @PUT("subscriptions/{id}/verify-proof")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> verifySubscriptionProof(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /**
     * One subscription in full: stage/headline/detail, the plan's meals, and the
     * audit trail. Served to the owning customer AND the owning cook; `viewer`
     * says which you are. Anyone else gets 403.
     */
    @GET("subscriptions/{id}/detail")
    Call<com.tiffincraft.app.models.SubscriptionDetailResponse> getSubscriptionDetail(
            @Header("Authorization") String token, @Path("id") int subscriptionId);

    // ==================== Per-day custom meal swaps ====================

    /**
     * Customer asks for a different meal on one day. Body is
     * {delivery_date, meal_id?, note?} — at least one of meal_id or note.
     *
     * A structured row tied to the subscription and the date, not a text message:
     * the cook answers it with respondToCustomMealRequest and the cook's daily
     * list shows the swap. Refused with a specific reason for a day that is
     * already skipped, delivered, past its cutoff, or already has a request.
     */
    @POST("subscriptions/{id}/custom-meal")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> createCustomMealRequest(
            @Header("Authorization") String token, @Path("id") int subscriptionId,
            @Body com.google.gson.JsonObject requestBody);

    /** Every swap ever asked for on one subscription, newest delivery day first. */
    @GET("subscriptions/{id}/custom-meals")
    Call<com.tiffincraft.app.models.CustomMealsResponse> getCustomMealRequests(
            @Header("Authorization") String token, @Path("id") int subscriptionId);

    /**
     * Cook answers one swap. Body is {action: 'accept'|'decline', note?}.
     *
     * Addressed by REQUEST id, not subscription id, because the cook taps this
     * from a chat card that only carries the request id.
     */
    @PUT("custom-meals/{requestId}/respond")
    Call<com.tiffincraft.app.models.SubscriptionActionResponse> respondToCustomMealRequest(
            @Header("Authorization") String token, @Path("requestId") int requestId,
            @Body com.google.gson.JsonObject requestBody);

    /** Customer withdraws their own swap. Only possible while it is still pending. */
    @DELETE("custom-meals/{requestId}")
    Call<RegisterResponse> cancelCustomMealRequest(
            @Header("Authorization") String token, @Path("requestId") int requestId);

    // ==================== Combo Deals (cook-authored, one-time bundle) ====================

    @POST("combos")
    Call<com.tiffincraft.app.models.ComboResponse> createCombo(@Header("Authorization") String token, @Body com.tiffincraft.app.models.ComboRequest request);

    @GET("combos/my")
    Call<com.tiffincraft.app.models.ComboResponse> getMyCombos(@Header("Authorization") String token);

    /** Public — powers the "Combo Deals" section on a cook's profile page. */
    @GET("combos/cook/{cookId}")
    Call<com.tiffincraft.app.models.ComboResponse> getCombosByCook(@Path("cookId") int cookId);

    @GET("combos/{id}")
    Call<com.tiffincraft.app.models.ComboResponse> getComboById(@Path("id") int comboId);

    @PUT("combos/{id}")
    Call<com.tiffincraft.app.models.ComboResponse> updateCombo(@Header("Authorization") String token, @Path("id") int comboId, @Body com.tiffincraft.app.models.ComboRequest request);

    @DELETE("combos/{id}")
    Call<RegisterResponse> deleteCombo(@Header("Authorization") String token, @Path("id") int comboId);

    @POST("combos/{id}/order")
    Call<RegisterResponse> buyCombo(@Header("Authorization") String token, @Path("id") int comboId, @Body com.tiffincraft.app.models.BuyComboRequest request);

    @GET("referrals/my")
    Call<ReferralInfoResponse> getMyReferralInfo(@Header("Authorization") String token);

    @POST("referrals/apply")
    Call<ApplyReferralResponse> applyReferralCode(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    // ==================== eSewa Intent Payment ====================

    @POST("payments/esewa/initiate")
    Call<EsewaInitiateResponse> initiateEsewaPayment(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    @GET("payments/esewa/status/{transactionUuid}")
    Call<EsewaStatusResponse> getEsewaPaymentStatus(@Header("Authorization") String token, @Path("transactionUuid") String transactionUuid);

    @POST("payments/esewa/cancel")
    Call<RegisterResponse> cancelEsewaPayment(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    /** ePay v2 fallback — see EsewaEpayCheckoutActivity for why this exists alongside Intent Payment. */
    @POST("payments/esewa-epay/initiate")
    Call<EpayInitiateResponse> initiateEpayPayment(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);

    // ==================== Refunds ====================

    @POST("refunds/request")
    Call<RegisterResponse> requestRefund(@Header("Authorization") String token, @Body com.google.gson.JsonObject requestBody);
}
