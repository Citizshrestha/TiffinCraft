package com.tiffincraft.app.activities.common;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.tiffincraft.app.models.DeleteChatMessagesRequest;
import com.tiffincraft.app.models.EditChatMessageRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SendChatMessageRequest;
import com.tiffincraft.app.models.SendChatMessageResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;
import com.tiffincraft.app.utils.ImageUploadHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int REQUEST_PICK_CHAT_MEDIA = 4101;

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONTACT_NAME    = "contact_name";
    public static final String EXTRA_CONTACT_PHONE   = "contact_phone";
    public static final String EXTRA_IS_ONLINE       = "is_online";

    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private FrameLayout   btnSend;
    private ImageView     btnBack, btnAttachment, btnCall;
    private TextView      tvChatName, tvChatStatus;

    private View      chatHeader;
    private View      selectionHeader;
    private TextView  tvSelectedCount;
    private ImageView btnCloseSelection, btnBulkEdit, btnBulkDelete;

    private View      editBar;
    private ImageView btnCancelEdit;
    private ChatMessage editingMessage;

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
    private boolean messageLoadRetried = false;
    private boolean skipNextResumeReload = true;

    // Permission launcher for media access
    private ActivityResultLauncher<String[]> mediaPermissionLauncher;

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

        // Initialize permission launcher
        initializePermissionLauncher();

        initViews();
        setupHeader(contactName, isOnline);
        setupRecyclerView();
        setupListeners();
        setupSocket();
        loadMessages();
        markConversationRead();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    /** Exit selection mode / cancel an in-progress edit before leaving the screen. */
    private void handleBackNavigation() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.clearSelection();
        } else if (editingMessage != null) {
            cancelEditing();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeReload) {
            skipNextResumeReload = false;
            return;
        }
        if (conversationId > 0) {
            loadMessages();
        }
    }

    private void initViews() {
        rvMessages   = findViewById(R.id.rvMessages);
        etMessage    = findViewById(R.id.etMessage);
        btnSend      = findViewById(R.id.btnSend);
        btnBack      = findViewById(R.id.btnBack);
        btnAttachment = findViewById(R.id.btnAttachment);
        btnCall      = findViewById(R.id.btnCall);
        tvChatName   = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);

        chatHeader        = findViewById(R.id.chatHeader);
        selectionHeader    = findViewById(R.id.selectionHeader);
        tvSelectedCount    = findViewById(R.id.tvSelectedCount);
        btnCloseSelection  = findViewById(R.id.btnCloseSelection);
        btnBulkEdit        = findViewById(R.id.btnBulkEdit);
        btnBulkDelete      = findViewById(R.id.btnBulkDelete);

        editBar       = findViewById(R.id.editBar);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
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
        adapter.setOnMessageActionListener(this::onSelectionChanged);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);
    }

    // ==================== Selection mode ====================

    private void onSelectionChanged(int selectedCount, boolean canEditSelection) {
        if (selectedCount <= 0) {
            selectionHeader.setVisibility(View.GONE);
            chatHeader.setVisibility(View.VISIBLE);
            return;
        }
        chatHeader.setVisibility(View.GONE);
        selectionHeader.setVisibility(View.VISIBLE);
        tvSelectedCount.setText(selectedCount == 1 ? "1 selected" : selectedCount + " selected");
        btnBulkEdit.setVisibility(canEditSelection ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> handleBackNavigation());

        btnSend.setOnClickListener(v -> onSendPressed());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            onSendPressed();
            return true;
        });

        btnCancelEdit.setOnClickListener(v -> cancelEditing());

        btnCloseSelection.setOnClickListener(v -> adapter.clearSelection());

        btnBulkEdit.setOnClickListener(v -> {
            List<ChatMessage> selected = adapter.getSelectedMessages();
            if (selected.size() != 1) return;
            ChatMessage message = selected.get(0);
            int position = adapter.indexOfMessage(message);
            adapter.clearSelection();
            startEditingMessage(message, position);
        });

        btnBulkDelete.setOnClickListener(v -> confirmDeleteSelection());

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

        btnAttachment.setOnClickListener(v -> openMediaPicker());
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

        socketManager.onChatMessageEdited(args -> {
            if (args.length == 0) return;
            try {
                JSONObject json = (JSONObject) args[0];
                ChatApiMessage apiMsg = gson.fromJson(json.toString(), ChatApiMessage.class);
                if (apiMsg.getConversationId() != conversationId) return;
                runOnUiThread(() -> applyEditedMessage(apiMsg));
            } catch (Exception e) {
                Log.e(TAG, "Error handling chatMessageEdited", e);
            }
        });

        socketManager.onChatMessageDeleted(args -> {
            if (args.length == 0) return;
            try {
                JSONObject json = (JSONObject) args[0];
                int deletedConversationId = json.optInt("conversation_id", 0);
                int deletedMessageId = json.optInt("message_id", 0);
                if (deletedConversationId != conversationId || deletedMessageId <= 0) return;
                runOnUiThread(() -> adapter.markMessageDeleted(deletedMessageId));
            } catch (Exception e) {
                Log.e(TAG, "Error handling chatMessageDeleted", e);
            }
        });
    }

    // ==================== Data loading ====================

    private void loadMessages() {
        String token = "Bearer " + sessionManager.getToken();
        Log.d(TAG, "Loading messages for conversation " + conversationId);
        apiService.getChatMessages(token, conversationId, null, 50)
                .enqueue(new Callback<ChatMessagesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ChatMessagesResponse> call,
                                           @NonNull Response<ChatMessagesResponse> response) {
                        Log.d(TAG, "loadMessages response code: " + response.code());
                        Log.d(TAG, "loadMessages isSuccessful: " + response.isSuccessful());
                        Log.d(TAG, "loadMessages body is null: " + (response.body() == null));

                        if (response.body() != null) {
                            Log.d(TAG, "loadMessages body.isSuccess: " + response.body().isSuccess());
                            if (response.body().getMessages() != null) {
                                Log.d(TAG, "loadMessages message count: " + response.body().getMessages().size());
                            } else {
                                Log.d(TAG, "loadMessages messages list is null");
                            }
                        }

                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Response not successful. Code: " + response.code());
                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error body: " + errorBody);
                                } catch (Exception e) {
                                    Log.e(TAG, "Could not read error body", e);
                                }
                            }
                        }

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            messageLoadRetried = false;
                            renderMessages(response.body().getMessages());
                        } else {
                            Toast.makeText(ChatActivity.this,
                                    "Failed to load messages.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ChatMessagesResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "loadMessages network failure", t);
                        if (!messageLoadRetried) {
                            messageLoadRetried = true;
                            rvMessages.postDelayed(ChatActivity.this::loadMessages, 750);
                            return;
                        }
                        Toast.makeText(ChatActivity.this,
                                "Could not load chat messages.", Toast.LENGTH_SHORT).show();
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

    private void onSendPressed() {
        if (editingMessage != null) {
            saveEdit();
        } else {
            sendTextMessage();
        }
    }

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
        Log.d(TAG, "Sending message to conversation " + conversationId + ": " + text);

        apiService.sendChatMessage(token, conversationId, new SendChatMessageRequest(text))
                .enqueue(new Callback<SendChatMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SendChatMessageResponse> call,
                                           @NonNull Response<SendChatMessageResponse> response) {
                        Log.d(TAG, "sendTextMessage response code: " + response.code());

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "Message sent successfully!");
                            ChatApiMessage serverMessage = response.body().getData();
                            if (serverMessage != null) {
                                // Replace optimistic message with server version
                                runOnUiThread(() -> {
                                    if (optimisticIndex < messages.size()) {
                                        messages.set(optimisticIndex, ChatMessage.fromApi(serverMessage, myUserId));
                                        adapter.notifyItemChanged(optimisticIndex);
                                    }
                                });
                            }
                            return; // Success!
                        } else {
                            Log.e(TAG, "Response not successful or body is null");
                            if (response.body() != null) {
                                Log.e(TAG, "API returned success=false: " + response.body().getMessage());
                            }
                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error body: " + errorBody);
                                } catch (Exception e) {
                                    Log.e(TAG, "Could not read error body", e);
                                }
                            }
                        }
                        handleSendFailure(optimisticIndex, text);
                    }

                    @Override
                    public void onFailure(@NonNull Call<SendChatMessageResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "sendTextMessage network failure", t);
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


    /**
     * Initialize permission launcher for requesting media access permissions
     */
    private void initializePermissionLauncher() {
        mediaPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean imagesGranted = true;
                    boolean videosGranted = true;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Boolean img = result.get(Manifest.permission.READ_MEDIA_IMAGES);
                        Boolean vid = result.get(Manifest.permission.READ_MEDIA_VIDEO);
                        imagesGranted = img != null && img;
                        videosGranted = vid != null && vid;
                    } else {
                        Boolean storage = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                        imagesGranted = videosGranted = storage != null && storage;
                    }

                    if (imagesGranted || videosGranted) {
                        openMediaPickerIntent();
                    } else {
                        showPermissionDeniedDialog();
                    }
                });
    }

    /**
     * Check if media permissions are granted
     */
    private boolean hasMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — allow if either images or videos is granted
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Get required permissions based on Android version
     */
    private String[] getRequiredMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    private boolean shouldShowMediaRationale() {
        for (String permission : getRequiredMediaPermissions()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Open media picker — always requests gallery permission when not granted.
     */
    private void openMediaPicker() {
        hideKeyboard();

        if (hasMediaPermissions()) {
            openMediaPickerIntent();
            return;
        }

        // Explain why we need access, then request system permission
        if (shouldShowMediaRationale()) {
            new AlertDialog.Builder(this)
                    .setTitle("Gallery access needed")
                    .setMessage("TiffinCraft needs permission to access your photos and videos so you can share them in chat.")
                    .setPositiveButton("Allow", (d, w) ->
                            mediaPermissionLauncher.launch(getRequiredMediaPermissions()))
                    .setNegativeButton("Not now", null)
                    .show();
        } else {
            // First time (or permanent denial path handled after result)
            new AlertDialog.Builder(this)
                    .setTitle("Allow gallery access?")
                    .setMessage("To send images and videos in chat, please allow access to your gallery.")
                    .setPositiveButton("Continue", (d, w) ->
                            mediaPermissionLauncher.launch(getRequiredMediaPermissions()))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showPermissionDeniedDialog() {
        boolean permanentlyDenied = true;
        for (String permission : getRequiredMediaPermissions()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                permanentlyDenied = false;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage(permanentlyDenied
                        ? "Gallery access is disabled. Open Settings and allow Photos and videos for TiffinCraft."
                        : "Media access permission is required to send images and videos in chat.")
                .setNegativeButton("Cancel", null);

        if (permanentlyDenied) {
            builder.setPositiveButton("Open Settings", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            });
        } else {
            builder.setPositiveButton("Try again", (d, w) ->
                    mediaPermissionLauncher.launch(getRequiredMediaPermissions()));
        }
        builder.show();
    }

    /**
     * Open the actual media picker intent (gallery / files).
     */
    private void openMediaPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Select image or video"), REQUEST_PICK_CHAT_MEDIA);
    }

    // ==================== Edit / Delete ====================

    /** Instagram-style inline edit: fills the composer and shows the "Editing message" bar. */
    private void startEditingMessage(ChatMessage message, int position) {
        if (message == null || !message.canEdit()) return;
        editingMessage = message;
        etMessage.setText(message.getText());
        if (etMessage.getText() != null) {
            etMessage.setSelection(etMessage.getText().length());
        }
        editBar.setVisibility(View.VISIBLE);
        etMessage.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void cancelEditing() {
        editingMessage = null;
        editBar.setVisibility(View.GONE);
        etMessage.setText("");
    }

    private void saveEdit() {
        if (editingMessage == null) return;
        String newText = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(newText)) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage message = editingMessage;
        int position = adapter.indexOfMessage(message);

        if (newText.equals(message.getText())) {
            cancelEditing();
            return;
        }

        cancelEditing();
        editMessageOnServer(message, position, newText);
    }

    private void editMessageOnServer(ChatMessage message, int position, String newText) {
        String token = "Bearer " + sessionManager.getToken();
        String previous = message.getText();
        boolean wasEdited = message.isEdited();
        message.setText(newText);
        message.setEdited(true);
        if (position >= 0 && position < messages.size()) {
            adapter.notifyItemChanged(position);
        }

        apiService.editChatMessage(token, conversationId, message.getServerId(),
                        new EditChatMessageRequest(newText))
                .enqueue(new Callback<SendChatMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SendChatMessageResponse> call,
                                           @NonNull Response<SendChatMessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {
                            runOnUiThread(() -> applyEditedMessage(response.body().getData()));
                        } else {
                            runOnUiThread(() -> {
                                message.setText(previous);
                                message.setEdited(wasEdited);
                                if (position >= 0 && position < messages.size()) {
                                    adapter.notifyItemChanged(position);
                                }
                                Toast.makeText(ChatActivity.this,
                                        "Could not edit message.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SendChatMessageResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "editMessageOnServer failed", t);
                        runOnUiThread(() -> {
                            message.setText(previous);
                            message.setEdited(wasEdited);
                            if (position >= 0 && position < messages.size()) {
                                adapter.notifyItemChanged(position);
                            }
                            Toast.makeText(ChatActivity.this,
                                    "Could not edit message.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /** Delete confirmation for whatever is currently selected — works for one or many. */
    private void confirmDeleteSelection() {
        List<ChatMessage> selected = adapter.getSelectedMessages();
        if (selected.isEmpty()) return;

        String label = selected.size() == 1
                ? deleteLabelFor(selected.get(0))
                : "Delete " + selected.size() + " messages?";

        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage(label + "\nThis cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteSelectedOnServer(selected))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String deleteLabelFor(ChatMessage message) {
        int vt = message.getViewType();
        if (vt == ChatMessage.TYPE_IMAGE_SENT) return "Delete this image?";
        if (vt == ChatMessage.TYPE_VIDEO_SENT) return "Delete this video?";
        return "Delete this message?";
    }

    private void deleteSelectedOnServer(List<ChatMessage> selected) {
        final List<Integer> ids = new ArrayList<>();
        for (ChatMessage m : selected) ids.add(m.getServerId());

        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteChatMessages(token, conversationId, new DeleteChatMessagesRequest(ids))
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call,
                                           @NonNull Response<RegisterResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            runOnUiThread(() -> {
                                for (int id : ids) {
                                    adapter.markMessageDeleted(id);
                                    if (editingMessage != null && editingMessage.getServerId() == id) {
                                        cancelEditing();
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this,
                                    "Could not delete messages.", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "deleteSelectedOnServer failed", t);
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this,
                                "Could not delete messages.", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void applyEditedMessage(ChatApiMessage apiMsg) {
        if (apiMsg == null || apiMsg.getId() <= 0) return;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m.getServerId() == apiMsg.getId()) {
                messages.set(i, ChatMessage.fromApi(apiMsg, myUserId));
                adapter.notifyItemChanged(i);
                return;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_CHAT_MEDIA || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri mediaUri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    mediaUri,
                    data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        uploadAndSendMedia(mediaUri);
    }

    private void uploadAndSendMedia(Uri mediaUri) {
        String mimeType = ImageUploadHelper.getMimeType(this, mediaUri);
        if (mimeType == null || (!mimeType.startsWith("image/") && !mimeType.startsWith("video/"))) {
            Toast.makeText(this, "Please select an image or video.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isVideo = mimeType.startsWith("video/");
        long maxSizeMb = isVideo ? 100 : 10;
        if (!ImageUploadHelper.isValidFileSize(this, mediaUri, maxSizeMb)) {
            Toast.makeText(this, isVideo ? "Video must be 100MB or smaller." : "Image must be 10MB or smaller.", Toast.LENGTH_SHORT).show();
            return;
        }

        MultipartBody.Part mediaPart = ImageUploadHelper.createMediaPart(this, mediaUri);
        if (mediaPart == null) {
            Toast.makeText(this, "Could not prepare media.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading media...", Toast.LENGTH_SHORT).show();
        String token = "Bearer " + sessionManager.getToken();
        apiService.uploadChatMediaCloudinary(token, mediaPart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null && response.body().getData().getUrl() != null) {
                    String url = response.body().getData().getUrl();
                    sendMediaMessage(isVideo ? ChatApiMessage.TYPE_VIDEO : ChatApiMessage.TYPE_IMAGE, url);
                } else {
                    Toast.makeText(ChatActivity.this, "Media upload failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "uploadAndSendMedia failed", t);
                Toast.makeText(ChatActivity.this, "Media upload failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMediaMessage(String messageType, String mediaUrl) {
        boolean isVideo = ChatApiMessage.TYPE_VIDEO.equals(messageType);
        ChatMessage optimistic = ChatMessage.media(
                isVideo ? ChatMessage.TYPE_VIDEO_SENT : ChatMessage.TYPE_IMAGE_SENT,
                mediaUrl,
                ChatMessage.formatTime(null),
                true);
        int optimisticIndex = messages.size();
        messages.add(optimistic);
        adapter.notifyItemInserted(optimisticIndex);
        rvMessages.scrollToPosition(messages.size() - 1);

        String token = "Bearer " + sessionManager.getToken();
        apiService.sendChatMessage(token, conversationId, SendChatMessageRequest.media(messageType, mediaUrl))
                .enqueue(new Callback<SendChatMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SendChatMessageResponse> call,
                                           @NonNull Response<SendChatMessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ChatApiMessage serverMessage = response.body().getData();
                            if (serverMessage != null) {
                                // Replace optimistic message with server version
                                runOnUiThread(() -> {
                                    if (optimisticIndex < messages.size()) {
                                        messages.set(optimisticIndex, ChatMessage.fromApi(serverMessage, myUserId));
                                        adapter.notifyItemChanged(optimisticIndex);
                                    }
                                });
                            }
                            return;
                        }
                        handleMediaSendFailure(optimisticIndex);
                    }

                    @Override
                    public void onFailure(@NonNull Call<SendChatMessageResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "sendMediaMessage failed", t);
                        handleMediaSendFailure(optimisticIndex);
                    }
                });
    }

    private void handleMediaSendFailure(int index) {
        runOnUiThread(() -> {
            if (index < messages.size()) {
                messages.remove(index);
                adapter.notifyItemRemoved(index);
            }
            Toast.makeText(this, "Media message failed to send.", Toast.LENGTH_SHORT).show();
        });
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
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
