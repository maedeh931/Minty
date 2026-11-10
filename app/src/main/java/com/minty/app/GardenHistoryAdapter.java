package com.minty.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.minty.app.databinding.ItemGardenHistoryBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GardenHistoryAdapter extends RecyclerView.Adapter<GardenHistoryAdapter.HistoryViewHolder> {

    private final List<GardenHistoryItem> historyList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public GardenHistoryAdapter(List<GardenHistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGardenHistoryBinding binding = ItemGardenHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        GardenHistoryItem item = historyList.get(position);
        holder.binding.tvHistoryTitle.setText(item.getTitle());
        holder.binding.tvHistoryDesc.setText(item.getDescription());
        holder.binding.tvHistoryDate.setText(dateFormat.format(new Date(item.getTimestamp())));

        if (GardenHistoryItem.EVENT_BLOOMED.equals(item.getEventType())) {
            holder.binding.tvHistoryIcon.setText("🌸");
        } else if (GardenHistoryItem.EVENT_STAGE_UP.equals(item.getEventType())) {
            holder.binding.tvHistoryIcon.setText("🌱");
        } else if (GardenHistoryItem.EVENT_PLANTED.equals(item.getEventType())) {
            holder.binding.tvHistoryIcon.setText("🌰");
        } else if (GardenHistoryItem.EVENT_WATERED.equals(item.getEventType())) {
            holder.binding.tvHistoryIcon.setText("💧");
        } else {
            holder.binding.tvHistoryIcon.setText("🏆");
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ItemGardenHistoryBinding binding;
        HistoryViewHolder(ItemGardenHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
