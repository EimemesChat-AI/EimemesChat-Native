package com.eimemes.chat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.R;
import com.eimemes.chat.models.Conversation;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conv);
    }

    private final List<Conversation> conversations;
    private final OnConversationClickListener listener;
    private String activeId = null;

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener      = listener;
    }

    public void setActiveId(String id) {
        this.activeId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        holder.tvTitle.setText(conv.getTitle());
        boolean isActive = conv.getId().equals(activeId);
        holder.itemView.setSelected(isActive);
        holder.tvTitle.setAlpha(isActive ? 1f : 0.75f);
        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conv));
    }

    @Override
    public int getItemCount() { return conversations.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
        }
    }
}
