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

    /**
     * One listener per event, owned by whichever screen registered last.
     * Stored here (not just on the socket) so they survive reconnects:
     * connect() re-attaches everything after creating a fresh socket, and
     * screens may register before the socket exists without losing the
     * registration.
     */
    private final Map<String, Emitter.Listener> screenListeners = new HashMap<>();

    /**
     * Tracks the last-joined rooms so they can be re-joined after a reconnect.
     * Stored as an ordered list of emit payloads (room type → value).
     */
    private final java.util.List<Runnable> pendingRoomJoins = new java.util.ArrayList<>();

    /**
     * FCM token cache — set by the calling Activity so we can re-send it
     * after reconnect via PUT /api/auth/fcm-token.
     */
    private String cachedFcmToken = null;
    private boolean fcmTokenSent = false;
    private okhttp3.OkHttpClient httpClient = null;

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

                    // 🔁 Re-join every room that was joined before the reconnect.
                    // This is critical: when the socket reconnects, the server
                    // forgets all previous room memberships (they were per-socket).
                    for (Runnable join : pendingRoomJoins) {
                        try {
                            join.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to re-join room after reconnect", e);
                        }
                    }

                    // 📲 Re-send FCM token if we have a cached one from before
                    if (cachedFcmToken != null && !fcmTokenSent) {
                        sendFcmTokenToServer(cachedFcmToken);
                    }
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
                    // Reset so the token is re-sent on next reconnect
                    fcmTokenSent = false;
                    Log.d(TAG, "Socket disconnected");
                }
            });

            // Global listener: raise a system notification (sound + heads-up) for
            // any incoming chat message, then hand the event to the current
            // screen's badge listener (if any). Lives on the socket itself so it
            // keeps working as the user moves between Activities. ChatNotifier
            // suppresses the conversation currently on screen.
            socket.on("chatNotification", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject data = (JSONObject) args[0];
                            int conversationId = data.optInt("conversation_id", 0);
                            String senderName = data.optString("sender_name", "New message");
                            String preview = data.optString("preview", "You have a new message");
                            ChatNotifier.showMessageNotification(context, conversationId, senderName, preview);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "chatNotification handling error", e);
                    }

                    Emitter.Listener badgeListener = screenListeners.get("chatNotification");
                    if (badgeListener != null) {
                        badgeListener.call(args);
                    }
                }
            });

            // Re-attach every screen listener that was registered before the
            // socket existed (or on a previous, now-discarded socket instance).
            for (Map.Entry<String, Emitter.Listener> entry : screenListeners.entrySet()) {
                if (!"chatNotification".equals(entry.getKey())) {
                    socket.on(entry.getKey(), entry.getValue());
                }
            }

            socket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        }
    }

    /**
     * Registers the single active listener for an event, replacing any previous
     * one so listeners never stack up as screens re-register on resume.
     */
    private void attachListener(String event, Emitter.Listener listener) {
        screenListeners.put(event, listener);
        // chatNotification is dispatched through the internal global handler
        // above; attaching it directly would fire the badge listener twice.
        if (socket != null && !"chatNotification".equals(event)) {
            socket.off(event);
            socket.on(event, listener);
        }
    }

    public void joinOrderRoom(int orderId) {
        final int id = orderId;
        pendingRoomJoins.add(() -> {
            if (socket != null && socket.connected()) {
                socket.emit("joinOrderRoom", id);
            }
        });
        if (socket != null && socket.connected()) {
            socket.emit("joinOrderRoom", orderId);
            Log.d(TAG, "Joined order room: " + orderId);
        } else {
            Log.e(TAG, "Cannot join order room: Socket not connected — queued for re-join");
        }
    }

    public void leaveOrderRoom(int orderId) {
        if (socket != null && socket.connected()) {
            socket.emit("leaveOrderRoom", orderId);
            Log.d(TAG, "Left order room: " + orderId);
        }
    }

    public void joinCookRoom(int cookId) {
        final int id = cookId;
        pendingRoomJoins.add(() -> {
            if (socket != null && socket.connected()) {
                socket.emit("joinCookRoom", id);
            }
        });
        if (socket != null && socket.connected()) {
            socket.emit("joinCookRoom", cookId);
            Log.d(TAG, "Joined cook room: " + cookId);
        } else {
            Log.e(TAG, "Cannot join cook room: Socket not connected — queued for re-join");
        }
    }

    public void onNewOrder(Emitter.Listener listener) {
        attachListener("newOrder", listener);
    }

    public void onOrderStatusUpdated(Emitter.Listener listener) {
        attachListener("orderStatusUpdated", listener);
    }

    public void onOrderCancelled(Emitter.Listener listener) {
        attachListener("orderCancelled", listener);
    }

    /** Fired when admin approves/rejects the cook's kitchen. */
    public void onCookApprovalUpdate(Emitter.Listener listener) {
        attachListener("cookApprovalUpdate", listener);
    }

    /** Broadcast to every connected client when a cook toggles holiday mode — used by the customer nearby-cooks map to refresh live without re-polling GPS. */
    public void onCookAvailabilityChanged(Emitter.Listener listener) {
        attachListener("cookAvailabilityChanged", listener);
    }

    // ==================== Chat ====================

    public void joinChatRoom(int conversationId) {
        final int id = conversationId;
        pendingRoomJoins.add(() -> {
            if (socket != null && socket.connected()) {
                socket.emit("joinChatRoom", id);
            }
        });
        if (socket != null && socket.connected()) {
            socket.emit("joinChatRoom", conversationId);
            Log.d(TAG, "Joined chat room: " + conversationId);
        } else {
            Log.e(TAG, "Cannot join chat room: Socket not connected — queued for re-join");
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
        attachListener("newChatMessage", listener);
    }

    /** Fired to the recipient's personal room for any new chat message (badges/notifications). */
    public void onChatNotification(Emitter.Listener listener) {
        attachListener("chatNotification", listener);
    }

    /** Fired when the other participant reads the conversation. */
    public void onChatMessagesRead(Emitter.Listener listener) {
        attachListener("chatMessagesRead", listener);
    }

    /** Fired when the other participant is typing. */
    public void onChatTyping(Emitter.Listener listener) {
        attachListener("chatTyping", listener);
    }

    /** Fired when a message is edited in a joined chat room. */
    public void onChatMessageEdited(Emitter.Listener listener) {
        attachListener("chatMessageEdited", listener);
    }

    /** Fired when a message is deleted in a joined chat room. */
    public void onChatMessageDeleted(Emitter.Listener listener) {
        attachListener("chatMessageDeleted", listener);
    }

    /** Fired globally whenever any user's online/offline status changes. */
    public void onPresenceChanged(Emitter.Listener listener) {
        attachListener("presenceChanged", listener);
    }

    /** Remove chat listeners registered by a screen (call in onDestroy). */
    /** Unregisters a single screen-owned listener (e.g. in onDestroy) so a finished Activity is never retained via a stale callback. */
    public void off(String event) {
        screenListeners.remove(event);
        if (socket != null) {
            socket.off(event);
        }
    }

    public void offChatListeners() {
        String[] events = {"newChatMessage", "chatMessagesRead", "chatTyping",
                "chatMessageEdited", "chatMessageDeleted", "presenceChanged"};
        for (String event : events) {
            screenListeners.remove(event);
            if (socket != null) {
                socket.off(event);
            }
        }
    }

    /**
     * Tears the socket down completely. Only call this on logout — the socket
     * is a singleton shared by every screen, so disconnecting on a screen's
     * onPause silently kills real-time updates for the whole app.
     */
    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
            isConnected = false;
            Log.d(TAG, "Socket disconnected and cleaned up");
        }
        screenListeners.clear();
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.connected();
    }

    private String getAuthToken() {
        return RetrofitClient.getAuthToken(context);
    }

    // ==================== FCM Token ====================

    /**
     * Cache the FCM device token and immediately upload it to the backend via
     * HTTP, regardless of whether the socket is connected.
     *
     * Previous behaviour gated the upload on isConnected() — meaning that if
     * the socket was still dialling when a Home activity called
     * fetchAndSendFcmToken() at startup, the token upload was silently skipped
     * and the cook's fcm_token stayed NULL in the database, so every
     * subsequent push notification call found no token to send to.
     *
     * The token is also re-sent whenever the socket reconnects (see
     * EVENT_CONNECT listener), which covers the edge-case where the HTTP call
     * below races against a very-first login before the auth token is stored.
     */
    public void setFcmToken(String fcmToken) {
        this.cachedFcmToken = fcmToken;
        this.fcmTokenSent = false;
        // Always attempt the HTTP upload immediately — do NOT gate on isConnected().
        sendFcmTokenToServer(fcmToken);
    }

    private void sendFcmTokenToServer(String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) return;

        // Build HTTP client lazily
        if (httpClient == null) {
            httpClient = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        }

        String serverUrl = RetrofitClient.getServerUrl(context);
        if (serverUrl == null) return;
        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        String apiUrl = baseUrl + "api/auth/fcm-token";
        String token = getAuthToken();

        okhttp3.MediaType jsonMedia = okhttp3.MediaType.parse("application/json; charset=utf-8");
        String json = "{\"fcm_token\":\"" + fcmToken.replace("\"", "\\\"") + "\"}";
        okhttp3.RequestBody body = okhttp3.RequestBody.create(json, jsonMedia);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(apiUrl)
                .put(body)
                .header("Authorization", token != null ? token : "")
                .header("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Failed to send FCM token: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                if (response.isSuccessful()) {
                    fcmTokenSent = true;
                    Log.d(TAG, "✅ FCM token sent to server successfully");
                } else {
                    Log.e(TAG, "❌ FCM token upload failed: " + response.code());
                }
                response.close();
            }
        });
    }
}
