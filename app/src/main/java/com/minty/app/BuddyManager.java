package com.minty.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Singleton manager coordinating all two-way Accountability Buddy features in Minty:
 * - Public profiles & invitation codes
 * - User search & friend invites
 * - Buddy requests (send, accept, reject, cancel)
 * - Connected buddies & Primary Buddy selection
 * - Shared productivity dashboard & privacy controls
 * - Two-way encouragement messages with read receipts
 * - Study challenges with dual progress tracking & completion
 * - Offline SQLite caching and notification delivery
 */
public class BuddyManager {

    private static final String TAG = "BuddyManager";
    public static final String CHANNEL_BUDDY = "minty_buddies";
    public static final String CHANNEL_BUDDY_NAME = "Minty Buddies & Challenges";

    public static final String PREFS_BUDDY = "minty_buddy_prefs";
    public static final String KEY_MY_INVITE_CODE = "my_invite_code";
    public static final String KEY_MY_USERNAME = "my_username";
    public static final String KEY_PRIMARY_BUDDY_UID = "primary_buddy_uid";
    public static final String KEY_SHARE_TASKS = "share_tasks";
    public static final String KEY_SHARE_FOCUS = "share_focus";
    public static final String KEY_SHARE_STREAK = "share_streak";
    public static final String KEY_SHARE_BALANCE = "share_balance";
    public static final String KEY_SHARE_WEEKLY = "share_weekly";
    public static final String KEY_SHARING_ENABLED = "sharing_enabled";

    private static volatile BuddyManager instance;
    private final Context appContext;
    private final FirebaseFirestore db;
    private final DatabaseHelper dbHelper;

    public interface ActionCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface UserSearchCallback {
        void onResults(List<UserProfile> users);
        void onError(String error);
    }

    public interface RequestListener {
        void onRequestsUpdated(List<BuddyRequest> requests);
        void onError(String error);
    }

    public interface BuddyListListener {
        void onBuddiesUpdated(List<UserProfile> buddies, String primaryBuddyUid);
        void onError(String error);
    }

    public interface StatsCallback {
        void onStatsLoaded(SharedStats stats);
        void onError(String error);
    }

    public interface MessageListListener {
        void onMessagesUpdated(List<BuddyMessage> messages);
        void onError(String error);
    }

    public interface ChallengeListListener {
        void onChallengesUpdated(List<StudyChallenge> activeChallenges, List<StudyChallenge> pendingChallenges);
        void onError(String error);
    }

