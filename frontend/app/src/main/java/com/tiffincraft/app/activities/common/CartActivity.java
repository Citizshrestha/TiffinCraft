package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.CartAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CartItem;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.UpdateCartItemRequest;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartActionListener {

    private RecyclerView rvCart;
    private TextView tvGrandTotal, tvItemCount, tvClearCart;
    private MaterialButton btnCheckout, btnBrowseCooks;
    private LinearLayout layoutEmptyCart, checkoutPanel;
    private View imgBack;
    private BottomNavigationView bottomNavigation;

    private CartAdapter adapter;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        rvCart          = findViewById(R.id.rvCart);
        tvGrandTotal    = findViewById(R.id.tvGrandTotal);
        tvItemCount     = findViewById(R.id.tvItemCount);
        tvClearCart     = findViewById(R.id.tvClearCart);
        btnCheckout     = findViewById(R.id.btnCheckout);
        btnBrowseCooks  = findViewById(R.id.btnBrowseCooks);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        checkoutPanel   = findViewById(R.id.checkoutPanel);
        imgBack         = findViewById(R.id.imgBack);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        rvCart.setLayoutManager(new LinearLayoutManager(this));

        imgBack.setOnClickListener(v -> finish());
        btnCheckout.setOnClickListener(v -> goToCheckout());

        if (btnBrowseCooks != null) {
            btnBrowseCooks.setOnClickListener(v -> {
                startActivity(new Intent(this, com.tiffincraft.app.activities.customer.CustomerMenuActivity.class));
                finish();
            });
        }

        if (tvClearCart != null) {
            tvClearCart.setOnClickListener(v -> clearEntireCart());
        }

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        String token = "Bearer " + sessionManager.getToken();

        apiService.getCart(token).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CartResponse body = response.body();

                    if (body.getCartGroups() == null || body.getCartGroups().isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    layoutEmptyCart.setVisibility(View.GONE);
                    rvCart.setVisibility(View.VISIBLE);
                    if (checkoutPanel != null) checkoutPanel.setVisibility(View.VISIBLE);
                    btnCheckout.setEnabled(true);
                    if (tvClearCart != null) tvClearCart.setVisibility(View.VISIBLE);

                    if (adapter == null) {
                        adapter = new CartAdapter(CartActivity.this, body.getCartGroups(), CartActivity.this);
                        rvCart.setAdapter(adapter);
                    } else {
                        adapter.setGroups(body.getCartGroups());
                    }

                    int count = body.getItemCount();
                    int cookCount = body.getCartGroups() != null ? body.getCartGroups().size() : 0;
                    tvGrandTotal.setText(String.format("₹%.0f", body.getGrandTotal()));
                    String countLabel = count + (count == 1 ? " item" : " items");
                    if (cookCount > 1) {
                        countLabel += " · " + cookCount + " cooks";
                    }
                    tvItemCount.setText(countLabel);
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Failed to load cart", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        rvCart.setVisibility(View.GONE);
        layoutEmptyCart.setVisibility(View.VISIBLE);
        if (checkoutPanel != null) checkoutPanel.setVisibility(View.GONE);
        tvGrandTotal.setText("₹0");
        tvItemCount.setText("0 items");
        btnCheckout.setEnabled(false);
        if (tvClearCart != null) tvClearCart.setVisibility(View.GONE);
    }

    private void clearEntireCart() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.clearCart(token).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                Toast.makeText(CartActivity.this, "Cart cleared", Toast.LENGTH_SHORT).show();
                showEmptyState();
                if (adapter != null) adapter.setGroups(new java.util.ArrayList<>());
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Failed to clear cart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.updateCartItem(token, item.getCartItemId(), new UpdateCartItemRequest(newQuantity))
                .enqueue(new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        loadCart();
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(CartActivity.this, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                        loadCart();
                    }
                });
    }

    @Override
    public void onRemoveItem(CartItem item) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.removeCartItem(token, item.getCartItemId()).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                Toast.makeText(CartActivity.this, "Item removed", Toast.LENGTH_SHORT).show();
                loadCart();
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Failed to remove item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToCheckout() {
        Intent intent = new Intent(this, com.tiffincraft.app.activities.order.OrderSummaryActivity.class);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int itemId = item.getItemId();
                    
                    if (itemId == R.id.nav_home) {
                        startActivity(new Intent(CartActivity.this, com.tiffincraft.app.activities.customer.CustomerHomeActivity.class));
                        finish();
                        return true;
                    } else if (itemId == R.id.nav_search) {
                        startActivity(new Intent(CartActivity.this, com.tiffincraft.app.activities.customer.CustomerMenuActivity.class));
                        finish();
                        return true;
                    } else if (itemId == R.id.nav_orders) {
                        startActivity(new Intent(CartActivity.this, com.tiffincraft.app.activities.customer.OrderHistoryActivity.class));
                        finish();
                        return true;
                    } else if (itemId == R.id.nav_profile) {
                        startActivity(new Intent(CartActivity.this, com.tiffincraft.app.activities.customer.CustomerProfileActivity.class));
                        finish();
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}