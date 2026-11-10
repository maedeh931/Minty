package com.minty.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ItemUserSearchBinding;

import java.util.List;

/**
 * Adapter for user search results.
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.SearchViewHolder> {

    public interface OnUserSelectedListener {
        void onAddBuddy(UserProfile user);
    }

    private List<UserProfile> users;
    private final OnUserSelectedListener listener;

    public UserSearchAdapter(List<UserProfile> users, OnUserSelectedListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void setUsers(List<UserProfile> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserSearchBinding binding = ItemUserSearchBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SearchViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.binding.tvSearchDisplayName.setText(user.getDisplayName());
        holder.binding.tvSearchUsername.setText(user.getUsername());
        holder.binding.tvSearchCode.setText("Code: " + user.getInvitationCode() + " • " + user.getTitleLevel());

        holder.binding.btnAddBuddy.setOnClickListener(v -> {
            if (listener != null) listener.onAddBuddy(user);
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        final ItemUserSearchBinding binding;

        public SearchViewHolder(@NonNull ItemUserSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
