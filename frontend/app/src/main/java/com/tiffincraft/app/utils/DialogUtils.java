package com.tiffincraft.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;

public final class DialogUtils {

    private DialogUtils() {}

    /** Shows the app's styled logout confirmation dialog (icon + rounded card + colored actions). */
    public static void showLogoutConfirmation(Activity activity, Runnable onConfirm) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_logout_confirm, null);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelLogout);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmLogout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.show();
    }
}
