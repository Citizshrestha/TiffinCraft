package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.CommissionSettlement;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.util.List;

/** Renders a cook's past commission settlements — see CommissionHistoryActivity. */
public class CommissionSettlementAdapter extends RecyclerView.Adapter<CommissionSettlementAdapter.ViewHolder> {

    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final List<CommissionSettlement> items;

    public CommissionSettlementAdapter(List<CommissionSettlement> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_commission_settlement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommissionSettlement item = items.get(position);

        holder.tvPeriod.setText(MONTH_NAMES[item.getMonth() - 1] + " " + item.getYear());
        holder.tvOrderCount.setText(item.getOrderCount() + (item.getOrderCount() == 1 ? " order" : " orders"));
        holder.tvAmountDue.setText(CurrencyUtils.formatRupees(item.getAmountDue()));

        // Year header on the first row of each year. The list arrives newest-first
        // from the server, so comparing against the previous row is enough.
        boolean newYear = position == 0 || items.get(position - 1).getYear() != item.getYear();
        holder.tvYearHeader.setVisibility(newYear ? View.VISIBLE : View.GONE);
        if (newYear) holder.tvYearHeader.setText(String.valueOf(item.getYear()));

        int edge;
        switch (item.getStatus()) {
            case "verified":
                holder.tvStatusChip.setText("Verified");
                holder.tvStatusChip.setBackgroundResource(R.drawable.status_chip_delivered);
                holder.tvStatusChip.setTextColor(holder.itemView.getResources().getColor(R.color.status_delivered_text));
                holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                edge = holder.itemView.getResources().getColor(R.color.green_primary_dark);
                break;
            case "submitted":
                holder.tvStatusChip.setText("Submitted");
                holder.tvStatusChip.setBackgroundResource(R.drawable.status_chip_preparing);
                holder.tvStatusChip.setTextColor(holder.itemView.getResources().getColor(R.color.status_preparing_text));
                holder.ivStatusIcon.setImageResource(R.drawable.ic_receipt);
                edge = holder.itemView.getResources().getColor(R.color.commission_review_accent);
                break;
            case "rejected":
                holder.tvStatusChip.setText("Rejected");
                holder.tvStatusChip.setBackgroundResource(R.drawable.status_chip_sold_out);
                holder.tvStatusChip.setTextColor(0xFFA32D2D);
                holder.ivStatusIcon.setImageResource(R.drawable.ic_close);
                edge = 0xFFA32D2D;
                break;
            default: // pending
                holder.tvStatusChip.setText("Pending");
                holder.tvStatusChip.setBackgroundResource(R.drawable.status_chip_pending);
                holder.tvStatusChip.setTextColor(holder.itemView.getResources().getColor(R.color.status_pending_text));
                holder.ivStatusIcon.setImageResource(R.drawable.ic_wallet);
                edge = holder.itemView.getResources().getColor(R.color.commission_due_accent);
                break;
        }

        holder.viewStateEdge.setBackgroundColor(edge);

        holder.viewDivider.setVisibility(position == items.size() - 1 ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatusIcon;
        TextView tvPeriod, tvOrderCount, tvAmountDue, tvStatusChip;
        View viewDivider, viewStateEdge;
        TextView tvYearHeader;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
            tvOrderCount = itemView.findViewById(R.id.tvOrderCount);
            tvAmountDue = itemView.findViewById(R.id.tvAmountDue);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            viewDivider = itemView.findViewById(R.id.viewDivider);
            viewStateEdge = itemView.findViewById(R.id.viewStateEdge);
            tvYearHeader = itemView.findViewById(R.id.tvYearHeader);
        }
    }
}
