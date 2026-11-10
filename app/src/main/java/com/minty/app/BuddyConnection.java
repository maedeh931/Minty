package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an active mutual connection between two Minty users.
 * Deterministic ID: alphabetically ordered u1_u2.
 */
public class BuddyConnection {

    private String connectionId;
    private String user1Uid;
    private String user2Uid;
    private long createdAt;
    private String status;

    public BuddyConnection() {
        // Required for Firestore
    }

    public BuddyConnection(String connectionId, String user1Uid, String user2Uid, long createdAt, String status) {
        this.connectionId = connectionId;
        this.user1Uid = user1Uid;
        this.user2Uid = user2Uid;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static String buildConnectionId(String uidA, String uidB) {
        if (uidA == null || uidB == null) return "";
        return uidA.compareTo(uidB) < 0 ? (uidA + "_" + uidB) : (uidB + "_" + uidA);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getUser1Uid() {
        return user1Uid;
    }

    public void setUser1Uid(String user1Uid) {
        this.user1Uid = user1Uid;
    }

    public String getUser2Uid() {
        return user2Uid;
    }

    public void setUser2Uid(String user2Uid) {
        this.user2Uid = user2Uid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status != null ? status : "active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean containsUser(String uid) {
        return uid != null && (uid.equals(user1Uid) || uid.equals(user2Uid));
    }

    public String getOtherUserUid(String myUid) {
        if (myUid == null) return "";
        return myUid.equals(user1Uid) ? user2Uid : user1Uid;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("connectionId", connectionId);
        map.put("user1Uid", user1Uid);
        map.put("user2Uid", user2Uid);
        map.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis());
        map.put("status", getStatus());
        return map;
    }

    public static BuddyConnection fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String connectionId = (String) data.get("connectionId");
        if (connectionId == null || connectionId.isEmpty()) connectionId = docId;

        String user1Uid = (String) data.get("user1Uid");
        String user2Uid = (String) data.get("user2Uid");
        Long createdAt = (Long) data.get("createdAt");
        String status = (String) data.get("status");

        return new BuddyConnection(
                connectionId,
                user1Uid != null ? user1Uid : "",
                user2Uid != null ? user2Uid : "",
                createdAt != null ? createdAt : System.currentTimeMillis(),
                status != null ? status : "active"
        );
    }
}
