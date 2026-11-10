package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ShowcaseManager controls the university presentation Virtual Garden Showcase Mode.
 *
 * Provides:
 * - Isolated demo garden state completely separated from SQLite minty.db and Firestore.
 * - Accelerated demo growth intervals (5s, 15s, 30s).
 * - Instant next-stage advancement and on-demand blooming demonstrations.
 * - Dedicated demo seed inventory and demo plant lifecycle (Seed -> Sprout -> Seedling -> Growing -> Blooming).
 * - Safe demo reset that never alters real tasks, balance, streaks, or normal garden records.
 */
public class ShowcaseManager {

    private static final String PREF_NAME = "minty_showcase_prefs";

    // Configuration keys
    private static final String KEY_DEMO_ENABLED = "key_demo_enabled";
    private static final String KEY_SPEED_SECONDS = "key_speed_seconds";

    // Isolated Demo Plant State keys
    private static final String KEY_DEMO_PLANT_ID = "demo_plant_id";
    private static final String KEY_DEMO_PLANT_NAME = "demo_plant_name";
    private static final String KEY_DEMO_PLANT_SPECIES = "demo_plant_species";
    private static final String KEY_DEMO_PLANT_POINTS = "demo_plant_points";
    private static final String KEY_DEMO_PLANT_BLOOMED_AT = "demo_plant_bloomed_at";

    // Isolated Demo Seed Inventory counts
    private static final String KEY_SEED_DAISY = "demo_seed_daisy";
    private static final String KEY_SEED_SUNFLOWER = "demo_seed_sunflower";
    private static final String KEY_SEED_TULIP = "demo_seed_tulip";
    private static final String KEY_SEED_ROSE = "demo_seed_rose";
    private static final String KEY_SEED_MINT = "demo_seed_mint";

