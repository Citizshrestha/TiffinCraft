package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.ChatContact;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(ChatContact contact);
    }

    private final Context context;
    private final List<ChatContact> contacts;
    private final OnContactClickListener listener;

    public ChatListAdapter(Context context, List<ChatContact> contacts, OnContactClickListener listener) {
        this.context  = context;
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_chat_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ChatContact contact = contacts.get(position);

        holder.tvName.setText(contact.getName());
        holder.tvLastMessage.setText(contact.getLastMessage());
        holder.tvTimestamp.setText(contact.getTimestamp());

        holder.viewOnlineDot.setVisibility(contact.isOnline() ? View.VISIBLE : View.GONE);

        int unread = contact.getUnreadCount();
        if (unread > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(unread > 99 ? "99+" : String.valueOf(unread));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(contact.getAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(holder.ivAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContactClick(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts == null ? 0 : contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        View      viewOnlineDot;
        TextView  tvName, tvLastMessage, tvTimestamp, tvUnreadCount;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar      = itemView.findViewById(R.id.ivContactAvatar);
            viewOnlineDot = itemView.findViewById(R.id.viewOnlineDot);
            tvName        = itemView.findViewById(R.id.tvContactName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp   = itemView.findViewById(R.id.tvContactTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
