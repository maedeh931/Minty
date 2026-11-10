package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a user's public profile in Minty.
 * Searchable by username or invitation code without exposing private email or phone.
 */
public class UserProfile {

    private String uid;
    private String displayName;
    private String username;
    private String invitationCode;
    private String photoUrl;
    private String titleLevel;
    private long updatedAt;

    public UserProfile() {
        // Required for Firestore
    }

    public UserProfile(String uid, String displayName, String username, String invitationCode,
                       String photoUrl, String titleLevel, long updatedAt) {
        this.uid = uid;
        this.displayName = displayName;
        this.username = username;
        this.invitationCode = invitationCode;
        this.photoUrl = photoUrl;
        this.titleLevel = titleLevel;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid != null ? uid : "";
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName != null && !displayName.isEmpty() ? displayName : "Minty Friend";
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInvitationCode() {
        return invitationCode != null ? invitationCode : "";
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTitleLevel() {
        return titleLevel != null && !titleLevel.isEmpty() ? titleLevel : "Beginner";
    }

    public void setTitleLevel(String titleLevel) {
        this.titleLevel = titleLevel;
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
        map.put("displayName", getDisplayName());
        map.put("username", getUsername());
        map.put("invitationCode", getInvitationCode());
        map.put("photoUrl", photoUrl != null ? photoUrl : "");
        map.put("titleLevel", getTitleLevel());
        map.put("updatedAt", updatedAt > 0 ? updatedAt : System.currentTimeMillis());
        return map;
    }

    public static UserProfile fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String uid = (String) data.get("uid");
        if (uid == null || uid.isEmpty()) uid = docId;
        String displayName = (String) data.get("displayName");
        String username = (String) data.get("username");
        String invitationCode = (String) data.get("invitationCode");
        String photoUrl = (String) data.get("photoUrl");
        String titleLevel = (String) data.get("titleLevel");
        Long updatedAt = (Long) data.get("updatedAt");

        return new UserProfile(
                uid,
                displayName != null ? displayName : "Minty Friend",
                username != null ? username : "",
                invitationCode != null ? invitationCode : "",
                photoUrl,
                titleLevel != null ? titleLevel : "Beginner",
                updatedAt != null ? updatedAt : System.currentTimeMillis()
        );
    }
}
