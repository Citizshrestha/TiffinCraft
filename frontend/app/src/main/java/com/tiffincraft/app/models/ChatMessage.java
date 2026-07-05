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

    // Call states
    public static final String CALL_ENDED    = "Call Ended";
    public static final String CALL_DECLINED = "Declined";
    public static final String CALL_MISSED   = "Missed";

    private int     serverId;      // chat_messages.id, 0 for local-only rows
    private int     viewType;
    private String  text;          // message body or date label
    private String  timestamp;     // e.g. "12:36 PM"
    private String  callDuration;  // e.g. "0:15" — null for declined / non-call
    private String  callState;     // CALL_ENDED / CALL_DECLINED / CALL_MISSED
    private boolean isSentByMe;    // for call bubbles: which side to show
    private String  createdAtRaw;  // ISO timestamp from server for date grouping

    // ---- Constructors ----

    /** Text message */
    public ChatMessage(int viewType, String text, String timestamp) {
        this.viewType  = viewType;
        this.text      = text;
        this.timestamp = timestamp;
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

        switch (type) {
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
        return m;
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
    public String getCreatedAtRaw() { return createdAtRaw; }
}
