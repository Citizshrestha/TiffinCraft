package com.tiffincraft.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.MediaViewerActivity;
import com.tiffincraft.app.models.ChatApiMessage;
import com.tiffincraft.app.models.ChatMessage;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Only signal fired by the adapter — the activity reads getSelectedMessages() to act. */
    public interface OnMessageActionListener {
        void onSelectionChanged(int selectedCount, boolean canEditSelection);
    }

    /**
     * A tap on Accept/Decline inside a structured card.
     *
     * The adapter deliberately does no API call and no optimistic re-render: the
     * decision endpoints are guarded on the row's current state, so the activity
     * reloads the thread from the server and the card's buttons disappear because
     * `live_status` moved on — not because a client guessed that it would.
     */
    public interface OnCardDecisionListener {
        void onCardDecision(ChatMessage message, boolean accept);
    }

    private static final int SELECTED_HIGHLIGHT = 0x1A00897B; // translucent teal
    /** Same tint as bg_bubble_sent, so a card reads as one of your own messages. */
    private static final int SENT_CARD_TINT = 0xFFE8F5E9;

    private final Context context;
    private final List<ChatMessage> messages;
    private String contactAvatar;
    private final int contactAvatarPlaceholder;
    private OnMessageActionListener actionListener;
    private OnCardDecisionListener cardDecisionListener;

    private boolean selectionMode = false;
    private final Set<Integer> selectedServerIds = new LinkedHashSet<>();

    public MessageAdapter(Context context, List<ChatMessage> messages) {
        this(context, messages, null, R.drawable.avatar_customer);
    }

    public MessageAdapter(Context context, List<ChatMessage> messages,
                          String contactAvatar, int contactAvatarPlaceholder) {
        this.context  = context;
        this.messages = messages;
        this.contactAvatar = contactAvatar;
        this.contactAvatarPlaceholder = contactAvatarPlaceholder;
    }

    public void setContactAvatar(String contactAvatar) {
        this.contactAvatar = contactAvatar;
        notifyDataSetChanged();
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    public void setOnCardDecisionListener(OnCardDecisionListener listener) {
        this.cardDecisionListener = listener;
    }

    // ==================== Selection mode ====================

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return selectedServerIds.size();
    }

    public List<ChatMessage> getSelectedMessages() {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (selectedServerIds.contains(m.getServerId())) result.add(m);
        }
        return result;
    }

    public int indexOfMessage(ChatMessage target) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) == target) return i;
        }
        return -1;
    }

    public void clearSelection() {
        if (selectedServerIds.isEmpty() && !selectionMode) return;
        selectedServerIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /** Turn a live message into a "deleted" placeholder in place (soft-delete UX). */
    public void markMessageDeleted(int serverId) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m.getServerId() == serverId) {
                boolean wasSelected = selectedServerIds.remove(serverId);
                m.markDeleted();
                // The row's item view TYPE changes here (text/media -> deleted).
                // notifyItemChanged() would try to rebind the old-type ViewHolder in
                // place and crash ("view holder is of a different type"); remove+insert
                // forces RecyclerView to create a fresh holder of the new type instead.
                notifyItemRemoved(i);
                notifyItemInserted(i);
                if (wasSelected) {
                    selectionMode = !selectedServerIds.isEmpty();
                    notifySelectionChanged();
                }
                return;
            }
        }
    }

    private void toggleSelection(ChatMessage msg) {
        if (msg.getServerId() <= 0) return;
        if (!selectedServerIds.remove(msg.getServerId())) {
            selectedServerIds.add(msg.getServerId());
        }
        selectionMode = !selectedServerIds.isEmpty();
        int index = indexOfMessage(msg);
        if (index >= 0) notifyItemChanged(index);
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (actionListener == null) return;
        List<ChatMessage> selected = getSelectedMessages();
        boolean canEdit = selected.size() == 1 && selected.get(0).canEdit();
        actionListener.onSelectionChanged(selected.size(), canEdit);
    }

    // ==================== Adapter ====================

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
            case ChatMessage.TYPE_IMAGE_SENT:
            case ChatMessage.TYPE_VIDEO_SENT:
                return new MediaViewHolder(inflater.inflate(R.layout.item_message_media_sent, parent, false));
            case ChatMessage.TYPE_IMAGE_RECEIVED:
            case ChatMessage.TYPE_VIDEO_RECEIVED:
                return new MediaViewHolder(inflater.inflate(R.layout.item_message_media_received, parent, false));
            case ChatMessage.TYPE_CALL:
                return new CallViewHolder(inflater.inflate(R.layout.item_message_call, parent, false));
            case ChatMessage.TYPE_DELETED_SENT:
                return new DeletedViewHolder(inflater.inflate(R.layout.item_message_deleted_sent, parent, false));
            case ChatMessage.TYPE_DELETED_RECEIVED:
                return new DeletedViewHolder(inflater.inflate(R.layout.item_message_deleted_received, parent, false));
            case ChatMessage.TYPE_CARD:
                return new CardViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
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
            String timeLabel = msg.getTimestamp() != null ? msg.getTimestamp() : "";
            if (msg.isEdited()) {
                timeLabel = timeLabel.isEmpty() ? "edited" : timeLabel + " · edited";
            }
            vh.tvTimestamp.setText(timeLabel);
            if (msg.getViewType() == ChatMessage.TYPE_TEXT_RECEIVED && vh.ivAvatar != null) {
                ImageUrlHelper.loadCircle(vh.ivAvatar, contactAvatar, contactAvatarPlaceholder);
            }
            bindMessageActions(vh.itemView, msg, null);

        } else if (holder instanceof MediaViewHolder) {
            MediaViewHolder vh = (MediaViewHolder) holder;
            vh.tvTimestamp.setText(msg.getTimestamp());

            boolean isVideo = msg.getViewType() == ChatMessage.TYPE_VIDEO_SENT
                    || msg.getViewType() == ChatMessage.TYPE_VIDEO_RECEIVED;
            String url = msg.getMediaUrl() != null ? msg.getMediaUrl() : msg.getText();

            if (isVideo) {
                vh.ivMedia.setVisibility(View.GONE);
                vh.tvMediaLabel.setVisibility(View.VISIBLE);
                vh.tvMediaLabel.setText("Play video");
            } else {
                vh.tvMediaLabel.setVisibility(View.GONE);
                vh.ivMedia.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(vh.ivMedia);
            }

            if ((msg.getViewType() == ChatMessage.TYPE_IMAGE_RECEIVED
                    || msg.getViewType() == ChatMessage.TYPE_VIDEO_RECEIVED)
                    && vh.ivAvatar != null) {
                ImageUrlHelper.loadCircle(vh.ivAvatar, contactAvatar, contactAvatarPlaceholder);
            }

            bindMessageActions(vh.itemView, msg, () -> {
                if (url == null || url.trim().isEmpty()) return;
                // Open in-app full-screen (Instagram/Facebook style) instead of
                // launching the raw Cloudinary URL in an external browser.
                Intent intent = new Intent(context, MediaViewerActivity.class);
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, url);
                intent.putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, isVideo);
                context.startActivity(intent);
            });

        } else if (holder instanceof CallViewHolder) {
            CallViewHolder vh = (CallViewHolder) holder;

            boolean declined = ChatMessage.CALL_DECLINED.equals(msg.getCallState());

            vh.tvCallLabel.setText(msg.getCallState());

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

            LinearLayout root = (LinearLayout) vh.itemView;
            if (msg.isSentByMe()) {
                root.setGravity(Gravity.END);
                root.setPaddingRelative(48, root.getPaddingTop(), 4, root.getPaddingBottom());
            } else {
                root.setGravity(Gravity.START);
                root.setPaddingRelative(4, root.getPaddingTop(), 48, root.getPaddingBottom());
            }

        } else if (holder instanceof DeletedViewHolder) {
            DeletedViewHolder vh = (DeletedViewHolder) holder;
            vh.tvDeletedLabel.setText(msg.getDeletedLabel());
            vh.tvTimestamp.setText(msg.getTimestamp());
            vh.itemView.setOnLongClickListener(null);
            vh.itemView.setLongClickable(false);
            vh.itemView.setOnClickListener(null);
            vh.itemView.setBackgroundColor(Color.TRANSPARENT);

        } else if (holder instanceof CardViewHolder) {
            bindCard((CardViewHolder) holder, msg);

        } else if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).tvDate.setText(msg.getText());
        }
    }

    /**
     * A structured card: the server's sentence, plus a decision only if there is
     * still one to make.
     *
     * Actions and the status line are mutually exclusive, which is the whole point
     * — a card that already has an outcome must not also offer to change it.
     */
    private void bindCard(CardViewHolder vh, ChatMessage msg) {
        String label;
        if (ChatApiMessage.TYPE_CUSTOM_MEAL_REQUEST.equals(msg.getCardType())) {
            label = "MEAL CHANGE REQUEST";
        } else if (ChatApiMessage.TYPE_SUBSCRIPTION_REQUEST.equals(msg.getCardType())) {
            label = "SUBSCRIPTION REQUEST";
        } else {
            label = "SUBSCRIPTION";
        }

        // Sided like a text bubble. Centred cards left the customer's request and
        // the cook's answer looking identical, so a thread of them read as one
        // anonymous log rather than a conversation.
        boolean mine = msg.isSentByMe();
        vh.card.setCardBackgroundColor(mine ? SENT_CARD_TINT : Color.WHITE);
        int wide = dp(48), narrow = dp(12);
        vh.root.setPaddingRelative(mine ? wide : narrow, vh.root.getPaddingTop(),
                mine ? narrow : wide, vh.root.getPaddingBottom());

        vh.tvLabel.setText(mine ? "YOU · " + label : label);
        vh.tvBody.setText(msg.getText() != null ? msg.getText() : "");
        vh.tvTimestamp.setText(msg.getTimestamp());

        boolean actionable = msg.isCardActionable();
        vh.layoutActions.setVisibility(actionable ? View.VISIBLE : View.GONE);

        String status = msg.getCardStatusLine();
        vh.tvStatus.setText(status != null ? status : "");
        vh.tvStatus.setVisibility(!actionable && status != null ? View.VISIBLE : View.GONE);

        if (actionable) {
            vh.btnAccept.setOnClickListener(v -> {
                if (cardDecisionListener != null) cardDecisionListener.onCardDecision(msg, true);
            });
            vh.btnDecline.setOnClickListener(v -> {
                if (cardDecisionListener != null) cardDecisionListener.onCardDecision(msg, false);
            });
        } else {
            // Recycled holders keep their listeners, and a stale one here would fire
            // a decision on the wrong subscription.
            vh.btnAccept.setOnClickListener(null);
            vh.btnDecline.setOnClickListener(null);
        }

        vh.itemView.setOnLongClickListener(null);
        vh.itemView.setLongClickable(false);
        vh.itemView.setOnClickListener(null);
        vh.itemView.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * Wires long-press-to-select / tap-to-toggle for selectable rows, and highlights
     * the row while selected. {@code normalClickAction} runs on a plain tap when not
     * in selection mode (e.g. opening media); pass null when there's nothing to do.
     */
    private void bindMessageActions(View itemView, ChatMessage msg, Runnable normalClickAction) {
        boolean selectable = msg.canEdit() || msg.canDelete();
        boolean isSelected = selectable && selectedServerIds.contains(msg.getServerId());
        itemView.setBackgroundColor(isSelected ? SELECTED_HIGHLIGHT : Color.TRANSPARENT);

        if (!selectable) {
            itemView.setOnLongClickListener(null);
            itemView.setLongClickable(false);
            itemView.setOnClickListener(normalClickAction != null ? v -> normalClickAction.run() : null);
            return;
        }

        itemView.setOnLongClickListener(v -> {
            toggleSelection(msg);
            return true;
        });

        itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(msg);
            } else if (normalClickAction != null) {
                normalClickAction.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // ---- ViewHolders ----

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvBody, tvStatus, tvTimestamp;
        View layoutActions, root;
        com.google.android.material.card.MaterialCardView card;
        com.google.android.material.button.MaterialButton btnAccept, btnDecline;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.layoutCardRoot);
            card = itemView.findViewById(R.id.cardMessage);
            tvLabel = itemView.findViewById(R.id.tvCardLabel);
            tvBody = itemView.findViewById(R.id.tvCardBody);
            tvStatus = itemView.findViewById(R.id.tvCardStatus);
            tvTimestamp = itemView.findViewById(R.id.tvCardTimestamp);
            layoutActions = itemView.findViewById(R.id.layoutCardActions);
            btnAccept = itemView.findViewById(R.id.btnCardAccept);
            btnDecline = itemView.findViewById(R.id.btnCardDecline);
        }
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTimestamp;
        ImageView ivAvatar;
        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText      = itemView.findViewById(R.id.tvMessageText);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivAvatar    = itemView.findViewById(R.id.ivAvatar);
        }
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia, ivAvatar;
        TextView tvMediaLabel, tvTimestamp;
        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvMediaLabel = itemView.findViewById(R.id.tvMediaLabel);
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

    static class DeletedViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeletedLabel, tvTimestamp;
        DeletedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeletedLabel = itemView.findViewById(R.id.tvDeletedLabel);
            tvTimestamp    = itemView.findViewById(R.id.tvTimestamp);
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
