package com.tiffincraft.app.activities.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.FragmentManager;

import android.widget.Toast;

import androidx.activity.result.IntentSenderRequest;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;

import io.socket.emitter.Emitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer-side "cooks near me" discovery map. One-shot location fetch per
 * screen-open (never continuous tracking — see MapImplementation.md Section 7
 * Performance & Battery for the rationale); cook appear/disappear is kept
 * live via the existing Socket.IO connection's "cookAvailabilityChanged"
 * event (holiday-mode toggles), not by re-polling GPS.
 */
public class NearbyCooksMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final long GPS_TIMEOUT_MS = 15_000L;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource gpsCancelSource;
    private ApiService apiService;
    private SessionManager sessionManager;
    private SocketManager socketManager;

    private double radiusKm = 10.0;
    private Double myLat = null;
    private Double myLng = null;
    private final Map<Marker, CookProfile> markerCooks = new HashMap<>();

    private View layoutLoading;
    private TextView tvLoadingMessage;
    private View layoutState;
    private TextView tvStateEmoji, tvStateTitle, tvStateMessage, btnStateSecondary;
    private MaterialButton btnStateAction;
    private MaterialCardView cardResultCount;
    private MaterialCardView cardLegend;
    private TextView tvResultCount;
    private ChipGroup chipGroupRadius;

    private final Emitter.Listener availabilityListener = args -> runOnUiThread(() -> {
        // A cook toggled holiday mode somewhere — silently refresh the pins if
        // we already have a fix. No GPS re-fetch, no user-visible spinner.
        if (myLat != null && myLng != null) {
            loadNearbyCooks(myLat, myLng, false);
        }
    });

    private final ActivityResultLauncher<IntentSenderRequest> locationResolutionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchGpsAndLoad();
                } else {
                    showLocationServicesOffState();
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    requestLocationThenLoad();
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermanentlyDeniedState();
                } else {
                    showDeniedState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_cooks_map);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        socketManager = SocketManager.getInstance(this);

        initViews();
        setupListeners();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        socketManager.connect();
        socketManager.onCookAvailabilityChanged(availabilityListener);
        // Recovers automatically if the user granted permission or turned on
        // location services from the Settings app and came back.
        if (layoutState.getVisibility() == View.VISIBLE && myLat == null) {
            requestLocationThenLoad();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gpsCancelSource != null) {
            gpsCancelSource.cancel();
        }
        // Prevents the singleton SocketManager from retaining this Activity
        // via a stale "cookAvailabilityChanged" callback after the screen closes.
        socketManager.off("cookAvailabilityChanged");
    }

    private void initViews() {
        layoutLoading = findViewById(R.id.layoutLoading);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        layoutState = findViewById(R.id.layoutState);
        tvStateEmoji = findViewById(R.id.tvStateEmoji);
        tvStateTitle = findViewById(R.id.tvStateTitle);
        tvStateMessage = findViewById(R.id.tvStateMessage);
        btnStateAction = findViewById(R.id.btnStateAction);
        btnStateSecondary = findViewById(R.id.btnStateSecondary);
        cardResultCount = findViewById(R.id.cardResultCount);
        cardLegend = findViewById(R.id.cardLegend);
        tvResultCount = findViewById(R.id.tvResultCount);
        chipGroupRadius = findViewById(R.id.chipGroupRadius);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        ImageView btnRecenter = findViewById(R.id.btnRecenter);
        btnRecenter.setOnClickListener(v -> {
            // Jump straight back to the "You" pin if we already have a fix;
            // only re-run the full GPS flow when we don't.
            if (map != null && myLat != null && myLng != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLat, myLng), 15f));
            } else {
                requestLocationThenLoad();
            }
        });

        chipGroupRadius.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip5km) radiusKm = 5.0;
            else if (id == R.id.chip10km) radiusKm = 10.0;
            else if (id == R.id.chip25km) radiusKm = 25.0;
            else if (id == R.id.chip50km) radiusKm = 50.0;

            if (myLat != null && myLng != null) {
                loadNearbyCooks(myLat, myLng, true);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_tiffincraft));
        } catch (Exception ignored) {
            // Styling is cosmetic — an unparsable style must never block the map.
        }
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        map.setOnMarkerClickListener(marker -> {
            CookProfile cook = markerCooks.get(marker);
            if (cook != null) {
                CookMapBottomSheet.newInstance(cook)
                        .show(getSupportFragmentManager(), "cook_map_sheet");
            }
            return true;
        });

        requestLocationThenLoad();
    }

    // ==================== Permission & location ====================

    private void requestLocationThenLoad() {
        if (!hasLocationPermission()) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleState();
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
            return;
        }

        if (!isLocationServicesEnabled()) {
            promptEnableLocationServices();
            return;
        }

        fetchGpsAndLoad();
    }

    /**
     * OS location services are off — ask Google Play services to show the
     * native "turn on location" dialog instead of bouncing the user out to
     * the Settings app. Falls back to the settings deep-link state when the
     * dialog can't be shown (e.g. policy-restricted devices).
     */
    private void promptEnableLocationServices() {
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build())
                .build();
        LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener(r -> fetchGpsAndLoad())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            locationResolutionLauncher.launch(new IntentSenderRequest.Builder(
                                    ((ResolvableApiException) e).getResolution()).build());
                        } catch (Exception ex) {
                            showLocationServicesOffState();
                        }
                    } else {
                        showLocationServicesOffState();
                    }
                });
    }

    private void fetchGpsAndLoad() {
        showLoading("Finding your location…");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showDeniedState();
            return;
        }

        gpsCancelSource = new CancellationTokenSource();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (gpsCancelSource != null) gpsCancelSource.cancel();
        }, GPS_TIMEOUT_MS);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        gpsCancelSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLng = location.getLongitude();
                        loadNearbyCooks(myLat, myLng, true);
                    } else {
                        showGpsFailedState();
                    }
                })
                .addOnFailureListener(e -> showGpsFailedState());
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationServicesEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        return lm != null && LocationManagerCompat.isLocationEnabled(lm);
    }

    // ==================== Data loading ====================

    private void loadNearbyCooks(double lat, double lng, boolean showSpinner) {
        if (showSpinner) {
            showLoading("Finding cooks near you…");
        }
        String token = "Bearer " + sessionManager.getToken();

        apiService.getNearbyCooks(token, lat, lng, radiusKm).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    renderMarkers(lat, lng, response.body().getCooks());
                } else {
                    showApiFailedState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showApiFailedState();
            }
        });
    }

    private void renderMarkers(double lat, double lng, List<CookProfile> cooks) {
        hideOverlays();
        if (map == null) return;

        map.clear();
        markerCooks.clear();

        LatLng myPosition = new LatLng(lat, lng);
        map.addCircle(new CircleOptions()
                .center(myPosition)
                .radius(radiusKm * 1000)
                .strokeColor(0x334CAF50)
                .fillColor(0x1A4CAF50)
                .strokeWidth(2f));
        map.addMarker(new MarkerOptions()
                .position(myPosition)
                .title("You")
                .anchor(0.5f, 1f)
                .zIndex(1f)
                .icon(labeledPin("You", 0xFF1E88E5)));

        if (cooks == null || cooks.isEmpty()) {
            // Backend already falls back to the nearest cooks regardless of
            // radius — an empty list means no cooks exist anywhere yet.
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 13f));
            showEmptyState();
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(myPosition);

        for (CookProfile cook : cooks) {
            if (!cook.hasCoordinates()) continue; // defense in depth; backend already filters NULLs
            LatLng position = offsetIfColliding(cook, cooks);
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(cook.getKitchenName())
                    .snippet(cook.getDistanceKm() != null
                            ? String.format(java.util.Locale.getDefault(), "%.1f km away", cook.getDistanceKm())
                            : null)
                    .anchor(0.5f, 1f)
                    .icon(labeledPin(cookPinLabel(cook), 0xFF2E7D32)));
            if (marker != null) {
                markerCooks.put(marker, cook);
            }
            boundsBuilder.include(position);
        }

        // Backend fallback: results sorted by distance, so if even the nearest
        // one is outside the radius, all of them came from the fallback query.
        Double nearest = cooks.get(0).getDistanceKm();
        boolean beyondRadius = nearest != null && nearest > radiusKm;
        if (beyondRadius) {
            tvResultCount.setText("None within " + formatRadius(radiusKm) + " — showing nearest");
        } else {
            tvResultCount.setText(cooks.size() == 1 ? "1 cook nearby" : cooks.size() + " cooks nearby");
        }
        cardResultCount.setVisibility(View.VISIBLE);
        cardLegend.setVisibility(View.VISIBLE);

        try {
            LatLngBounds bounds = boundsBuilder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        } catch (IllegalStateException e) {
            // Bounds needs 2+ distinct points; fall back to a simple zoom.
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 13f));
        }
    }

    /**
     * Spreads markers whose coordinates collide within ~11 m (same building /
     * identical geocode) onto a small circle around the shared point so both
     * remain individually tappable, without pulling in a clustering library
     * for the current small cook count.
     */
    private LatLng offsetIfColliding(CookProfile cook, List<CookProfile> allCooks) {
        double lat = cook.getLatitude();
        double lng = cook.getLongitude();

        int collisionIndex = 0;
        int collisionCount = 0;
        for (CookProfile other : allCooks) {
            if (!other.hasCoordinates()) continue;
            boolean samePoint = Math.abs(other.getLatitude() - lat) < 0.0001
                    && Math.abs(other.getLongitude() - lng) < 0.0001;
            if (samePoint) {
                if (other.getId() == cook.getId()) collisionIndex = collisionCount;
                collisionCount++;
            }
        }
        if (collisionCount <= 1) {
            return new LatLng(lat, lng);
        }

        double angle = (2 * Math.PI / collisionCount) * collisionIndex;
        double offsetDegrees = 0.00008; // ≈ 9 m
        return new LatLng(lat + offsetDegrees * Math.cos(angle), lng + offsetDegrees * Math.sin(angle));
    }

    // ==================== State overlays ====================

    private void showLoading(String message) {
        layoutState.setVisibility(View.GONE);
        cardResultCount.setVisibility(View.GONE);
        cardLegend.setVisibility(View.GONE);
        tvLoadingMessage.setText(message);
        layoutLoading.setVisibility(View.VISIBLE);
    }

    private void hideOverlays() {
        layoutLoading.setVisibility(View.GONE);
        layoutState.setVisibility(View.GONE);
    }

    private void showRationaleState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("📍");
        tvStateTitle.setText("Find cooks near you");
        tvStateMessage.setText("Your location is used once to find nearby cooks — it is never stored or tracked.");
        btnStateAction.setText("Allow Location Access");
        btnStateAction.setOnClickListener(v -> permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }));
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showDeniedState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("🚫");
        tvStateTitle.setText("Location permission needed");
        tvStateMessage.setText("We couldn't find cooks near you without your location.");
        btnStateAction.setText("Try Again");
        btnStateAction.setOnClickListener(v -> requestLocationThenLoad());
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showPermanentlyDeniedState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("⚙️");
        tvStateTitle.setText("Location permission needed");
        tvStateMessage.setText("Location access was permanently denied. Enable it from app settings to see cooks near you.");
        btnStateAction.setText("Open Settings");
        btnStateAction.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showLocationServicesOffState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("📡");
        tvStateTitle.setText("Turn on location services");
        tvStateMessage.setText("Your device's location services are off. Turn them on to find cooks near you.");
        btnStateAction.setText("Open Location Settings");
        btnStateAction.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showGpsFailedState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("🛰️");
        tvStateTitle.setText("Couldn't get your location");
        tvStateMessage.setText("Make sure you're not in airplane mode and try again.");
        btnStateAction.setText("Retry");
        btnStateAction.setOnClickListener(v -> requestLocationThenLoad());
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showApiFailedState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("⚠️");
        tvStateTitle.setText("Couldn't load nearby cooks");
        tvStateMessage.setText("Check your connection and retry.");
        btnStateAction.setText("Retry");
        btnStateAction.setOnClickListener(v -> {
            // Reuse the coordinates we already have — no second GPS wait.
            if (myLat != null && myLng != null) {
                loadNearbyCooks(myLat, myLng, true);
            } else {
                requestLocationThenLoad();
            }
        });
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        layoutLoading.setVisibility(View.GONE);
        tvStateEmoji.setText("🍽️");
        tvStateTitle.setText("No cooks available yet");
        tvStateMessage.setText("No cooks have joined in your region so far — check back soon!");
        btnStateAction.setText("Retry");
        btnStateAction.setOnClickListener(v -> {
            if (myLat != null && myLng != null) {
                loadNearbyCooks(myLat, myLng, true);
            } else {
                requestLocationThenLoad();
            }
        });
        btnStateSecondary.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
    }

    // ==================== Marker rendering helpers ====================

    /**
     * Draws a rounded name pill with a pointer tail as a marker icon, so users
     * can tell at a glance which pin is them (blue "You") and which pins are
     * cooks (green, labelled with the cook's name).
     */
    private com.google.android.gms.maps.model.BitmapDescriptor labeledPin(String label, int bgColor) {
        float density = getResources().getDisplayMetrics().density;

        android.graphics.Paint textPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(13f * density);
        textPaint.setColor(android.graphics.Color.WHITE);
        textPaint.setFakeBoldText(true);

        float padH = 10f * density;
        float padV = 6f * density;
        float tail = 7f * density;
        float pillW = textPaint.measureText(label) + padH * 2;
        float pillH = (textPaint.descent() - textPaint.ascent()) + padV * 2;

        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                (int) Math.ceil(pillW), (int) Math.ceil(pillH + tail),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

        android.graphics.Paint bgPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        canvas.drawRoundRect(0f, 0f, pillW, pillH, pillH / 2f, pillH / 2f, bgPaint);

        android.graphics.Path pointer = new android.graphics.Path();
        pointer.moveTo(pillW / 2f - tail, pillH - 1f);
        pointer.lineTo(pillW / 2f + tail, pillH - 1f);
        pointer.lineTo(pillW / 2f, pillH + tail);
        pointer.close();
        canvas.drawPath(pointer, bgPaint);

        canvas.drawText(label, padH, padV - textPaint.ascent(), textPaint);
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    /** Cook's real name for the pin label, trimmed so pills stay compact. */
    private String cookPinLabel(CookProfile cook) {
        String name = cook.getFullName();
        if (name == null || name.trim().isEmpty()) name = cook.getKitchenName();
        if (name == null || name.trim().isEmpty()) name = "Cook";
        name = name.trim();
        return name.length() > 20 ? name.substring(0, 19) + "…" : name;
    }

    private String formatRadius(double radius) {
        return (radius == Math.floor(radius) ? String.valueOf((int) radius) : String.valueOf(radius)) + " km";
    }
}
