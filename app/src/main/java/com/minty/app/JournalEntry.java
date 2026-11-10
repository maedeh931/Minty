package com.minty.app;

public class JournalEntry {

    private String entryId;
    private long timestamp;
    private String topicWorkedOn;
    private String keyTakeaway;
    private int productivityRating; // 1 to 5 stars
    private String notes;

    public JournalEntry() {
    }

    public JournalEntry(String entryId, long timestamp, String topicWorkedOn,
                        String keyTakeaway, int productivityRating, String notes) {
        this.entryId = entryId;
        this.timestamp = timestamp;
        this.topicWorkedOn = topicWorkedOn;
        this.keyTakeaway = keyTakeaway;
        this.productivityRating = productivityRating;
        this.notes = notes;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopicWorkedOn() {
        return topicWorkedOn;
    }

    public void setTopicWorkedOn(String topicWorkedOn) {
        this.topicWorkedOn = topicWorkedOn;
    }

    public String getKeyTakeaway() {
        return keyTakeaway;
    }

    public void setKeyTakeaway(String keyTakeaway) {
        this.keyTakeaway = keyTakeaway;
    }

    public int getProductivityRating() {
        return productivityRating;
    }

    public void setProductivityRating(int productivityRating) {
        this.productivityRating = productivityRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
