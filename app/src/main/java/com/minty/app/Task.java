package com.minty.app;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a Task in Minty.
 * Tracks task details, minty minutes reward, completion status, timestamps,
 * target date, start time, deadline, priority, category, and reminder settings.
 */
public class Task {

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_STUDY = "Study";
    public static final String CATEGORY_WORK = "Work";
    public static final String CATEGORY_PERSONAL = "Personal";
    public static final String CATEGORY_HEALTH = "Health";

    private long id;
    private String title;
    private int minutes;
    private boolean completed;
    private long createdAt;
    private long completedAt;

    // Daily Planner & Scheduling fields
    private String description = "";
    private long targetDateMillis = 0; // Midnight timestamp of planned day
    private long startTimeMillis = 0;  // Specific scheduled time
    private long deadlineMillis = 0;   // Deadline timestamp
    private int priority = PRIORITY_MEDIUM;
    private String category = CATEGORY_GENERAL;
    private int reminderMinutes = 0;   // 0 = None, 5, 10, 15, 30
    private boolean reminderScheduled = false;

    public Task() {
    }

    // Backwards-compatible constructor
    public Task(long id, String title, int minutes, boolean completed, long createdAt, long completedAt) {
        this.id = id;
        this.title = title;
        this.minutes = minutes;
        this.completed = completed;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.targetDateMillis = getTodayMidnightMillis();
    }

    // Backwards-compatible constructor without ID
    public Task(String title, int minutes, boolean completed, long createdAt, long completedAt) {
        this.title = title;
        this.minutes = minutes;
        this.completed = completed;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.targetDateMillis = getTodayMidnightMillis();
    }

    // Full constructor
    public Task(long id, String title, int minutes, boolean completed, long createdAt, long completedAt,
                String description, long targetDateMillis, long startTimeMillis, long deadlineMillis,
                int priority, String category, int reminderMinutes, boolean reminderScheduled) {
        this.id = id;
        this.title = title;
        this.minutes = minutes;
        this.completed = completed;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.description = description != null ? description : "";
        this.targetDateMillis = targetDateMillis > 0 ? targetDateMillis : getTodayMidnightMillis();
        this.startTimeMillis = startTimeMillis;
        this.deadlineMillis = deadlineMillis;
        this.priority = priority;
        this.category = category != null && !category.isEmpty() ? category : CATEGORY_GENERAL;
        this.reminderMinutes = reminderMinutes;
        this.reminderScheduled = reminderScheduled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTargetDateMillis() {
        return targetDateMillis;
    }

    public void setTargetDateMillis(long targetDateMillis) {
        this.targetDateMillis = targetDateMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    public void setDeadlineMillis(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category != null && !category.isEmpty() ? category : CATEGORY_GENERAL;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(int reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public boolean isReminderScheduled() {
        return reminderScheduled;
    }

    public void setReminderScheduled(boolean reminderScheduled) {
        this.reminderScheduled = reminderScheduled;
    }

    public boolean isOverdue() {
        if (completed) return false;
        long todayStart = getTodayMidnightMillis();
        // If targetDateMillis is before today's start, or deadline has passed
        if (targetDateMillis > 0 && targetDateMillis < todayStart) {
            return true;
        }
        if (deadlineMillis > 0 && deadlineMillis < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public boolean isForToday() {
        long todayStart = getTodayMidnightMillis();
        long tomorrowStart = todayStart + 86400000L;
        return targetDateMillis >= todayStart && targetDateMillis < tomorrowStart;
    }

    public boolean isForTomorrow() {
        long todayStart = getTodayMidnightMillis();
        long tomorrowStart = todayStart + 86400000L;
        long dayAfterTomorrow = tomorrowStart + 86400000L;
        return targetDateMillis >= tomorrowStart && targetDateMillis < dayAfterTomorrow;
    }

    public static long getTodayMidnightMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getTomorrowMidnightMillis() {
        return getTodayMidnightMillis() + 86400000L;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("minutes", minutes);
        map.put("completed", completed);
        map.put("createdAt", createdAt);
        map.put("completedAt", completedAt);
        map.put("description", description);
        map.put("targetDateMillis", targetDateMillis);
        map.put("startTimeMillis", startTimeMillis);
        map.put("deadlineMillis", deadlineMillis);
        map.put("priority", priority);
        map.put("category", category);
        map.put("reminderMinutes", reminderMinutes);
        map.put("reminderScheduled", reminderScheduled);
        return map;
    }
}
