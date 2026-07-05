package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.ChatContact;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(ChatContact contact);
    }

    private final Context context;
    private final List<ChatContact> contacts;
    private final OnContactClickListener listener;

    public ChatListAdapter(Context context, List<ChatContact> contacts, OnContactClickListener listener) {
        this.context   = context;
        this.contacts  = contacts;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatContact contact = contacts.get(position);

        holder.tvName.setText(contact.getName());
        holder.tvLastMessage.setText(contact.getLastMessage());
        holder.tvTimestamp.setText(contact.getTimestamp());

        // Online indicator
        holder.viewOnlineDot.setVisibility(contact.isOnline() ? View.VISIBLE : View.GONE);

        // Avatar: use provided drawable or keep circle placeholder
        if (contact.getAvatarResId() != 0) {
            holder.ivAvatar.setImageResource(contact.getAvatarResId());
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_avatar_circle);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContactClick(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts == null ? 0 : contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        View      viewOnlineDot;
        TextView  tvName, tvLastMessage, tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar       = itemView.findViewById(R.id.ivContactAvatar);
            viewOnlineDot  = itemView.findViewById(R.id.viewOnlineDot);
            tvName         = itemView.findViewById(R.id.tvContactName);
            tvLastMessage  = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp    = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