    public static final int SPEED_ULTRA_FAST = 5;
    public static final int SPEED_STANDARD = 15;
    public static final int SPEED_PACED = 30;

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDemoModeEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_DEMO_ENABLED, false);
    }

    public static void setDemoModeEnabled(@NonNull Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_DEMO_ENABLED, enabled).apply();
    }

    public static int getGrowthSpeedSeconds(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_SPEED_SECONDS, SPEED_STANDARD);
    }

    public static void setGrowthSpeedSeconds(@NonNull Context context, int seconds) {
        getPrefs(context).edit().putInt(KEY_SPEED_SECONDS, seconds).apply();
    }

    /**
     * Retrieves the isolated demo plant. Initializes default demo plant if none exists.
     */
    @NonNull
    public static GardenPlant getDemoPlant(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        String plantId = prefs.getString(KEY_DEMO_PLANT_ID, null);

        if (plantId == null) {
            // First time initialization: Start at Stage 0 (Seed) or Stage 1 (Sprout)
            GardenPlant initialPlant = new GardenPlant(
                    "demo_plant_001",
                    "Madhu",
                    GardenPlant.SPECIES_DAISY,
                    0,
                    GardenPlant.STAGE_SEED,
                    System.currentTimeMillis(),
                    0,
                    true,
                    true
            );
            saveDemoPlant(context, initialPlant);
            initDemoSeedsIfEmpty(context);
            return initialPlant;
        }

        String name = prefs.getString(KEY_DEMO_PLANT_NAME, "Madhu");
        String species = prefs.getString(KEY_DEMO_PLANT_SPECIES, GardenPlant.SPECIES_DAISY);
        int points = prefs.getInt(KEY_DEMO_PLANT_POINTS, 0);
        long bloomedAt = prefs.getLong(KEY_DEMO_PLANT_BLOOMED_AT, 0);
        int stage = GardenPlant.calculateStageFromPoints(points);

        return new GardenPlant(
                plantId,
                name,
                species,
                points,
                stage,
                System.currentTimeMillis() - 60000,
                bloomedAt,
                true,
                true
        );
    }

    public static void saveDemoPlant(@NonNull Context context, @NonNull GardenPlant plant) {
        getPrefs(context).edit()
                .putString(KEY_DEMO_PLANT_ID, plant.getPlantId())
                .putString(KEY_DEMO_PLANT_NAME, plant.getName())
                .putString(KEY_DEMO_PLANT_SPECIES, plant.getSpecies())
                .putInt(KEY_DEMO_PLANT_POINTS, plant.getGrowthPoints())
                .putLong(KEY_DEMO_PLANT_BLOOMED_AT, plant.getBloomedAt())
                .apply();
    }

    /**
     * Adds accelerated growth points to the demo plant.
     * Returns true if this addition triggered a brand new bloom milestone!
     */
    public static boolean addDemoGrowthPoints(@NonNull Context context, int pointsToAdd) {
        GardenPlant plant = getDemoPlant(context);
        int oldPoints = plant.getGrowthPoints();
        int newPoints = oldPoints + pointsToAdd;
        int oldStage = plant.getStage();
        int newStage = GardenPlant.calculateStageFromPoints(newPoints);

        plant.setGrowthPoints(newPoints);

        boolean justBloomed = (newStage >= GardenPlant.STAGE_BLOOMING) && (plant.getBloomedAt() == 0);
        if (justBloomed) {
            plant.setBloomedAt(System.currentTimeMillis());
        }

        saveDemoPlant(context, plant);
        return justBloomed;
    }

    /**
     * Instantly advances the demo plant to the next growth stage threshold:
     * Seed (0) -> Sprout (15) -> Seedling (45) -> Growing (90) -> Blooming (150).
     * If already bloomed, cycles back to Seed (0) for presentation replay.
     */
    public static boolean advanceToNextStage(@NonNull Context context) {
        GardenPlant plant = getDemoPlant(context);
        int currentStage = plant.getStage();
        int targetPoints;

        switch (currentStage) {
            case GardenPlant.STAGE_SEED:
                targetPoints = GardenPlant.THRESHOLD_SPROUT;
                break;
            case GardenPlant.STAGE_SPROUT:
                targetPoints = GardenPlant.THRESHOLD_SEEDLING;
                break;
            case GardenPlant.STAGE_SEEDLING:
                targetPoints = GardenPlant.THRESHOLD_GROWING;
                break;
            case GardenPlant.STAGE_GROWING:
                targetPoints = GardenPlant.THRESHOLD_BLOOMING;
                break;
            case GardenPlant.STAGE_BLOOMING:
            default:
                targetPoints = 0; // Reset back to seed to repeat demo
                plant.setBloomedAt(0);
                break;
        }

        plant.setGrowthPoints(targetPoints);
        boolean justBloomed = (plant.getStage() >= GardenPlant.STAGE_BLOOMING) && (plant.getBloomedAt() == 0);
        if (justBloomed) {
            plant.setBloomedAt(System.currentTimeMillis());
        }
        saveDemoPlant(context, plant);
        return justBloomed;
    }

    /**
     * Renames the demo plant.
     */
    public static void renameDemoPlant(@NonNull Context context, @NonNull String newName) {
        GardenPlant plant = getDemoPlant(context);
        plant.setName(newName.trim());
        saveDemoPlant(context, plant);
    }

    /**
     * Waters demo plant, awarding +5 demo growth points.
     */
    public static boolean waterDemoPlant(@NonNull Context context) {
        return addDemoGrowthPoints(context, 5);
    }

    /**
     * Plants a new seed in the demo pot.
     */
    public static void plantDemoSeed(@NonNull Context context, @NonNull String species, @NonNull String customName) {
        deductDemoSeed(context, species);
        GardenPlant newPlant = new GardenPlant(
                "demo_plant_" + System.currentTimeMillis(),
                customName.trim().isEmpty() ? "My " + species : customName.trim(),
                species,
                0,
                GardenPlant.STAGE_SEED,
                System.currentTimeMillis(),
                0,
                true,
                true
        );
        saveDemoPlant(context, newPlant);
    }

    /**
     * Resets only the demo garden state back to initial Seed. Real garden data is untouched.
     */
    public static void resetDemoGarden(@NonNull Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(KEY_DEMO_PLANT_ID);
        editor.remove(KEY_DEMO_PLANT_NAME);
        editor.remove(KEY_DEMO_PLANT_SPECIES);
        editor.remove(KEY_DEMO_PLANT_POINTS);
        editor.remove(KEY_DEMO_PLANT_BLOOMED_AT);
        editor.apply();

        initDemoSeeds(context);
    }

    private static void initDemoSeedsIfEmpty(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (!prefs.contains(KEY_SEED_DAISY)) {
            initDemoSeeds(context);
        }
    }

    private static void initDemoSeeds(@NonNull Context context) {
        getPrefs(context).edit()
                .putInt(KEY_SEED_DAISY, 5)
                .putInt(KEY_SEED_SUNFLOWER, 5)
                .putInt(KEY_SEED_TULIP, 3)
                .putInt(KEY_SEED_ROSE, 3)
                .putInt(KEY_SEED_MINT, 5)
                .apply();
    }

    public static List<GardenSeed> getDemoSeedInventory(@NonNull Context context) {
        initDemoSeedsIfEmpty(context);
        SharedPreferences prefs = getPrefs(context);
        List<GardenSeed> list = new ArrayList<>();
        list.add(new GardenSeed(GardenPlant.SPECIES_DAISY, "Daisy Seed", "Sprightly white and pastel petals", prefs.getInt(KEY_SEED_DAISY, 5), true, "Demo Seed", 1));
        list.add(new GardenSeed(GardenPlant.SPECIES_SUNFLOWER, "Sunflower Seed", "Bright sunny petals that radiate warmth", prefs.getInt(KEY_SEED_SUNFLOWER, 5), true, "Demo Seed", 1));
        list.add(new GardenSeed(GardenPlant.SPECIES_MINT, "Mint Seed", "Refreshing and aromatic botanical herb", prefs.getInt(KEY_SEED_MINT, 5), true, "Demo Seed", 1));
        list.add(new GardenSeed(GardenPlant.SPECIES_TULIP, "Tulip Bulb", "Graceful cup-shaped spring bloom", prefs.getInt(KEY_SEED_TULIP, 3), true, "Demo Seed", 2));
        list.add(new GardenSeed(GardenPlant.SPECIES_ROSE, "Rose Cutting", "Classic velvety bloom with delicate fragrance", prefs.getInt(KEY_SEED_ROSE, 3), true, "Demo Seed", 3));
        return list;
    }

    private static void deductDemoSeed(@NonNull Context context, @NonNull String species) {
        SharedPreferences prefs = getPrefs(context);
        String key = getSeedKeyForSpecies(species);
        if (key != null) {
            int cur = prefs.getInt(key, 5);
            prefs.edit().putInt(key, Math.max(0, cur - 1)).apply();
        }
    }

    private static String getSeedKeyForSpecies(@NonNull String species) {
        switch (species.toLowerCase()) {
            case GardenPlant.SPECIES_DAISY: return KEY_SEED_DAISY;
            case GardenPlant.SPECIES_SUNFLOWER: return KEY_SEED_SUNFLOWER;
            case GardenPlant.SPECIES_TULIP: return KEY_SEED_TULIP;
            case GardenPlant.SPECIES_ROSE: return KEY_SEED_ROSE;
            case GardenPlant.SPECIES_MINT:
            default: return KEY_SEED_MINT;
        }
    }

    /**
     * Encouraging messages tailored to each plant stage for showcase demonstrations.
     */
    public static String getEncouragingMessage(@NonNull GardenPlant plant) {
        String name = plant.getName();
        switch (plant.getStage()) {
            case GardenPlant.STAGE_SEED:
                return "Your little seed is sleeping peacefully in the soil. Waking up soon! 🌱";
            case GardenPlant.STAGE_SPROUT:
                return name + " has sprouted two cute green leaves! Keep focusing! 🌱";
            case GardenPlant.STAGE_SEEDLING:
                return name + " is growing tall and strong with every focus breath! 🌿";
            case GardenPlant.STAGE_GROWING:
                return name + " is full of lush leaves and almost ready to bloom! 🌼";
            case GardenPlant.STAGE_BLOOMING:
            default:
                return name + " has bloomed into a full " + plant.getSpeciesDisplayName() + "! What a beautiful achievement! 🌸";
        }
    }
}
