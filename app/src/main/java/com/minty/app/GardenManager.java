package com.minty.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.UUID;

/**
 * Central coordinator and growth engine for Minty's Virtual Garden.
 * Connects verified productivity (focus sessions & task completions) to plant growth,
 * prevents duplicate rewards via session deduplication, and manages seed planting & blooming.
 */
public class GardenManager {

    private static final String TAG = "GardenManager";
    public static final String CHANNEL_GARDEN = "minty_garden";

    private static volatile GardenManager instance;
    private final Context appContext;
    private final DatabaseHelper dbHelper;

    public interface GardenUpdateListener {
        void onGardenUpdated(GardenPlant activePlant, GardenProgress progress);
    }

    private GardenManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(appContext);
        createNotificationChannel();
    }

    public static synchronized GardenManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new GardenManager(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_GARDEN,
                    "Garden Milestones & Growth",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Celebrations when your plants grow, reach new stages, or bloom.");
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Primary entry point when a focus session completes.
     * Evaluates growth points, advances plant stages, updates goals, and triggers blooming.
     */
    public synchronized boolean onFocusSessionCompleted(@Nullable String sessionId, int minutes, @NonNull String sessionTitle) {
        if (minutes <= 0) return false;

        // 1. Session Deduplication Check
        if (sessionId != null && !sessionId.isEmpty()) {
            if (dbHelper.isSessionProcessed(sessionId)) {
                Log.w(TAG, "onFocusSessionCompleted: Session " + sessionId + " was already rewarded. Skipping duplicate.");
                return false;
            }
            dbHelper.markSessionProcessed(sessionId, minutes);
        }

        Log.d(TAG, "onFocusSessionCompleted: Processing " + minutes + " focus minutes for session " + sessionId);

        // 2. Fetch or initialize active plant
        GardenPlant plant = dbHelper.getActivePlant();
        if (plant == null) {
            plant = new GardenPlant(
                    "plant_" + UUID.randomUUID().toString().substring(0, 8),
                    "Minty",
                    GardenPlant.SPECIES_MINT,
                    15,
                    GardenPlant.STAGE_SPROUT,
                    System.currentTimeMillis(),
                    0,
                    true,
                    true
            );
            dbHelper.savePlant(plant);
        }

        int oldStage = plant.getStage();
        int newPoints = plant.getGrowthPoints() + minutes;
        plant.setGrowthPoints(newPoints);
        int newStage = plant.getStage();

        boolean stageAdvanced = newStage > oldStage;
        boolean justBloomed = (newStage >= GardenPlant.STAGE_BLOOMING) && (plant.getBloomedAt() == 0);

        if (justBloomed) {
            plant.setBloomedAt(System.currentTimeMillis());
            dbHelper.unlockAchievement("ach_first_bloom");
            if (dbHelper.getBloomedPlantsCount() + 1 >= 5) {
                dbHelper.unlockAchievement("ach_blooms_5");
            }

            // Log blooming event
            dbHelper.addGardenHistory(new GardenHistoryItem(
                    UUID.randomUUID().toString(),
                    "Flower Bloomed! 🌸",
                    "Your " + plant.getName() + " has bloomed into a full " + plant.getSpeciesDisplayName() + "!",
                    System.currentTimeMillis(),
                    GardenHistoryItem.EVENT_BLOOMED,
                    plant.getSpecies()
            ));

            // Chronicle blooming in The Little Memory Library
            try {
                MemoryManager.getInstance(appContext).onFlowerBloomed(plant.getName(), plant.getSpeciesDisplayName());
            } catch (Exception e) {
                Log.e(TAG, "Error chronicling bloom memory: " + e.getMessage());
            }

            postGardenNotification(
                    "Your flower has bloomed! 🌸",
                    "Congratulations! Your " + plant.getName() + " is now in full bloom! Check your garden.",
                    2001
            );
        } else if (stageAdvanced) {
            if (newStage == GardenPlant.STAGE_SPROUT) {
                dbHelper.unlockAchievement("ach_first_sprout");
            }

            dbHelper.addGardenHistory(new GardenHistoryItem(
                    UUID.randomUUID().toString(),
                    "Plant Grew to " + plant.getStageName() + "! 🌱",
                    plant.getName() + " reached " + plant.getStageName() + " with " + newPoints + " growth points.",
                    System.currentTimeMillis(),
                    GardenHistoryItem.EVENT_STAGE_UP,
                    plant.getSpecies()
            ));

            postGardenNotification(
                    "Your plant is growing! 🌱",
                    plant.getName() + " reached the " + plant.getStageName() + " stage! Keep focusing.",
                    2002
            );
        }

        // Save updated plant in SQLite
        dbHelper.savePlant(plant);

        // 3. Update Goals (Sessions count, Minutes progress)
        updateGoalsOnSession(minutes);

        // 4. Update Garden Level and cumulative GardenProgress
        syncGardenProgressToCloud();

        return true;
    }

    /**
     * Entry point when a task is completed directly.
     */
    public synchronized void onTaskCompletedDirectly(@NonNull Task task) {
        String sessionId = "task_direct_" + task.getId();
        boolean processed = onFocusSessionCompleted(sessionId, task.getMinutes(), task.getTitle());
        if (!processed) {
            Log.d(TAG, "Task " + task.getId() + " already credited garden rewards; skipping duplicate goal increment.");
            return;
        }

        // Update task-specific goals
        List<GardenGoal> goals = dbHelper.getAllGoals();
        for (GardenGoal g : goals) {
            if (GardenGoal.TYPE_TASKS.equals(g.getGoalType()) && !g.isCompleted()) {
                int newProgress = g.getCurrentProgress() + 1;
                boolean completed = newProgress >= g.getTarget();
                dbHelper.updateGoalProgress(g.getGoalId(), newProgress, completed);
                if (completed) {
                    awardGoalReward(g);
                }
            }
        }
    }

    private void updateGoalsOnSession(int minutes) {
        List<GardenGoal> goals = dbHelper.getAllGoals();
        for (GardenGoal g : goals) {
            if (g.isCompleted()) continue;

            if (GardenGoal.TYPE_SESSIONS.equals(g.getGoalType())) {
                int newProgress = g.getCurrentProgress() + 1;
                boolean completed = newProgress >= g.getTarget();
                dbHelper.updateGoalProgress(g.getGoalId(), newProgress, completed);
                if (completed) awardGoalReward(g);
            } else if (GardenGoal.TYPE_MINUTES.equals(g.getGoalType())) {
                int newProgress = g.getCurrentProgress() + minutes;
                boolean completed = newProgress >= g.getTarget();
                dbHelper.updateGoalProgress(g.getGoalId(), newProgress, completed);
                if (completed) awardGoalReward(g);
            }
        }
    }

    private void awardGoalReward(@NonNull GardenGoal goal) {
        if (goal.getRewardSeedSpecies() != null) {
            dbHelper.addSeeds(goal.getRewardSeedSpecies(), 1);
        }
        dbHelper.addGardenHistory(new GardenHistoryItem(
                UUID.randomUUID().toString(),
                "Goal Completed: " + goal.getTitle() + " 🎯",
                "Earned bonus seeds and garden points!",
                System.currentTimeMillis(),
                GardenHistoryItem.EVENT_ACHIEVEMENT,
                goal.getRewardSeedSpecies()
        ));

        // Chronicle challenge in The Little Memory Library
        try {
            MemoryManager.getInstance(appContext).onChallengeCompleted(goal.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error chronicling goal memory: " + e.getMessage());
        }

        postGardenNotification(
                "Garden Goal Achieved! 🎯",
                "You completed: " + goal.getTitle() + "! Bonus seeds have been added to your inventory.",
                2003
        );
    }

    /**
     * Waters the active plant with visual animation.
     */
    public synchronized void waterActivePlant() {
        GardenPlant plant = dbHelper.getActivePlant();
        if (plant == null) return;

        dbHelper.addGardenHistory(new GardenHistoryItem(
                UUID.randomUUID().toString(),
                "Watered " + plant.getName() + " 💧",
                "Garden refreshed with love and care.",
                System.currentTimeMillis(),
                GardenHistoryItem.EVENT_WATERED,
                plant.getSpecies()
        ));

        syncGardenProgressToCloud();
    }

    /**
     * Plants a new seed from the inventory.
     */
    public synchronized boolean plantNewSeed(@NonNull String species, @NonNull String customName) {
        boolean consumed = dbHelper.consumeSeed(species);
        if (!consumed) return false;

        String plantId = "plant_" + UUID.randomUUID().toString().substring(0, 8);
        GardenPlant newPlant = new GardenPlant(
                plantId,
                customName.trim().isEmpty() ? "My " + species : customName.trim(),
                species,
                0,
                GardenPlant.STAGE_SEED,
                System.currentTimeMillis(),
                0,
                true,
                true
        );

        dbHelper.savePlant(newPlant);
        dbHelper.setSelectedPlant(plantId);
        dbHelper.unlockAchievement("ach_first_seed");

        dbHelper.addGardenHistory(new GardenHistoryItem(
                UUID.randomUUID().toString(),
                "Planted " + newPlant.getName() + " 🌱",
                "Planted a new " + newPlant.getSpeciesDisplayName() + " seed in the garden pot.",
                System.currentTimeMillis(),
                GardenHistoryItem.EVENT_PLANTED,
                species
        ));

        syncGardenProgressToCloud();
        return true;
    }

    /**
     * Renames the currently active plant.
     */
    public synchronized void renameActivePlant(@NonNull String newName) {
        GardenPlant plant = dbHelper.getActivePlant();
        if (plant != null && !newName.trim().isEmpty()) {
            plant.setName(newName.trim());
            dbHelper.savePlant(plant);
            syncGardenProgressToCloud();
        }
    }

    /**
     * Switches the active plant in the pot.
     */
    public synchronized void selectPlant(@NonNull String plantId) {
        dbHelper.setSelectedPlant(plantId);
        syncGardenProgressToCloud();
    }

    /**
     * Calculates current GardenProgress model and syncs it to Firestore.
     */
    public synchronized GardenProgress getGardenProgress() {
        GardenPlant activePlant = dbHelper.getActivePlant();
        int totalMinutesEarned = dbHelper.getTotalMinutesEarned();
        int bloomedCount = dbHelper.getBloomedPlantsCount();
        int seedCount = 0;
        List<GardenSeed> seeds = dbHelper.getSeedInventory();
        for (GardenSeed s : seeds) {
            seedCount += s.getCount();
        }

        int level = Math.max(1, Math.min(5, totalMinutesEarned / 120 + 1));
        dbHelper.unlockSeedsForGardenLevel(level);

        GardenProgress progress = new GardenProgress(
                activePlant != null ? activePlant.getStageName() : "Little Sprout",
                totalMinutesEarned,
                level,
                System.currentTimeMillis()
        );

        progress.setGardenLevel(level);
        progress.setGardenPoints(totalMinutesEarned);
        if (activePlant != null) {
            progress.setActivePlantId(activePlant.getPlantId());
            progress.setActivePlantName(activePlant.getName());
            progress.setActivePlantSpecies(activePlant.getSpecies());
            progress.setActiveGrowthStage(activePlant.getStage());
            progress.setActiveGrowthPoints(activePlant.getGrowthPoints());
        }
        progress.setTotalFlowersBloomed(bloomedCount);
        progress.setUnlockedPlantCount(dbHelper.getAllPlants().size());
        progress.setAvailableSeedCount(seedCount);

        return progress;
    }

    public synchronized void syncGardenProgressToCloud() {
        GardenProgress progress = getGardenProgress();
        FirestoreManager.getInstance().saveGardenProgress(progress);
    }

    private void postGardenNotification(String title, String message, int notificationId) {
        try {
            Intent intent = new Intent(appContext, GardenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    appContext,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_GARDEN)
                    .setSmallIcon(R.drawable.mascot_minty)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Error posting garden notification", e);
        }
    }
}
