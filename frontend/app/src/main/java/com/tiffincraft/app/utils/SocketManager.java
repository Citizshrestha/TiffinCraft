package com.tiffincraft.app.utils;

import android.content.Context;
import android.util.Log;

import com.tiffincraft.app.api.RetrofitClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {

    private static final String TAG = "SocketManager";

    private static SocketManager instance;
    private Socket socket;
    private Context context;
    private boolean isConnected = false;

    private SocketManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    public void connect() {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Socket already connected");
            return;
        }

        String token = getAuthToken();
        if (token == null) {
            Log.e(TAG, "Cannot connect: No auth token available");
            return;
        }

        // Get dynamic server URL from RetrofitClient
        String serverUrl = RetrofitClient.getServerUrl(context);
        if (serverUrl == null || serverUrl.isEmpty()) {
            Log.e(TAG, "Cannot connect: Server URL not available");
            return;
        }

        Log.d(TAG, "Connecting socket to: " + serverUrl);

        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionDelay = 1000;
            options.reconnectionAttempts = 5;

            Map<String, String> auth = new HashMap<>();
            auth.put("token", token.replace("Bearer ", ""));
            options.auth = auth;

            socket = IO.socket(serverUrl, options);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    isConnected = true;
                    Log.d(TAG, "Socket connected successfully");
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    isConnected = false;
                    Log.e(TAG, "Socket connection error: " + args[0]);
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    isConnected = false;
                    Log.d(TAG, "Socket disconnected");
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        }
    }

    public void joinOrderRoom(int orderId) {
        if (socket != null && socket.connected()) {
            socket.emit("joinOrderRoom", orderId);
            Log.d(TAG, "Joined order room: " + orderId);
        } else {
            Log.e(TAG, "Cannot join order room: Socket not connected");
        }
    }

    public void joinCookRoom(int cookId) {
        if (socket != null && socket.connected()) {
            socket.emit("joinCookRoom", cookId);
            Log.d(TAG, "Joined cook room: " + cookId);
        } else {
            Log.e(TAG, "Cannot join cook room: Socket not connected");
        }
    }

    public void onNewOrder(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("newOrder", listener);
        }
    }

    public void onOrderStatusUpdated(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("orderStatusUpdated", listener);
        }
    }

    public void onOrderCancelled(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("orderCancelled", listener);
        }
    }

    // ==================== Chat ====================

    public void joinChatRoom(int conversationId) {
        if (socket != null && socket.connected()) {
            socket.emit("joinChatRoom", conversationId);
            Log.d(TAG, "Joined chat room: " + conversationId);
        } else {
            Log.e(TAG, "Cannot join chat room: Socket not connected");
        }
    }

    public void leaveChatRoom(int conversationId) {
        if (socket != null && socket.connected()) {
            socket.emit("leaveChatRoom", conversationId);
            Log.d(TAG, "Left chat room: " + conversationId);
        }
    }

    public void emitChatTyping(int conversationId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("conversation_id", conversationId);
                socket.emit("chatTyping", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting typing event", e);
            }
        }
    }

    /** Fired when a new message arrives in a joined chat room. */
    public void onNewChatMessage(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("newChatMessage", listener);
        }
    }

    /** Fired to the recipient's personal room for any new chat message (badges/notifications). */
    public void onChatNotification(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("chatNotification", listener);
        }
    }

    /** Fired when the other participant reads the conversation. */
    public void onChatMessagesRead(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("chatMessagesRead", listener);
        }
    }

    /** Fired when the other participant is typing. */
    public void onChatTyping(Emitter.Listener listener) {
        if (socket != null) {
            socket.on("chatTyping", listener);
        }
    }

    /** Remove chat listeners registered by a screen (call in onDestroy). */
    public void offChatListeners() {
        if (socket != null) {
            socket.off("newChatMessage");
            socket.off("chatMessagesRead");
            socket.off("chatTyping");
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
            isConnected = false;
            Log.d(TAG, "Socket disconnected and cleaned up");
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.connected();
    }

    private String getAuthToken() {
        return RetrofitClient.getAuthToken(context);
    }
}
