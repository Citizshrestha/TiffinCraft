package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class EditChatMessageRequest {

    @SerializedName("content")
    private final String content;

    public EditChatMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
