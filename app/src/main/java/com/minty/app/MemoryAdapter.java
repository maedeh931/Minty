package com.minty.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.MemoryItemBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for rendering memory scrapbook milestone cards in The Little Memory Library.
 */
public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    public interface OnMemoryInteractionListener {
        void onFavoriteToggle(MemoryItem memory);
        void onEditNote(MemoryItem memory);
        void onShare(MemoryItem memory);
    }

    private List<MemoryItem> memoryList;
    private final OnMemoryInteractionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public MemoryAdapter(List<MemoryItem> memoryList, OnMemoryInteractionListener listener) {
        this.memoryList = memoryList;
        this.listener = listener;
    }

    public void updateList(List<MemoryItem> newList) {
        this.memoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MemoryItemBinding binding = MemoryItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new MemoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        holder.bind(memoryList.get(position));
    }

    @Override
    public int getItemCount() {
        return memoryList == null ? 0 : memoryList.size();
    }

    class MemoryViewHolder extends RecyclerView.ViewHolder {

        private final MemoryItemBinding binding;

        MemoryViewHolder(MemoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MemoryItem memory) {
            Context context = itemView.getContext();

            // Emoji icon
            binding.tvMemoryEmoji.setText(memory.getEmojiIcon() != null && !memory.getEmojiIcon().isEmpty()
                    ? memory.getEmojiIcon() : "🌱");

            // Title
            binding.tvMemoryTitle.setText(memory.getTitle());

            // Date & category subtitle
            String dateFormatted = dateFormat.format(new Date(memory.getTimestamp()));
            String categoryDisplay = formatCategoryDisplay(memory.getCategory());
            binding.tvMemoryDateCategory.setText(dateFormatted + " • " + categoryDisplay);

            // Description story
            binding.tvMemoryDescription.setText(memory.getDescription());

            // Personal reflection note
            if (!TextUtils.isEmpty(memory.getPersonalNote())) {
                binding.tvPersonalNote.setText(memory.getPersonalNote());
                binding.tvPersonalNote.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
            } else {
                binding.tvPersonalNote.setText("Tap to add your personal thoughts...");
                binding.tvPersonalNote.setTextColor(ContextCompat.getColor(context, R.color.aspen_green_dark));
            }

            binding.cardPersonalNote.setOnClickListener(v -> {
                if (listener != null) listener.onEditNote(memory);
            });

            // Favorite button
            if (memory.isFavorite()) {
                binding.btnFavoriteMemory.setImageResource(R.drawable.ic_favorite_filled);
                binding.btnFavoriteMemory.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
            } else {
                binding.btnFavoriteMemory.setImageResource(R.drawable.ic_favorite_border);
                binding.btnFavoriteMemory.setColorFilter(ContextCompat.getColor(context, R.color.aspen_green_dark));
            }

            binding.btnFavoriteMemory.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteToggle(memory);
            });

            // Share button
            binding.btnShareMemory.setOnClickListener(v -> {
                if (listener != null) listener.onShare(memory);
            });
        }

        private String formatCategoryDisplay(String category) {
            if (category == null) return "Milestone";
            switch (category) {
                case MemoryItem.CATEGORY_BLOOM:
                    return "Garden Bloom 🌸";
                case MemoryItem.CATEGORY_FOCUS:
                    return "Focus Milestone 🏆";
                case MemoryItem.CATEGORY_DAILY_PLAN:
                    return "Daily Plan 📋";
                case MemoryItem.CATEGORY_STREAK:
                    return "Streak 🌿";
                case MemoryItem.CATEGORY_CHALLENGE:
                    return "Challenge 🎯";
                case MemoryItem.CATEGORY_REFLECTION:
                    return "Weekly Reflection 💭";
                case MemoryItem.CATEGORY_WELCOME_BACK:
                    return "Welcome Back 🌱";
                default:
                    return "Milestone";
            }
        }
    }
}
