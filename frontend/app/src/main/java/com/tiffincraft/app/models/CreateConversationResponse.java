package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateConversationResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("conversation")
    private ChatConversation conversation;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public ChatConversation getConversation() { return conversation; }
}
