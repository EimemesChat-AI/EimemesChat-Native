package com.eimemes.chat.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.R;
import com.eimemes.chat.models.Message;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER      = 0;
    private static final int TYPE_ASSISTANT = 1;
    private static final int TYPE_TYPING    = 2;

    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        String role = messages.get(position).getRole();
        if (Message.ROLE_TYPING.equals(role)) return TYPE_TYPING;
        if (Message.ROLE_USER.equals(role))   return TYPE_USER;
        return TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserViewHolder(inf.inflate(R.layout.item_message_user, parent, false));
        } else if (viewType == TYPE_TYPING) {
            return new TypingViewHolder(inf.inflate(R.layout.item_typing, parent, false));
        } else {
            return new AssistantViewHolder(inf.inflate(R.layout.item_message_assistant, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AssistantViewHolder) {
            ((AssistantViewHolder) holder).bind(msg);
        }
        // TypingViewHolder needs no binding — it's animated in XML
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── User message ViewHolder ───────────────────────────────────────────────
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        UserViewHolder(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tvContent);
            tvTime    = v.findViewById(R.id.tvTime);
        }
        void bind(Message msg) {
            tvContent.setText(msg.getContent());
            if (msg.getTime() != null) tvTime.setText(msg.getTime());
        }
    }

    // ── Assistant message ViewHolder ──────────────────────────────────────────
    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime, tvDisclaimer;
        AssistantViewHolder(View v) {
            super(v);
            tvContent    = v.findViewById(R.id.tvContent);
            tvTime       = v.findViewById(R.id.tvTime);
            tvDisclaimer = v.findViewById(R.id.tvDisclaimer);
        }
        void bind(Message msg) {
            tvContent.setText(msg.getContent());
            if (msg.getTime() != null) tvTime.setText(msg.getTime());
            tvDisclaimer.setVisibility(msg.isDisclaimer() ? View.VISIBLE : View.GONE);
        }
    }

    // ── Typing indicator ViewHolder ───────────────────────────────────────────
    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View v) { super(v); }
    }
}
