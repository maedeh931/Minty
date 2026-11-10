package com.minty.app;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model representing a daily quest in Minty.
 */
public class DailyQuest {

    private String id;
    private String title;
    private String description;
    private String iconEmoji;
    private int currentProgress;
    private int targetCount;
    private int rewardMinutes;
    private boolean claimed;
    private String dateKey;

    public DailyQuest(String id, String title, String description, String iconEmoji,
                      int currentProgress, int targetCount, int rewardMinutes,
                      boolean claimed, String dateKey) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconEmoji = iconEmoji;
        this.currentProgress = currentProgress;
        this.targetCount = targetCount;
        this.rewardMinutes = rewardMinutes;
        this.claimed = claimed;
        this.dateKey = dateKey;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getRewardMinutes() {
        return rewardMinutes;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public String getDateKey() {
        return dateKey;
    }

    public boolean isCompleted() {
        return currentProgress >= targetCount;
    }

    public int getProgressPercent() {
        if (targetCount <= 0) return 100;
        return Math.min(100, (int) ((currentProgress / (float) targetCount) * 100));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("description", description);
        obj.put("iconEmoji", iconEmoji);
        obj.put("currentProgress", currentProgress);
        obj.put("targetCount", targetCount);
        obj.put("rewardMinutes", rewardMinutes);
        obj.put("claimed", claimed);
        obj.put("dateKey", dateKey);
        return obj;
    }

    public static DailyQuest fromJson(JSONObject obj) throws JSONException {
        return new DailyQuest(
                obj.getString("id"),
                obj.getString("title"),
                obj.getString("description"),
                obj.optString("iconEmoji", "🌱"),
                obj.getInt("currentProgress"),
                obj.getInt("targetCount"),
                obj.getInt("rewardMinutes"),
                obj.getBoolean("claimed"),
                obj.getString("dateKey")
        );
    }
}
