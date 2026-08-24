package com.tiffincraft.app.utils;

import android.util.Log;

import org.json.JSONObject;

import retrofit2.Response;

/**
 * Turns a failed Retrofit call into the message the backend actually sent.
 *
 * Why this exists: on any non-2xx response Retrofit leaves {@code body()} null
 * and puts the payload in {@code errorBody()}. Call sites that only read
 * {@code body().getMessage()} therefore throw away every real reason — "You
 * already have an active subscription to this plan", "Session expired",
 * "This plan is currently unavailable" — and replace it with one hardcoded
 * string. That is exactly what made subscribing fail with a blank "Failed to
 * subscribe" and no way to tell the causes apart.
 *
 * The status code is logged alongside the raw body so the next failure is
 * diagnosable from logcat instead of guesswork.
 */
public final class ApiErrorMessage {

    private static final String TAG = "ApiError";

    private ApiErrorMessage() { }

    /**
     * @param response the non-successful response
     * @param fallback used only when the server sent nothing usable
     */
    public static String from(Response<?> response, String fallback) {
        int code = response.code();
        String raw = null;

        if (response.errorBody() != null) {
            try {
                raw = response.errorBody().string();
            } catch (Exception e) {
                Log.e(TAG, "Could not read errorBody (HTTP " + code + ")", e);
            }
        }

        Log.e(TAG, "HTTP " + code + " " + response.raw().request().url() + " → " + raw);

        String serverMessage = null;
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                serverMessage = emptyToNull(new JSONObject(raw).optString("message", null));
            } catch (Exception ignored) {
                // Not JSON (HTML error page, proxy response, truncated body).
            }
        }

        switch (code) {
            case 401:
            case 403:
                // The server's own text here is usually "Not authorized, token
                // failed", which tells the user nothing actionable.
                return "Your session expired. Please log in again.";
            case 404:
                return serverMessage != null ? serverMessage : "Not found. It may have been removed.";
            case 429:
                return serverMessage != null
                        ? serverMessage
                        : "Too many attempts. Please wait a few minutes and try again.";
            default:
                if (serverMessage != null) return serverMessage;
                if (code >= 500) return "Server error (" + code + "). Please try again in a moment.";
                return fallback;
        }
    }

    /** Human-readable reason for an {@code onFailure} throwable (no HTTP response at all). */
    public static String fromFailure(Throwable t) {
        Log.e(TAG, "Request failed before a response arrived", t);
        if (t instanceof java.net.UnknownHostException) {
            return "No internet connection. Check your network and try again.";
        }
        if (t instanceof java.net.SocketTimeoutException) {
            return "The server took too long to respond. Please try again.";
        }
        if (t instanceof java.net.ConnectException) {
            return "Cannot reach the server. Please try again shortly.";
        }
        return "Network error. Please check your connection and try again.";
    }

    /** True when the failure is an expired/invalid token and the user must re-authenticate. */
    public static boolean isAuthFailure(Response<?> response) {
        return response.code() == 401 || response.code() == 403;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty() || "null".equals(s)) ? null : s;
    }
}
