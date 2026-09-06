package com.tiffincraft.app.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.R;

/** A consistent, accessible action chooser for customer subscription controls. */
public final class SubscriptionManageDialog {

    public interface Actions {
        void onSchedule();
        void onPauseOrResume();
        void onCancel();
    }

    private SubscriptionManageDialog() { }

    public static void show(@NonNull Activity activity, boolean isPaused, @NonNull Actions actions) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_manage_subscription, null);
        TextView pauseTitle = content.findViewById(R.id.tvManagePauseTitle);
        TextView pauseDescription = content.findViewById(R.id.tvManagePauseDescription);
        ImageView pauseIcon = content.findViewById(R.id.ivManagePause);

        if (isPaused) {
            pauseTitle.setText("Resume subscription");
            pauseDescription.setText("Restart your future deliveries");
            pauseIcon.setImageResource(R.drawable.ic_play);
            content.findViewById(R.id.rowManagePause).setContentDescription("Resume subscription. Restart your future deliveries.");
        } else {
            content.findViewById(R.id.rowManagePause).setContentDescription("Pause subscription. Temporarily stop future deliveries.");
        }
        content.findViewById(R.id.rowManageSchedule).setContentDescription("Delivery schedule. View or skip a delivery day.");
        content.findViewById(R.id.rowManageCancel).setContentDescription("Cancel subscription. End this plan permanently.");

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.RoundedWhiteDialog)
                .setView(content)
                .show();

        content.findViewById(R.id.rowManageSchedule).setOnClickListener(v -> {
            dialog.dismiss();
            actions.onSchedule();
        });
        content.findViewById(R.id.rowManagePause).setOnClickListener(v -> {
            dialog.dismiss();
            actions.onPauseOrResume();
        });
        content.findViewById(R.id.rowManageCancel).setOnClickListener(v -> {
            dialog.dismiss();
            actions.onCancel();
        });
    }
}
