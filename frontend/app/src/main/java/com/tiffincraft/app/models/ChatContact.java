package com.tiffincraft.app.models;

public class ChatContact {
    private String name;
    private String lastMessage;
    private String timestamp;
    private boolean isOnline;
    private int avatarResId; // optional drawable res id; 0 = use initials

    public ChatContact(String name, String lastMessage, String timestamp, boolean isOnline, int avatarResId) {
        this.name        = name;
        this.lastMessage = lastMessage;
        this.timestamp   = timestamp;
        this.isOnline    = isOnline;
        this.avatarResId = avatarResId;
    }

    public String getName()        { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTimestamp()   { return timestamp; }
    public boolean isOnline()      { return isOnline; }
    public int getAvatarResId()    { return avatarResId; }
}
