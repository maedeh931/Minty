package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing plant/mascot growth in Minty's focus garden.
 * Synchronized with Cloud Firestore document "users/{uid}/garden/status".
 * Preserves legacy fields while providing full support for the multi-flower virtual garden system.
 */
public class GardenProgress {

    // Legacy fields (preserved for backward compatibility)
    private String stageName = "Mint Seedling";
    private int totalMinutesGrown = 0;
    private int growthLevel = 1;
    private long lastWateredAt = 0;

    // Extended Virtual Garden fields
    private int gardenLevel = 1;
    private int gardenPoints = 0;
    private String activePlantId = "";
    private String activePlantName = "Minty";
    private String activePlantSpecies = "mint";
    private int activeGrowthStage = 1; // 0=Seed, 1=Sprout, 2=Seedling, 3=Growing, 4=Bloomed
    private int activeGrowthPoints = 15;
    private int totalFlowersBloomed = 0;
    private int unlockedPlantCount = 1;
    private int availableSeedCount = 2;
    private long updatedAt = System.currentTimeMillis();

    // Required zero-argument constructor for Firestore
    public GardenProgress() {
    }

    public GardenProgress(String stageName, int totalMinutesGrown, int growthLevel, long lastWateredAt) {
        this.stageName = stageName;
        this.totalMinutesGrown = totalMinutesGrown;
        this.growthLevel = growthLevel;
        this.lastWateredAt = lastWateredAt;
        this.gardenPoints = totalMinutesGrown;
        this.gardenLevel = Math.max(1, Math.min(5, totalMinutesGrown / 120 + 1));
        this.updatedAt = System.currentTimeMillis();
    }

    // --- Legacy Getters & Setters ---

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public int getTotalMinutesGrown() {
        return totalMinutesGrown;
    }

    public void setTotalMinutesGrown(int totalMinutesGrown) {
        this.totalMinutesGrown = totalMinutesGrown;
    }

    public int getGrowthLevel() {
        return growthLevel;
    }

    public void setGrowthLevel(int growthLevel) {
        this.growthLevel = growthLevel;
    }

    public long getLastWateredAt() {
        return lastWateredAt;
    }

    public void setLastWateredAt(long lastWateredAt) {
        this.lastWateredAt = lastWateredAt;
    }

    // --- Extended Virtual Garden Getters & Setters ---

    public int getGardenLevel() {
        return gardenLevel;
    }

    public void setGardenLevel(int gardenLevel) {
        this.gardenLevel = gardenLevel;
    }

    public int getGardenPoints() {
        return gardenPoints;
    }

    public void setGardenPoints(int gardenPoints) {
        this.gardenPoints = gardenPoints;
    }

    public String getActivePlantId() {
        return activePlantId;
    }

    public void setActivePlantId(String activePlantId) {
        this.activePlantId = activePlantId;
    }

    public String getActivePlantName() {
        return activePlantName;
    }

    public void setActivePlantName(String activePlantName) {
        this.activePlantName = activePlantName;
    }

    public String getActivePlantSpecies() {
        return activePlantSpecies;
    }

    public void setActivePlantSpecies(String activePlantSpecies) {
        this.activePlantSpecies = activePlantSpecies;
    }

    public int getActiveGrowthStage() {
        return activeGrowthStage;
    }

    public void setActiveGrowthStage(int activeGrowthStage) {
        this.activeGrowthStage = activeGrowthStage;
    }

    public int getActiveGrowthPoints() {
        return activeGrowthPoints;
    }

    public void setActiveGrowthPoints(int activeGrowthPoints) {
        this.activeGrowthPoints = activeGrowthPoints;
    }

    public int getTotalFlowersBloomed() {
        return totalFlowersBloomed;
    }

    public void setTotalFlowersBloomed(int totalFlowersBloomed) {
        this.totalFlowersBloomed = totalFlowersBloomed;
    }

    public int getUnlockedPlantCount() {
        return unlockedPlantCount;
    }

    public void setUnlockedPlantCount(int unlockedPlantCount) {
        this.unlockedPlantCount = unlockedPlantCount;
    }

    public int getAvailableSeedCount() {
        return availableSeedCount;
    }

