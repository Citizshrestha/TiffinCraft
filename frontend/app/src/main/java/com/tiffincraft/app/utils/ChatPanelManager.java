package com.tiffincraft.app.utils;

import android.app.Activity;
import android.content.Intent;
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
import com.tiffincraft.app.models.CreateConversationRequest;
import com.tiffincraft.app.models.CreateConversationResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

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
    private TextView        tabChats, tabContacts, tabChannels;

    private ApiService     apiService;
    private SessionManager sessionManager;

    private boolean showingContactsTab = false;

    private ChatPanelManager(Activity activity) {
        this.activity = activity;
    }

    public static ChatPanelManager attach(Activity activity) {
        ChatPanelManager manager = new ChatPanelManager(activity);
        manager.setup();
        return manager;
    }

    private void setup() {
        apiService     = RetrofitClient.getInstance(activity).getApiService();
        sessionManager = new SessionManager(activity);

        ViewGroup root = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        // --- Inflate FAB ---
        View fabView = LayoutInflater.from(activity).inflate(R.layout.fab_chat_button, root, false);
        root.addView(fabView);

        FrameLayout fab = fabView.findViewById(R.id.fabChat);
        fab.setOnClickListener(v -> togglePanel());

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
        tabChannels = panelView.findViewById(R.id.tabChannels);

        tabChats.setOnClickListener(v -> switchTab(false));
        tabContacts.setOnClickListener(v -> switchTab(true));
        tabChannels.setOnClickListener(v ->
                Toast.makeText(activity, "Channels coming soon", Toast.LENGTH_SHORT).show());

        // RecyclerView
        rvChatList = panelView.findViewById(R.id.rvChatList);
        adapter = new ChatListAdapter(activity, allContacts, this::openConversation);
        rvChatList.setLayoutManager(new LinearLayoutManager(activity));
        rvChatList.setAdapter(adapter);

        // Search filter
        EditText etSearch = panelView.findViewById(R.id.etChatSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
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

        if (contactsTab) loadContacts(); else loadConversations();
    }

    // ==================== Data loading ====================

    private void loadConversations() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatConversations(token).enqueue(new Callback<ChatConversationsResponse>() {
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
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatContacts(token).enqueue(new Callback<ChatContactsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatContactsResponse> call,
                                   @NonNull Response<ChatContactsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    allContacts.clear();
                    List<ChatContactsResponse.ApiContact> contacts = response.body().getContacts();
                    if (contacts != null) {
                        for (ChatContactsResponse.ApiContact c : contacts) {
                            allContacts.add(ChatContact.fromApiContact(c));
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

    private void updateContactCount(int count) {
        if (tvContactCount != null) {
            tvContactCount.setText(count + (count == 1 ? " contact" : " contacts"));
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
        activity.startActivity(intent);
    }

    // ==================== Search ====================

    private void filterContacts(String query) {
        List<ChatContact> filtered = new ArrayList<>();
        for (ChatContact c : allContacts) {
            if (c.getName() != null
                    && c.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(c);
            }
        }
        ChatListAdapter filteredAdapter =
                new ChatListAdapter(activity, filtered, this::openConversation);
        rvChatList.setAdapter(filteredAdapter);
    }

    // ==================== Panel visibility ====================

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
