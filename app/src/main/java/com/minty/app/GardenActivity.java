package com.minty.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minty.app.databinding.ActivityGardenBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Main interactive Virtual Garden Activity.
 * Displays the user's active plant with live growth stages, interactive watering,
 * seed planting, plant switching, and progression to blooming.
 */
public class GardenActivity extends AppCompatActivity {

    private ActivityGardenBinding binding;
    private DatabaseHelper dbHelper;
    private GardenManager gardenManager;

    private final Handler demoHandler = new Handler(Looper.getMainLooper());
    private boolean isAutoGrowRunning = false;
    private Runnable autoGrowRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGardenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        gardenManager = GardenManager.getInstance(this);

        // Setup unified bottom navigation
        NavigationHelper.setupBottomNav(this, binding.bottomNavigationView, R.id.nav_garden);

        // Setup interaction listeners
        binding.btnWaterPlant.setOnClickListener(v -> performWateringAnimation());
        binding.btnPlantSeed.setOnClickListener(v -> showPlantSeedDialog());
        binding.btnSwitchPlant.setOnClickListener(v -> showSwitchPlantDialog());
        binding.layoutPlantName.setOnClickListener(v -> showRenamePlantDialog());

        binding.btnOpenCollection.setOnClickListener(v -> {
            Intent intent = new Intent(GardenActivity.this, GardenCollectionActivity.class);
            intent.putExtra(GardenCollectionActivity.EXTRA_TAB, GardenCollectionActivity.TAB_COLLECTION);
            startActivity(intent);
        });

        binding.btnOpenHistory.setOnClickListener(v -> {
            Intent intent = new Intent(GardenActivity.this, GardenCollectionActivity.class);
            intent.putExtra(GardenCollectionActivity.EXTRA_TAB, GardenCollectionActivity.TAB_HISTORY);
            startActivity(intent);
        });

        binding.btnOpenScrapbook.setOnClickListener(v -> {
            Intent intent = new Intent(GardenActivity.this, ScrapbookActivity.class);
            startActivity(intent);
        });

