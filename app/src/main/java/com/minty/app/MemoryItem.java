package com.minty.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Model class representing an illustrated milestone memory in The Little Memory Library.
 */
public class MemoryItem {

    public static final String CAT_FOCUS = "Focus";
    public static final String CAT_GARDEN = "Garden";
    public static final String CAT_STREAK = "Streak";
    public static final String CAT_PLAN = "Daily Plan";
    public static final String CAT_REFLECTION = "Reflection";
    public static final String CAT_CHALLENGE = "Challenge";

    public static final String CATEGORY_BLOOM = CAT_GARDEN;
    public static final String CATEGORY_FOCUS = CAT_FOCUS;
    public static final String CATEGORY_STREAK = CAT_STREAK;
    public static final String CATEGORY_DAILY_PLAN = CAT_PLAN;
    public static final String CATEGORY_REFLECTION = CAT_REFLECTION;
    public static final String CATEGORY_CHALLENGE = CAT_CHALLENGE;
    public static final String CATEGORY_WELCOME_BACK = "Welcome Back";

    private String id;
    private String title;
    private String description;
    private String category;
    private long timestamp;
    private String iconEmoji;
    private boolean isFavorite;
    private String personalNote;
    private String milestoneKey; // Unique key to guarantee deduplication
    private int statsValue;
    private String monthYear;

    public MemoryItem() {
    }

    public MemoryItem(String id, String title, String description, String category,
                      long timestamp, String iconEmoji, boolean isFavorite,
                      String personalNote, String milestoneKey, int statsValue) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.iconEmoji = iconEmoji != null ? iconEmoji : "🌱";
        this.isFavorite = isFavorite;
        this.personalNote = personalNote != null ? personalNote : "";
        this.milestoneKey = milestoneKey != null ? milestoneKey : id;
        this.statsValue = statsValue;

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        this.monthYear = sdf.format(new Date(this.timestamp));
    }

    public String getId() {
        return id;
    }

    public String getMemoryId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getIconEmoji() {
        return iconEmoji != null ? iconEmoji : "🌱";
    }

    public String getEmojiIcon() {
        return getIconEmoji();
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getPersonalNote() {
        return personalNote != null ? personalNote : "";
    }

    public void setPersonalNote(String personalNote) {
        this.personalNote = personalNote;
    }

    public String getMilestoneKey() {
        return milestoneKey;
    }

    public void setMilestoneKey(String milestoneKey) {
        this.milestoneKey = milestoneKey;
    }

    public int getStatsValue() {
        return statsValue;
    }

    public void setStatsValue(int statsValue) {
        this.statsValue = statsValue;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("timestamp", timestamp);
        map.put("iconEmoji", iconEmoji);
        map.put("isFavorite", isFavorite);
        map.put("personalNote", personalNote);
        map.put("milestoneKey", milestoneKey);
        map.put("statsValue", statsValue);
        map.put("monthYear", monthYear);
        return map;
    }
}
