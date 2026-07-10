package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class SendChatMessageRequest {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";

    @SerializedName("message_type")
    private String messageType;

    @SerializedName("content")
    private String content;

    @SerializedName("call_duration_seconds")
    private Integer callDurationSeconds;

    /** Text message */
    public SendChatMessageRequest(String content) {
        this.messageType = TYPE_TEXT;
        this.content = content;
    }

    /** Media message */
    public static SendChatMessageRequest media(String messageType, String url) {
        SendChatMessageRequest request = new SendChatMessageRequest(url);
        request.messageType = messageType;
        return request;
    }

    /** Call log message */
    public SendChatMessageRequest(String messageType, Integer callDurationSeconds) {
        this.messageType = messageType;
        this.callDurationSeconds = callDurationSeconds;
    }
}
