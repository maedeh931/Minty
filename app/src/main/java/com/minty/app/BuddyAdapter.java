package com.minty.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ItemBuddyBinding;

import java.util.List;

/**
 * Adapter for displaying connected buddies.
 */
public class BuddyAdapter extends RecyclerView.Adapter<BuddyAdapter.BuddyViewHolder> {

    public interface OnBuddyActionListener {
        void onViewDashboard(UserProfile buddy);
        void onSendEncouragement(UserProfile buddy);
        void onTogglePrimary(UserProfile buddy, boolean currentlyPrimary);
        void onRemoveBuddy(UserProfile buddy);
    }

    private List<UserProfile> buddies;
    private String primaryBuddyUid;
    private final OnBuddyActionListener listener;

    public BuddyAdapter(List<UserProfile> buddies, String primaryBuddyUid, OnBuddyActionListener listener) {
        this.buddies = buddies;
        this.primaryBuddyUid = primaryBuddyUid;
        this.listener = listener;
    }

    public void updateData(List<UserProfile> buddies, String primaryBuddyUid) {
        this.buddies = buddies;
        this.primaryBuddyUid = primaryBuddyUid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BuddyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuddyBinding binding = ItemBuddyBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new BuddyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BuddyViewHolder holder, int position) {
        UserProfile buddy = buddies.get(position);
        boolean isPrimary = buddy.getUid().equals(primaryBuddyUid);

        holder.binding.tvBuddyDisplayName.setText(buddy.getDisplayName());
        holder.binding.tvBuddyUsername.setText(buddy.getUsername());
        holder.binding.tvBuddyLevel.setText(buddy.getTitleLevel() + " 🌱");

        // Primary badge visibility
        holder.binding.tvPrimaryBadge.setVisibility(isPrimary ? View.VISIBLE : View.GONE);
        holder.binding.btnTogglePrimary.setText(isPrimary ? "Remove Primary" : "Set as Primary ★");

        holder.binding.btnViewDashboard.setOnClickListener(v -> {
            if (listener != null) listener.onViewDashboard(buddy);
        });

        holder.binding.btnSendEncouragement.setOnClickListener(v -> {
            if (listener != null) listener.onSendEncouragement(buddy);
        });

        holder.binding.btnTogglePrimary.setOnClickListener(v -> {
            if (listener != null) listener.onTogglePrimary(buddy, isPrimary);
        });

        holder.binding.btnRemoveBuddy.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveBuddy(buddy);
        });
    }

    @Override
    public int getItemCount() {
        return buddies != null ? buddies.size() : 0;
    }

    public static class BuddyViewHolder extends RecyclerView.ViewHolder {
        final ItemBuddyBinding binding;

        public BuddyViewHolder(@NonNull ItemBuddyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
