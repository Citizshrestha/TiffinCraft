package com.tiffincraft.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.adapters.ChatListAdapter;
import com.tiffincraft.app.models.ChatContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaches a floating chat FAB + slide-up chat panel to any Activity.
 *
 * Usage inside onCreate (after setContentView):
 *   ChatPanelManager.attach(this);
 */
public class ChatPanelManager {

    private final Activity activity;
    private View            panelView;
    private boolean         panelVisible = false;

    private List<ChatContact>   allContacts;
    private ChatListAdapter     adapter;
    private RecyclerView        rvChatList;

    private ChatPanelManager(Activity activity) {
        this.activity = activity;
    }

    public static ChatPanelManager attach(Activity activity) {
        ChatPanelManager manager = new ChatPanelManager(activity);
        manager.setup();
        return manager;
    }

    private void setup() {
        ViewGroup root = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        // --- Inflate FAB ---
        View fabView = LayoutInflater.from(activity).inflate(R.layout.fab_chat_button, root, false);
        root.addView(fabView);

        FrameLayout fab = fabView.findViewById(R.id.fabChat);
        fab.setOnClickListener(v -> togglePanel(root));

        // --- Inflate Panel ---
        panelView = LayoutInflater.from(activity).inflate(R.layout.panel_chat_list, root, false);
        root.addView(panelView);

        // Close button
        panelView.findViewById(R.id.btnCloseChat).setOnClickListener(v -> hidePanel());
        // Dim background tap
        panelView.setOnClickListener(v -> hidePanel());
        // Prevent tap-through on the card
        panelView.findViewById(R.id.chatPanelCard).setOnClickListener(v -> { /* consume */ });

        // RecyclerView
        rvChatList = panelView.findViewById(R.id.rvChatList);
        buildDummyContacts();
        adapter = new ChatListAdapter(activity, allContacts, contact -> {
            hidePanel();
            Intent intent = new Intent(activity, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME,  contact.getName());
            intent.putExtra(ChatActivity.EXTRA_IS_ONLINE,     contact.isOnline());
            activity.startActivity(intent);
        });
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

    private void buildDummyContacts() {
        allContacts = new ArrayList<>();
        allContacts.add(new ChatContact("Farhan Alam",  "hey",       "Mar 28", true,  0));
        allContacts.add(new ChatContact("Priya Sharma", "On my way!", "Mar 27", false, 0));
        allContacts.add(new ChatContact("Arjun Mehta",  "Thanks!",   "Mar 26", false, 0));
        allContacts.add(new ChatContact("Sunita Devi",  "Order placed", "Mar 25", true, 0));
        allContacts.add(new ChatContact("Ravi Kumar",   "Will deliver by 1pm", "Mar 24", false, 0));
    }

    private void filterContacts(String query) {
        List<ChatContact> filtered = new ArrayList<>();
        for (ChatContact c : allContacts) {
            if (c.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(c);
            }
        }
        ChatListAdapter filteredAdapter = new ChatListAdapter(activity, filtered, contact -> {
            hidePanel();
            Intent intent = new Intent(activity, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME, contact.getName());
            intent.putExtra(ChatActivity.EXTRA_IS_ONLINE,    contact.isOnline());
            activity.startActivity(intent);
        });
        rvChatList.setAdapter(filteredAdapter);
    }

    private void togglePanel(ViewGroup root) {
        if (panelVisible) hidePanel(); else showPanel();
    }

    private void showPanel() {
        panelView.setVisibility(View.VISIBLE);
        panelVisible = true;
    }

    private void hidePanel() {
        panelView.setVisibility(View.GONE);
        panelVisible = false;
    }
}
