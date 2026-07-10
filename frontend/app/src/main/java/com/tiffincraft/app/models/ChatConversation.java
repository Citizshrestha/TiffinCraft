package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ChatConversation {

    @SerializedName("id")
    private int id;

    @SerializedName("customer_id")
    private int customerId;

    @SerializedName("cook_id")
    private int cookId;

    @SerializedName("last_message_at")
    private String lastMessageAt;

    @SerializedName("other_user_id")
    private int otherUserId;

    @SerializedName("other_user_name")
    private String otherUserName;

    @SerializedName("other_user_image")
    private String otherUserImage;

    @SerializedName("other_user_role")
    private String otherUserRole;

    @SerializedName("last_message_type")
    private String lastMessageType;

    @SerializedName("last_message_content")
    private String lastMessageContent;

    @SerializedName("last_message_sender_id")
    private int lastMessageSenderId;

    /** Accept both MySQL 0/1 and JSON true/false (may also be null). */
    @SerializedName("last_message_deleted")
    private Object lastMessageDeletedRaw;

    @SerializedName("unread_count")
    private int unreadCount;

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getCookId() { return cookId; }
    public String getLastMessageAt() { return lastMessageAt; }
    public int getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherUserImage() { return otherUserImage; }
    public String getOtherUserRole() { return otherUserRole; }
    public String getLastMessageType() { return lastMessageType; }
    public String getLastMessageContent() { return lastMessageContent; }
    public int getLastMessageSenderId() { return lastMessageSenderId; }
    public int getUnreadCount() { return unreadCount; }

    public boolean isLastMessageDeleted() {
        if (lastMessageDeletedRaw == null) return false;
        if (lastMessageDeletedRaw instanceof Boolean) return (Boolean) lastMessageDeletedRaw;
        if (lastMessageDeletedRaw instanceof Number) return ((Number) lastMessageDeletedRaw).intValue() != 0;
        if (lastMessageDeletedRaw instanceof String) {
            String s = ((String) lastMessageDeletedRaw).trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }
}
