package com.tiffincraft.app.models;

import java.util.List;

public class CookProfileResponse {
    private boolean success;
    private String message;
    private CookProfile profile;
    private List<CookProfile> cooks;
    private CookProfile cook;

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

    public CookProfile getProfile() {
        return profile;
    }

    public void setProfile(CookProfile profile) {
        this.profile = profile;
    }

    public List<CookProfile> getCooks() {
        return cooks;
    }

    public void setCooks(List<CookProfile> cooks) {
        this.cooks = cooks;
    }

    public CookProfile getCook() {
        return cook;
    }

    public void setCook(CookProfile cook) {
        this.cook = cook;
    }
}
