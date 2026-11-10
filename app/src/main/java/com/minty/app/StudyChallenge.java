package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a collaborative study/focus challenge between buddies.
 */
public class StudyChallenge {

    public static final String TYPE_FOCUS_MINUTES = "focus_minutes";
    public static final String TYPE_TASK_COUNT = "task_count";
    public static final String TYPE_STREAK = "streak";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";

    private String challengeId;
    private String title;
    private String type;
    private int target;
    private int durationDays;
    private long startDate;
    private long endDate;
    private String creatorUid;
    private String creatorName;
    private String invitedUid;
    private String invitedName;
    private String status;
    private int creatorProgress;
    private int invitedProgress;
    private long createdAt;
    private long completedAt;

    public StudyChallenge() {
        // Required for Firestore
    }

    public StudyChallenge(String challengeId, String title, String type, int target, int durationDays,
                          long startDate, long endDate, String creatorUid, String creatorName,
                          String invitedUid, String invitedName, String status, int creatorProgress,
                          int invitedProgress, long createdAt, long completedAt) {
        this.challengeId = challengeId;
        this.title = title;
        this.type = type;
        this.target = target;
        this.durationDays = durationDays;
        this.startDate = startDate;
        this.endDate = endDate;
        this.creatorUid = creatorUid;
        this.creatorName = creatorName;
        this.invitedUid = invitedUid;
        this.invitedName = invitedName;
        this.status = status;
        this.creatorProgress = creatorProgress;
        this.invitedProgress = invitedProgress;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getTitle() {
        return title != null ? title : "Study Challenge";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type != null ? type : TYPE_FOCUS_MINUTES;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTarget() {
        return target > 0 ? target : 60;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getDurationDays() {
        return durationDays > 0 ? durationDays : 7;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public String getCreatorName() {
        return creatorName != null && !creatorName.isEmpty() ? creatorName : "Minty Friend";
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getInvitedUid() {
        return invitedUid;
    }

    public void setInvitedUid(String invitedUid) {
        this.invitedUid = invitedUid;
    }

    public String getInvitedName() {
        return invitedName != null && !invitedName.isEmpty() ? invitedName : "Buddy";
    }

    public void setInvitedName(String invitedName) {
        this.invitedName = invitedName;
    }

    public String getStatus() {
        return status != null ? status : STATUS_PENDING;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCreatorProgress() {
        return creatorProgress;
    }

    public void setCreatorProgress(int creatorProgress) {
        this.creatorProgress = creatorProgress;
    }

    public int getInvitedProgress() {
        return invitedProgress;
    }

    public void setInvitedProgress(int invitedProgress) {
        this.invitedProgress = invitedProgress;
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

    public int getMyProgress(String myUid) {
        if (myUid != null && myUid.equals(creatorUid)) {
            return creatorProgress;
        }
        return invitedProgress;
    }

    public int getBuddyProgress(String myUid) {
        if (myUid != null && myUid.equals(creatorUid)) {
            return invitedProgress;
        }
        return creatorProgress;
    }

    public String getTargetDisplay() {
        if (TYPE_FOCUS_MINUTES.equals(type)) {
            return target + " minutes";
        } else if (TYPE_TASK_COUNT.equals(type)) {
            return target + (target == 1 ? " task" : " tasks");
        } else if (TYPE_STREAK.equals(type)) {
            return target + (target == 1 ? " day streak" : " days streak");
        }
        return String.valueOf(target);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("challengeId", challengeId);
        map.put("title", getTitle());
        map.put("type", getType());
        map.put("target", getTarget());
        map.put("durationDays", getDurationDays());
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("creatorUid", creatorUid);
        map.put("creatorName", getCreatorName());
        map.put("invitedUid", invitedUid);
        map.put("invitedName", getInvitedName());
        map.put("status", getStatus());
        map.put("creatorProgress", creatorProgress);
        map.put("invitedProgress", invitedProgress);
        map.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis());
        map.put("completedAt", completedAt);
        return map;
    }

    public static StudyChallenge fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String challengeId = (String) data.get("challengeId");
        if (challengeId == null || challengeId.isEmpty()) challengeId = docId;

        String title = (String) data.get("title");
        String type = (String) data.get("type");
        Long target = (Long) data.get("target");
        Long durationDays = (Long) data.get("durationDays");
        Long startDate = (Long) data.get("startDate");
        Long endDate = (Long) data.get("endDate");
        String creatorUid = (String) data.get("creatorUid");
        String creatorName = (String) data.get("creatorName");
        String invitedUid = (String) data.get("invitedUid");
        String invitedName = (String) data.get("invitedName");
        String status = (String) data.get("status");
        Long creatorProgress = (Long) data.get("creatorProgress");
        Long invitedProgress = (Long) data.get("invitedProgress");
        Long createdAt = (Long) data.get("createdAt");
        Long completedAt = (Long) data.get("completedAt");

        return new StudyChallenge(
                challengeId,
                title != null ? title : "Study Challenge",
                type != null ? type : TYPE_FOCUS_MINUTES,
                target != null ? target.intValue() : 60,
                durationDays != null ? durationDays.intValue() : 7,
                startDate != null ? startDate : System.currentTimeMillis(),
                endDate != null ? endDate : System.currentTimeMillis() + 7 * 86400000L,
                creatorUid != null ? creatorUid : "",
                creatorName != null ? creatorName : "Minty Friend",
                invitedUid != null ? invitedUid : "",
                invitedName != null ? invitedName : "Buddy",
                status != null ? status : STATUS_PENDING,
                creatorProgress != null ? creatorProgress.intValue() : 0,
                invitedProgress != null ? invitedProgress.intValue() : 0,
                createdAt != null ? createdAt : System.currentTimeMillis(),
                completedAt != null ? completedAt : 0
        );
    }
}
