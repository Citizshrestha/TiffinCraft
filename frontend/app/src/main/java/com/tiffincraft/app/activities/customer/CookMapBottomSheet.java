package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.meal.CookDetailsActivity;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookProfile;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Cook summary card shown when a map marker is tapped. Everything here is
 * data already returned by GET /cook/nearby — no extra network call needed.
 */
public class CookMapBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_COOK_ID = "cook_id";
    private static final String ARG_KITCHEN_NAME = "kitchen_name";
    private static final String ARG_COOK_NAME = "cook_name";
    private static final String ARG_PROFILE_IMAGE = "profile_image";
    private static final String ARG_RATING = "rating";
    private static final String ARG_DISTANCE_KM = "distance_km";
    private static final String ARG_FOOD_TYPE = "food_type";
    private static final String ARG_DISH_COUNT = "dish_count";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_VERIFIED = "verified";

    public static CookMapBottomSheet newInstance(CookProfile cook) {
        CookMapBottomSheet sheet = new CookMapBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_COOK_ID, cook.getId());
        args.putString(ARG_KITCHEN_NAME, cook.getKitchenName());
        args.putString(ARG_COOK_NAME, cook.getFullName());
        args.putString(ARG_PROFILE_IMAGE, cook.getProfileImage());
        args.putDouble(ARG_RATING, cook.getRating());
        args.putDouble(ARG_DISTANCE_KM, cook.getDistanceKm() != null ? cook.getDistanceKm() : -1);
        args.putString(ARG_FOOD_TYPE, cook.getFoodType());
        args.putInt(ARG_DISH_COUNT, cook.getDishCount());
        args.putString(ARG_ADDRESS, cook.getAddress());
        args.putBoolean(ARG_VERIFIED, cook.isVerified());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_cook_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        int cookId = args.getInt(ARG_COOK_ID);
        String kitchenName = args.getString(ARG_KITCHEN_NAME);
        String cookName = args.getString(ARG_COOK_NAME);
        String profileImage = args.getString(ARG_PROFILE_IMAGE);
        double rating = args.getDouble(ARG_RATING);
        double distanceKm = args.getDouble(ARG_DISTANCE_KM);
        String foodType = args.getString(ARG_FOOD_TYPE);
        int dishCount = args.getInt(ARG_DISH_COUNT);
        String address = args.getString(ARG_ADDRESS);
        boolean verified = args.getBoolean(ARG_VERIFIED);

        CircleImageView imgAvatar = view.findViewById(R.id.imgCookAvatar);
        TextView tvKitchenName = view.findViewById(R.id.tvKitchenName);
        TextView tvCookName = view.findViewById(R.id.tvCookName);
        TextView tvRating = view.findViewById(R.id.tvRating);
        TextView tvDistance = view.findViewById(R.id.tvDistance);
        TextView tvFoodType = view.findViewById(R.id.tvFoodType);
        TextView tvDishCount = view.findViewById(R.id.tvDishCount);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        TextView tvVerifiedBadge = view.findViewById(R.id.tvVerifiedBadge);
        MaterialButton btnViewProfile = view.findViewById(R.id.btnViewProfile);

        tvKitchenName.setText(kitchenName != null && !kitchenName.isEmpty() ? kitchenName : "Home Kitchen");
        tvCookName.setText(cookName != null ? "by " + cookName : "");
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));
        tvDistance.setText(distanceKm >= 0
                ? String.format(Locale.getDefault(), "%.1f km away", distanceKm)
                : "");
        tvFoodType.setText(foodType != null && !foodType.isEmpty() ? foodType : "Home Cooking");
        tvDishCount.setText(dishCount == 1 ? "1 dish" : dishCount + " dishes");
        tvAddress.setText(address != null && !address.isEmpty() ? address : "Address not provided");
        tvVerifiedBadge.setVisibility(verified ? View.VISIBLE : View.GONE);

        String fullImageUrl = getFullImageUrl(profileImage);
        if (fullImageUrl != null) {
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.avatar_cook)
                    .error(R.drawable.avatar_cook)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.avatar_cook);
        }

        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CookDetailsActivity.class);
            intent.putExtra(CookDetailsActivity.EXTRA_COOK_ID, cookId);
            startActivity(intent);
            dismiss();
        });
    }

    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return RetrofitClient.SERVER_URL + imageUrl;
    }
}
