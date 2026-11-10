package com.minty.app;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ItemBuddyMessageBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for two-way encouragement messages.
 */
public class BuddyMessageAdapter extends RecyclerView.Adapter<BuddyMessageAdapter.MessageViewHolder> {

    private List<BuddyMessage> messages;
    private final String currentUid;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public BuddyMessageAdapter(List<BuddyMessage> messages, String currentUid) {
        this.messages = messages;
        this.currentUid = currentUid;
    }

    public void setMessages(List<BuddyMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuddyMessageBinding binding = ItemBuddyMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        BuddyMessage message = messages.get(position);
        boolean isMe = currentUid != null && currentUid.equals(message.getSenderUid());

        holder.binding.tvMessageBody.setText(message.getMessage());
        holder.binding.tvMessageTime.setText(timeFormat.format(new Date(message.getCreatedAt())));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.binding.cardMessageBubble.getLayoutParams();

        if (isMe) {
            holder.binding.layoutMessageContainer.setGravity(Gravity.END);
            params.gravity = Gravity.END;
            holder.binding.tvMessageSender.setText("You");
            holder.binding.cardMessageBubble.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.mint)
            );
            holder.binding.tvReadReceipt.setVisibility(View.VISIBLE);
            holder.binding.tvReadReceipt.setText(message.isRead() ? "Read ✓✓" : "Sent ✓");
        } else {
            holder.binding.layoutMessageContainer.setGravity(Gravity.START);
            params.gravity = Gravity.START;
            holder.binding.tvMessageSender.setText(message.getSenderName());
            holder.binding.cardMessageBubble.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.card_white)
            );
            holder.binding.tvReadReceipt.setVisibility(View.GONE);
        }
        holder.binding.cardMessageBubble.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        final ItemBuddyMessageBinding binding;

        public MessageViewHolder(@NonNull ItemBuddyMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
