package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.EarningsTransaction;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.util.List;

public class EarningsAdapter extends RecyclerView.Adapter<EarningsAdapter.TransactionViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(EarningsTransaction transaction);
    }

    private final Context context;
    private final List<EarningsTransaction> transactions;
    private final OnTransactionClickListener listener;

    public EarningsAdapter(Context context, List<EarningsTransaction> transactions,
                           OnTransactionClickListener listener) {
        this.context = context;
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_earning_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        EarningsTransaction t = transactions.get(position);

        String name = t.getCustomerName() != null ? t.getCustomerName() : "Customer";
        holder.tvTitle.setText("Order #" + t.getOrderId() + " · " + name);

        String date = t.getDate() != null ? t.getDate() : "";
        String time = t.getTime() != null ? t.getTime() : "";
        String subtitle = date.isEmpty() ? time : (time.isEmpty() ? date : date + ", " + time);
        holder.tvSubtitle.setText(subtitle);

        holder.tvAmount.setText("+ " + CurrencyUtils.formatRupees(t.getAmount()));

        if (t.isOnlinePayment()) {
            holder.tvPaymentMethod.setText("Online");
            holder.tvPaymentMethod.setBackgroundResource(R.drawable.status_chip_new);
            holder.tvPaymentMethod.setTextColor(0xFF1565C0);
        } else {
            holder.tvPaymentMethod.setText("COD");
            holder.tvPaymentMethod.setBackgroundResource(R.drawable.status_chip_delivered);
            holder.tvPaymentMethod.setTextColor(0xFF2E7D32);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTransactionClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return transactions == null ? 0 : transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvAmount, tvPaymentMethod;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle          = itemView.findViewById(R.id.tvTransactionTitle);
            tvSubtitle       = itemView.findViewById(R.id.tvTransactionSubtitle);
            tvAmount         = itemView.findViewById(R.id.tvTransactionAmount);
            tvPaymentMethod  = itemView.findViewById(R.id.tvTransactionPaymentMethod);
        }
    }
}
