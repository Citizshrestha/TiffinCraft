package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CustomerDashboardResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private DashboardData data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public DashboardData getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public static class DashboardData {
        @SerializedName("user")
        private UserSummary user;

        @SerializedName("stats")
        private DashboardStats stats;

        @SerializedName("activeOrders")
        private List<ActiveOrder> activeOrders;

        @SerializedName("popularCooks")
        private List<PopularCook> popularCooks;

        @SerializedName("notifications")
        private List<Notification> notifications;

        @SerializedName("unreadNotificationsCount")
        private int unreadNotificationsCount;

        public UserSummary getUser() {
            return user;
        }

        public DashboardStats getStats() {
            return stats;
        }

        public List<ActiveOrder> getActiveOrders() {
            return activeOrders;
        }

        public List<PopularCook> getPopularCooks() {
            return popularCooks;
        }

        public List<Notification> getNotifications() {
            return notifications;
        }

        public int getUnreadNotificationsCount() {
            return unreadNotificationsCount;
        }
    }

    public static class UserSummary {
        @SerializedName("id")
        private int id;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("profileImage")
        private String profileImage;

        public int getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProfileImage() {
            return profileImage;
        }
    }

    public static class DashboardStats {
        @SerializedName("activeOrders")
        private int activeOrders;

        @SerializedName("cartItemsCount")
        private int cartItemsCount;

        @SerializedName("favoriteCount")
        private int favoriteCount;

        @SerializedName("totalOrders")
        private int totalOrders;

        public int getActiveOrders() {
            return activeOrders;
        }

        public int getCartItemsCount() {
            return cartItemsCount;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public int getTotalOrders() {
            return totalOrders;
        }
    }

    public static class ActiveOrder {
        @SerializedName("orderId")
        private int orderId;

        @SerializedName("cookName")
        private String cookName;

        @SerializedName("status")
        private String status;

        @SerializedName("estimatedTime")
        private String estimatedTime;

        @SerializedName("totalAmount")
        private double totalAmount;

        public int getOrderId() {
            return orderId;
        }

        public String getCookName() {
            return cookName;
        }

        public String getStatus() {
            return status;
        }

        public String getEstimatedTime() {
            return estimatedTime;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }

    public static class PopularCook {
        @SerializedName("cookId")
        private int cookId;

        @SerializedName("cookName")
        private String cookName;

        @SerializedName("kitchenName")
        private String kitchenName;

        @SerializedName("profileImage")
        private String profileImage;

        @SerializedName("rating")
        private double rating;

        @SerializedName("reviewCount")
        private int reviewCount;

        @SerializedName("avgDeliveryTime")
        private int avgDeliveryTime;

        @SerializedName("distance")
        private double distance;

        @SerializedName("isOpen")
        private boolean isOpen;

        public int getCookId() {
            return cookId;
        }

        public String getCookName() {
            return cookName;
        }

        public String getKitchenName() {
            return kitchenName;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public double getRating() {
            return rating;
        }

        public int getReviewCount() {
            return reviewCount;
        }

        public int getAvgDeliveryTime() {
            return avgDeliveryTime;
        }

        public double getDistance() {
            return distance;
        }

        public boolean isOpen() {
            return isOpen;
        }
    }

    public static class Notification {
        @SerializedName("id")
        private int id;

        @SerializedName("title")
        private String title;

        @SerializedName("message")
        private String message;

        @SerializedName("type")
        private String type;

        @SerializedName("is_read")
        private boolean isRead;

        @SerializedName("order_id")
        private Integer orderId;

        @SerializedName("created_at")
        private String createdAt;

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }

        public boolean isRead() {
            return isRead;
        }

        public Integer getOrderId() {
            return orderId;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
