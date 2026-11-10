package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing daily and weekly productivity goals tied to garden rewards.
 */
public class GardenGoal {

    public static final String TYPE_SESSIONS = "sessions";
    public static final String TYPE_MINUTES = "minutes";
    public static final String TYPE_TASKS = "tasks";
    public static final String TYPE_STREAK = "streak";

    private String goalId;
    private String title;
    private String goalType;
    private int target;
    private int currentProgress;
    private boolean isWeekly;
    private boolean isCompleted;
    private String rewardSeedSpecies;
    private int rewardGrowthPoints;

    public GardenGoal() {
    }

    public GardenGoal(String goalId, String title, String goalType, int target,
                      int currentProgress, boolean isWeekly, boolean isCompleted,
                      String rewardSeedSpecies, int rewardGrowthPoints) {
        this.goalId = goalId;
        this.title = title;
        this.goalType = goalType;
        this.target = target;
        this.currentProgress = currentProgress;
        this.isWeekly = isWeekly;
        this.isCompleted = isCompleted;
        this.rewardSeedSpecies = rewardSeedSpecies;
        this.rewardGrowthPoints = rewardGrowthPoints;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = Math.min(target, Math.max(0, currentProgress));
        if (this.currentProgress >= target) {
            this.isCompleted = true;
        }
    }

    public boolean isWeekly() {
        return isWeekly;
    }

    public void setWeekly(boolean weekly) {
        isWeekly = weekly;
    }

    public boolean isCompleted() {
        return isCompleted || currentProgress >= target;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getRewardSeedSpecies() {
        return rewardSeedSpecies;
    }

    public void setRewardSeedSpecies(String rewardSeedSpecies) {
        this.rewardSeedSpecies = rewardSeedSpecies;
    }

    public int getRewardGrowthPoints() {
        return rewardGrowthPoints;
    }

    public void setRewardGrowthPoints(int rewardGrowthPoints) {
        this.rewardGrowthPoints = rewardGrowthPoints;
    }

    public int getProgressPercent() {
        if (target <= 0) return 100;
        return Math.min(100, (int) ((currentProgress / (float) target) * 100));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("goalId", goalId);
        map.put("title", title);
        map.put("goalType", goalType);
        map.put("target", target);
        map.put("currentProgress", currentProgress);
        map.put("isWeekly", isWeekly);
        map.put("isCompleted", isCompleted);
        map.put("rewardSeedSpecies", rewardSeedSpecies);
        map.put("rewardGrowthPoints", rewardGrowthPoints);
        return map;
    }

    public static GardenGoal fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GardenGoal goal = new GardenGoal();
        goal.setGoalId((String) map.get("goalId"));
        goal.setTitle((String) map.get("title"));
        goal.setGoalType((String) map.get("goalType"));

        if (map.containsKey("target") && map.get("target") instanceof Number) {
            goal.setTarget(((Number) map.get("target")).intValue());
        }
        if (map.containsKey("currentProgress") && map.get("currentProgress") instanceof Number) {
            goal.setCurrentProgress(((Number) map.get("currentProgress")).intValue());
        }
        if (map.containsKey("isWeekly") && map.get("isWeekly") instanceof Boolean) {
            goal.setWeekly((Boolean) map.get("isWeekly"));
        }
        if (map.containsKey("isCompleted") && map.get("isCompleted") instanceof Boolean) {
            goal.setCompleted((Boolean) map.get("isCompleted"));
        }
        goal.setRewardSeedSpecies((String) map.get("rewardSeedSpecies"));
        if (map.containsKey("rewardGrowthPoints") && map.get("rewardGrowthPoints") instanceof Number) {
            goal.setRewardGrowthPoints(((Number) map.get("rewardGrowthPoints")).intValue());
        }

        return goal;
    }
}
