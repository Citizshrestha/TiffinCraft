package com.tiffincraft.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.adapters.ChatListAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ChatContact;
import com.tiffincraft.app.models.ChatContactsResponse;
import com.tiffincraft.app.models.ChatConversation;
import com.tiffincraft.app.models.ChatConversationsResponse;
import com.tiffincraft.app.models.ChatUnreadCountResponse;
import com.tiffincraft.app.models.CreateConversationRequest;
import com.tiffincraft.app.models.CreateConversationResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Attaches a floating chat FAB + slide-up chat panel to any Activity.
 * Loads real conversations and contacts from the backend.
 *
 * Usage inside onCreate (after setContentView):
 *   ChatPanelManager.attach(this);
 */
public class ChatPanelManager {

    private static final String TAG = "ChatPanelManager";

    private final Activity activity;
    private View    panelView;
    private boolean panelVisible = false;

    private final List<ChatContact> allContacts = new ArrayList<>();
    private ChatListAdapter adapter;
    private RecyclerView    rvChatList;
    private TextView        tvContactCount;
    private TextView        tabChats, tabContacts;
    private TextView        chatFabBadge;

    private ApiService     apiService;
    private SessionManager sessionManager;
    private SocketManager  socketManager;

    private boolean showingContactsTab = false;

    private static final int SEARCH_DEBOUNCE_MS = 300;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable      pendingSearch;
    private String        currentQuery = "";
    private ActivityResultLauncher<String> contactsPermissionLauncher;

    private ChatPanelManager(Activity activity) {
        this.activity = activity;
    }

    /** Attaches the chat panel with its own floating chat button. */
    public static ChatPanelManager attach(Activity activity) {
        return attach(activity, true);
    }

    /**
     * Attaches the slide-up chat panel.
     * @param inflateOwnFab when false, the screen already has its own chat
     *                      button in its layout — call {@link #open()} from
     *                      that button's click listener instead.
     */
    public static ChatPanelManager attach(Activity activity, boolean inflateOwnFab) {
        ChatPanelManager manager = new ChatPanelManager(activity);
        manager.setup(inflateOwnFab);
        return manager;
    }

