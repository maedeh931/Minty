package com.minty.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ItemBuddyRequestBinding;

import java.util.List;

/**
 * Adapter for incoming and sent buddy requests.
 */
public class BuddyRequestAdapter extends RecyclerView.Adapter<BuddyRequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(BuddyRequest request);
        void onReject(BuddyRequest request);
        void onCancel(BuddyRequest request);
    }

    private List<BuddyRequest> requests;
    private final boolean isIncoming;
    private final OnRequestActionListener listener;

    public BuddyRequestAdapter(List<BuddyRequest> requests, boolean isIncoming, OnRequestActionListener listener) {
        this.requests = requests;
        this.isIncoming = isIncoming;
        this.listener = listener;
    }

    public void setRequests(List<BuddyRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuddyRequestBinding binding = ItemBuddyRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        BuddyRequest request = requests.get(position);

        if (isIncoming) {
            holder.binding.tvRequestName.setText(request.getSenderName());
            holder.binding.tvRequestUsername.setText(request.getSenderUsername());
            holder.binding.layoutIncomingActions.setVisibility(View.VISIBLE);
            holder.binding.layoutOutgoingActions.setVisibility(View.GONE);
        } else {
            holder.binding.tvRequestName.setText(request.getReceiverName());
            holder.binding.tvRequestUsername.setText("Pending Acceptance");
            holder.binding.layoutIncomingActions.setVisibility(View.GONE);
            holder.binding.layoutOutgoingActions.setVisibility(View.VISIBLE);
        }

        holder.binding.tvRequestStatus.setText(request.getStatus());

        holder.binding.btnAcceptRequest.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });

        holder.binding.btnRejectRequest.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });

        holder.binding.btnCancelRequest.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        final ItemBuddyRequestBinding binding;

        public RequestViewHolder(@NonNull ItemBuddyRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
