package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeleteChatMessagesRequest {

    @SerializedName("message_ids")
    private final List<Integer> messageIds;

    public DeleteChatMessagesRequest(List<Integer> messageIds) {
        this.messageIds = messageIds;
    }

    public List<Integer> getMessageIds() {
        return messageIds;
    }
}
