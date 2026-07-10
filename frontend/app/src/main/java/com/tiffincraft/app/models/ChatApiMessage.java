package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ChatApiMessage {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_CALL_ENDED = "call_ended";
    public static final String TYPE_CALL_DECLINED = "call_declined";
    public static final String TYPE_CALL_MISSED = "call_missed";

    @SerializedName("id")
    private int id;

    @SerializedName("conversation_id")
    private int conversationId;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("message_type")
    private String messageType;

    @SerializedName("content")
    private String content;

    @SerializedName("call_duration_seconds")
    private Integer callDurationSeconds;

    /**
     * Accept both MySQL-style 0/1 and JSON true/false.
     * A raw Java boolean field throws JsonSyntaxException on 0/1, which is why
     * messages saved successfully but the chat screen showed load/send failures.
     */
    @SerializedName("is_read")
    private Object isReadRaw;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("edited_at")
    private String editedAt;

    @SerializedName("is_edited")
    private Object isEditedRaw;

    @SerializedName("is_deleted")
    private Object isDeletedRaw;

    public int getId() { return id; }
    public int getConversationId() { return conversationId; }
    public int getSenderId() { return senderId; }
    public String getMessageType() { return messageType; }
    public String getContent() { return content; }
    public Integer getCallDurationSeconds() { return callDurationSeconds; }

    public boolean isRead() {
        if (isReadRaw == null) return false;
        if (isReadRaw instanceof Boolean) return (Boolean) isReadRaw;
        if (isReadRaw instanceof Number) return ((Number) isReadRaw).intValue() != 0;
        if (isReadRaw instanceof String) {
            String s = ((String) isReadRaw).trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }

    public String getCreatedAt() { return createdAt; }
    public String getEditedAt() { return editedAt; }

    public boolean isEdited() {
        if (editedAt != null && !editedAt.trim().isEmpty()) return true;
        if (isEditedRaw == null) return false;
        if (isEditedRaw instanceof Boolean) return (Boolean) isEditedRaw;
        if (isEditedRaw instanceof Number) return ((Number) isEditedRaw).intValue() != 0;
        if (isEditedRaw instanceof String) {
            String s = ((String) isEditedRaw).trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }

    public boolean isDeleted() {
        if (isDeletedRaw == null) return false;
        if (isDeletedRaw instanceof Boolean) return (Boolean) isDeletedRaw;
        if (isDeletedRaw instanceof Number) return ((Number) isDeletedRaw).intValue() != 0;
        if (isDeletedRaw instanceof String) {
            String s = ((String) isDeletedRaw).trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }
}
