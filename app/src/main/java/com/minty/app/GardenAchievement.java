package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing virtual garden achievements.
 */
public class GardenAchievement {

    private String achievementId;
    private String title;
    private String description;
    private String badgeIcon;
    private boolean isUnlocked;
    private long unlockedAt;
    private int currentProgress;
    private int targetProgress;

    public GardenAchievement() {
    }

    public GardenAchievement(String achievementId, String title, String description,
                             String badgeIcon, boolean isUnlocked, long unlockedAt,
                             int currentProgress, int targetProgress) {
        this.achievementId = achievementId;
        this.title = title;
        this.description = description;
        this.badgeIcon = badgeIcon;
        this.isUnlocked = isUnlocked;
        this.unlockedAt = unlockedAt;
        this.currentProgress = currentProgress;
        this.targetProgress = targetProgress;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBadgeIcon() {
        return badgeIcon;
    }

    public void setBadgeIcon(String badgeIcon) {
        this.badgeIcon = badgeIcon;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public long getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(int targetProgress) {
        this.targetProgress = targetProgress;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("achievementId", achievementId);
        map.put("title", title);
        map.put("description", description);
        map.put("badgeIcon", badgeIcon);
        map.put("isUnlocked", isUnlocked);
        map.put("unlockedAt", unlockedAt);
        map.put("currentProgress", currentProgress);
        map.put("targetProgress", targetProgress);
        return map;
    }

    public static GardenAchievement fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GardenAchievement ach = new GardenAchievement();
        ach.setAchievementId((String) map.get("achievementId"));
        ach.setTitle((String) map.get("title"));
        ach.setDescription((String) map.get("description"));
        ach.setBadgeIcon((String) map.get("badgeIcon"));

        if (map.containsKey("isUnlocked") && map.get("isUnlocked") instanceof Boolean) {
            ach.setUnlocked((Boolean) map.get("isUnlocked"));
        }
        if (map.containsKey("unlockedAt") && map.get("unlockedAt") instanceof Number) {
            ach.setUnlockedAt(((Number) map.get("unlockedAt")).longValue());
        }
        if (map.containsKey("currentProgress") && map.get("currentProgress") instanceof Number) {
            ach.setCurrentProgress(((Number) map.get("currentProgress")).intValue());
        }
        if (map.containsKey("targetProgress") && map.get("targetProgress") instanceof Number) {
            ach.setTargetProgress(((Number) map.get("targetProgress")).intValue());
        }

        return ach;
    }
}
