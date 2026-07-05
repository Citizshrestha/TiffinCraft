package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.MessageAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ChatApiMessage;
import com.tiffincraft.app.models.ChatMessage;
import com.tiffincraft.app.models.ChatMessagesResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SendChatMessageRequest;
import com.tiffincraft.app.models.SendChatMessageResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONTACT_NAME    = "contact_name";
    public static final String EXTRA_CONTACT_PHONE   = "contact_phone";
    public static final String EXTRA_IS_ONLINE       = "is_online";

    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private FrameLayout   btnSend;
    private ImageView     btnBack, btnEmoji, btnCall;
    private TextView      tvChatName, tvChatStatus;

    private MessageAdapter    adapter;
    private List<ChatMessage> messages;

    private ApiService     apiService;
    private SocketManager  socketManager;
    private SessionManager sessionManager;
    private final Gson gson = new Gson();

    private int    conversationId;
    private int    myUserId;
    private String contactName;
    private String contactPhone;
    private boolean isOnline;

    private String lastDateLabel = null;
    private long   lastTypingEmit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getIntExtra(EXTRA_CONVERSATION_ID, 0);
        contactName    = getIntent().getStringExtra(EXTRA_CONTACT_NAME);
        contactPhone   = getIntent().getStringExtra(EXTRA_CONTACT_PHONE);
        isOnline       = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);

        if (conversationId <= 0) {
            Toast.makeText(this, "Conversation not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        try {
            myUserId = Integer.parseInt(sessionManager.getUserId());
        } catch (NumberFormatException e) {
            myUserId = 0;
        }

        apiService    = RetrofitClient.getInstance(this).getApiService();
        socketManager = SocketManager.getInstance(this);

        initViews();
        setupHeader(contactName, isOnline);
        setupRecyclerView();
        setupListeners();
        setupSocket();
        loadMessages();
        markConversationRead();
    }

    private void initViews() {
        rvMessages   = findViewById(R.id.rvMessages);
        etMessage    = findViewById(R.id.etMessage);
        btnSend      = findViewById(R.id.btnSend);
        btnBack      = findViewById(R.id.btnBack);
        btnEmoji     = findViewById(R.id.btnEmoji);
        btnCall      = findViewById(R.id.btnCall);
        tvChatName   = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);
    }

    private void setupHeader(String name, boolean online) {
        tvChatName.setText(name != null ? name : "Chat");
        tvChatStatus.setText(online ? "Online" : "Offline");
        int statusColor = online
                ? getColor(R.color.green_primary)
                : getColor(R.color.text_hint);
        tvChatStatus.setTextColor(statusColor);
        findViewById(R.id.viewStatusDot).setVisibility(online ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendTextMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendTextMessage();
            return true;
        });

        // Throttled typing indicator (max 1 emit per 2s)
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                long now = System.currentTimeMillis();
                if (s.length() > 0 && now - lastTypingEmit > 2000) {
                    lastTypingEmit = now;
                    socketManager.emitChatTyping(conversationId);
                }
            }
        });

        btnCall.setOnClickListener(v -> initiateCall());

        btnEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji picker coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    // ==================== Socket ====================

    private void setupSocket() {
        socketManager.connect();
        socketManager.joinChatRoom(conversationId);

        socketManager.onNewChatMessage(args -> {
            if (args.length == 0) return;
            try {
                JSONObject json = (JSONObject) args[0];
                ChatApiMessage apiMsg = gson.fromJson(json.toString(), ChatApiMessage.class);

                // Skip own messages (already rendered optimistically)
                if (apiMsg.getSenderId() == myUserId) return;
                if (apiMsg.getConversationId() != conversationId) return;

                runOnUiThread(() -> {
                    appendMessage(ChatMessage.fromApi(apiMsg, myUserId), apiMsg.getCreatedAt());
                    markConversationRead();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error handling newChatMessage", e);
            }
        });

        socketManager.onChatTyping(args -> runOnUiThread(() -> {
            tvChatStatus.setText("typing...");
            tvChatStatus.postDelayed(() ->
                    tvChatStatus.setText(isOnline ? "Online" : "Offline"), 3000);
        }));
    }

    // ==================== Data loading ====================

    private void loadMessages() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatMessages(token, conversationId, null, 50)
                .enqueue(new Callback<ChatMessagesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ChatMessagesResponse> call,
                                           @NonNull Response<ChatMessagesResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            renderMessages(response.body().getMessages());
                        } else {
                            Toast.makeText(ChatActivity.this,
                                    "Failed to load messages.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ChatMessagesResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "loadMessages failed", t);
                        Toast.makeText(ChatActivity.this,
                                "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderMessages(List<ChatApiMessage> apiMessages) {
        messages.clear();
        lastDateLabel = null;

        if (apiMessages != null) {
            for (ChatApiMessage apiMsg : apiMessages) {
                String dateLabel = ChatMessage.formatDateLabel(apiMsg.getCreatedAt());
                if (!TextUtils.isEmpty(dateLabel) && !dateLabel.equals(lastDateLabel)) {
                    messages.add(ChatMessage.dateSeparator(dateLabel));
                    lastDateLabel = dateLabel;
                }
                messages.add(ChatMessage.fromApi(apiMsg, myUserId));
            }
        }

        adapter.notifyDataSetChanged();
        if (!messages.isEmpty()) {
            rvMessages.scrollToPosition(messages.size() - 1);
        }
    }

    /** Append a message, inserting a date separator when the day changes. */
    private void appendMessage(ChatMessage message, String createdAtIso) {
        String dateLabel = ChatMessage.formatDateLabel(createdAtIso);
        if (!TextUtils.isEmpty(dateLabel) && !dateLabel.equals(lastDateLabel)) {
            messages.add(ChatMessage.dateSeparator(dateLabel));
            lastDateLabel = dateLabel;
            adapter.notifyItemInserted(messages.size() - 1);
        }
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    // ==================== Sending ====================

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");

        // Optimistic render
        String time = ChatMessage.formatTime(null);
        ChatMessage optimistic = new ChatMessage(ChatMessage.TYPE_TEXT_SENT, text, time);
        int optimisticIndex = messages.size();
        messages.add(optimistic);
        adapter.notifyItemInserted(optimisticIndex);
        rvMessages.scrollToPosition(messages.size() - 1);

        String token = "Bearer " + sessionManager.getToken();
        apiService.sendChatMessage(token, conversationId, new SendChatMessageRequest(text))
                .enqueue(new Callback<SendChatMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SendChatMessageResponse> call,
                                           @NonNull Response<SendChatMessageResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || !response.body().isSuccess()) {
                            handleSendFailure(optimisticIndex, text);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SendChatMessageResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "sendTextMessage failed", t);
                        handleSendFailure(optimisticIndex, text);
                    }
                });
    }

    private void handleSendFailure(int index, String text) {
        runOnUiThread(() -> {
            if (index < messages.size()) {
                messages.remove(index);
                adapter.notifyItemRemoved(index);
            }
            etMessage.setText(text);
            Toast.makeText(this, "Message failed to send.", Toast.LENGTH_SHORT).show();
        });
    }

    /** Log a call event to the conversation (called after a phone call attempt). */
    private void logCallMessage(String messageType) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.sendChatMessage(token, conversationId,
                        new SendChatMessageRequest(messageType, null))
                .enqueue(new Callback<SendChatMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SendChatMessageResponse> call,
                                           @NonNull Response<SendChatMessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {
                            ChatApiMessage data = response.body().getData();
                            runOnUiThread(() -> appendMessage(
                                    ChatMessage.fromApi(data, myUserId), data.getCreatedAt()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SendChatMessageResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "logCallMessage failed", t);
                    }
                });
    }

    // ==================== Read receipts ====================

    private void markConversationRead() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.markChatConversationRead(token, conversationId)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call,
                                           @NonNull Response<RegisterResponse> response) { }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "markConversationRead failed", t);
                    }
                });
    }

    // ==================== Call ====================

    private void initiateCall() {
        if (!TextUtils.isEmpty(contactPhone)) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contactPhone));
            startActivity(intent);
            logCallMessage(ChatApiMessage.TYPE_CALL_ENDED);
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketManager != null) {
            socketManager.offChatListeners();
            socketManager.leaveChatRoom(conversationId);
        }
    }
}
