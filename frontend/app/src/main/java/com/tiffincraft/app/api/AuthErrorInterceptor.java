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
 * Handles 401 (Unauthorized) and 403 (Forbidden) responses globally.
 *
 * IMPORTANT: Multiple endpoints legitimately return 401 for business-rule reasons
 * (e.g. wrong current password in change-password or delete-account). The interceptor
 * must NOT treat those as "session expired" — only clear the session when the 401
 * is truly an auth failure (invalid/expired token, not a password mismatch).
 *
 * The backend returns 401 with body {"message": "Current password is incorrect."}
 * for password mismatches, and 401 with body {"message": "Invalid or expired token"}
 * for auth failures. Since we can't read the body from an interceptor's Response object
 * (it's a stream that can be consumed only once), we use the request path as a heuristic:
 * if the request targets an endpoint where 401 means "wrong input" (not "bad token"),
 * we skip the automatic logout.
 */
public class AuthErrorInterceptor implements Interceptor {

    // Endpoints where 401 means "wrong current password" not "session expired"
    private static final String[] PASSWORD_CONFIRM_PATHS = {
        "auth/change-password",
        "auth/account"
    };

    private final Context context;

    public AuthErrorInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() == 401) {
            String path = chain.request().url().encodedPath();
            // Check if this is a password-confirmation endpoint (not a real auth failure)
            for (String confirmPath : PASSWORD_CONFIRM_PATHS) {
                if (path.contains(confirmPath)) {
                    // 401 on these means "wrong password", not expired token — let the calling
                    // Activity handle it. Do NOT clear session or redirect.
                    return response;
                }
            }
            // Real auth failure — token expired/invalid
            handleUnauthorized();
        } else if (response.code() == 403) {
            handleForbidden();
        }

        return response;
    }

    private void handleUnauthorized() {
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            SessionManager sessionManager = new SessionManager(context);
            sessionManager.logout();

            Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show();

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
