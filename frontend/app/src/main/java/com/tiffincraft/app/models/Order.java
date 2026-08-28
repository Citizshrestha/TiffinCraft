package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Order {

    @SerializedName("id")
    private int id;

    @SerializedName("customer_id")
    private int customerId;

    @SerializedName("cook_id")
    private int cookId;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Joined fields from query
    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("customer_phone")
    private String customerPhone;

    @SerializedName("customer_profile_image")
    private String customerProfileImage;

    @SerializedName("meal_name")
    private String mealName;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("meal_price")
    private double mealPrice;

    @SerializedName("meal_image")
    private String mealImage;

    // Customer-side order history joined fields
    @SerializedName("cook_name")
    private String cookName;

    @SerializedName("kitchen_name")
    private String kitchenName;

    @SerializedName("items_count")
    private int itemsCount;

    @SerializedName("items_summary")
    private String itemsSummary;

    @SerializedName("special_instructions")
    private String specialInstructions;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("payment_status")
    private String paymentStatus;

    @SerializedName("payment_screenshot_url")
    private String paymentScreenshotUrl;

    @SerializedName("payment_verified_at")
    private String paymentVerifiedAt;

    // Cancellation + refund fields (feature 1.5)
    @SerializedName("cancelled_by")
    private String cancelledBy;

    @SerializedName("cancellation_reason")
    private String cancellationReason;

    @SerializedName("refund_status")
    private String refundStatus;

    // Tracking-map coordinates — only populated by GET /orders/{orderId}.
    // map_customer_* falls back to the customer's profile location server-side
    // when the order itself has no captured delivery coords.
    @SerializedName("cook_latitude")
    private Double cookLatitude;

    @SerializedName("cook_longitude")
    private Double cookLongitude;

    @SerializedName("map_customer_latitude")
    private Double customerLatitude;

    @SerializedName("map_customer_longitude")
    private Double customerLongitude;

    // True when a 'payments' row confirms this order was paid via the eSewa
    // Intent flow — distinguishes that from the older manual screenshot flow,
    // which also reaches payment_status='paid' but means "awaiting cook
    // verification" instead of "fully confirmed, nothing left to do".
    @SerializedName("esewa_confirmed")
    private boolean esewaConfirmed;

    public boolean isEsewaConfirmed() { return esewaConfirmed; }

    // Cook's eSewa QR image (from cook_profiles.bank_details) — shown so the
    // customer can scan and pay the cook directly. Null if the cook hasn't
    // uploaded one.
    @SerializedName("cook_esewa_qr_url")
    private String cookEsewaQrUrl;

    public String getCookEsewaQrUrl() { return cookEsewaQrUrl; }

    // Per-item breakdown — only populated by GET /orders/{orderId} (getOrderById),
    // which attaches the real order_items rows. List-returning endpoints
    // (customer/cook order history) instead pre-aggregate itemsSummary.
    @SerializedName("items")
    private List<OrderItem> items;

    public List<OrderItem> getItems() { return items; }

    public static class OrderItem {
        @SerializedName("id")
        private int id;
        @SerializedName("meal_id")
        private int mealId;
        @SerializedName("meal_name")
        private String mealName;
        @SerializedName("quantity")
        private int quantity;
        @SerializedName("price_at_time")
        private double priceAtTime;
        @SerializedName("image_url")
        private String imageUrl;

        public int getId() { return id; }
        public int getMealId() { return mealId; }
        public String getMealName() { return mealName; }
        public int getQuantity() { return quantity; }
        public double getPriceAtTime() { return priceAtTime; }
        public String getImageUrl() { return imageUrl; }
    }

    // Getters
    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getCookId() { return cookId; }
    public double getTotalAmount() { return totalAmount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerProfileImage() { return customerProfileImage; }
    public String getMealName() { return mealName; }
    public int getQuantity() { return quantity; }
    public double getMealPrice() { return mealPrice; }
    public String getMealImage() { return mealImage; }
    public String getCookName() { return cookName; }
    public String getKitchenName() { return kitchenName; }
    public int getItemsCount() { return itemsCount; }
    public String getItemsSummary() { return itemsSummary; }

    /**
     * The first {@code max} items, with "+N more" standing in for the rest.
     *
     * A five-item order wrapped to three lines on a card and pushed the status and
     * the buttons off screen. The full list is on the order's details screen, which
     * is where someone who wants to read all of it is going anyway.
     *
     * Splits the server's own comma-joined summary rather than re-deriving from
     * getItems(), because list endpoints don't send the items array at all.
     */
    public String getItemsSummaryPreview(int max) {
        if (itemsSummary == null || itemsSummary.trim().isEmpty()) return "";
        String[] parts = itemsSummary.split(",\\s*");
        if (parts.length <= max) return itemsSummary;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts[i]);
        }
        return sb + "  +" + (parts.length - max) + " more";
    }
    public String getSpecialInstructions() { return specialInstructions; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
    public String getPaymentVerifiedAt() { return paymentVerifiedAt; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public String getRefundStatus() { return refundStatus; }
    public Double getCookLatitude() { return cookLatitude; }
    public Double getCookLongitude() { return cookLongitude; }
    public Double getCustomerLatitude() { return customerLatitude; }
    public Double getCustomerLongitude() { return customerLongitude; }

    public boolean hasMapCoordinates() {
        return cookLatitude != null && cookLongitude != null
                && customerLatitude != null && customerLongitude != null;
    }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setPaymentScreenshotUrl(String paymentScreenshotUrl) { this.paymentScreenshotUrl = paymentScreenshotUrl; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
}
