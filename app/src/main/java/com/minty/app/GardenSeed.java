package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing seed inventory items and plant varieties.
 */
public class GardenSeed {

    private String species;
    private String displayName;
    private String description;
    private int count;
    private boolean isUnlocked;
    private String unlockRequirement;
    private int requiredGardenLevel;

    public GardenSeed() {
    }

    public GardenSeed(String species, String displayName, String description, int count,
                      boolean isUnlocked, String unlockRequirement, int requiredGardenLevel) {
        this.species = species;
        this.displayName = displayName;
        this.description = description;
        this.count = count;
        this.isUnlocked = isUnlocked;
        this.unlockRequirement = unlockRequirement;
        this.requiredGardenLevel = requiredGardenLevel;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public String getUnlockRequirement() {
        return unlockRequirement;
    }

    public void setUnlockRequirement(String unlockRequirement) {
        this.unlockRequirement = unlockRequirement;
    }

    public int getRequiredGardenLevel() {
        return requiredGardenLevel;
    }

    public void setRequiredGardenLevel(int requiredGardenLevel) {
        this.requiredGardenLevel = requiredGardenLevel;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("species", species);
        map.put("displayName", displayName);
        map.put("description", description);
        map.put("count", count);
        map.put("isUnlocked", isUnlocked);
        map.put("unlockRequirement", unlockRequirement);
        map.put("requiredGardenLevel", requiredGardenLevel);
        return map;
    }

    public static GardenSeed fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GardenSeed seed = new GardenSeed();
        seed.setSpecies((String) map.get("species"));
        seed.setDisplayName((String) map.get("displayName"));
        seed.setDescription((String) map.get("description"));

        if (map.containsKey("count") && map.get("count") instanceof Number) {
            seed.setCount(((Number) map.get("count")).intValue());
        }
        if (map.containsKey("isUnlocked") && map.get("isUnlocked") instanceof Boolean) {
            seed.setUnlocked((Boolean) map.get("isUnlocked"));
        }
        seed.setUnlockRequirement((String) map.get("unlockRequirement"));
        if (map.containsKey("requiredGardenLevel") && map.get("requiredGardenLevel") instanceof Number) {
            seed.setRequiredGardenLevel(((Number) map.get("requiredGardenLevel")).intValue());
        }

        return seed;
    }
}
