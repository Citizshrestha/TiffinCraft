package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatContactsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("contacts")
    private List<ApiContact> contacts;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<ApiContact> getContacts() { return contacts; }

    public static class ApiContact {
        @SerializedName("id")
        private int id;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("profile_image")
        private String profileImage;

        @SerializedName("role")
        private String role;

        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getProfileImage() { return profileImage; }
        public String getRole() { return role; }
    }
}
