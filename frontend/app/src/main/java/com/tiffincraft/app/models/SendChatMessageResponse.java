package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class SendChatMessageResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ChatApiMessage data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public ChatApiMessage getData() { return data; }
}
