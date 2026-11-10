package com.minty.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.minty.app.databinding.ItemJournalEntryBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {

    private final List<JournalEntry> entries;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    public JournalAdapter(List<JournalEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJournalEntryBinding binding = ItemJournalEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new JournalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = entries.get(position);
        holder.binding.tvEntryTopic.setText(entry.getTopicWorkedOn());
        holder.binding.tvEntryDate.setText(dateFormat.format(new Date(entry.getTimestamp())));
        holder.binding.tvEntryTakeaway.setText("Key Accomplishment: " + entry.getKeyTakeaway());

        if (entry.getNotes() != null && !entry.getNotes().isEmpty()) {
            holder.binding.tvEntryNotes.setText("Notes: " + entry.getNotes());
            holder.binding.tvEntryNotes.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvEntryNotes.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        final ItemJournalEntryBinding binding;
        JournalViewHolder(ItemJournalEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
