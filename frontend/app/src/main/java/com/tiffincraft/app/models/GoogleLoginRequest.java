package com.tiffincraft.app.models;

public class GoogleLoginRequest {
    private String idToken;
    private String role;

    public GoogleLoginRequest(String idToken, String role) {
        this.idToken = idToken;
        this.role = role;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
