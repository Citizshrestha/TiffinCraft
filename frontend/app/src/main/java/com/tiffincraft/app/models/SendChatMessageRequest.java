package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class SendChatMessageRequest {

    @SerializedName("message_type")
    private String messageType;

    @SerializedName("content")
    private String content;

    @SerializedName("call_duration_seconds")
    private Integer callDurationSeconds;

    /** Text message */
    public SendChatMessageRequest(String content) {
        this.messageType = ChatApiMessage.TYPE_TEXT;
        this.content = content;
    }

    /** Call log message */
    public SendChatMessageRequest(String messageType, Integer callDurationSeconds) {
        this.messageType = messageType;
        this.callDurationSeconds = callDurationSeconds;
    }
}
