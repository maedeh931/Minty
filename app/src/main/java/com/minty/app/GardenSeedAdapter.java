package com.minty.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.minty.app.databinding.ItemGardenSeedBinding;
import java.util.List;

public class GardenSeedAdapter extends RecyclerView.Adapter<GardenSeedAdapter.SeedViewHolder> {

    private final List<GardenSeed> seeds;

    public GardenSeedAdapter(List<GardenSeed> seeds) {
        this.seeds = seeds;
    }

    @NonNull
    @Override
    public SeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGardenSeedBinding binding = ItemGardenSeedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SeedViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SeedViewHolder holder, int position) {
        GardenSeed seed = seeds.get(position);
        holder.binding.tvSeedName.setText(seed.getDisplayName());
        holder.binding.tvSeedDescription.setText(seed.getDescription());
        holder.binding.tvSeedCount.setText("x " + seed.getCount());

        if (seed.isUnlocked()) {
            holder.binding.tvSeedRequirement.setText("✅ Unlocked");
            holder.binding.tvSeedEmoji.setText("🌰");
        } else {
            holder.binding.tvSeedRequirement.setText("🔒 " + seed.getUnlockRequirement());
            holder.binding.tvSeedEmoji.setText("🔒");
        }
    }

    @Override
    public int getItemCount() {
        return seeds.size();
    }

    static class SeedViewHolder extends RecyclerView.ViewHolder {
        final ItemGardenSeedBinding binding;
        SeedViewHolder(ItemGardenSeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
