package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing exported productivity statistics shared between connected buddies.
 * Honors user privacy toggles.
 */
public class SharedStats {

    private String uid;
    private int completedTasksToday;
    private int focusMinutesToday;
    private int streakDays;
    private int balanceMinutes;
    private int weeklyMinutes;
    private int weeklyTasksCount;
    private String recentAchievement;
    private boolean shareCompletedTasks;
    private boolean shareFocusTime;
    private boolean shareDailyStreak;
    private boolean shareBalance;
    private boolean shareWeeklyProgress;
    private boolean sharingEnabled;
    private long updatedAt;

    public SharedStats() {
        // Defaults
        this.shareCompletedTasks = true;
        this.shareFocusTime = true;
        this.shareDailyStreak = true;
        this.shareBalance = false;
        this.shareWeeklyProgress = true;
        this.sharingEnabled = true;
    }

    public SharedStats(String uid, int completedTasksToday, int focusMinutesToday, int streakDays,
                       int balanceMinutes, int weeklyMinutes, int weeklyTasksCount, String recentAchievement,
                       boolean shareCompletedTasks, boolean shareFocusTime, boolean shareDailyStreak,
                       boolean shareBalance, boolean shareWeeklyProgress, boolean sharingEnabled, long updatedAt) {
        this.uid = uid;
        this.completedTasksToday = completedTasksToday;
        this.focusMinutesToday = focusMinutesToday;
        this.streakDays = streakDays;
        this.balanceMinutes = balanceMinutes;
        this.weeklyMinutes = weeklyMinutes;
        this.weeklyTasksCount = weeklyTasksCount;
        this.recentAchievement = recentAchievement;
        this.shareCompletedTasks = shareCompletedTasks;
        this.shareFocusTime = shareFocusTime;
        this.shareDailyStreak = shareDailyStreak;
        this.shareBalance = shareBalance;
        this.shareWeeklyProgress = shareWeeklyProgress;
        this.sharingEnabled = sharingEnabled;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getCompletedTasksToday() {
        return completedTasksToday;
    }

    public void setCompletedTasksToday(int completedTasksToday) {
        this.completedTasksToday = completedTasksToday;
    }

    public int getFocusMinutesToday() {
        return focusMinutesToday;
    }

    public void setFocusMinutesToday(int focusMinutesToday) {
        this.focusMinutesToday = focusMinutesToday;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public int getBalanceMinutes() {
        return balanceMinutes;
    }

    public void setBalanceMinutes(int balanceMinutes) {
        this.balanceMinutes = balanceMinutes;
    }

    public int getWeeklyMinutes() {
        return weeklyMinutes;
    }

    public void setWeeklyMinutes(int weeklyMinutes) {
        this.weeklyMinutes = weeklyMinutes;
    }

    public int getWeeklyTasksCount() {
        return weeklyTasksCount;
    }

    public void setWeeklyTasksCount(int weeklyTasksCount) {
        this.weeklyTasksCount = weeklyTasksCount;
    }

    public String getRecentAchievement() {
        return recentAchievement != null ? recentAchievement : "Beginner";
    }

    public void setRecentAchievement(String recentAchievement) {
        this.recentAchievement = recentAchievement;
    }

    public boolean isShareCompletedTasks() {
        return shareCompletedTasks;
    }

    public void setShareCompletedTasks(boolean shareCompletedTasks) {
        this.shareCompletedTasks = shareCompletedTasks;
    }

    public boolean isShareFocusTime() {
        return shareFocusTime;
    }

    public void setShareFocusTime(boolean shareFocusTime) {
        this.shareFocusTime = shareFocusTime;
    }

    public boolean isShareDailyStreak() {
        return shareDailyStreak;
    }

    public void setShareDailyStreak(boolean shareDailyStreak) {
        this.shareDailyStreak = shareDailyStreak;
    }

    public boolean isShareBalance() {
        return shareBalance;
    }

    public void setShareBalance(boolean shareBalance) {
        this.shareBalance = shareBalance;
    }

    public boolean isShareWeeklyProgress() {
        return shareWeeklyProgress;
    }

    public void setShareWeeklyProgress(boolean shareWeeklyProgress) {
        this.shareWeeklyProgress = shareWeeklyProgress;
    }

    public boolean isSharingEnabled() {
        return sharingEnabled;
    }

    public void setSharingEnabled(boolean sharingEnabled) {
        this.sharingEnabled = sharingEnabled;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("completedTasksToday", shareCompletedTasks ? completedTasksToday : 0);
        map.put("focusMinutesToday", shareFocusTime ? focusMinutesToday : 0);
        map.put("streakDays", shareDailyStreak ? streakDays : 0);
        map.put("balanceMinutes", shareBalance ? balanceMinutes : -1);
        map.put("weeklyMinutes", shareWeeklyProgress ? weeklyMinutes : 0);
        map.put("weeklyTasksCount", shareWeeklyProgress ? weeklyTasksCount : 0);
        map.put("recentAchievement", getRecentAchievement());
        map.put("shareCompletedTasks", shareCompletedTasks);
        map.put("shareFocusTime", shareFocusTime);
        map.put("shareDailyStreak", shareDailyStreak);
        map.put("shareBalance", shareBalance);
        map.put("shareWeeklyProgress", shareWeeklyProgress);
        map.put("sharingEnabled", sharingEnabled);
        map.put("updatedAt", updatedAt > 0 ? updatedAt : System.currentTimeMillis());
        return map;
    }

    public static SharedStats fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String uid = (String) data.get("uid");
        if (uid == null || uid.isEmpty()) uid = docId;

        Long completedToday = (Long) data.get("completedTasksToday");
        Long focusToday = (Long) data.get("focusMinutesToday");
        Long streak = (Long) data.get("streakDays");
        Long balance = (Long) data.get("balanceMinutes");
        Long weeklyMins = (Long) data.get("weeklyMinutes");
        Long weeklyTasks = (Long) data.get("weeklyTasksCount");
        String achievement = (String) data.get("recentAchievement");

        Boolean shareTasks = (Boolean) data.get("shareCompletedTasks");
        Boolean shareFocus = (Boolean) data.get("shareFocusTime");
        Boolean shareStreak = (Boolean) data.get("shareDailyStreak");
        Boolean shareBal = (Boolean) data.get("shareBalance");
        Boolean shareWeekly = (Boolean) data.get("shareWeeklyProgress");
        Boolean enabled = (Boolean) data.get("sharingEnabled");
        Long updatedAt = (Long) data.get("updatedAt");

        return new SharedStats(
                uid,
                completedToday != null ? completedToday.intValue() : 0,
                focusToday != null ? focusToday.intValue() : 0,
                streak != null ? streak.intValue() : 0,
                balance != null ? balance.intValue() : -1,
                weeklyMins != null ? weeklyMins.intValue() : 0,
                weeklyTasks != null ? weeklyTasks.intValue() : 0,
                achievement != null ? achievement : "Beginner",
                shareTasks != null ? shareTasks : true,
                shareFocus != null ? shareFocus : true,
                shareStreak != null ? shareStreak : true,
                shareBal != null ? shareBal : false,
                shareWeekly != null ? shareWeekly : true,
                enabled != null ? enabled : true,
                updatedAt != null ? updatedAt : System.currentTimeMillis()
        );
    }
}