    private BuddyManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.dbHelper = new DatabaseHelper(appContext);
        createNotificationChannel();
    }

    public static BuddyManager getInstance(Context context) {
        if (instance == null) {
            synchronized (BuddyManager.class) {
                if (instance == null) {
                    instance = new BuddyManager(context);
                }
            }
        }
        return instance;
    }

    @Nullable
    public String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_BUDDY,
                    CHANNEL_BUDDY_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for buddy requests, encouragement messages, and study challenges");
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // =========================================================================
    // 1. PUBLIC PROFILES & INVITATIONS
    // =========================================================================

    /**
     * Ensures the current user has a PublicProfile in Firestore and local preferences.
     */
    public void ensureCurrentUserProfile(@Nullable ActionCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }

        String uid = user.getUid();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);

        String savedCode = prefs.getString(KEY_MY_INVITE_CODE, null);
        String savedUsername = prefs.getString(KEY_MY_USERNAME, null);

        if (savedCode == null || savedCode.isEmpty()) {
            savedCode = generateInvitationCode(uid);
            prefs.edit().putString(KEY_MY_INVITE_CODE, savedCode).apply();
        }

        if (savedUsername == null || savedUsername.isEmpty()) {
            savedUsername = generateUsername(user);
            prefs.edit().putString(KEY_MY_USERNAME, savedUsername).apply();
        }

        final String finalCode = savedCode;
        final String finalUsername = savedUsername;

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            if (user.getEmail() != null) {
                displayName = user.getEmail().split("@")[0];
            } else {
                displayName = "Minty Friend";
            }
        }

        int totalEarned = dbHelper.getTotalMinutesEarned();
        String titleLevel;
        if (totalEarned < 60) {
            titleLevel = "Beginner";
        } else if (totalEarned < 300) {
            titleLevel = "Focused";
        } else {
            titleLevel = "Champion";
        }

        UserProfile profile = new UserProfile(
                uid,
                displayName,
                finalUsername,
                finalCode,
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                titleLevel,
                System.currentTimeMillis()
        );

        db.collection("publicProfiles").document(uid)
                .set(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Public profile registered for " + finalUsername + " (code: " + finalCode + ")");
                    publishMySharedStats();
                    if (callback != null) callback.onSuccess("Profile active: " + finalUsername);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to save public profile: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public String getMyInvitationCode() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        String code = prefs.getString(KEY_MY_INVITE_CODE, null);
        if (code == null) {
            String uid = getCurrentUid();
            code = generateInvitationCode(uid);
            prefs.edit().putString(KEY_MY_INVITE_CODE, code).apply();
        }
        return code;
    }

    public String getMyUsername() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        String username = prefs.getString(KEY_MY_USERNAME, null);
        if (username == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            username = generateUsername(user);
            prefs.edit().putString(KEY_MY_USERNAME, username).apply();
        }
        return username;
    }

    public String getInvitationShareText() {
        String code = getMyInvitationCode();
        return "Hey! I'm using Minty to stay focused and productive. Add me with my invitation code "
                + code + " so we can motivate each other! 🌱";
    }

    private String generateInvitationCode(String uid) {
        if (uid != null && uid.length() >= 4) {
            String suffix = uid.substring(uid.length() - 4).toUpperCase(Locale.US);
            return "MINT-" + suffix;
        }
        return "MINT-" + ((int) (1000 + Math.random() * 9000));
    }

    private String generateUsername(FirebaseUser user) {
        if (user != null && user.getEmail() != null) {
            String prefix = user.getEmail().split("@")[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(Locale.US);
            return "@" + prefix;
        }
        return "@minty_" + ((int) (100 + Math.random() * 900));
    }

    // =========================================================================
    // 2. SEARCH MINTY USERS
    // =========================================================================

    /**
     * Searches registered public profiles by username or invitation code.
     * Excludes private emails and the searcher's own profile.
     */
    public void searchUsers(@NonNull String queryText, @NonNull UserSearchCallback callback) {
        String myUid = getCurrentUid();
        String cleanQuery = queryText.trim();
        if (cleanQuery.isEmpty()) {
            callback.onResults(new ArrayList<>());
            return;
        }

        // Support searching with or without '@'
        String usernameQuery = cleanQuery.startsWith("@") ? cleanQuery.toLowerCase(Locale.US) : ("@" + cleanQuery.toLowerCase(Locale.US));
        String codeQuery = cleanQuery.toUpperCase(Locale.US);

        db.collection("publicProfiles")
                .whereEqualTo("username", usernameQuery)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserProfile> results = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        UserProfile up = UserProfile.fromMap(doc.getId(), doc.getData());
                        if (up != null && !up.getUid().equals(myUid)) {
                            results.add(up);
                        }
                    }

                    if (!results.isEmpty()) {
                        callback.onResults(results);
                    } else {
                        // Search by invitation code fallback
                        searchByInvitationCode(codeQuery, myUid, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Search by username failed, trying code query: " + e.getMessage());
                    searchByInvitationCode(codeQuery, myUid, callback);
                });
    }

    private void searchByInvitationCode(String codeQuery, String myUid, UserSearchCallback callback) {
        db.collection("publicProfiles")
                .whereEqualTo("invitationCode", codeQuery)
                .get()
                .addOnSuccessListener(codeSnapshots -> {
                    List<UserProfile> results = new ArrayList<>();
                    for (DocumentSnapshot doc : codeSnapshots.getDocuments()) {
                        UserProfile up = UserProfile.fromMap(doc.getId(), doc.getData());
                        if (up != null && !up.getUid().equals(myUid)) {
                            results.add(up);
                        }
                    }
                    callback.onResults(results);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================================
    // 3. BUDDY REQUESTS & CONNECTIONS
    // =========================================================================

    /**
     * Sends a connection request to another registered Minty user.
     */
    public void sendBuddyRequest(@NonNull UserProfile targetUser, @NonNull ActionCallback callback) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            callback.onError("Please sign in first");
            return;
        }

        if (myUid.equals(targetUser.getUid())) {
            callback.onError("You cannot send a buddy request to yourself.");
            return;
        }

        // Verify not already connected
        String connectionId = BuddyConnection.buildConnectionId(myUid, targetUser.getUid());
        db.collection("buddyConnections").document(connectionId).get()
                .addOnSuccessListener(connDoc -> {
                    if (connDoc != null && connDoc.exists()) {
                        callback.onError("You are already buddies with " + targetUser.getDisplayName() + "!");
                        return;
                    }

                    // Create or update request
                    String requestId = BuddyRequest.buildRequestId(myUid, targetUser.getUid());
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String senderName = currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                            ? currentUser.getDisplayName() : getMyUsername();

                    BuddyRequest request = new BuddyRequest(
                            requestId,
                            myUid,
                            senderName,
                            getMyUsername(),
                            targetUser.getUid(),
                            targetUser.getDisplayName(),
                            BuddyRequest.STATUS_PENDING,
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                    );

                    db.collection("buddyRequests").document(requestId)
                            .set(request.toMap())
                            .addOnSuccessListener(aVoid -> {
                                dbHelper.cacheBuddyRequest(request, false);
                                callback.onSuccess("Buddy request sent to " + targetUser.getDisplayName() + "!");
                            })
                            .addOnFailureListener(e -> callback.onError("Failed to send request: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Accepts a pending incoming request and activates the mutual connection.
     */
    public void acceptBuddyRequest(@NonNull BuddyRequest request, @NonNull ActionCallback callback) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            callback.onError("Please sign in first");
            return;
        }

        // 1. Update request status to accepted
        Map<String, Object> update = new HashMap<>();
        update.put("status", BuddyRequest.STATUS_ACCEPTED);
        update.put("updatedAt", System.currentTimeMillis());

        db.collection("buddyRequests").document(request.getRequestId())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    // 2. Create mutual connection document
                    String connectionId = BuddyConnection.buildConnectionId(request.getSenderUid(), request.getReceiverUid());
                    BuddyConnection connection = new BuddyConnection(
                            connectionId,
                            request.getSenderUid(),
                            request.getReceiverUid(),
                            System.currentTimeMillis(),
                            "active"
                    );

                    db.collection("buddyConnections").document(connectionId)
                            .set(connection.toMap())
                            .addOnSuccessListener(aVoid2 -> {
                                // 3. Cache buddy profile locally
                                UserProfile newBuddy = new UserProfile(
                                        request.getSenderUid(),
                                        request.getSenderName(),
                                        request.getSenderUsername(),
                                        "",
                                        "",
                                        "Buddy",
                                        System.currentTimeMillis()
                                );
                                dbHelper.cacheBuddy(newBuddy, false);
                                dbHelper.deleteCachedRequest(request.getRequestId());

                                // Send celebratory local notification
                                showLocalNotification(
                                        "Buddy Connected! 🌱",
                                        "You and " + request.getSenderName() + " are now Accountability Buddies!",
                                        101
                                );

                                callback.onSuccess("You and " + request.getSenderName() + " are now buddies!");
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Rejects an incoming buddy request.
     */
    public void rejectBuddyRequest(@NonNull BuddyRequest request, @NonNull ActionCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", BuddyRequest.STATUS_REJECTED);
        update.put("updatedAt", System.currentTimeMillis());

        db.collection("buddyRequests").document(request.getRequestId())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    dbHelper.deleteCachedRequest(request.getRequestId());
                    callback.onSuccess("Request rejected");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Cancels an outgoing pending buddy request.
     */
    public void cancelBuddyRequest(@NonNull BuddyRequest request, @NonNull ActionCallback callback) {
        db.collection("buddyRequests").document(request.getRequestId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    dbHelper.deleteCachedRequest(request.getRequestId());
                    callback.onSuccess("Request cancelled");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Real-time listener for incoming pending requests.
     */
    public ListenerRegistration listenToIncomingRequests(@NonNull RequestListener listener) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            listener.onRequestsUpdated(dbHelper.getCachedRequests(true));
            return null;
        }

        return db.collection("buddyRequests")
                .whereEqualTo("receiverUid", myUid)
                .whereEqualTo("status", BuddyRequest.STATUS_PENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        listener.onRequestsUpdated(dbHelper.getCachedRequests(true));
                        listener.onError(e.getMessage());
                        return;
                    }

                    List<BuddyRequest> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            BuddyRequest req = BuddyRequest.fromMap(doc.getId(), doc.getData());
                            if (req != null) {
                                list.add(req);
                                dbHelper.cacheBuddyRequest(req, true);
                            }
                        }
                    }
                    listener.onRequestsUpdated(list);
                });
    }

    /**
     * Real-time listener for sent pending requests.
     */
    public ListenerRegistration listenToSentRequests(@NonNull RequestListener listener) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            listener.onRequestsUpdated(dbHelper.getCachedRequests(false));
            return null;
        }

        return db.collection("buddyRequests")
                .whereEqualTo("senderUid", myUid)
                .whereEqualTo("status", BuddyRequest.STATUS_PENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        listener.onRequestsUpdated(dbHelper.getCachedRequests(false));
                        listener.onError(e.getMessage());
                        return;
                    }

                    List<BuddyRequest> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            BuddyRequest req = BuddyRequest.fromMap(doc.getId(), doc.getData());
                            if (req != null) {
                                list.add(req);
                                dbHelper.cacheBuddyRequest(req, false);
                            }
                        }
                    }
                    listener.onRequestsUpdated(list);
                });
    }

    /**
     * Real-time listener for active connected buddies.
     */
    public ListenerRegistration listenToConnectedBuddies(@NonNull BuddyListListener listener) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            listener.onBuddiesUpdated(dbHelper.getCachedBuddies(), dbHelper.getPrimaryBuddyUid());
            return null;
        }

        // Query connections where myUid is either user1 or user2
        return db.collection("buddyConnections")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onBuddiesUpdated(dbHelper.getCachedBuddies(), dbHelper.getPrimaryBuddyUid());
                        listener.onError(e.getMessage());
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        listener.onBuddiesUpdated(new ArrayList<>(), null);
                        return;
                    }

                    Set<String> buddyUids = new HashSet<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String u1 = doc.getString("user1Uid");
                        String u2 = doc.getString("user2Uid");
                        if (myUid.equals(u1) && u2 != null) buddyUids.add(u2);
                        else if (myUid.equals(u2) && u1 != null) buddyUids.add(u1);
                    }

                    if (buddyUids.isEmpty()) {
                        listener.onBuddiesUpdated(new ArrayList<>(), null);
                        return;
                    }

                    // Fetch public profiles for connected UIDs
                    List<UserProfile> buddyProfiles = new ArrayList<>();
                    String primaryUid = getPrimaryBuddyUid();

                    for (String bUid : buddyUids) {
                        db.collection("publicProfiles").document(bUid).get()
                                .addOnSuccessListener(pDoc -> {
                                    if (pDoc != null && pDoc.exists()) {
                                        UserProfile profile = UserProfile.fromMap(pDoc.getId(), pDoc.getData());
                                        if (profile != null) {
                                            buddyProfiles.add(profile);
                                            dbHelper.cacheBuddy(profile, profile.getUid().equals(primaryUid));
                                        }
                                    }
                                    if (buddyProfiles.size() == buddyUids.size()) {
                                        listener.onBuddiesUpdated(buddyProfiles, primaryUid);
                                    }
                                });
                    }
                });
    }

    public void setPrimaryBuddy(@Nullable String buddyUid, @Nullable ActionCallback callback) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PRIMARY_BUDDY_UID, buddyUid != null ? buddyUid : "").apply();
        dbHelper.setPrimaryBuddy(buddyUid);
        if (callback != null) {
            callback.onSuccess(buddyUid != null ? "Primary buddy updated! 🌟" : "Primary buddy cleared");
        }
    }

    @Nullable
    public String getPrimaryBuddyUid() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        String uid = prefs.getString(KEY_PRIMARY_BUDDY_UID, null);
        if (uid == null || uid.isEmpty()) {
            uid = dbHelper.getPrimaryBuddyUid();
        }
        return uid;
    }

    public void removeBuddyConnection(@NonNull String buddyUid, @NonNull ActionCallback callback) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            callback.onError("Sign in required");
            return;
        }

        String connectionId = BuddyConnection.buildConnectionId(myUid, buddyUid);
        db.collection("buddyConnections").document(connectionId).delete()
                .addOnSuccessListener(aVoid -> {
                    dbHelper.deleteCachedBuddy(buddyUid);
                    if (buddyUid.equals(getPrimaryBuddyUid())) {
                        setPrimaryBuddy(null, null);
                    }
                    callback.onSuccess("Buddy removed.");
                })
                .addOnFailureListener(e -> callback.onError("Failed to remove connection: " + e.getMessage()));
    }

    // =========================================================================
    // 4. SHARED PRODUCTIVITY & PRIVACY CONTROLS
    // =========================================================================

    /**
     * Publishes the user's permitted productivity statistics to /sharedStats/{uid}.
     */
    public void publishMySharedStats() {
        String myUid = getCurrentUid();
        if (myUid == null) return;

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_SHARING_ENABLED, true);
        boolean shareTasks = prefs.getBoolean(KEY_SHARE_TASKS, true);
        boolean shareFocus = prefs.getBoolean(KEY_SHARE_FOCUS, true);
        boolean shareStreak = prefs.getBoolean(KEY_SHARE_STREAK, true);
        boolean shareBal = prefs.getBoolean(KEY_SHARE_BALANCE, false);
        boolean shareWeekly = prefs.getBoolean(KEY_SHARE_WEEKLY, true);

        // Calculate actual stats from SQLite
        List<Task> completedTasks = dbHelper.getCompletedTasks();
        int completedToday = 0;
        int focusMinutesToday = 0;
        int weeklyMinutes = 0;
        int weeklyTasks = 0;

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        long startOfToday = todayCal.getTimeInMillis();

        Calendar weekCal = Calendar.getInstance();
        weekCal.add(Calendar.DAY_OF_YEAR, -7);
        long startOfWeek = weekCal.getTimeInMillis();

        for (Task t : completedTasks) {
            if (t.getCompletedAt() >= startOfToday) {
                completedToday++;
                focusMinutesToday += t.getMinutes();
            }
            if (t.getCompletedAt() >= startOfWeek) {
                weeklyTasks++;
                weeklyMinutes += t.getMinutes();
            }
        }

        int streak = StreakManager.getDisplayStreak(appContext);
        int balance = dbHelper.getBalance();

        int totalEarned = dbHelper.getTotalMinutesEarned();
        String achievement;
        if (totalEarned < 60) achievement = "Beginner";
        else if (totalEarned < 300) achievement = "Focused";
        else achievement = "Champion";

        SharedStats stats = new SharedStats(
                myUid,
                completedToday,
                focusMinutesToday,
                streak,
                balance,
                weeklyMinutes,
                weeklyTasks,
                achievement,
                shareTasks,
                shareFocus,
                shareStreak,
                shareBal,
                shareWeekly,
                enabled,
                System.currentTimeMillis()
        );

        db.collection("sharedStats").document(myUid)
                .set(stats.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Shared productivity stats published to cloud"))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to publish shared stats: " + e.getMessage()));

        dbHelper.cacheSharedStats(stats);
    }

    public void fetchBuddySharedStats(@NonNull String buddyUid, @NonNull StatsCallback callback) {
        db.collection("sharedStats").document(buddyUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        SharedStats stats = SharedStats.fromMap(doc.getId(), doc.getData());
                        if (stats != null) {
                            dbHelper.cacheSharedStats(stats);
                            callback.onStatsLoaded(stats);
                            return;
                        }
                    }
                    // Fallback to local cache
                    SharedStats cached = dbHelper.getCachedSharedStats(buddyUid);
                    if (cached != null) {
                        callback.onStatsLoaded(cached);
                    } else {
                        callback.onStatsLoaded(new SharedStats(buddyUid, 0, 0, 0, -1, 0, 0, "Minty Buddy", true, true, true, false, true, true, 0));
                    }
                })
                .addOnFailureListener(e -> {
                    SharedStats cached = dbHelper.getCachedSharedStats(buddyUid);
                    if (cached != null) {
                        callback.onStatsLoaded(cached);
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void updatePrivacySetting(String key, boolean value) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
        publishMySharedStats();
    }

    public boolean getPrivacySetting(String key, boolean defaultValue) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_BUDDY, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }

    // =========================================================================
    // 5. TWO-WAY ENCOURAGEMENT SYSTEM
    // =========================================================================

    public void sendEncouragementMessage(@NonNull String receiverUid, @NonNull String receiverName,
                                         @NonNull String messageText, @NonNull String messageType,
                                         @NonNull ActionCallback callback) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            callback.onError("Sign in required");
            return;
        }

        if (messageText.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }

        if (messageText.length() > 200) {
            callback.onError("Message too long (max 200 characters)");
            return;
        }

        String messageId = UUID.randomUUID().toString();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String senderName = user != null && user.getDisplayName() != null ? user.getDisplayName() : getMyUsername();

        BuddyMessage message = new BuddyMessage(
                messageId,
                myUid,
                senderName,
                receiverUid,
                receiverName,
                messageText.trim(),
                messageType,
                System.currentTimeMillis(),
                false
        );

        db.collection("buddyMessages").document(messageId)
                .set(message.toMap())
                .addOnSuccessListener(aVoid -> {
                    dbHelper.cacheMessage(message);
                    callback.onSuccess("Encouragement sent! ✨");
                })
                .addOnFailureListener(e -> callback.onError("Failed to send message: " + e.getMessage()));
    }

    public ListenerRegistration listenToMessages(@NonNull String otherUid, @NonNull MessageListListener listener) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            listener.onMessagesUpdated(dbHelper.getCachedMessages(otherUid));
            return null;
        }

        // Realtime query for messages where receiver is myUid and sender is otherUid
        // Or sender is myUid and receiver is otherUid
        return db.collection("buddyMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onMessagesUpdated(dbHelper.getCachedMessages(otherUid));
                        listener.onError(e.getMessage());
                        return;
                    }

                    List<BuddyMessage> conversation = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            BuddyMessage msg = BuddyMessage.fromMap(doc.getId(), doc.getData());
                            if (msg != null) {
                                boolean isBetweenUs = (myUid.equals(msg.getSenderUid()) && otherUid.equals(msg.getReceiverUid()))
                                        || (otherUid.equals(msg.getSenderUid()) && myUid.equals(msg.getReceiverUid()));
                                if (isBetweenUs) {
                                    conversation.add(msg);
                                    dbHelper.cacheMessage(msg);

                                    // Mark incoming message as read if unread
                                    if (myUid.equals(msg.getReceiverUid()) && !msg.isRead()) {
                                        markMessageAsRead(msg.getMessageId());
                                    }
                                }
                            }
                        }
                    }
                    listener.onMessagesUpdated(conversation);
                });
    }

    public void markMessageAsRead(String messageId) {
        if (messageId == null) return;
        dbHelper.markMessageReadLocally(messageId);
        db.collection("buddyMessages").document(messageId)
                .update("isRead", true)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to mark message read in cloud", e));
    }

    // =========================================================================
    // 6. STUDY CHALLENGES
    // =========================================================================

    public void createChallenge(@NonNull String title, @NonNull String type, int target, int durationDays,
                                @NonNull UserProfile buddy, @NonNull ActionCallback callback) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            callback.onError("Sign in required");
            return;
        }

        String challengeId = UUID.randomUUID().toString();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String creatorName = user != null && user.getDisplayName() != null ? user.getDisplayName() : getMyUsername();

        long now = System.currentTimeMillis();
        long end = now + (long) durationDays * 86400000L;

        StudyChallenge challenge = new StudyChallenge(
                challengeId,
                title.trim(),
                type,
                target,
                durationDays,
                now,
                end,
                myUid,
                creatorName,
                buddy.getUid(),
                buddy.getDisplayName(),
                StudyChallenge.STATUS_PENDING,
                0,
                0,
                now,
                0
        );

        db.collection("challenges").document(challengeId)
                .set(challenge.toMap())
                .addOnSuccessListener(aVoid -> {
                    dbHelper.cacheChallenge(challenge);
                    callback.onSuccess("Challenge created! Invitation sent to " + buddy.getDisplayName());
                })
                .addOnFailureListener(e -> callback.onError("Failed to create challenge: " + e.getMessage()));
    }

    public void acceptChallenge(@NonNull StudyChallenge challenge, @NonNull ActionCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", StudyChallenge.STATUS_ACTIVE);

        db.collection("challenges").document(challenge.getChallengeId())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    challenge.setStatus(StudyChallenge.STATUS_ACTIVE);
                    dbHelper.cacheChallenge(challenge);
                    showLocalNotification(
                            "Challenge Accepted! 🎯",
                            "Challenge '" + challenge.getTitle() + "' is now active. Time to focus!",
                            102
                    );
                    callback.onSuccess("Challenge accepted! Let's get growing!");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void rejectChallenge(@NonNull StudyChallenge challenge, @NonNull ActionCallback callback) {
        db.collection("challenges").document(challenge.getChallengeId())
                .update("status", StudyChallenge.STATUS_REJECTED)
                .addOnSuccessListener(aVoid -> {
                    challenge.setStatus(StudyChallenge.STATUS_REJECTED);
                    dbHelper.cacheChallenge(challenge);
                    callback.onSuccess("Challenge declined");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToChallenges(@NonNull ChallengeListListener listener) {
        String myUid = getCurrentUid();
        if (myUid == null) {
            listener.onChallengesUpdated(new ArrayList<>(), new ArrayList<>());
            return null;
        }

        return db.collection("challenges")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onChallengesUpdated(dbHelper.getCachedChallenges(), new ArrayList<>());
                        listener.onError(e.getMessage());
                        return;
                    }

                    List<StudyChallenge> activeList = new ArrayList<>();
                    List<StudyChallenge> pendingList = new ArrayList<>();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            StudyChallenge ch = StudyChallenge.fromMap(doc.getId(), doc.getData());
                            if (ch != null && (myUid.equals(ch.getCreatorUid()) || myUid.equals(ch.getInvitedUid()))) {
                                dbHelper.cacheChallenge(ch);
                                if (StudyChallenge.STATUS_ACTIVE.equals(ch.getStatus())) {
                                    activeList.add(ch);
                                } else if (StudyChallenge.STATUS_PENDING.equals(ch.getStatus())) {
                                    pendingList.add(ch);
                                }
                            }
                        }
                    }
                    listener.onChallengesUpdated(activeList, pendingList);
                });
    }

    /**
     * Called automatically when user completes a task or focus session to advance active challenges.
     */
    public void onProgressEvent(String type, int amount) {
        String myUid = getCurrentUid();
        if (myUid == null || amount <= 0) return;

        publishMySharedStats();

        db.collection("challenges")
                .whereEqualTo("status", StudyChallenge.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null) return;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        StudyChallenge ch = StudyChallenge.fromMap(doc.getId(), doc.getData());
                        if (ch != null && type.equals(ch.getType())) {
                            boolean isCreator = myUid.equals(ch.getCreatorUid());
                            boolean isInvited = myUid.equals(ch.getInvitedUid());

                            if (isCreator || isInvited) {
                                int currentProg = isCreator ? ch.getCreatorProgress() : ch.getInvitedProgress();
                                int updatedProg = currentProg + amount;

                                Map<String, Object> updates = new HashMap<>();
                                if (isCreator) {
                                    updates.put("creatorProgress", updatedProg);
                                } else {
                                    updates.put("invitedProgress", updatedProg);
                                }

                                if (updatedProg >= ch.getTarget()) {
                                    updates.put("status", StudyChallenge.STATUS_COMPLETED);
                                    updates.put("completedAt", System.currentTimeMillis());
                                    showLocalNotification(
                                            "Challenge Completed! 🏆",
                                            "You completed the '" + ch.getTitle() + "' challenge target!",
                                            103
                                    );
                                }

                                db.collection("challenges").document(ch.getChallengeId()).update(updates);
                            }
                        }
                    }
                });
    }

    public void onTaskCompleted(Task task) {
        if (task != null) {
            onProgressEvent(StudyChallenge.TYPE_TASK_COUNT, 1);
        }
    }

    public void onFocusSessionFinished(int minutes) {
        if (minutes > 0) {
            onProgressEvent(StudyChallenge.TYPE_FOCUS_MINUTES, minutes);
        }
    }

    // =========================================================================
    // 7. LOCAL NOTIFICATIONS
    // =========================================================================

    public void showLocalNotification(String title, String message, int notificationId) {
        Intent intent = new Intent(appContext, BuddyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, notificationId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_BUDDY)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted", e);
        }
    }
}