    private void setup(boolean inflateOwnFab) {
        if (activity instanceof ComponentActivity) {
            contactsPermissionLauncher = ((ComponentActivity) activity).registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (granted) {
                            loadContacts(currentQuery);
                        } else {
                            clearContactResults();
                            Toast.makeText(activity,
                                    "Contacts permission is required to show saved contacts.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        apiService     = RetrofitClient.getInstance(activity).getApiService();
        sessionManager = new SessionManager(activity);
        socketManager  = SocketManager.getInstance(activity);

        ViewGroup root = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        // --- Inflate FAB (optional — skip if the screen has its own button) ---
        if (inflateOwnFab) {
            View fabView = LayoutInflater.from(activity).inflate(R.layout.fab_chat_button, root, false);
            root.addView(fabView);

            FrameLayout fab = fabView.findViewById(R.id.fabChat);
            fab.setOnClickListener(v -> togglePanel());
        }

        // Both fabChat layouts (inflated or already in the host screen) declare a
        // sibling tvChatFabBadge for the unread-message count.
        chatFabBadge = root.findViewById(R.id.tvChatFabBadge);

        // Unread badge: initial load + live updates via socket while this screen is open.
        socketManager.connect();
        socketManager.onChatNotification(args -> activity.runOnUiThread(this::refreshUnreadBadge));
        refreshUnreadBadge();

        // --- Inflate Panel ---
        panelView = LayoutInflater.from(activity).inflate(R.layout.panel_chat_list, root, false);
        root.addView(panelView);

        panelView.findViewById(R.id.btnCloseChat).setOnClickListener(v -> hidePanel());
        panelView.setOnClickListener(v -> hidePanel());
        panelView.findViewById(R.id.chatPanelCard).setOnClickListener(v -> { /* consume */ });

        tvContactCount = panelView.findViewById(R.id.tvContactCount);

        // Tabs
        tabChats    = panelView.findViewById(R.id.tabChats);
        tabContacts = panelView.findViewById(R.id.tabContacts);

        tabChats.setOnClickListener(v -> switchTab(false));
        tabContacts.setOnClickListener(v -> switchTab(true));

        // RecyclerView
        rvChatList = panelView.findViewById(R.id.rvChatList);
        adapter = new ChatListAdapter(activity, allContacts, this::openConversation);
        rvChatList.setLayoutManager(new LinearLayoutManager(activity));
        rvChatList.setAdapter(adapter);

        // Customers usually have zero existing conversations, which left the panel
        // opening on an empty "Chats" tab with no way to see who they can message.
        // Default customers straight to the Contacts tab (all cooks) for now so the
        // chat list isn't empty on first use.
        if ("customer".equalsIgnoreCase(sessionManager.getRole())) {
            showingContactsTab = true;
            tabChats.setBackground(null);
            tabChats.setTextColor(0xCCFFFFFF);
            tabChats.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabContacts.setBackgroundResource(R.drawable.bg_chat_tab_selected);
            tabContacts.setTextColor(0xFFFFFFFF);
            tabContacts.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // Search — debounced, hits the backend so results aren't limited to
        // whatever page of contacts/conversations happened to already be loaded.
        EditText etSearch = panelView.findViewById(R.id.etChatSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ==================== Tabs ====================

    private void switchTab(boolean contactsTab) {
        showingContactsTab = contactsTab;

        if (contactsTab) {
            tabChats.setBackground(null);
            tabChats.setTextColor(0xCCFFFFFF);
            tabChats.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabContacts.setBackgroundResource(R.drawable.bg_chat_tab_selected);
            tabContacts.setTextColor(0xFFFFFFFF);
            tabContacts.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tabContacts.setBackground(null);
            tabContacts.setTextColor(0xCCFFFFFF);
            tabContacts.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabChats.setBackgroundResource(R.drawable.bg_chat_tab_selected);
            tabChats.setTextColor(0xFFFFFFFF);
            tabChats.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        if (contactsTab) loadContacts(currentQuery); else loadConversations(currentQuery);
    }

    // ==================== Data loading ====================

    private void loadConversations() {
        loadConversations(currentQuery);
    }

    private void loadConversations(String search) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatConversations(token, search).enqueue(new Callback<ChatConversationsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatConversationsResponse> call,
                                   @NonNull Response<ChatConversationsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    allContacts.clear();
                    List<ChatConversation> conversations = response.body().getConversations();
                    if (conversations != null) {
                        for (ChatConversation c : conversations) {
                            allContacts.add(ChatContact.fromConversation(c));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateContactCount(allContacts.size());
                } else {
                    Log.e(TAG, "loadConversations: unsuccessful response");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatConversationsResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "loadConversations failed", t);
                Toast.makeText(activity, "Could not load chats.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContacts() {
        loadContacts(currentQuery);
    }

    private void loadContacts(String search) {
        if (!hasContactsPermission()) {
            clearContactResults();
            requestContactsPermission();
            return;
        }

        final Set<String> savedPhoneNumbers = readSavedPhoneNumbers();
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatContacts(token, search).enqueue(new Callback<ChatContactsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatContactsResponse> call,
                                   @NonNull Response<ChatContactsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    allContacts.clear();
                    List<ChatContactsResponse.ApiContact> contacts = response.body().getContacts();
                    if (contacts != null) {
                        for (ChatContactsResponse.ApiContact c : contacts) {
                            String contactNumber = normalizePhoneNumber(c.getPhone());
                            if (!contactNumber.isEmpty() && savedPhoneNumbers.contains(contactNumber)) {
                                allContacts.add(ChatContact.fromApiContact(c));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateContactCount(allContacts.size());
                } else {
                    Log.e(TAG, "loadContacts: unsuccessful response");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatContactsResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "loadContacts failed", t);
                Toast.makeText(activity, "Could not load contacts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission() {
        if (contactsPermissionLauncher != null) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        } else {
            Toast.makeText(activity,
                    "Contacts permission is unavailable on this screen.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Read phone numbers only; names and other address-book data are not needed. */
    private Set<String> readSavedPhoneNumbers() {
        Set<String> numbers = new HashSet<>();
        ContentResolver resolver = activity.getContentResolver();
        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null)) {
            if (cursor != null) {
                int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (numberColumn >= 0 && cursor.moveToNext()) {
                    String normalized = normalizePhoneNumber(cursor.getString(numberColumn));
                    if (!normalized.isEmpty()) numbers.add(normalized);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to read saved phone contacts", e);
        }
        return numbers;
    }

    /** Compare local/international formats by their final ten digits. */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() > 10) return digits.substring(digits.length() - 10);
        return digits;
    }

    private void clearContactResults() {
        allContacts.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        updateContactCount(0);
    }

    private void updateContactCount(int count) {
        if (tvContactCount != null) {
            tvContactCount.setText(count + (count == 1 ? " contact" : " contacts"));
        }
    }

    // ==================== Unread badge ====================

    /**
     * Re-fetches the total unread message count and updates the FAB badge.
     * Public so host screens can call it (e.g. in onResume, after returning
     * from a chat where messages were just marked read) to bring the count
     * down without waiting for a live socket event.
     */
    public void refreshUnreadBadge() {
        if (chatFabBadge == null) return;

        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatUnreadCount(token).enqueue(new Callback<ChatUnreadCountResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatUnreadCountResponse> call,
                                   @NonNull Response<ChatUnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    setBadgeCount(response.body().getUnreadCount());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatUnreadCountResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "refreshUnreadBadge failed", t);
            }
        });
    }

    private void setBadgeCount(int count) {
        if (chatFabBadge == null) return;
        if (count <= 0) {
            chatFabBadge.setVisibility(View.GONE);
        } else {
            chatFabBadge.setVisibility(View.VISIBLE);
            chatFabBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }

    // ==================== Navigation ====================

    private void openConversation(ChatContact contact) {
        if (contact.getConversationId() > 0) {
            startChatActivity(contact.getConversationId(), contact);
            return;
        }

        // No conversation yet: create one first
        String token = "Bearer " + sessionManager.getToken();
        apiService.createChatConversation(token,
                        new CreateConversationRequest(contact.getOtherUserId()))
                .enqueue(new Callback<CreateConversationResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CreateConversationResponse> call,
                                           @NonNull Response<CreateConversationResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getConversation() != null) {
                            int conversationId = response.body().getConversation().getId();
                            contact.setConversationId(conversationId);
                            startChatActivity(conversationId, contact);
                        } else {
                            Toast.makeText(activity,
                                    "Could not start conversation.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CreateConversationResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "createChatConversation failed", t);
                        Toast.makeText(activity,
                                "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startChatActivity(int conversationId, ChatContact contact) {
        hidePanel();
        Intent intent = new Intent(activity, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME, contact.getName());
        intent.putExtra(ChatActivity.EXTRA_IS_ONLINE, contact.isOnline());
        intent.putExtra(ChatActivity.EXTRA_CONTACT_ID, contact.getOtherUserId());
        intent.putExtra(ChatActivity.EXTRA_CONTACT_ROLE, contact.getRole());
        intent.putExtra(ChatActivity.EXTRA_CONTACT_PHONE, contact.getPhone());
        intent.putExtra(ChatActivity.EXTRA_CONTACT_AVATAR, contact.getAvatarUrl());
        activity.startActivity(intent);
    }

    // ==================== Search ====================

    private void scheduleSearch(String query) {
        currentQuery = query == null ? "" : query.trim();
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> {
            if (showingContactsTab) loadContacts(currentQuery); else loadConversations(currentQuery);
        };
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    // ==================== Panel visibility ====================

    /** Opens the chat panel. Call this from any custom chat button's click listener. */
    public void open() {
        showPanel();
    }

    private void togglePanel() {
        if (panelVisible) hidePanel(); else showPanel();
    }

    private void showPanel() {
        panelView.setVisibility(View.VISIBLE);
        panelVisible = true;
        if (showingContactsTab) loadContacts(); else loadConversations();
    }

    private void hidePanel() {
        panelView.setVisibility(View.GONE);
        panelVisible = false;
    }
}
