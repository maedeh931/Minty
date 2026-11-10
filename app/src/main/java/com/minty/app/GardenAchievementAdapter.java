package com.minty.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.minty.app.databinding.ItemGardenAchievementBinding;
import java.util.List;

public class GardenAchievementAdapter extends RecyclerView.Adapter<GardenAchievementAdapter.AchViewHolder> {

    private final List<GardenAchievement> achievements;

    public GardenAchievementAdapter(List<GardenAchievement> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public AchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGardenAchievementBinding binding = ItemGardenAchievementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AchViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AchViewHolder holder, int position) {
        GardenAchievement ach = achievements.get(position);
        holder.binding.tvAchTitle.setText(ach.getTitle());
        holder.binding.tvAchDescription.setText(ach.getDescription());

        if (ach.isUnlocked()) {
            holder.binding.tvAchStatus.setText("Unlocked 🌟");
            holder.binding.tvAchBadge.setText("🏆");
        } else {
            holder.binding.tvAchStatus.setText("Locked 🔒");
            holder.binding.tvAchBadge.setText("🔒");
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    static class AchViewHolder extends RecyclerView.ViewHolder {
        final ItemGardenAchievementBinding binding;
        AchViewHolder(ItemGardenAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
