package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ChatApiMessage {

    public static final String TYPE_TEXT = "text";
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

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getConversationId() { return conversationId; }
    public int getSenderId() { return senderId; }
    public String getMessageType() { return messageType; }
    public String getContent() { return content; }
    public Integer getCallDurationSeconds() { return callDurationSeconds; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
}
