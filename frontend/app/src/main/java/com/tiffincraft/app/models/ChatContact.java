package com.tiffincraft.app.models;

/**
 * UI model for a row in the chat list panel.
 * Backed by a conversation (conversationId > 0) or a plain contact.
 */
public class ChatContact {

    private int     conversationId; // 0 = no conversation yet
    private int     otherUserId;
    private String  name;
    private String  lastMessage;
    private String  timestamp;
    private boolean isOnline;
    private String  avatarUrl;      // Cloudinary URL, null = initials placeholder
    private int     unreadCount;

    public ChatContact(int conversationId, int otherUserId, String name, String lastMessage,
                       String timestamp, boolean isOnline, String avatarUrl, int unreadCount) {
        this.conversationId = conversationId;
        this.otherUserId    = otherUserId;
        this.name           = name;
        this.lastMessage    = lastMessage;
        this.timestamp      = timestamp;
        this.isOnline       = isOnline;
        this.avatarUrl      = avatarUrl;
        this.unreadCount    = unreadCount;
    }

    /** Build from a conversation returned by GET /chat/conversations */
    public static ChatContact fromConversation(ChatConversation c) {
        String preview;
        if (c.getLastMessageType() == null) {
            preview = "Say hello!";
        } else if (ChatApiMessage.TYPE_TEXT.equals(c.getLastMessageType())) {
            preview = c.getLastMessageContent();
        } else {
            preview = "Call";
        }

        String time = c.getLastMessageAt() != null
                ? ChatMessage.formatDateLabel(c.getLastMessageAt())
                : "";

        return new ChatContact(
                c.getId(),
                c.getOtherUserId(),
                c.getOtherUserName(),
                preview,
                time,
                false,
                c.getOtherUserImage(),
                c.getUnreadCount()
        );
    }

    /** Build from a contact returned by GET /chat/contacts (no conversation yet) */
    public static ChatContact fromApiContact(ChatContactsResponse.ApiContact c) {
        return new ChatContact(
                0,
                c.getId(),
                c.getFullName(),
                "Tap to start chatting",
                "",
                false,
                c.getProfileImage(),
                0
        );
    }

    public int getConversationId() { return conversationId; }
    public int getOtherUserId()    { return otherUserId; }
    public String getName()        { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTimestamp()   { return timestamp; }
    public boolean isOnline()      { return isOnline; }
    public String getAvatarUrl()   { return avatarUrl; }
    public int getUnreadCount()    { return unreadCount; }

    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setOnline(boolean online) { this.isOnline = online; }
}
