package com.minty.app;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an individual plant/flower in the user's Virtual Garden.
 */
public class GardenPlant {

    // Plant growth stage constants
    public static final int STAGE_SEED = 0;
    public static final int STAGE_SPROUT = 1;
    public static final int STAGE_SEEDLING = 2;
    public static final int STAGE_GROWING = 3;
    public static final int STAGE_BLOOMING = 4;

    // Configurable growth thresholds in focus/growth points
    public static final int THRESHOLD_SPROUT = 15;
    public static final int THRESHOLD_SEEDLING = 45;
    public static final int THRESHOLD_GROWING = 90;
    public static final int THRESHOLD_BLOOMING = 150;

    // Supported flower species
    public static final String SPECIES_MINT = "mint";
    public static final String SPECIES_SUNFLOWER = "sunflower";
    public static final String SPECIES_DAISY = "daisy";
    public static final String SPECIES_TULIP = "tulip";
    public static final String SPECIES_ROSE = "rose";
    public static final String SPECIES_LAVENDER = "lavender";
    public static final String SPECIES_CHERRY_BLOSSOM = "cherry_blossom";

    private String plantId;
    private String name;
    private String species;
    private int growthPoints;
    private int stage;
    private long plantedAt;
    private long bloomedAt;
    private boolean isSelected;
    private boolean isUnlocked;

    public GardenPlant() {
    }

    public GardenPlant(String plantId, String name, String species, int growthPoints,
                       int stage, long plantedAt, long bloomedAt, boolean isSelected, boolean isUnlocked) {
        this.plantId = plantId;
        this.name = name;
        this.species = species;
        this.growthPoints = growthPoints;
        this.stage = stage;
        this.plantedAt = plantedAt;
        this.bloomedAt = bloomedAt;
        this.isSelected = isSelected;
        this.isUnlocked = isUnlocked;
    }

    public String getPlantId() {
        return plantId;
    }

    public void setPlantId(String plantId) {
        this.plantId = plantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species != null ? species : SPECIES_MINT;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public int getGrowthPoints() {
        return growthPoints;
    }

    public void setGrowthPoints(int growthPoints) {
        this.growthPoints = growthPoints;
        this.stage = calculateStageFromPoints(growthPoints);
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public long getPlantedAt() {
        return plantedAt;
    }

    public void setPlantedAt(long plantedAt) {
        this.plantedAt = plantedAt;
    }

    public long getBloomedAt() {
        return bloomedAt;
    }

    public void setBloomedAt(long bloomedAt) {
        this.bloomedAt = bloomedAt;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public boolean isBloomed() {
        return stage >= STAGE_BLOOMING || bloomedAt > 0;
    }

    /**
     * Determines growth stage based on total points.
     */
    public static int calculateStageFromPoints(int points) {
        if (points < THRESHOLD_SPROUT) return STAGE_SEED;
        if (points < THRESHOLD_SEEDLING) return STAGE_SPROUT;
        if (points < THRESHOLD_GROWING) return STAGE_SEEDLING;
        if (points < THRESHOLD_BLOOMING) return STAGE_GROWING;
        return STAGE_BLOOMING;
    }

    /**
     * Human-readable name of current stage.
     */
    public String getStageName() {
        switch (stage) {
            case STAGE_SEED: return "Seed";
            case STAGE_SPROUT: return "Sprout";
            case STAGE_SEEDLING: return "Seedling";
            case STAGE_GROWING: return "Growing Plant";
            case STAGE_BLOOMING: return "Blooming Flower";
            default: return "Sprout";
        }
    }

    /**
     * Capitalized display name for the flower species.
     */
    public String getSpeciesDisplayName() {
        if (species == null) return "Mint";
        switch (species.toLowerCase()) {
            case SPECIES_SUNFLOWER: return "Sunflower";
            case SPECIES_DAISY: return "Daisy";
            case SPECIES_TULIP: return "Tulip";
            case SPECIES_ROSE: return "Rose";
            case SPECIES_LAVENDER: return "Lavender";
            case SPECIES_CHERRY_BLOSSOM: return "Cherry Blossom";
            case SPECIES_MINT:
            default: return "Mint Herb";
        }
    }

    /**
     * Target points needed for the next stage.
     */
    public int getNextStageThreshold() {
        switch (stage) {
            case STAGE_SEED: return THRESHOLD_SPROUT;
            case STAGE_SPROUT: return THRESHOLD_SEEDLING;
            case STAGE_SEEDLING: return THRESHOLD_GROWING;
            case STAGE_GROWING: return THRESHOLD_BLOOMING;
            default: return THRESHOLD_BLOOMING;
        }
    }

    /**
     * Points needed for current stage beginning.
     */
    public int getCurrentStageBasePoints() {
        switch (stage) {
            case STAGE_SPROUT: return THRESHOLD_SPROUT;
            case STAGE_SEEDLING: return THRESHOLD_SEEDLING;
            case STAGE_GROWING: return THRESHOLD_GROWING;
            case STAGE_BLOOMING: return THRESHOLD_BLOOMING;
            case STAGE_SEED:
            default: return 0;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("plantId", plantId);
        map.put("name", name);
        map.put("species", species);
        map.put("growthPoints", growthPoints);
        map.put("stage", stage);
        map.put("plantedAt", plantedAt);
        map.put("bloomedAt", bloomedAt);
        map.put("isSelected", isSelected);
        map.put("isUnlocked", isUnlocked);
        return map;
    }

    public static GardenPlant fromMap(String plantId, Map<String, Object> map) {
        if (map == null) return null;
        GardenPlant plant = new GardenPlant();
        plant.setPlantId(plantId != null ? plantId : (String) map.get("plantId"));
        plant.setName((String) map.get("name"));
        plant.setSpecies((String) map.get("species"));

        if (map.containsKey("growthPoints") && map.get("growthPoints") instanceof Number) {
            plant.setGrowthPoints(((Number) map.get("growthPoints")).intValue());
        }
        if (map.containsKey("stage") && map.get("stage") instanceof Number) {
            plant.setStage(((Number) map.get("stage")).intValue());
        }
        if (map.containsKey("plantedAt") && map.get("plantedAt") instanceof Number) {
            plant.setPlantedAt(((Number) map.get("plantedAt")).longValue());
        }
        if (map.containsKey("bloomedAt") && map.get("bloomedAt") instanceof Number) {
            plant.setBloomedAt(((Number) map.get("bloomedAt")).longValue());
        }
        if (map.containsKey("isSelected") && map.get("isSelected") instanceof Boolean) {
            plant.setSelected((Boolean) map.get("isSelected"));
        }
        if (map.containsKey("isUnlocked") && map.get("isUnlocked") instanceof Boolean) {
            plant.setUnlocked((Boolean) map.get("isUnlocked"));
        }

        return plant;
    }
}
