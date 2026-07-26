package com.tiffincraft.app.activities.order;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityTrackOrderBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Short/map-style order tracking screen — compact timeline + a map placeholder,
 * as opposed to {@link OrderDetailsCustomerActivity}'s full itemized order details.
 * The two screens show the same order and are reachable from each other.
 */
public class TrackOrderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TrackOrderActivity";
    private ActivityTrackOrderBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private SocketManager socketManager;

    private int orderId;
    private String currentStatus;

    private GoogleMap map;
    private Order loadedOrder;

    private LinearLayout[] stepRows;
    private ImageView[] stepIcons;
    private FrameLayout[] stepIconWraps;
    private TextView[] stepLabels;
    private View[] connectorLines;

    private static final String[] STEP_LABELS = {
            "Order Confirmed",
            "Cooking in Progress",
            "Out For Delivery",
            "Delivered"
    };
    private static final int[] STEP_ICONS = {
            R.drawable.ic_check_circle,
            R.drawable.ic_cook,
            R.drawable.ic_delivery,
            R.drawable.ic_home
    };

    private static final int COLOR_DONE = 0xFF4CAF50;
    private static final int COLOR_CURRENT = 0xFFFF6D00;
    private static final int COLOR_PENDING_BG = 0xFFEEEEEE;
    private static final int COLOR_PENDING_ICON = 0xFFBDBDBD;
    private static final int COLOR_PENDING_TEXT = 0xFF9E9E9E;
    private static final int COLOR_CANCELLED = 0xFFE53935;
    /** Light amber — the delivery is underway but not yet complete. */
    private static final int COLOR_ROUTE_IN_TRANSIT = 0x99FFA726;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrackOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        socketManager = SocketManager.getInstance(this);

        binding.btnBack.setOnClickListener(v -> finish());

        orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId == -1) {
            Log.e(TAG, "No order_id provided");
            finish();
            return;
        }

        binding.tvOrderId.setText("Order ID: TC" + orderId);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.trackMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        buildTimelineRows();
        loadOrderDetails();
        connectSocket();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_tiffincraft));
        } catch (Exception ignored) {
            // Styling is cosmetic — never let it block the route from rendering.
        }
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        // The order may have already loaded before the map finished initializing.
        if (loadedOrder != null) {
            drawRoute(loadedOrder);
        }
    }

    private void buildTimelineRows() {
        LinearLayout container = binding.timelineContainer;
        container.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int stepCount = STEP_LABELS.length;

        stepRows = new LinearLayout[stepCount];
        stepIcons = new ImageView[stepCount];
        stepIconWraps = new FrameLayout[stepCount];
        stepLabels = new TextView[stepCount];
        connectorLines = new View[stepCount - 1];

        for (int i = 0; i < stepCount; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            container.addView(row);
            stepRows[i] = row;

            FrameLayout iconWrap = new FrameLayout(this);
            LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density));
            iconWrap.setLayoutParams(wrapParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            iconWrap.setBackground(circle);
            row.addView(iconWrap);
            stepIconWraps[i] = iconWrap;

            ImageView icon = new ImageView(this);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams((int) (16 * density), (int) (16 * density), Gravity.CENTER);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(STEP_ICONS[i]);
            iconWrap.addView(icon);
            stepIcons[i] = icon;

            TextView label = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.setMarginStart((int) (12 * density));
            label.setLayoutParams(labelParams);
            label.setText(STEP_LABELS[i]);
            label.setTextSize(14);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(label);
            stepLabels[i] = label;

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, i == 0 ? 0 : (int) (4 * density), 0, 0);
            row.setLayoutParams(rowParams);

            if (i < stepCount - 1) {
                View line = new View(this);
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams((int) (2 * density), (int) (20 * density));
                lineParams.setMargins((int) (15 * density), 0, 0, 0);
                line.setLayoutParams(lineParams);
                container.addView(line);
                connectorLines[i] = line;
            }
        }
    }

    private void loadOrderDetails() {
        String token = "Bearer " + sessionManager.getToken();

        apiService.getOrderDetails(token, orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Order order = response.body().getOrder();
                    if (order != null) {
                        bindOrder(order);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load order", t);
            }
        });
    }

    private void bindOrder(Order order) {
        binding.tvOrderId.setText("Order ID: TC" + order.getId());

        String cookLabel = order.getKitchenName() != null && !order.getKitchenName().isEmpty()
                ? order.getKitchenName()
                : order.getCookName();
        binding.tvCookName.setText(cookLabel != null ? cookLabel : "");

        currentStatus = order.getStatus();
        updateTimeline(order.getStatus(), order.getCreatedAt());

        loadedOrder = order;
        drawRoute(order);
    }

    // ==================== Route map ====================

    /**
     * Draws the cook → customer route. The line's appearance encodes the order
     * stage: nothing while the food is still being prepared (there is no journey
     * yet), a light dashed line once it's out for delivery (in progress), and a
     * solid green line once delivered (completed).
     */
    private void drawRoute(Order order) {
        if (map == null) return; // onMapReady will re-invoke this

        if (!order.hasMapCoordinates()) {
            binding.layoutMapUnavailable.setVisibility(View.VISIBLE);
            binding.tvRouteStage.setVisibility(View.GONE);
            return;
        }
        binding.layoutMapUnavailable.setVisibility(View.GONE);

        LatLng cook = new LatLng(order.getCookLatitude(), order.getCookLongitude());
        LatLng customer = new LatLng(order.getCustomerLatitude(), order.getCustomerLongitude());

        map.clear();

        map.addMarker(new MarkerOptions()
                .position(cook)
                .title(order.getKitchenName() != null && !order.getKitchenName().isEmpty()
                        ? order.getKitchenName() : "Kitchen")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        map.addMarker(new MarkerOptions()
                .position(customer)
                .title("Delivery address")
                .snippet(order.getDeliveryAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        applyRouteStyle(order.getStatus(), cook, customer);

        try {
            LatLngBounds bounds = new LatLngBounds.Builder().include(cook).include(customer).build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (IllegalStateException e) {
            // Both points identical (cook delivering to their own address).
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(cook, 14f));
        }
    }

    private void applyRouteStyle(String status, LatLng cook, LatLng customer) {
        String s = status != null ? status.toLowerCase() : "";

        if (s.contains("cancel")) {
            binding.tvRouteStage.setVisibility(View.VISIBLE);
            binding.tvRouteStage.setText("Order cancelled");
            return; // no route line for a journey that never happened
        }

        // Still in the kitchen — deliberately no line yet.
        if (s.equals("pending") || s.equals("placed") || s.equals("confirmed")
                || s.equals("accepted") || s.equals("preparing") || s.equals("in_progress")
                || s.equals("cooking")) {
            binding.tvRouteStage.setVisibility(View.VISIBLE);
            binding.tvRouteStage.setText("Preparing your order");
            return;
        }

        boolean delivered = s.equals("delivered") || s.equals("completed");

        PolylineOptions line = new PolylineOptions()
                .add(cook, customer)
                .width(delivered ? 10f : 8f)
                .color(delivered ? COLOR_DONE : COLOR_ROUTE_IN_TRANSIT)
                .startCap(new RoundCap())
                .endCap(new RoundCap());

        if (!delivered) {
            // Dashed = journey underway but not finished.
            line.pattern(java.util.Arrays.asList(new com.google.android.gms.maps.model.Dash(24),
                    new com.google.android.gms.maps.model.Gap(14)));
        }

        map.addPolyline(line);

        binding.tvRouteStage.setVisibility(View.VISIBLE);
        binding.tvRouteStage.setText(delivered ? "Delivered" : "On the way");
    }

    private void updateTimeline(String status, String createdAt) {
        if (stepLabels == null) return;

        String lowerStatus = status != null ? status.toLowerCase() : "";
        boolean isCancelled = lowerStatus.contains("cancel");

        int activeStep = mapToDisplayStep(lowerStatus);

        binding.tvCurrentStatusLabel.setText(isCancelled
                ? "Order Cancelled"
                : (activeStep >= 0 && activeStep < STEP_LABELS.length ? STEP_LABELS[activeStep] : "Order Placed"));

        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < STEP_LABELS.length; i++) {
            GradientDrawable circle = (GradientDrawable) stepIconWraps[i].getBackground();

            if (isCancelled) {
                circle.setColor(i <= activeStep ? COLOR_CANCELLED : COLOR_PENDING_BG);
                stepIcons[i].setColorFilter(i <= activeStep ? 0xFFFFFFFF : COLOR_PENDING_ICON);
                stepLabels[i].setTextColor(i <= activeStep ? COLOR_CANCELLED : COLOR_PENDING_TEXT);
            } else if (i < activeStep) {
                circle.setColor(COLOR_DONE);
                stepIcons[i].setColorFilter(0xFFFFFFFF);
                stepLabels[i].setTextColor(COLOR_DONE);
            } else if (i == activeStep) {
                circle.setColor(COLOR_CURRENT);
                stepIcons[i].setColorFilter(0xFFFFFFFF);
                stepLabels[i].setTextColor(COLOR_CURRENT);
                animateStep(stepIconWraps[i]);
            } else {
                circle.setColor(COLOR_PENDING_BG);
                stepIcons[i].setColorFilter(COLOR_PENDING_ICON);
                stepLabels[i].setTextColor(COLOR_PENDING_TEXT);
            }

            if (i < connectorLines.length) {
                boolean lineDone = !isCancelled && i < activeStep;
                connectorLines[i].setBackgroundColor(lineDone ? COLOR_DONE : (isCancelled && i < activeStep ? COLOR_CANCELLED : 0xFFE0E0E0));
            }
        }

        // Only "Order Confirmed" has a real backend timestamp (created_at);
        // the other steps aren't timestamped server-side, so no time is shown for them.
        String confirmedTime = formatTime(createdAt);
        if (confirmedTime != null) {
            stepLabels[0].setText(STEP_LABELS[0] + "  ·  " + confirmedTime);
        }
    }

    /** Collapses the backend's 6-value status model onto the design's 4 visual steps. */
    private int mapToDisplayStep(String lowerStatus) {
        switch (lowerStatus) {
            case "pending":
            case "placed":
                return 0;
            case "confirmed":
            case "accepted":
                return 0;
            case "in_progress":
            case "preparing":
            case "cooking":
                return 1;
            // "ready" is the backend's final pre-delivery state (see
            // OrderAdapter.getNextStatus: preparing → ready → delivered,
            // there is no separate "out_for_delivery" status in this app) —
            // it belongs on the "Out For Delivery" step, not lumped in with
            // "preparing", or the tracker looks stuck one stage behind reality.
            case "ready":
            case "prepared":
            case "out_for_delivery":
            case "shipped":
            case "out for delivery":
                return 2;
            case "delivered":
            case "completed":
                return 3;
            default:
                return 0;
        }
    }

    private void animateStep(final View view) {
        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(800);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        view.startAnimation(pulse);
    }

    private String formatTime(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return null;
        Date date = null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String pattern : patterns) {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
            if (pattern.contains("'Z'")) {
                fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            try {
                date = fmt.parse(rawDate);
                if (date != null) break;
            } catch (ParseException ignored) { }
        }
        if (date == null) return null;
        return new SimpleDateFormat("h:mm a", Locale.US).format(date);
    }

    // ==================== Socket ====================

    private void connectSocket() {
        if (!socketManager.isConnected()) {
            socketManager.connect();
        }

        new Handler(getMainLooper()).postDelayed(() -> socketManager.joinOrderRoom(orderId), 500);

        socketManager.onOrderStatusUpdated(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String newStatus = data.optString("status", "");
                        if (!newStatus.isEmpty()) {
                            runOnUiThread(() -> {
                                currentStatus = newStatus;
                                updateTimeline(newStatus, null);
                                // Keep the route line in sync with the new stage.
                                if (loadedOrder != null) {
                                    loadedOrder.setStatus(newStatus);
                                    drawRoute(loadedOrder);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing orderStatusUpdated", e);
                    }
                }
            }
        });

        socketManager.onOrderCancelled(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    if (currentStatus != null) {
                        updateTimeline("cancelled", null);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketManager != null) {
            socketManager.off("orderStatusUpdated");
            socketManager.off("orderCancelled");
            socketManager.leaveOrderRoom(orderId);
        }
    }
}
