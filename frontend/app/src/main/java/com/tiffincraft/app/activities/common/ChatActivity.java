package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.MessageAdapter;
import com.tiffincraft.app.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_NAME  = "contact_name";
    public static final String EXTRA_CONTACT_PHONE = "contact_phone";
    public static final String EXTRA_IS_ONLINE     = "is_online";

    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private FrameLayout   btnSend;
    private ImageView     btnBack, btnEmoji, btnCall;
    private TextView      tvChatName, tvChatStatus;

    private MessageAdapter    adapter;
    private List<ChatMessage> messages;

    private String contactName;
    private String contactPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        contactName  = getIntent().getStringExtra(EXTRA_CONTACT_NAME);
        contactPhone = getIntent().getStringExtra(EXTRA_CONTACT_PHONE);
        boolean isOnline = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);

        initViews();
        setupHeader(contactName, isOnline);
        loadDummyMessages();
        setupRecyclerView();
        setupListeners();
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

    private void setupHeader(String name, boolean isOnline) {
        tvChatName.setText(name != null ? name : "Chat");
        tvChatStatus.setText(isOnline ? "Online" : "Offline");
        int statusColor = isOnline
                ? getColor(R.color.green_primary)
                : getColor(R.color.text_hint);
        tvChatStatus.setTextColor(statusColor);
        findViewById(R.id.viewStatusDot).setVisibility(isOnline ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void loadDummyMessages() {
        messages = new ArrayList<>();

        // Older calls
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "0:15", "5:58 PM", false));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "7:42", "6:10 PM", false));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "0:31", "6:18 PM", false));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "1:13", "6:39 PM", false));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "4:26", "6:41 PM", true));
        messages.add(new ChatMessage(ChatMessage.CALL_DECLINED, null,  "6:57 PM", false));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "18:04", "6:57 PM", true));
        messages.add(new ChatMessage(ChatMessage.CALL_ENDED, "0:46", "7:21 PM", false));

        // Date separator
        messages.add(ChatMessage.dateSeparator("Sat, Mar 28"));

        // Text messages
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_RECEIVED, "hey", "12:36 PM"));
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_SENT,     "Hi there! How are you?", "12:37 PM"));
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_RECEIVED, "I'm good, thanks! What's for lunch today?", "12:38 PM"));
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_SENT,     "Chicken biryani and dal tadka today. Want to order?", "12:39 PM"));
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_RECEIVED, "Sounds amazing! Yes please.", "12:40 PM"));
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        btnCall.setOnClickListener(v -> initiateCall());

        btnEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji picker coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        messages.add(new ChatMessage(ChatMessage.TYPE_TEXT_SENT, text, time));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
        etMessage.setText("");
    }

    private void initiateCall() {
        if (!TextUtils.isEmpty(contactPhone)) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contactPhone));
            startActivity(intent);
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }
}
