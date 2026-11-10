package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a two-way buddy connection request.
 * Deterministic ID: senderUid + "_" + receiverUid.
 */
public class BuddyRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    private String requestId;
    private String senderUid;
    private String senderName;
    private String senderUsername;
    private String receiverUid;
    private String receiverName;
    private String status;
    private long createdAt;
    private long updatedAt;

    public BuddyRequest() {
        // Required for Firestore
    }

    public BuddyRequest(String requestId, String senderUid, String senderName, String senderUsername,
                        String receiverUid, String receiverName, String status, long createdAt, long updatedAt) {
        this.requestId = requestId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.senderUsername = senderUsername;
        this.receiverUid = receiverUid;
        this.receiverName = receiverName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String buildRequestId(String senderUid, String receiverUid) {
        return senderUid + "_" + receiverUid;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName != null && !senderName.isEmpty() ? senderName : "Minty Friend";
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderUsername() {
        return senderUsername != null ? senderUsername : "";
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public void setReceiverUid(String receiverUid) {
        this.receiverUid = receiverUid;
    }

    public String getReceiverName() {
        return receiverName != null && !receiverName.isEmpty() ? receiverName : "Minty Friend";
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getStatus() {
        return status != null ? status : STATUS_PENDING;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("senderUid", senderUid);
        map.put("senderName", getSenderName());
        map.put("senderUsername", getSenderUsername());
        map.put("receiverUid", receiverUid);
        map.put("receiverName", getReceiverName());
        map.put("status", getStatus());
        map.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis());
        map.put("updatedAt", updatedAt > 0 ? updatedAt : System.currentTimeMillis());
        return map;
    }

    public static BuddyRequest fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String requestId = (String) data.get("requestId");
        if (requestId == null || requestId.isEmpty()) requestId = docId;

        String senderUid = (String) data.get("senderUid");
        String senderName = (String) data.get("senderName");
        String senderUsername = (String) data.get("senderUsername");
        String receiverUid = (String) data.get("receiverUid");
        String receiverName = (String) data.get("receiverName");
        String status = (String) data.get("status");
        Long createdAt = (Long) data.get("createdAt");
        Long updatedAt = (Long) data.get("updatedAt");

        return new BuddyRequest(
                requestId,
                senderUid != null ? senderUid : "",
                senderName != null ? senderName : "Minty Friend",
                senderUsername != null ? senderUsername : "",
                receiverUid != null ? receiverUid : "",
                receiverName != null ? receiverName : "Minty Friend",
                status != null ? status : STATUS_PENDING,
                createdAt != null ? createdAt : System.currentTimeMillis(),
                updatedAt != null ? updatedAt : System.currentTimeMillis()
        );
    }
}
