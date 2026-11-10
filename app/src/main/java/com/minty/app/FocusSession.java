package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a focus or scroll session in Minty.
 * Synchronized with Cloud Firestore collection "users/{uid}/sessions".
 */
public class FocusSession {

    private String sessionId;
    private long taskId;
    private String taskTitle;
    private int minutes;
    private String sessionType; // "focus" (earned minutes) or "scroll" (spent minutes)
    private long startedAt;
    private long completedAt;
    private boolean completedSuccessfully;

    // Required zero-argument constructor for Firestore
    public FocusSession() {
    }

    public FocusSession(String sessionId, long taskId, String taskTitle, int minutes,
                        String sessionType, long startedAt, long completedAt,
                        boolean completedSuccessfully) {
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.minutes = minutes;
        this.sessionType = sessionType;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.completedSuccessfully = completedSuccessfully;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isCompletedSuccessfully() {
        return completedSuccessfully;
    }

    public void setCompletedSuccessfully(boolean completedSuccessfully) {
        this.completedSuccessfully = completedSuccessfully;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", sessionId);
        map.put("taskId", taskId);
        map.put("taskTitle", taskTitle);
        map.put("minutes", minutes);
        map.put("sessionType", sessionType);
        map.put("startedAt", startedAt);
        map.put("completedAt", completedAt);
        map.put("completedSuccessfully", completedSuccessfully);
        return map;
    }
}
