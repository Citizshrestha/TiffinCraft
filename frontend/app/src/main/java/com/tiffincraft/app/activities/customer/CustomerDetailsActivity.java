package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CustomerDetails;
import com.tiffincraft.app.models.CustomerDetailsResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Read-only customer profile shown to a cook — the mirror of {@link com.tiffincraft.app.activities.meal.CookDetailsActivity}
 * but for the other side of a shared order/chat (no menu, no favorites, no ordering — a cook can't order from a customer).
 */
public class CustomerDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private ImageView ivCustomerAvatar;
    private TextView tvCustomerName, tvCustomerPhoneHeader, tvCustomerPhone, tvCustomerAddress;
    private TextView tvTotalOrdersCustomer, tvMemberSince;
    private View btnCallCustomer, btnMessageCustomer, rowPhone;
    private ProgressBar progressBar;

    private int customerId = -1;
    private String customerPhone;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_details);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        customerId = getIntent().getIntExtra(EXTRA_CUSTOMER_ID, -1);
        if (customerId == -1) {
            Toast.makeText(this, "Invalid customer", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadCustomerDetails();
    }

    private void initViews() {
        ivCustomerAvatar = findViewById(R.id.ivCustomerAvatar);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerPhoneHeader = findViewById(R.id.tvCustomerPhoneHeader);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvTotalOrdersCustomer = findViewById(R.id.tvTotalOrdersCustomer);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        btnCallCustomer = findViewById(R.id.btnCallCustomer);
        btnMessageCustomer = findViewById(R.id.btnMessageCustomer);
        rowPhone = findViewById(R.id.rowPhone);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCallCustomer.setOnClickListener(v -> callCustomer());
        rowPhone.setOnClickListener(v -> callCustomer());
        btnMessageCustomer.setOnClickListener(v -> messageCustomer());
    }

    private void loadCustomerDetails() {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerById(token, customerId).enqueue(new Callback<CustomerDetailsResponse>() {
            @Override
            public void onResponse(Call<CustomerDetailsResponse> call, Response<CustomerDetailsResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getCustomer() != null) {
                    populateCustomer(response.body().getCustomer());
                } else {
                    Toast.makeText(CustomerDetailsActivity.this, "Failed to load customer details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CustomerDetailsResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailsActivity.this, "Network error loading customer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateCustomer(CustomerDetails customer) {
        String name = customer.getFullName() != null ? customer.getFullName() : "Customer";
        tvCustomerName.setText(name);

        customerPhone = customer.getPhone();
        tvCustomerPhoneHeader.setText(customerPhone != null && !customerPhone.isEmpty() ? customerPhone : "No phone on file");
        tvCustomerPhone.setText(customerPhone != null && !customerPhone.isEmpty() ? customerPhone : "—");

        tvCustomerAddress.setText(customer.getAddress() != null && !customer.getAddress().isEmpty()
                ? customer.getAddress() : "Not provided");

        tvTotalOrdersCustomer.setText(String.valueOf(customer.getTotalOrders()));
        tvMemberSince.setText(formatMemberSince(customer.getCreatedAt()));

        ImageUrlHelper.load(ivCustomerAvatar, customer.getProfileImage(), R.drawable.avatar_customer);

        boolean hasPhone = customerPhone != null && !customerPhone.isEmpty();
        btnCallCustomer.setEnabled(hasPhone);
        btnCallCustomer.setAlpha(hasPhone ? 1f : 0.4f);
    }

    private void callCustomer() {
        if (TextUtils.isEmpty(customerPhone)) {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customerPhone)));
    }

    private void messageCustomer() {
        // The chat panel (shown from the cook's home/orders screen) already knows how to
        // get-or-create a conversation by other_user_id; re-opening it here would duplicate
        // that flow, so just let the cook know to use the chat entry point.
        Toast.makeText(this, "Open this customer's chat from the Chat tab", Toast.LENGTH_SHORT).show();
    }

    private String formatMemberSince(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "—";
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
        if (date == null) return rawDate;
        return new SimpleDateFormat("MMM yyyy", Locale.US).format(date);
    }
}
