package com.tiffincraft.app.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * UI model for a single row in the chat RecyclerView.
 * Built either locally (optimistic send) or from a server ChatApiMessage.
 */
public class ChatMessage {

    // View types
    public static final int TYPE_TEXT_SENT      = 0;
    public static final int TYPE_TEXT_RECEIVED  = 1;
    public static final int TYPE_CALL           = 2;
    public static final int TYPE_DATE_SEPARATOR = 3;
    public static final int TYPE_IMAGE_SENT     = 4;
    public static final int TYPE_IMAGE_RECEIVED = 5;
    public static final int TYPE_VIDEO_SENT     = 6;
    public static final int TYPE_VIDEO_RECEIVED = 7;
    public static final int TYPE_DELETED_SENT     = 8;
    public static final int TYPE_DELETED_RECEIVED = 9;
    /**
     * One view type for all three structured cards, both sides. The card body is
     * the server's sentence and the only thing that varies is whether there are
     * buttons, so three layouts (or six, sent and received) would be three copies
     * of the same bubble.
     */
    public static final int TYPE_CARD             = 10;

    // Call states
    public static final String CALL_ENDED    = "Call Ended";
    public static final String CALL_DECLINED = "Declined";
    public static final String CALL_MISSED   = "Missed";

    private int     serverId;      // chat_messages.id, 0 for local-only rows
    private int     viewType;
    private String  text;          // message body or date label
    private String  timestamp;     // e.g. "12:36 PM"
    private String  callDuration;  // e.g. "0:15" ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â null for declined / non-call
    private String  callState;     // CALL_ENDED / CALL_DECLINED / CALL_MISSED
    private boolean isSentByMe;    // for call/media bubbles: which side to show
    private String  mediaUrl;
    private String  createdAtRaw;  // ISO timestamp from server for date grouping
    private boolean isEdited;
    private boolean isDeleted;
    private String  deletedOriginalType; // "text" / "image" / "video" — drives the placeholder label
    private String  cardType;      // subscription_request / subscription_update / custom_meal_request
    private Integer cardReferenceId;
    private String  cardLiveStatus;

    // ---- Constructors ----

    /** Text message */
    public ChatMessage(int viewType, String text, String timestamp) {
        this.viewType  = viewType;
        this.text      = text;
        this.timestamp = timestamp;
        this.isSentByMe = viewType == TYPE_TEXT_SENT
                || viewType == TYPE_IMAGE_SENT
                || viewType == TYPE_VIDEO_SENT;
    }

    /** Media message */
    public static ChatMessage media(int viewType, String url, String timestamp, boolean isSentByMe) {
        ChatMessage message = new ChatMessage(viewType, url, timestamp);
        message.mediaUrl = url;
        message.isSentByMe = isSentByMe;
        return message;
    }

    /** Call bubble */
    public ChatMessage(String callState, String callDuration, String timestamp, boolean isSentByMe) {
        this.viewType     = TYPE_CALL;
        this.callState    = callState;
        this.callDuration = callDuration;
        this.timestamp    = timestamp;
        this.isSentByMe   = isSentByMe;
    }

    /** Date separator */
    public static ChatMessage dateSeparator(String label) {
        return new ChatMessage(TYPE_DATE_SEPARATOR, label, null);
    }

    // ---- Server mapping ----

    /** Build a UI ChatMessage from a server ChatApiMessage. */
    public static ChatMessage fromApi(ChatApiMessage api, int myUserId) {
        boolean sentByMe = api.getSenderId() == myUserId;
        String time = formatTime(api.getCreatedAt());

        ChatMessage m;
        String type = api.getMessageType() != null ? api.getMessageType() : ChatApiMessage.TYPE_TEXT;

        if (api.isDeleted() && (ChatApiMessage.TYPE_TEXT.equals(type)
                || ChatApiMessage.TYPE_IMAGE.equals(type)
                || ChatApiMessage.TYPE_VIDEO.equals(type))) {
            m = new ChatMessage(sentByMe ? TYPE_DELETED_SENT : TYPE_DELETED_RECEIVED, null, time);
            m.isSentByMe = sentByMe;
            m.isDeleted = true;
            m.deletedOriginalType = type;
            m.serverId     = api.getId();
            m.createdAtRaw = api.getCreatedAt();
            return m;
        }

        switch (type) {
            case ChatApiMessage.TYPE_SUBSCRIPTION_REQUEST:
            case ChatApiMessage.TYPE_SUBSCRIPTION_UPDATE:
            case ChatApiMessage.TYPE_CUSTOM_MEAL_REQUEST:
                m = new ChatMessage(TYPE_CARD, api.getContent(), time);
                m.isSentByMe = sentByMe;
                m.cardType = type;
                m.cardReferenceId = api.getReferenceId();
                m.cardLiveStatus = api.getLiveStatus();
                break;
            case ChatApiMessage.TYPE_IMAGE:
                m = ChatMessage.media(
                        sentByMe ? TYPE_IMAGE_SENT : TYPE_IMAGE_RECEIVED,
                        api.getContent(), time, sentByMe);
                break;
            case ChatApiMessage.TYPE_VIDEO:
                m = ChatMessage.media(
                        sentByMe ? TYPE_VIDEO_SENT : TYPE_VIDEO_RECEIVED,
                        api.getContent(), time, sentByMe);
                break;
            case ChatApiMessage.TYPE_CALL_ENDED:
                m = new ChatMessage(CALL_ENDED, formatDuration(api.getCallDurationSeconds()), time, sentByMe);
                break;
            case ChatApiMessage.TYPE_CALL_DECLINED:
                m = new ChatMessage(CALL_DECLINED, null, time, sentByMe);
                break;
            case ChatApiMessage.TYPE_CALL_MISSED:
                m = new ChatMessage(CALL_MISSED, null, time, sentByMe);
                break;
            default:
                m = new ChatMessage(
                        sentByMe ? TYPE_TEXT_SENT : TYPE_TEXT_RECEIVED,
                        api.getContent(), time);
                m.isSentByMe = sentByMe;
                break;
        }

        m.serverId     = api.getId();
        m.createdAtRaw = api.getCreatedAt();
        m.isEdited     = api.isEdited();
        return m;
    }

