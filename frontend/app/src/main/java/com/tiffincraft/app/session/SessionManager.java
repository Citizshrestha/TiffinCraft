package com.tiffincraft.app.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "TiffinCraftSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_ROLE = "role";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_PROFILE_IMAGE = "profileImage";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createSession(String role, String token, String userId, String fullName) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }

    public void saveSession(String token, String role, int userId, String fullName) {
        createSession(role, token, String.valueOf(userId), fullName);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    // Save profile image URL
    public void saveProfileImage(String imageUrl) {
        editor.putString(KEY_PROFILE_IMAGE, imageUrl);
        editor.apply();
    }

    // Get profile image URL
    public String getProfileImage() {
        return prefs.getString(KEY_PROFILE_IMAGE, null);
    }

    // Save full name
    public void saveFullName(String fullName) {
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }

    // Save session with profile image
    public void saveSessionWithImage(String token, String role, int userId, String fullName, String profileImage) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_ID, String.valueOf(userId));
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_PROFILE_IMAGE, profileImage);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