        // Garden Spirit Mascot Tap Interaction
        binding.ivActivePlant.setOnClickListener(v -> {
            v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }).start();
            String greeting = GardenSpiritManager.getInstance().getMascotGreeting(GardenActivity.this);
            Toast.makeText(GardenActivity.this, greeting, Toast.LENGTH_SHORT).show();
        });

        // Setup Showcase Mode demo listeners
        binding.btnToggleShowcaseMode.setOnClickListener(v -> {
            boolean current = ShowcaseManager.isDemoModeEnabled(this);
            boolean newMode = !current;
            ShowcaseManager.setDemoModeEnabled(this, newMode);
            if (!newMode) {
                stopAutoGrow();
            }
            refreshGardenUI();
            Toast.makeText(this, newMode ? "Switched to SHOWCASE MODE (Demo ⚡)" : "Switched to NORMAL MODE (Real Progress 🌱)", Toast.LENGTH_SHORT).show();
        });

        binding.btnShowcaseToggleGrow.setOnClickListener(v -> toggleAutoGrow());
        binding.btnShowcaseNextStage.setOnClickListener(v -> advanceDemoStage());
        binding.btnShowcaseBloom.setOnClickListener(v -> triggerDemoBloom());
        binding.btnShowcaseReset.setOnClickListener(v -> resetDemoGarden());

        // Initial render
        refreshGardenUI();
        startGentlePlantSway();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoGrow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigationView.setSelectedItemId(R.id.nav_garden);
        refreshGardenUI();
    }

    private void refreshGardenUI() {
        boolean isDemo = ShowcaseManager.isDemoModeEnabled(this);
        binding.cardShowcaseBanner.setVisibility(isDemo ? View.VISIBLE : View.GONE);
        if (isDemo) {
            binding.btnToggleShowcaseMode.setText("🌱 Normal Mode");
        } else {
            binding.btnToggleShowcaseMode.setText("🧪 Demo Mode");
        }

        GardenPlant activePlant;
        if (isDemo) {
            activePlant = ShowcaseManager.getDemoPlant(this);
            binding.tvShowcaseSpeedLabel.setText("Speed: " + ShowcaseManager.getGrowthSpeedSeconds(this) + "s (+15 pts)");
            binding.tvShowcaseStatusMessage.setText(ShowcaseManager.getEncouragingMessage(activePlant));

            binding.tvGardenLevelHeader.setText("✨ SHOWCASE DEMO • Level 5");
            binding.tvBloomedCountHeader.setText(activePlant.isBloomed() ? "🌸 1 Bloomed (Demo)" : "🌸 0 Bloomed (Demo)");
            binding.tvSeedsCountHeader.setText("🌰 Demo Seeds Active");
        } else {
            activePlant = dbHelper.getActivePlant();
            GardenProgress progress = gardenManager.getGardenProgress();

            // 1. Top Header Stats
            binding.tvGardenLevelHeader.setText("🌱 Level " + progress.getGardenLevel() + " • " + getGardenLevelTitle(progress.getGardenLevel()));
            binding.tvBloomedCountHeader.setText("🌸 " + progress.getTotalFlowersBloomed() + " Bloomed");
            binding.tvSeedsCountHeader.setText("🌰 " + progress.getAvailableSeedCount() + " Seeds");
        }

        if (activePlant == null) return;

        // 2. Active Plant Details
        binding.tvActivePlantName.setText(activePlant.getName());
        binding.tvActivePlantStage.setText(activePlant.getSpeciesDisplayName() + " • " + activePlant.getStageName() + " Stage");

        // 3. Render Vector Graphic based on species and stage
        int drawableRes = PlantDrawableHelper.getPlantDrawable(activePlant.getSpecies(), activePlant.getStage());
        binding.ivActivePlant.setImageResource(drawableRes);

        // 4. Growth Progress Calculation
        int currentPoints = activePlant.getGrowthPoints();
        int basePoints = activePlant.getCurrentStageBasePoints();
        int targetPoints = activePlant.getNextStageThreshold();

        if (activePlant.isBloomed()) {
            binding.tvGrowthLabel.setText("Full Bloom");
            binding.tvGrowthFraction.setText(currentPoints + " pts (Max)");
            binding.pbGrowthProgress.setProgress(100);
            binding.tvGrowthHint.setText("🌸 " + activePlant.getName() + " is in full bloom! What a beautiful achievement!");
        } else {
            binding.tvGrowthLabel.setText("Stage Progress");
            binding.tvGrowthFraction.setText(currentPoints + " / " + targetPoints + " pts");

            int stageSpan = Math.max(1, targetPoints - basePoints);
            int pointsIntoStage = Math.max(0, currentPoints - basePoints);
            int percent = Math.min(100, Math.max(0, (pointsIntoStage * 100) / stageSpan));
            binding.pbGrowthProgress.setProgress(percent);

            binding.tvGrowthHint.setText(isDemo
                    ? "Demo mode: " + (targetPoints - currentPoints) + " pts to next stage. Auto-grow accelerates progress!"
                    : "Earn " + (targetPoints - currentPoints) + " more focus minutes to advance to next stage!");
        }

        // 5. Goals summary
        if (isDemo) {
            binding.tvGoalsSummary.setText("• Demo Auto-Grow: " + (isAutoGrowRunning ? "Running ▶ (+15 pts)" : "Paused ⏸")
                    + "\n• Isolated demo garden (Zero impact on real stats)");
        } else {
            List<GardenGoal> goals = dbHelper.getAllGoals();
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (GardenGoal g : goals) {
                if (count < 2) {
                    sb.append("• ").append(g.getTitle()).append(" (")
                            .append(g.getCurrentProgress()).append("/").append(g.getTarget()).append(")")
                            .append(g.isCompleted() ? " ✅ Done\n" : "\n");
                    count++;
                }
            }
            binding.tvGoalsSummary.setText(sb.toString().trim());
        }
    }

    private void performWateringAnimation() {
        binding.tvWaterAnimation.setVisibility(View.VISIBLE);
        binding.tvWaterAnimation.setAlpha(1.0f);
        binding.tvWaterAnimation.setTranslationY(-40f);

        binding.tvWaterAnimation.animate()
                .translationY(20f)
                .alpha(0.0f)
                .setDuration(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.tvWaterAnimation.setVisibility(View.GONE);
                    }
                })
                .start();

        if (ShowcaseManager.isDemoModeEnabled(this)) {
            boolean justBloomed = ShowcaseManager.waterDemoPlant(this);
            refreshGardenUI();
            if (justBloomed) {
                showBloomCelebrationDialog(ShowcaseManager.getDemoPlant(this));
            }
            Toast.makeText(this, "Demo plant refreshed with love! (+5 demo pts) 💧", Toast.LENGTH_SHORT).show();
        } else {
            gardenManager.waterActivePlant();
            Toast.makeText(this, "Plant refreshed with care! 💧", Toast.LENGTH_SHORT).show();
        }

        // Record Daily Quest progress
        try {
            DailyQuestManager.getInstance(this).recordProgress(DailyQuestManager.TYPE_GARDEN, 1);
        } catch (Exception e) {
            android.util.Log.e("GardenActivity", "Error recording garden quest progress: " + e.getMessage());
        }
    }

    private void startGentlePlantSway() {
        binding.ivActivePlant.animate()
                .rotation(2.5f)
                .setDuration(1800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    private boolean left = false;
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        left = !left;
                        binding.ivActivePlant.animate()
                                .rotation(left ? -2.5f : 2.5f)
                                .setDuration(1800)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setListener(this)
                                .start();
                    }
                })
                .start();
    }

    private void showPlantSeedDialog() {
        boolean isDemo = ShowcaseManager.isDemoModeEnabled(this);
        List<GardenSeed> inventory = isDemo ? ShowcaseManager.getDemoSeedInventory(this) : dbHelper.getSeedInventory();
        List<GardenSeed> available = new ArrayList<>();
        List<String> displayList = new ArrayList<>();

        for (GardenSeed s : inventory) {
            if (s.getCount() > 0) {
                available.add(s);
                displayList.add(s.getDisplayName() + " (" + s.getCount() + " available)");
            }
        }

        if (available.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("No Seeds Available")
                    .setMessage(isDemo ? "All demo seeds planted! Tap 🔄 to reset demo seeds." : "Complete focus sessions or daily study goals to earn more seeds!")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_seed, null);
        Spinner spinner = dialogView.findViewById(R.id.spinnerSeeds);
        EditText etName = dialogView.findViewById(R.id.etPlantName);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayList);
        spinner.setAdapter(adapter);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Plant in Pot 🌱", (dialog, which) -> {
                    int selectedPos = spinner.getSelectedItemPosition();
                    if (selectedPos >= 0 && selectedPos < available.size()) {
                        GardenSeed chosenSeed = available.get(selectedPos);
                        String customName = etName.getText().toString().trim();
                        if (isDemo) {
                            ShowcaseManager.plantDemoSeed(this, chosenSeed.getSpecies(), customName);
                            Toast.makeText(this, "Planted " + (customName.isEmpty() ? chosenSeed.getDisplayName() : customName) + "! 🌱 (Demo)", Toast.LENGTH_SHORT).show();
                            refreshGardenUI();
                        } else {
                            boolean success = gardenManager.plantNewSeed(chosenSeed.getSpecies(), customName);
                            if (success) {
                                Toast.makeText(this, "Planted " + (customName.isEmpty() ? chosenSeed.getDisplayName() : customName) + "! 🌱", Toast.LENGTH_SHORT).show();
                                refreshGardenUI();
                            } else {
                                Toast.makeText(this, "Could not plant seed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSwitchPlantDialog() {
        if (ShowcaseManager.isDemoModeEnabled(this)) {
            Toast.makeText(this, "Showcase Mode demonstrates the active flower in your pot. Plant a demo seed to change flower varieties! 🌰", Toast.LENGTH_LONG).show();
            return;
        }

        List<GardenPlant> plants = dbHelper.getAllPlants();
        if (plants.size() <= 1) {
            Toast.makeText(this, "You only have one plant. Plant more seeds to switch! 🌰", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] plantNames = new String[plants.size()];
        int selectedIndex = 0;
        for (int i = 0; i < plants.size(); i++) {
            GardenPlant p = plants.get(i);
            plantNames[i] = p.getName() + " (" + p.getSpeciesDisplayName() + " • " + p.getStageName() + ")";
            if (p.isSelected()) {
                selectedIndex = i;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Active Flower to Nurture")
                .setSingleChoiceItems(plantNames, selectedIndex, (dialog, which) -> {
                    gardenManager.selectPlant(plants.get(which).getPlantId());
                    refreshGardenUI();
                    dialog.dismiss();
                    Toast.makeText(this, "Active plant switched to " + plants.get(which).getName() + "!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenamePlantDialog() {
        boolean isDemo = ShowcaseManager.isDemoModeEnabled(this);
        GardenPlant activePlant = isDemo ? ShowcaseManager.getDemoPlant(this) : dbHelper.getActivePlant();
        if (activePlant == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_plant, null);
        EditText etName = dialogView.findViewById(R.id.etRenamePlant);
        etName.setText(activePlant.getName());
        etName.setSelection(activePlant.getName().length());

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (isDemo) {
                            ShowcaseManager.renameDemoPlant(this, newName);
                        } else {
                            gardenManager.renameActivePlant(newName);
                        }
                        refreshGardenUI();
                        Toast.makeText(this, "Plant renamed to " + newName + "!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // SHOWCASE AUTO-GROW & DEMO CONTROLS
    // =========================================================================

    private void toggleAutoGrow() {
        if (isAutoGrowRunning) {
            stopAutoGrow();
            Toast.makeText(this, "Demo growth paused ⏸️", Toast.LENGTH_SHORT).show();
        } else {
            startAutoGrow();
        }
    }

    private void startAutoGrow() {
        isAutoGrowRunning = true;
        binding.btnShowcaseToggleGrow.setText("⏸ Pause");
        binding.btnShowcaseToggleGrow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.flower_orange));

        int speedSeconds = ShowcaseManager.getGrowthSpeedSeconds(this);
        autoGrowRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAutoGrowRunning || !ShowcaseManager.isDemoModeEnabled(GardenActivity.this)) {
                    stopAutoGrow();
                    return;
                }

                boolean justBloomed = ShowcaseManager.addDemoGrowthPoints(GardenActivity.this, 15);
                refreshGardenUI();

                if (justBloomed) {
                    GardenPlant plant = ShowcaseManager.getDemoPlant(GardenActivity.this);
                    showBloomCelebrationDialog(plant);
                    stopAutoGrow();
                } else {
                    int interval = ShowcaseManager.getGrowthSpeedSeconds(GardenActivity.this);
                    demoHandler.postDelayed(this, interval * 1000L);
                }
            }
        };

        demoHandler.postDelayed(autoGrowRunnable, speedSeconds * 1000L);
        refreshGardenUI();
        Toast.makeText(this, "Auto-growth started! (+15 pts every " + speedSeconds + "s)", Toast.LENGTH_SHORT).show();
    }

    private void stopAutoGrow() {
        isAutoGrowRunning = false;
        demoHandler.removeCallbacksAndMessages(null);
        if (binding != null && binding.btnShowcaseToggleGrow != null) {
            binding.btnShowcaseToggleGrow.setText("▶ Start Grow");
            binding.btnShowcaseToggleGrow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.aspen_green));
        }
        refreshGardenUI();
    }

    private void advanceDemoStage() {
        boolean justBloomed = ShowcaseManager.advanceToNextStage(this);
        refreshGardenUI();
        GardenPlant plant = ShowcaseManager.getDemoPlant(this);
        if (justBloomed) {
            showBloomCelebrationDialog(plant);
        } else {
            Toast.makeText(this, plant.getName() + " reached " + plant.getStageName() + "! 🌱", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerDemoBloom() {
        GardenPlant plant = ShowcaseManager.isDemoModeEnabled(this)
                ? ShowcaseManager.getDemoPlant(this)
                : dbHelper.getActivePlant();
        showBloomCelebrationDialog(plant);
    }

    private void resetDemoGarden() {
        stopAutoGrow();
        ShowcaseManager.resetDemoGarden(this);
        refreshGardenUI();
        Toast.makeText(this, "Demo garden reset to initial seed! 🌱 (Real data untouched)", Toast.LENGTH_SHORT).show();
    }

    private void showBloomCelebrationDialog(GardenPlant plant) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bloom_celebration, null);
        ImageView ivFlower = dialogView.findViewById(R.id.ivCelebrationFlower);
        TextView tvMessage = dialogView.findViewById(R.id.tvCelebrationMessage);
        TextView tvReward = dialogView.findViewById(R.id.tvCelebrationReward);

        int flowerRes = PlantDrawableHelper.getBloomedFlowerDrawable(plant != null ? plant.getSpecies() : "mint");
        ivFlower.setImageResource(flowerRes);

        String plantName = (plant != null && plant.getName() != null) ? plant.getName() : "Your plant";
        tvMessage.setText(plantName + " has bloomed!\nWhat a beautiful achievement!\nYou planted consistency and grew something truly beautiful.");

        if (ShowcaseManager.isDemoModeEnabled(this)) {
            tvReward.setText("✨ Demo Bloom Achieved (Showcase Mode)");
        }

        // Engaging pop-in scale animation
        ivFlower.setScaleX(0.6f);
        ivFlower.setScaleY(0.6f);
        ivFlower.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(450)
                .withEndAction(() -> ivFlower.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start())
                .start();

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Wonderful! 🌸", null)
                .show();
    }

    private String getGardenLevelTitle(int level) {
        switch (level) {
            case 1: return "Little Seed";
            case 2: return "Tiny Sprout";
            case 3: return "Growing Garden";
            case 4: return "Blooming Garden";
            case 5:
            default: return "Flourishing Garden";
        }
    }
}
