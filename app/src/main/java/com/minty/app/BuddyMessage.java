package com.minty.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an encouragement message between connected buddies.
 */
public class BuddyMessage {

    public static final String TYPE_QUICK = "quick";
    public static final String TYPE_CUSTOM = "custom";

    private String messageId;
    private String senderUid;
    private String senderName;
    private String receiverUid;
    private String receiverName;
    private String message;
    private String messageType;
    private long createdAt;
    private boolean isRead;

    public BuddyMessage() {
        // Required for Firestore
    }

    public BuddyMessage(String messageId, String senderUid, String senderName, String receiverUid,
                        String receiverName, String message, String messageType, long createdAt, boolean isRead) {
        this.messageId = messageId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.receiverUid = receiverUid;
        this.receiverName = receiverName;
        this.message = message;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageType() {
        return messageType != null ? messageType : TYPE_QUICK;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("senderUid", senderUid);
        map.put("senderName", getSenderName());
        map.put("receiverUid", receiverUid);
        map.put("receiverName", getReceiverName());
        map.put("message", getMessage());
        map.put("messageType", getMessageType());
        map.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis());
        map.put("isRead", isRead);
        return map;
    }

    public static BuddyMessage fromMap(String docId, Map<String, Object> data) {
        if (data == null) return null;
        String messageId = (String) data.get("messageId");
        if (messageId == null || messageId.isEmpty()) messageId = docId;

        String senderUid = (String) data.get("senderUid");
        String senderName = (String) data.get("senderName");
        String receiverUid = (String) data.get("receiverUid");
        String receiverName = (String) data.get("receiverName");
        String message = (String) data.get("message");
        String messageType = (String) data.get("messageType");
        Long createdAt = (Long) data.get("createdAt");
        Boolean isRead = (Boolean) data.get("isRead");

        return new BuddyMessage(
                messageId,
                senderUid != null ? senderUid : "",
                senderName != null ? senderName : "Minty Friend",
                receiverUid != null ? receiverUid : "",
                receiverName != null ? receiverName : "Minty Friend",
                message != null ? message : "",
                messageType != null ? messageType : TYPE_QUICK,
                createdAt != null ? createdAt : System.currentTimeMillis(),
                isRead != null ? isRead : false
        );
    }
}
