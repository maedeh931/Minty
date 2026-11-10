package com.minty.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.minty.app.databinding.ItemGardenPlantBinding;
import java.util.List;

public class GardenPlantAdapter extends RecyclerView.Adapter<GardenPlantAdapter.PlantViewHolder> {

    private final List<GardenPlant> plants;
    private final OnPlantClickListener listener;

    public interface OnPlantClickListener {
        void onPlantClick(GardenPlant plant);
    }

    public GardenPlantAdapter(List<GardenPlant> plants, OnPlantClickListener listener) {
        this.plants = plants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGardenPlantBinding binding = ItemGardenPlantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PlantViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        GardenPlant plant = plants.get(position);
        holder.binding.tvPlantCardName.setText(plant.getName());
        holder.binding.tvPlantCardDetails.setText(plant.getSpeciesDisplayName() + " • " + plant.getStageName() + " (" + plant.getGrowthPoints() + " pts)");

        int iconRes = PlantDrawableHelper.getPlantDrawable(plant.getSpecies(), plant.getStage());
        holder.binding.ivPlantIcon.setImageResource(iconRes);

        int max = plant.getNextStageThreshold();
        int base = plant.getCurrentStageBasePoints();
        int span = Math.max(1, max - base);
        int pts = Math.max(0, plant.getGrowthPoints() - base);
        int percent = plant.isBloomed() ? 100 : Math.min(100, (pts * 100) / span);
        holder.binding.pbPlantCardProgress.setProgress(percent);

        if (plant.isSelected()) {
            holder.binding.tvPlantCardStatus.setText("Active in Pot 🌱");
            holder.binding.tvPlantCardStatus.setVisibility(View.VISIBLE);
        } else if (plant.isBloomed()) {
            holder.binding.tvPlantCardStatus.setText("Bloomed 🌸");
            holder.binding.tvPlantCardStatus.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvPlantCardStatus.setText("Growing 🪴");
            holder.binding.tvPlantCardStatus.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlantClick(plant);
        });
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        final ItemGardenPlantBinding binding;
        PlantViewHolder(ItemGardenPlantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
