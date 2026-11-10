package com.minty.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.minty.app.databinding.ActivityGardenCollectionBinding;

import java.util.List;

/**
 * Garden Collection & History Activity.
 * Allows users to inspect their plant collection, seed inventory, unlocked achievements,
 * and chronological garden events log.
 */
public class GardenCollectionActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "extra_tab";
    public static final int TAB_COLLECTION = 0;
    public static final int TAB_SEEDS = 1;
    public static final int TAB_ACHIEVEMENTS = 2;
    public static final int TAB_HISTORY = 3;

    private ActivityGardenCollectionBinding binding;
    private DatabaseHelper dbHelper;
    private int currentTab = TAB_COLLECTION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGardenCollectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbarCollection.setNavigationOnClickListener(v -> finish());
        binding.rvCollectionItems.setLayoutManager(new LinearLayoutManager(this));

        // Tab click listeners
        binding.btnTabPlants.setOnClickListener(v -> switchTab(TAB_COLLECTION));
        binding.btnTabSeeds.setOnClickListener(v -> switchTab(TAB_SEEDS));
        binding.btnTabAchievements.setOnClickListener(v -> switchTab(TAB_ACHIEVEMENTS));
        binding.btnTabHistory.setOnClickListener(v -> switchTab(TAB_HISTORY));

        int targetTab = getIntent().getIntExtra(EXTRA_TAB, TAB_COLLECTION);
        switchTab(targetTab);
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        updateTabButtonStyles();

        // Demonstrating LayoutManager variants: GridLayoutManager, StaggeredGridLayoutManager, LinearLayoutManager
        if (tab == TAB_COLLECTION) {
            binding.rvCollectionItems.setLayoutManager(new GridLayoutManager(this, 2));
            loadPlantsTab();
        } else if (tab == TAB_SEEDS) {
            binding.rvCollectionItems.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            loadSeedsTab();
        } else if (tab == TAB_ACHIEVEMENTS) {
            binding.rvCollectionItems.setLayoutManager(new LinearLayoutManager(this));
            loadAchievementsTab();
        } else if (tab == TAB_HISTORY) {
            binding.rvCollectionItems.setLayoutManager(new LinearLayoutManager(this));
            loadHistoryTab();
        }
    }

    private void updateTabButtonStyles() {
        styleTab(binding.btnTabPlants, currentTab == TAB_COLLECTION);
        styleTab(binding.btnTabSeeds, currentTab == TAB_SEEDS);
        styleTab(binding.btnTabAchievements, currentTab == TAB_ACHIEVEMENTS);
        styleTab(binding.btnTabHistory, currentTab == TAB_HISTORY);
    }

    private void styleTab(MaterialButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.aspen_green));
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
            button.setTextColor(ContextCompat.getColor(this, R.color.aspen_green_dark));
            button.setStrokeColor(ContextCompat.getColorStateList(this, R.color.aspen_green_dark));
            button.setStrokeWidth(2);
        }
    }

    private void loadPlantsTab() {
        List<GardenPlant> plants = dbHelper.getAllPlants();
        if (plants.isEmpty()) {
            showEmpty("No plants in your garden yet.");
        } else {
            hideEmpty();
            GardenPlantAdapter adapter = new GardenPlantAdapter(plants, plant -> {
                // Clicking sets plant active
                GardenManager.getInstance(this).selectPlant(plant.getPlantId());
                finish(); // Returns to GardenActivity with selected plant!
            });
            binding.rvCollectionItems.setAdapter(adapter);
        }
    }

    private void loadSeedsTab() {
        List<GardenSeed> seeds = dbHelper.getSeedInventory();
        if (seeds.isEmpty()) {
            showEmpty("No seeds in inventory.");
        } else {
            hideEmpty();
            GardenSeedAdapter adapter = new GardenSeedAdapter(seeds);
            binding.rvCollectionItems.setAdapter(adapter);
        }
    }

    private void loadAchievementsTab() {
        List<GardenAchievement> achievements = dbHelper.getAllAchievements();
        if (achievements.isEmpty()) {
            showEmpty("No achievements found.");
        } else {
            hideEmpty();
            GardenAchievementAdapter adapter = new GardenAchievementAdapter(achievements);
            binding.rvCollectionItems.setAdapter(adapter);
        }
    }

    private void loadHistoryTab() {
        List<GardenHistoryItem> history = dbHelper.getGardenHistory();
        if (history.isEmpty()) {
            showEmpty("No garden activity logged yet.\nComplete focus sessions to start growing!");
        } else {
            hideEmpty();
            GardenHistoryAdapter adapter = new GardenHistoryAdapter(history);
            binding.rvCollectionItems.setAdapter(adapter);
        }
    }

    private void showEmpty(String message) {
        binding.rvCollectionItems.setVisibility(View.GONE);
        binding.layoutEmptyCollection.setVisibility(View.VISIBLE);
        binding.tvEmptyMessage.setText(message);
    }

    private void hideEmpty() {
        binding.rvCollectionItems.setVisibility(View.VISIBLE);
        binding.layoutEmptyCollection.setVisibility(View.GONE);
    }
}
