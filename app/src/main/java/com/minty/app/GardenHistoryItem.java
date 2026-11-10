package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing chronological garden history events.
 */
public class GardenHistoryItem {

    public static final String EVENT_PLANTED = "planted";
    public static final String EVENT_STAGE_UP = "stage_up";
    public static final String EVENT_BLOOMED = "bloomed";
    public static final String EVENT_LEVEL_UP = "level_up";
    public static final String EVENT_ACHIEVEMENT = "achievement";
    public static final String EVENT_WATERED = "watered";

    private String historyId;
    private String title;
    private String description;
    private long timestamp;
    private String eventType;
    private String plantSpecies;

    public GardenHistoryItem() {
    }

    public GardenHistoryItem(String historyId, String title, String description,
                             long timestamp, String eventType, String plantSpecies) {
        this.historyId = historyId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.plantSpecies = plantSpecies;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPlantSpecies() {
        return plantSpecies;
    }

    public void setPlantSpecies(String plantSpecies) {
        this.plantSpecies = plantSpecies;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("historyId", historyId);
        map.put("title", title);
        map.put("description", description);
        map.put("timestamp", timestamp);
        map.put("eventType", eventType);
        map.put("plantSpecies", plantSpecies);
        return map;
    }

    public static GardenHistoryItem fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GardenHistoryItem item = new GardenHistoryItem();
        item.setHistoryId((String) map.get("historyId"));
        item.setTitle((String) map.get("title"));
        item.setDescription((String) map.get("description"));

        if (map.containsKey("timestamp") && map.get("timestamp") instanceof Number) {
            item.setTimestamp(((Number) map.get("timestamp")).longValue());
        }
        item.setEventType((String) map.get("eventType"));
        item.setPlantSpecies((String) map.get("plantSpecies"));

        return item;
    }
}
