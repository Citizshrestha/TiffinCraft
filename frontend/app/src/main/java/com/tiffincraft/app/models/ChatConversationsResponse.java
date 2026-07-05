package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatConversationsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("conversations")
    private List<ChatConversation> conversations;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<ChatConversation> getConversations() { return conversations; }
}
