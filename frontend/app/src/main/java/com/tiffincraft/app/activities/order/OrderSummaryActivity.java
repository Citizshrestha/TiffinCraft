package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CartGroup;
import com.tiffincraft.app.models.CartItem;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.CheckoutRequest;
import com.tiffincraft.app.models.CheckoutResponse;
import com.tiffincraft.app.models.CustomerProfile;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Confirms multi-cook cart checkout.
 * Backend splits the cart into one order per cook and notifies each cook
 * via in-app + FCM push.
 */
public class OrderSummaryActivity extends AppCompatActivity {

    private LinearLayout layoutOrderItems;
    private TextView tvMultiCookHint, tvSubtotal, tvGrandTotal;
    private TextInputEditText etDeliveryAddress, etSpecialInstructions;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCod, rbOnline;
    private MaterialButton btnPlaceOrder;
    private View btnBack;

    private SessionManager sessionManager;
    private ApiService apiService;
    private double grandTotal = 0;
    private int cookGroupCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        btnBack = findViewById(R.id.btnBack);
        layoutOrderItems = findViewById(R.id.layoutOrderItems);
        tvMultiCookHint = findViewById(R.id.tvMultiCookHint);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
        etSpecialInstructions = findViewById(R.id.etSpecialInstructions);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbCod = findViewById(R.id.rbCod);
        rbOnline = findViewById(R.id.rbOnline);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        btnBack.setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        loadCustomerAddress();
        loadCartSummary();
    }

    private void loadCustomerAddress() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call,
                                   Response<CustomerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getProfile() != null) {
                    CustomerProfile profile = response.body().getProfile();
                    if (profile.getAddress() != null && !profile.getAddress().isEmpty()) {
                        etDeliveryAddress.setText(profile.getAddress());
                    }
                }
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) {
                // keep empty address field
            }
        });
    }

    private void loadCartSummary() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCart(token).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(OrderSummaryActivity.this, "Could not load cart", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                CartResponse body = response.body();
                List<CartGroup> groups = body.getCartGroups();
                if (groups == null || groups.isEmpty()) {
                    Toast.makeText(OrderSummaryActivity.this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                renderGroups(groups);
                grandTotal = body.getGrandTotal();
                cookGroupCount = groups.size();
                tvSubtotal.setText(String.format(Locale.getDefault(), "₹%.0f", grandTotal));
                tvGrandTotal.setText(String.format(Locale.getDefault(), "₹%.0f", grandTotal));

                if (cookGroupCount > 1) {
                    tvMultiCookHint.setVisibility(View.VISIBLE);
                    tvMultiCookHint.setText(
                            cookGroupCount + " cooks in this cart — each will get a separate order and notification.");
                } else {
                    tvMultiCookHint.setVisibility(View.VISIBLE);
                    tvMultiCookHint.setText("Your cook will be notified as soon as you place the order.");
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(OrderSummaryActivity.this, "Network error loading cart", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderGroups(List<CartGroup> groups) {
        layoutOrderItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int g = 0; g < groups.size(); g++) {
            CartGroup group = groups.get(g);
            String cookLabel = group.getKitchenName() != null && !group.getKitchenName().isEmpty()
                    ? group.getKitchenName()
                    : group.getCookName();

            TextView header = new TextView(this);
            header.setText("👨‍🍳 " + cookLabel);
            header.setTextSize(14.5f);
            header.setTextColor(getResources().getColor(R.color.text_title));
            header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
            if (g > 0) {
                header.setPadding(0, 18, 0, 6);
            } else {
                header.setPadding(0, 0, 0, 6);
            }
            layoutOrderItems.addView(header);

            if (group.getItems() == null) continue;

            for (CartItem item : group.getItems()) {
                View row = inflater.inflate(R.layout.item_cart_meal, layoutOrderItems, false);
                // Hide qty controls on summary — read-only preview
                View minus = row.findViewById(R.id.tvMinus);
                View plus = row.findViewById(R.id.tvPlus);
                View remove = row.findViewById(R.id.imgRemove);
                if (minus != null) minus.setVisibility(View.GONE);
                if (plus != null) plus.setVisibility(View.GONE);
                if (remove != null) remove.setVisibility(View.GONE);

                ImageView img = row.findViewById(R.id.imgMeal);
                TextView name = row.findViewById(R.id.tvMealName);
                TextView price = row.findViewById(R.id.tvPrice);
                TextView qty = row.findViewById(R.id.tvQuantity);

                name.setText(item.getMealName());
                price.setText(String.format(Locale.getDefault(), "₹%.0f", item.getPrice()));
                qty.setText("×" + item.getQuantity());

                ImageUrlHelper.load(img, item.getImageUrl(), R.drawable.ic_food);

                layoutOrderItems.addView(row);
            }

            TextView sub = new TextView(this);
            sub.setText(String.format(Locale.getDefault(), "Subtotal: ₹%.0f", group.getSubtotal()));
            sub.setTextColor(getResources().getColor(R.color.green_primary_dark));
            sub.setTextSize(13f);
            sub.setPadding(0, 4, 0, 4);
            layoutOrderItems.addView(sub);
        }
    }

    private void placeOrder() {
        String address = etDeliveryAddress.getText() != null
                ? etDeliveryAddress.getText().toString().trim() : "";
        if (address.isEmpty()) {
            etDeliveryAddress.setError("Delivery address is required");
            etDeliveryAddress.requestFocus();
            return;
        }

        String notes = etSpecialInstructions.getText() != null
                ? etSpecialInstructions.getText().toString().trim() : null;
        if (notes != null && notes.isEmpty()) notes = null;

        String payment = (rbOnline != null && rbOnline.isChecked()) ? "online" : "cod";

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Placing order…");

        // Pin the delivery address so the order-tracking map can draw the
        // cook → customer route. Geocoder blocks — run it off the main thread.
        // Unlike the cook's kitchen address, a failure here is non-fatal: the
        // order still goes through, the tracking map just falls back to
        // "route unavailable" rather than blocking a paying customer.
        final String fNotes = notes;
        new Thread(() -> {
            Double lat = null, lng = null;
            try {
                java.util.List<android.location.Address> results =
                        new android.location.Geocoder(this, Locale.getDefault())
                                .getFromLocationName(address, 1);
                if (results != null && !results.isEmpty()) {
                    lat = results.get(0).getLatitude();
                    lng = results.get(0).getLongitude();
                }
            } catch (java.io.IOException ignored) {
                // offline / geocoder unavailable — order proceeds without coords
            }
            final Double fLat = lat;
            final Double fLng = lng;
            runOnUiThread(() -> submitCheckout(address, fLat, fLng, fNotes, payment));
        }).start();
    }

    private void submitCheckout(String address, Double lat, Double lng, String notes, String payment) {
        String token = "Bearer " + sessionManager.getToken();
        CheckoutRequest request = new CheckoutRequest(address, lat, lng, notes, payment);

        apiService.checkoutCart(token, request).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText(R.string.place_order);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CheckoutResponse body = response.body();
                    List<CheckoutResponse.CreatedOrder> createdOrders = body.getOrders();
                    int orderCount = createdOrders != null ? createdOrders.size() : 1;

                    Intent intent = new Intent(OrderSummaryActivity.this, OrderPlacedActivity.class);
                    intent.putExtra("order_count", orderCount);
                    intent.putExtra("message", body.getMessage());
                    intent.putExtra("payment_method", payment);
                    if (createdOrders != null) {
                        int[] orderIds = new int[createdOrders.size()];
                        for (int i = 0; i < createdOrders.size(); i++) {
                            orderIds[i] = createdOrders.get(i).getOrderId();
                        }
                        intent.putExtra("order_ids", orderIds);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String msg = "Checkout failed";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(OrderSummaryActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText(R.string.place_order);
                Toast.makeText(OrderSummaryActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
