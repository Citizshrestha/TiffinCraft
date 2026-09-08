package com.tiffincraft.app.utils;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.R;

/** Shared confirmation step used before any customer payment proof is uploaded. */
public final class PaymentScreenshotConfirmationDialog {

    private PaymentScreenshotConfirmationDialog() { }

    public static void show(Activity activity, Uri screenshot,
                            Runnable sendAction, Runnable chooseAnotherAction) {
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_confirm_payment_screenshot, null, false);

        ImageView preview = content.findViewById(R.id.ivSelectedScreenshot);
        TextView selectedName = content.findViewById(R.id.tvSelectedScreenshotName);
        MaterialButton send = content.findViewById(R.id.btnSendScreenshot);
        MaterialButton chooseAnother = content.findViewById(R.id.btnChooseAnotherScreenshot);
        MaterialButton cancel = content.findViewById(R.id.btnCancelScreenshot);

        Glide.with(activity).load(screenshot).fitCenter().into(preview);
        selectedName.setText(displayName(activity, screenshot));

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.RoundedWhiteDialog)
                .setView(content)
                .create();

        send.setOnClickListener(v -> {
            dialog.dismiss();
            sendAction.run();
        });
        chooseAnother.setOnClickListener(v -> {
            dialog.dismiss();
            chooseAnotherAction.run();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static String displayName(Activity activity, Uri uri) {
        String name = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = activity.getContentResolver().query(
                    uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) name = cursor.getString(index);
                }
            } catch (Exception ignored) { }
        }
        if (name == null || name.trim().isEmpty()) name = uri.getLastPathSegment();
        return name == null || name.trim().isEmpty() ? "Selected image" : name;
    }
}
