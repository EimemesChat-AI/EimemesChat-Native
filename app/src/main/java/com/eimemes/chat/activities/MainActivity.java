package com.eimemes.chat.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.R;
import com.eimemes.chat.adapters.ChatAdapter;
import com.eimemes.chat.adapters.ConversationAdapter;
import com.eimemes.chat.models.Conversation;
import com.eimemes.chat.models.Message;
import com.eimemes.chat.network.StreamingClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private DrawerLayout drawerLayout;
    private RecyclerView rvChat, rvConversations;
    private EditText etInput;
    private ImageButton btnSend, btnMenu, btnNewChat, btnStop;
    private TextView tvTopbarTitle, tvUserEmail;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Message>      messages      = new ArrayList<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private ChatAdapter         chatAdapter;
    private ConversationAdapter convAdapter;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private FirebaseUser      currentUser;
    private ListenerRegistration msgListener;
    private ListenerRegistration convListener;

    // ── State ─────────────────────────────────────────────────────────────────
    private String         currentConvId = null;
    private boolean        isSending     = false;
    private StreamingClient streamClient;
    private Message        streamingMsg  = null; // the live streaming bubble
    private int            streamingPos  = -1;

    private static final int    DAILY_LIMIT = 150;
    private static final int    MAX_CONVS   = 30;
    private static final String PREF_KEY    = "ec_daily";
    private static final String PREF_DATE   = "ec_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth       = FirebaseAuth.getInstance();
        db          = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        streamClient= new StreamingClient();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); return;
        }

        bindViews();
        setupGradientTitle();
        setupRecyclerViews();
        setupInput();
        setupDrawer();
        subscribeConversations();
    }

    // ── View binding ──────────────────────────────────────────────────────────
    private void bindViews() {
        drawerLayout    = findViewById(R.id.drawerLayout);
        rvChat          = findViewById(R.id.rvChat);
        rvConversations = findViewById(R.id.rvConversations);
        etInput         = findViewById(R.id.etInput);
        btnSend         = findViewById(R.id.btnSend);
        btnStop         = findViewById(R.id.btnStop);
        btnMenu         = findViewById(R.id.btnMenu);
        btnNewChat      = findViewById(R.id.btnNewChat);
        tvTopbarTitle   = findViewById(R.id.tvTopbarTitle);
        tvUserEmail     = findViewById(R.id.tvUserEmail);

        tvUserEmail.setText(currentUser.getEmail());

        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
        btnNewChat.setOnClickListener(v -> startNewChat());
        btnSend.setOnClickListener(v -> sendMessage());
        btnStop.setOnClickListener(v -> stopStreaming());
    }

    private void setupGradientTitle() {
        tvTopbarTitle.post(() -> {
            float w = tvTopbarTitle.getPaint().measureText(tvTopbarTitle.getText().toString());
            LinearGradient g = new LinearGradient(0, 0, w, 0,
                new int[]{0xFF5e9cff, 0xFFc96eff}, null, Shader.TileMode.CLAMP);
            tvTopbarTitle.getPaint().setShader(g);
            tvTopbarTitle.invalidate();
        });
    }

    private void setupRecyclerViews() {
        // Chat RecyclerView
        LinearLayoutManager chatLm = new LinearLayoutManager(this);
        chatLm.setStackFromEnd(true);
        rvChat.setLayoutManager(chatLm);
        chatAdapter = new ChatAdapter(messages);
        rvChat.setAdapter(chatAdapter);

        // Conversations sidebar RecyclerView
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        convAdapter = new ConversationAdapter(conversations, conv -> {
            drawerLayout.closeDrawers();
            loadConversation(conv.getId(), conv.getTitle());
        });
        rvConversations.setAdapter(convAdapter);
    }

    private void setupInput() {
        etInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                btnSend.setEnabled(s.toString().trim().length() > 0 && !isSending);
            }
        });
        btnSend.setEnabled(false);
    }

    private void setupDrawer() {
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(View drawerView) {}
            @Override public void onDrawerClosed(View drawerView) {}
        });
    }

    // ── Firestore subscriptions ───────────────────────────────────────────────
    private CollectionReference userConvs() {
        return db.collection("users").document(currentUser.getUid()).collection("conversations");
    }

    private void subscribeConversations() {
        if (convListener != null) convListener.remove();
        convListener = userConvs()
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(MAX_CONVS)
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                conversations.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String title = doc.getString("title");
                    conversations.add(new Conversation(doc.getId(), title));
                }
                convAdapter.notifyDataSetChanged();
            });
    }

    private void loadConversation(String convId, String title) {
        currentConvId = convId;
        tvTopbarTitle.setText(title != null ? title : "EimemesChat AI");
        setupGradientTitle();
        messages.clear();
        chatAdapter.notifyDataSetChanged();
        convAdapter.setActiveId(convId);

        if (msgListener != null) msgListener.remove();
        msgListener = userConvs().document(convId)
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null || !snap.exists()) return;
                List<Map<String, Object>> rawMsgs =
                    (List<Map<String, Object>>) snap.get("messages");
                if (rawMsgs == null) return;
                messages.clear();
                for (Map<String, Object> m : rawMsgs) {
                    String role    = (String) m.get("role");
                    String content = (String) m.get("content");
                    String time    = (String) m.get("time");
                    Boolean disc   = (Boolean) m.get("disclaimer");
                    Message msg    = new Message(role, content, time);
                    if (disc != null) msg.setDisclaimer(disc);
                    messages.add(msg);
                }
                chatAdapter.notifyDataSetChanged();
                scrollToBottom();
            });
    }

    // ── New chat ──────────────────────────────────────────────────────────────
    private void startNewChat() {
        currentConvId = null;
        messages.clear();
        chatAdapter.notifyDataSetChanged();
        tvTopbarTitle.setText("EimemesChat AI");
        setupGradientTitle();
        convAdapter.setActiveId(null);
        if (msgListener != null) { msgListener.remove(); msgListener = null; }
        drawerLayout.closeDrawers();
    }

    // ── Send message ──────────────────────────────────────────────────────────
    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || isSending) return;

        if (!checkDailyLimit()) {
            Toast.makeText(this, "Daily limit of " + DAILY_LIMIT + " messages reached. Try tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }

        isSending = true;
        btnSend.setEnabled(false);
        btnStop.setVisibility(View.VISIBLE);
        btnSend.setVisibility(View.GONE);
        etInput.setText("");

        // Add user message to UI immediately
        String time = getTime();
        Message userMsg = new Message(Message.ROLE_USER, text, time);
        messages.add(userMsg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        // Create conversation in Firestore if needed, then send
        if (currentConvId == null) {
            createNewConversation(text, () -> doSend(text));
        } else {
            saveUserMessage(text, time, () -> doSend(text));
        }
    }

    private void createNewConversation(String firstMsg, Runnable onDone) {
        String autoTitle = firstMsg.length() > 50 ? firstMsg.substring(0, 50) + "…" : firstMsg;
        Map<String, Object> conv = new HashMap<>();
        conv.put("title",     autoTitle);
        conv.put("messages",  new ArrayList<>());
        conv.put("createdAt", FieldValue.serverTimestamp());
        conv.put("updatedAt", FieldValue.serverTimestamp());

        userConvs().add(conv).addOnSuccessListener(ref -> {
            currentConvId = ref.getId();
            tvTopbarTitle.setText(autoTitle);
            setupGradientTitle();
            convAdapter.setActiveId(currentConvId);
            saveUserMessage(firstMsg, getTime(), onDone);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create chat.", Toast.LENGTH_SHORT).show();
            resetSendState();
        });
    }

    private void saveUserMessage(String text, String time, Runnable onDone) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role",    "user");
        msg.put("content", text);
        msg.put("time",    time);

        userConvs().document(currentConvId)
            .update("messages", FieldValue.arrayUnion(msg), "updatedAt", FieldValue.serverTimestamp())
            .addOnSuccessListener(v -> onDone.run())
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send.", Toast.LENGTH_SHORT).show();
                resetSendState();
            });
    }

    private void doSend(String text) {
        // Add streaming placeholder bubble
        streamingMsg = new Message(Message.ROLE_TYPING, "", getTime());
        messages.add(streamingMsg);
        streamingPos = messages.size() - 1;
        chatAdapter.notifyItemInserted(streamingPos);
        scrollToBottom();

        // Build history for API (last 10 messages)
        JSONArray history = buildHistory();

        // Get Firebase ID token then stream
        currentUser.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            streamClient.sendMessage(text, history, token, new StreamingClient.StreamCallback() {
                @Override
                public void onToken(String t) {
                    if (streamingMsg == null) return;
                    streamingMsg.setContent(streamingMsg.getContent() + t);
                    streamingMsg.setRole(Message.ROLE_ASSISTANT);
                    chatAdapter.notifyItemChanged(streamingPos);
                    scrollToBottom();
                }

                @Override
                public void onDone(String fullText, String model, boolean disclaimer) {
                    if (streamingMsg != null) {
                        streamingMsg.setContent(fullText);
                        streamingMsg.setRole(Message.ROLE_ASSISTANT);
                        streamingMsg.setDisclaimer(disclaimer);
                        chatAdapter.notifyItemChanged(streamingPos);
                    }
                    saveAiMessage(fullText, disclaimer);
                    resetSendState();
                }

                @Override
                public void onError(String error) {
                    if (streamingMsg != null) {
                        streamingMsg.setContent(error);
                        streamingMsg.setRole(Message.ROLE_ASSISTANT);
                        chatAdapter.notifyItemChanged(streamingPos);
                    }
                    resetSendState();
                }
            });
        });
    }

    private void saveAiMessage(String text, boolean disclaimer) {
        if (currentConvId == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("role",       "assistant");
        msg.put("content",    text);
        msg.put("time",       getTime());
        msg.put("disclaimer", disclaimer);
        userConvs().document(currentConvId)
            .update("messages", FieldValue.arrayUnion(msg), "updatedAt", FieldValue.serverTimestamp());
    }

    private void stopStreaming() {
        streamClient.cancel();
        resetSendState();
    }

    private void resetSendState() {
        isSending    = false;
        streamingMsg = null;
        streamingPos = -1;
        btnSend.setEnabled(etInput.getText().toString().trim().length() > 0);
        btnStop.setVisibility(View.GONE);
        btnSend.setVisibility(View.VISIBLE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JSONArray buildHistory() {
        JSONArray arr = new JSONArray();
        int start = Math.max(0, messages.size() - 11);
        for (int i = start; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (Message.ROLE_TYPING.equals(m.getRole())) continue;
            try {
                JSONObject obj = new JSONObject();
                obj.put("role",    m.getRole());
                obj.put("content", m.getContent());
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr;
    }

    private boolean checkDailyLimit() {
        android.content.SharedPreferences prefs =
            getSharedPreferences("eimemes", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = prefs.getString(PREF_DATE, "");
        int count = today.equals(savedDate) ? prefs.getInt(PREF_KEY, 0) : 0;
        if (count >= DAILY_LIMIT) return false;
        prefs.edit().putInt(PREF_KEY, count + 1).putString(PREF_DATE, today).apply();
        return true;
    }

    private String getTime() {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
    }

    private void scrollToBottom() {
        if (messages.size() > 0)
            rvChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void signOut() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener  != null) msgListener.remove();
        if (convListener != null) convListener.remove();
        streamClient.cancel();
    }
}