    public boolean canEdit() {
        return serverId > 0
                && isSentByMe
                && (viewType == TYPE_TEXT_SENT);
    }

    public boolean canDelete() {
        if (serverId <= 0 || !isSentByMe) return false;
        return viewType == TYPE_TEXT_SENT
                || viewType == TYPE_IMAGE_SENT
                || viewType == TYPE_VIDEO_SENT;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setEdited(boolean edited) {
        this.isEdited = edited;
    }

    /** Turn this row into a "message deleted" placeholder in place, keeping its serverId/side. */
    public void markDeleted() {
        String originalType;
        if (viewType == TYPE_IMAGE_SENT || viewType == TYPE_IMAGE_RECEIVED) {
            originalType = ChatApiMessage.TYPE_IMAGE;
        } else if (viewType == TYPE_VIDEO_SENT || viewType == TYPE_VIDEO_RECEIVED) {
            originalType = ChatApiMessage.TYPE_VIDEO;
        } else {
            originalType = ChatApiMessage.TYPE_TEXT;
        }
        this.deletedOriginalType = originalType;
        this.viewType  = isSentByMe ? TYPE_DELETED_SENT : TYPE_DELETED_RECEIVED;
        this.isDeleted = true;
        this.isEdited  = false;
        this.text      = null;
        this.mediaUrl  = null;
    }

    /** Placeholder copy shown in a deleted bubble, e.g. "This photo was deleted". */
    public String getDeletedLabel() {
        if (ChatApiMessage.TYPE_IMAGE.equals(deletedOriginalType)) return "This photo was deleted";
        if (ChatApiMessage.TYPE_VIDEO.equals(deletedOriginalType)) return "This video was deleted";
        return "This message was deleted";
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    /** "MM:SS" style duration from seconds; null-safe. */
    public static String formatDuration(Integer seconds) {
        if (seconds == null || seconds < 0) return null;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    /** Parse server ISO timestamp to local "h:mm a". */
    public static String formatTime(String isoTimestamp) {
        Date date = parseServerDate(isoTimestamp);
        if (date == null) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        }
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
    }

    /** Parse server ISO timestamp to "EEE, MMM d" for date separators. */
    public static String formatDateLabel(String isoTimestamp) {
        Date date = parseServerDate(isoTimestamp);
        if (date == null) return "";
        return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date);
    }

    public static Date parseServerDate(String isoTimestamp) {
        if (isoTimestamp == null) return null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                if (p.endsWith("'Z'")) sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(isoTimestamp);
            } catch (ParseException ignored) { }
        }
        return null;
    }

    // ---- Getters ----

    public int getServerId()        { return serverId; }
    public int getViewType()        { return viewType; }
    public String getText()         { return text; }
    public String getTimestamp()    { return timestamp; }
    public String getCallDuration() { return callDuration; }
    public String getCallState()    { return callState; }
    public boolean isSentByMe()     { return isSentByMe; }
    public String getMediaUrl()     { return mediaUrl; }
    public String getCreatedAtRaw() { return createdAtRaw; }
    public boolean isEdited()       { return isEdited; }
    public boolean isDeleted()      { return isDeleted; }

    public String getCardType()          { return cardType; }
    public Integer getCardReferenceId()  { return cardReferenceId; }
    public String getCardLiveStatus()    { return cardLiveStatus; }

    /**
     * Whether THIS card still has a decision on it, for the person looking at it.
     *
     * Two conditions, both required. `!isSentByMe` because only the recipient
     * decides — a customer must never see Accept on their own request. And the
     * status is the server's live one, so a card scrolled back to after the fact
     * offers nothing: the endpoints guard on the same state and would 409 anyway,
     * which is a worse way to find out.
     */
    public boolean isCardActionable() {
        if (viewType != TYPE_CARD || isSentByMe || cardReferenceId == null) return false;
        if (ChatApiMessage.TYPE_SUBSCRIPTION_REQUEST.equals(cardType)) {
            return "requested".equals(cardLiveStatus);
        }
        if (ChatApiMessage.TYPE_CUSTOM_MEAL_REQUEST.equals(cardType)) {
            return "pending".equals(cardLiveStatus);
        }
        return false; // subscription_update is an announcement, never a decision
    }

    /** Short "what happened to this" line, or null while it's still open. */
    public String getCardStatusLine() {
        if (viewType != TYPE_CARD) return null;
        if (cardReferenceId == null) return "No longer available";
        if (isCardActionable()) return null;
        if (cardLiveStatus == null) return "No longer available";
        switch (cardLiveStatus) {
            case "requested": return "Waiting for the cook";
            case "pending":   return "Waiting for the cook";
            case "accepted":  return "Accepted";
            case "declined":  return "Declined";
            case "rejected":  return "Declined";
            case "cancelled": return "Cancelled";
            case "expired":   return "Expired — the day's cutoff passed";
            default:          return cardLiveStatus.replace('_', ' ');
        }
    }

    /** Cards are announcements or decisions, never editable or deletable. */
    public boolean isCard() { return viewType == TYPE_CARD; }
}
