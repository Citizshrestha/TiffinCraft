package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatMessagesResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("messages")
    private List<ChatApiMessage> messages;

    @SerializedName("has_more")
    private boolean hasMore;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<ChatApiMessage> getMessages() { return messages; }
    public boolean hasMore() { return hasMore; }
}
