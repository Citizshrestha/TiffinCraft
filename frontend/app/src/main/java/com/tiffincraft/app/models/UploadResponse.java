package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private UploadData data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UploadData getData() {
        return data;
    }

    public void setData(UploadData data) {
        this.data = data;
    }

    public static class UploadData {
        @SerializedName("url")
        private String url;

        @SerializedName("publicId")
        private String publicId;

        @SerializedName("width")
        private int width;

        @SerializedName("height")
        private int height;

        @SerializedName("format")
        private String format;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }
}
