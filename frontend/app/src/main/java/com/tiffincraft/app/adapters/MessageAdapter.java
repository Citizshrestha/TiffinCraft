package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.ChatMessage;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<ChatMessage> messages;

    public MessageAdapter(Context context, List<ChatMessage> messages) {
        this.context  = context;
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case ChatMessage.TYPE_TEXT_SENT:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_text_sent, parent, false));
            case ChatMessage.TYPE_TEXT_RECEIVED:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_text_received, parent, false));
            case ChatMessage.TYPE_CALL:
                return new CallViewHolder(inflater.inflate(R.layout.item_message_call, parent, false));
            case ChatMessage.TYPE_DATE_SEPARATOR:
            default:
                return new DateViewHolder(inflater.inflate(R.layout.item_message_date_separator, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof TextViewHolder) {
            TextViewHolder vh = (TextViewHolder) holder;
            vh.tvText.setText(msg.getText());
            vh.tvTimestamp.setText(msg.getTimestamp());

        } else if (holder instanceof CallViewHolder) {
            CallViewHolder vh = (CallViewHolder) holder;

            boolean declined = ChatMessage.CALL_DECLINED.equals(msg.getCallState());

            // Label
            vh.tvCallLabel.setText(msg.getCallState());

            // Duration row: "0:15  5:58 PM" or just "6:57 PM" for declined
            if (declined) {
                vh.tvCallTime.setText(msg.getTimestamp());
                vh.tvCallLabel.setTextColor(context.getResources().getColor(R.color.error_red, null));
                vh.ivCallIcon.setImageResource(R.drawable.ic_call_declined);
                vh.ivCallIconBg.setBackgroundResource(R.drawable.bg_call_declined_circle);
            } else {
                String duration = msg.getCallDuration() != null ? msg.getCallDuration() + "  " : "";
                vh.tvCallTime.setText(duration + msg.getTimestamp());
                vh.tvCallLabel.setTextColor(context.getResources().getColor(R.color.text_primary, null));
                vh.ivCallIcon.setImageResource(R.drawable.ic_call_end);
                vh.ivCallIconBg.setBackgroundResource(R.drawable.bg_call_icon_circle);
            }

            // Align bubble to sent or received side
            LinearLayout root = (LinearLayout) vh.itemView;
            if (msg.isSentByMe()) {
                root.setGravity(Gravity.END);
                root.setPaddingRelative(48, root.getPaddingTop(), 4, root.getPaddingBottom());
            } else {
                root.setGravity(Gravity.START);
                root.setPaddingRelative(4, root.getPaddingTop(), 48, root.getPaddingBottom());
            }

        } else if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).tvDate.setText(msg.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    // ---- ViewHolders ----

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTimestamp;
        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText      = itemView.findViewById(R.id.tvMessageText);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    static class CallViewHolder extends RecyclerView.ViewHolder {
        TextView  tvCallLabel, tvCallTime;
        ImageView ivCallIcon;
        FrameLayout ivCallIconBg;
        CallViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCallLabel  = itemView.findViewById(R.id.tvCallLabel);
            tvCallTime   = itemView.findViewById(R.id.tvCallTime);
            ivCallIcon   = itemView.findViewById(R.id.ivCallIcon);
            ivCallIconBg = itemView.findViewById(R.id.ivCallIconBg);
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateLabel);
        }
    }
}
