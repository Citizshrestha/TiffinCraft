package com.tiffincraft.app.models;

public class ChatMessage {

    // View types
    public static final int TYPE_TEXT_SENT     = 0;
    public static final int TYPE_TEXT_RECEIVED = 1;
    public static final int TYPE_CALL          = 2;
    public static final int TYPE_DATE_SEPARATOR = 3;

    // Call states
    public static final String CALL_ENDED    = "Call Ended";
    public static final String CALL_DECLINED = "Declined";

    private int    viewType;
    private String text;          // message body or date label
    private String timestamp;     // e.g. "12:36 PM"
    private String callDuration;  // e.g. "0:15" — null for declined / non-call
    private String callState;     // CALL_ENDED or CALL_DECLINED
    private boolean isSentByMe;   // for call bubbles: which side to show

    // ---- Constructors ----

    /** Text message */
    public ChatMessage(int viewType, String text, String timestamp) {
        this.viewType  = viewType;
        this.text      = text;
        this.timestamp = timestamp;
    }

    /** Call bubble */
    public ChatMessage(String callState, String callDuration, String timestamp, boolean isSentByMe) {
        this.viewType      = TYPE_CALL;
        this.callState     = callState;
        this.callDuration  = callDuration;
        this.timestamp     = timestamp;
        this.isSentByMe    = isSentByMe;
    }

    /** Date separator */
    public static ChatMessage dateSeparator(String label) {
        ChatMessage m = new ChatMessage(TYPE_DATE_SEPARATOR, label, null);
        return m;
    }

    // ---- Getters ----

    public int getViewType()       { return viewType; }
    public String getText()        { return text; }
    public String getTimestamp()   { return timestamp; }
    public String getCallDuration(){ return callDuration; }
    public String getCallState()   { return callState; }
    public boolean isSentByMe()    { return isSentByMe; }
}
