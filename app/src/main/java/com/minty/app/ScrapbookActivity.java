package com.minty.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.minty.app.databinding.ActivityScrapbookBinding;

import java.util.List;

/**
 * Achievement Scrapbook Activity.
 * Unites the three pillars of personal development in Minty:
 * 1. Botanical Flower Album (Virtual Garden Collection)
 * 2. Memory Library Chronicles (Milestones & Reflections)
 * 3. Daily Quests & Garden Spirit Whispers
 */
public class ScrapbookActivity extends AppCompatActivity implements MemoryAdapter.OnMemoryInteractionListener {

    private ActivityScrapbookBinding binding;
    private DatabaseHelper dbHelper;
    private MemoryAdapter memoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScrapbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbarScrapbook.setNavigationOnClickListener(v -> finish());

        setupHeaderStats();
        setupFlowerAlbum();
        setupMemoryMilestones();
        setupDailyQuestsSection();
    }

    private void setupHeaderStats() {
        int balance = dbHelper.getBalance();
        int streak = StreakManager.getCurrentStreak(this);
        List<MemoryItem> memories = dbHelper.getAllMemories();
        binding.tvScrapbookStats.setText("🌿 " + balance + " Minty Minutes  •  🔥 " + streak + "-day streak  •  ✨ " + memories.size() + " Memories");
    }

    private void setupFlowerAlbum() {
        List<GardenPlant> plants = dbHelper.getAllPlants();
        binding.rvScrapbookFlowers.setLayoutManager(new GridLayoutManager(this, 2));
        GardenPlantAdapter plantAdapter = new GardenPlantAdapter(plants, plant -> {
            Toast.makeText(this, plant.getName() + " (" + plant.getSpecies() + ")", Toast.LENGTH_SHORT).show();
        });
        binding.rvScrapbookFlowers.setAdapter(plantAdapter);
    }

    private void setupMemoryMilestones() {
        List<MemoryItem> memories = dbHelper.getAllMemories();
        binding.rvScrapbookMemories.setLayoutManager(new LinearLayoutManager(this));
        memoryAdapter = new MemoryAdapter(memories, this);
        binding.rvScrapbookMemories.setAdapter(memoryAdapter);
    }

    private void setupDailyQuestsSection() {
        binding.layoutQuestsContainer.removeAllViews();
        List<DailyQuest> quests = DailyQuestManager.getInstance(this).getTodayQuests();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DailyQuest quest : quests) {
            View questView = inflater.inflate(R.layout.item_daily_quest_card, binding.layoutQuestsContainer, false);

            TextView tvTitle = questView.findViewById(R.id.tvQuestTitle);
            TextView tvDesc = questView.findViewById(R.id.tvQuestDesc);
            TextView tvProgress = questView.findViewById(R.id.tvQuestProgress);
            ProgressBar pbProgress = questView.findViewById(R.id.pbQuestProgress);
            MaterialButton btnClaim = questView.findViewById(R.id.btnClaimQuest);

            tvTitle.setText(quest.getIconEmoji() + " " + quest.getTitle());
            tvDesc.setText(quest.getDescription());
            tvProgress.setText(quest.getCurrentProgress() + " / " + quest.getTargetCount());
            pbProgress.setProgress(quest.getProgressPercent());

            if (quest.isClaimed()) {
                btnClaim.setText("Claimed ✅");
                btnClaim.setEnabled(false);
                btnClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD2E3C8));
            } else if (quest.isCompleted()) {
                btnClaim.setText("Claim +" + quest.getRewardMinutes() + "m 🎉");
                btnClaim.setEnabled(true);
                btnClaim.setOnClickListener(v -> {
                    String rewardMsg = DailyQuestManager.getInstance(ScrapbookActivity.this).claimReward(quest.getId());
                    if (rewardMsg != null) {
                        Toast.makeText(ScrapbookActivity.this, rewardMsg, Toast.LENGTH_SHORT).show();
                        setupDailyQuestsSection();
                        setupHeaderStats();
                    }
                });
            } else {
                btnClaim.setText("+" + quest.getRewardMinutes() + "m");
                btnClaim.setEnabled(false);
            }

            binding.layoutQuestsContainer.addView(questView);
        }
    }

    @Override
    public void onFavoriteToggle(MemoryItem memory) {
        boolean newFav = !memory.isFavorite();
        memory.setFavorite(newFav);
        MemoryManager.getInstance(this).toggleFavorite(memory.getMemoryId(), newFav);
        if (memoryAdapter != null) {
            memoryAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onEditNote(MemoryItem memory) {
        Toast.makeText(this, "Reflection note: " + (memory.getPersonalNote().isEmpty() ? "No personal note yet" : memory.getPersonalNote()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShare(MemoryItem memory) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "🌱 Milestone in Minty: " + memory.getTitle() + "\n" + memory.getDescription());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Milestone"));
    }
}
