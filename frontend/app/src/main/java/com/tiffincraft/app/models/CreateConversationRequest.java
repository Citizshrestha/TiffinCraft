package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateConversationRequest {

    @SerializedName("other_user_id")
    private int otherUserId;

    public CreateConversationRequest(int otherUserId) {
        this.otherUserId = otherUserId;
    }
}