    public void setAvailableSeedCount(int availableSeedCount) {
        this.availableSeedCount = availableSeedCount;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("stageName", stageName);
        map.put("totalMinutesGrown", totalMinutesGrown);
        map.put("growthLevel", growthLevel);
        map.put("lastWateredAt", lastWateredAt);

        // Extended fields
        map.put("gardenLevel", gardenLevel);
        map.put("gardenPoints", gardenPoints);
        map.put("activePlantId", activePlantId);
        map.put("activePlantName", activePlantName);
        map.put("activePlantSpecies", activePlantSpecies);
        map.put("activeGrowthStage", activeGrowthStage);
        map.put("activeGrowthPoints", activeGrowthPoints);
        map.put("totalFlowersBloomed", totalFlowersBloomed);
        map.put("unlockedPlantCount", unlockedPlantCount);
        map.put("availableSeedCount", availableSeedCount);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public static GardenProgress fromMap(Map<String, Object> map) {
        if (map == null) return new GardenProgress();
        GardenProgress progress = new GardenProgress();

        if (map.containsKey("stageName")) progress.setStageName((String) map.get("stageName"));
        if (map.containsKey("totalMinutesGrown") && map.get("totalMinutesGrown") instanceof Number) {
            progress.setTotalMinutesGrown(((Number) map.get("totalMinutesGrown")).intValue());
        }
        if (map.containsKey("growthLevel") && map.get("growthLevel") instanceof Number) {
            progress.setGrowthLevel(((Number) map.get("growthLevel")).intValue());
        }
        if (map.containsKey("lastWateredAt") && map.get("lastWateredAt") instanceof Number) {
            progress.setLastWateredAt(((Number) map.get("lastWateredAt")).longValue());
        }

        if (map.containsKey("gardenLevel") && map.get("gardenLevel") instanceof Number) {
            progress.setGardenLevel(((Number) map.get("gardenLevel")).intValue());
        }
        if (map.containsKey("gardenPoints") && map.get("gardenPoints") instanceof Number) {
            progress.setGardenPoints(((Number) map.get("gardenPoints")).intValue());
        }
        if (map.containsKey("activePlantId")) progress.setActivePlantId((String) map.get("activePlantId"));
        if (map.containsKey("activePlantName")) progress.setActivePlantName((String) map.get("activePlantName"));
        if (map.containsKey("activePlantSpecies")) progress.setActivePlantSpecies((String) map.get("activePlantSpecies"));
        if (map.containsKey("activeGrowthStage") && map.get("activeGrowthStage") instanceof Number) {
            progress.setActiveGrowthStage(((Number) map.get("activeGrowthStage")).intValue());
        }
        if (map.containsKey("activeGrowthPoints") && map.get("activeGrowthPoints") instanceof Number) {
            progress.setActiveGrowthPoints(((Number) map.get("activeGrowthPoints")).intValue());
        }
        if (map.containsKey("totalFlowersBloomed") && map.get("totalFlowersBloomed") instanceof Number) {
            progress.setTotalFlowersBloomed(((Number) map.get("totalFlowersBloomed")).intValue());
        }
        if (map.containsKey("unlockedPlantCount") && map.get("unlockedPlantCount") instanceof Number) {
            progress.setUnlockedPlantCount(((Number) map.get("unlockedPlantCount")).intValue());
        }
        if (map.containsKey("availableSeedCount") && map.get("availableSeedCount") instanceof Number) {
            progress.setAvailableSeedCount(((Number) map.get("availableSeedCount")).intValue());
        }
        if (map.containsKey("updatedAt") && map.get("updatedAt") instanceof Number) {
            progress.setUpdatedAt(((Number) map.get("updatedAt")).longValue());
        }

        return progress;
    }

    /**
     * Factory method to calculate stage name from total minutes.
     */
    public static String calculateStage(int totalMinutes) {
        if (totalMinutes < 30) return "Mint Seedling";
        if (totalMinutes < 90) return "Little Sprout";
        if (totalMinutes < 200) return "Leafy Herb";
        if (totalMinutes < 400) return "Flowering Mint";
        return "Golden Mint Tree";
    }

    /**
     * Factory method to create a GardenProgress instance from total minutes.
     */
    public static GardenProgress fromMinutes(int totalMinutes) {
        String stage = calculateStage(totalMinutes);
        int level = totalMinutes / 30 + 1;
        return new GardenProgress(stage, totalMinutes, level, System.currentTimeMillis());
    }
}
