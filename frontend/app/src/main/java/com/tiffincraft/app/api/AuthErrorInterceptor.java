package com.tiffincraft.app.api;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.tiffincraft.app.activities.auth.LoginActivity;
import com.tiffincraft.app.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * AuthErrorInterceptor
 * Handles 401 (Unauthorized) and 403 (Forbidden) responses globally
 * - 401: Token expired or invalid → Clear session and redirect to login
 * - 403: Access denied → Show toast message
 */
public class AuthErrorInterceptor implements Interceptor {

    private final Context context;

    public AuthErrorInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() == 401) {
            // Token expired or invalid - clear session and redirect to login
            handleUnauthorized();
        } else if (response.code() == 403) {
            // Access denied - show message
            handleForbidden();
        }

        return response;
    }

    private void handleUnauthorized() {
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            // Clear session
            SessionManager sessionManager = new SessionManager(context);
            sessionManager.logout();

            // Show toast
            Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show();

            // Redirect to login (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }

    private void handleForbidden() {
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            Toast.makeText(context, "Access denied. You don't have permission for this action.", Toast.LENGTH_LONG).show();
        });
    }
}
