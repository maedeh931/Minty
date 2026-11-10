package com.minty.app;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton manager for Firebase Realtime Database in Minty.
 * Demonstrates:
 * - DatabaseReference navigation
 * - JSON tree structure: users/{uid}/{balance, preferences, activeTimer, syncInfo}
 * - setValue() for atomic value replacement
 * - getValue() and DataSnapshot parsing
 * - ValueEventListener for real-time reactivity
 * - updateChildren() for multi-path dictionary updates
 * - removeValue() for node deletion
 * - Live presence detection via ".info/connected"
 */
public class RealtimeDbManager {

    private static final String TAG = "RealtimeDbManager";
    private static final String FALLBACK_RTDB_URL = "https://minty-93388-default-rtdb.firebaseio.com";

    private static volatile RealtimeDbManager instance;
    private FirebaseDatabase database;
    private boolean persistenceConfigured = false;

    public interface BalanceListener {
        void onBalanceChanged(int newBalance);
        void onError(String message);
    }

    public interface ConnectionStateListener {
        void onConnectionChanged(boolean isConnected);
    }

    public interface SingleValueCallback<T> {
        void onData(T value);
        void onError(String message);
    }

    public interface ActiveTimerListener {
        void onTimerUpdated(long taskId, String title, long remainingSeconds, boolean isRunning);
        void onTimerCleared();
    }

    private RealtimeDbManager() {
        initDatabase();
    }

    public static RealtimeDbManager getInstance() {
        if (instance == null) {
            synchronized (RealtimeDbManager.class) {
                if (instance == null) {
                    instance = new RealtimeDbManager();
                }
            }
        }
        return instance;
    }

    private synchronized void initDatabase() {
        if (database == null) {
            try {
                database = FirebaseDatabase.getInstance();
            } catch (Exception e) {
                Log.w(TAG, "Default database instance unavailable, falling back to URL: " + FALLBACK_RTDB_URL);
                try {
                    database = FirebaseDatabase.getInstance(FALLBACK_RTDB_URL);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to initialize FirebaseDatabase with fallback URL", ex);
                }
            }

            if (database != null && !persistenceConfigured) {
                try {
                    database.setPersistenceEnabled(true);
                    persistenceConfigured = true;
                    Log.d(TAG, "Realtime Database disk persistence enabled");
                } catch (Exception e) {
                    // Ignored if persistence was already enabled
                    Log.d(TAG, "Persistence already enabled or could not be set: " + e.getMessage());
                }
            }
        }
    }

    @Nullable
    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Nullable
    public DatabaseReference getUserRef() {
        String uid = getCurrentUid();
        if (uid == null || database == null) {
            return null;
        }
        DatabaseReference ref = database.getReference("users").child(uid);
        ref.keepSynced(true);
        return ref;
    }

    // =========================================================================
    // 1. BALANCE SYNCHRONIZATION (setValue, getValue, ValueEventListener)
    // =========================================================================

    /**
     * Pushes the authoritative balance to Realtime Database using setValue().
     */
    public void syncBalanceToCloud(int balance) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) {
            Log.w(TAG, "syncBalanceToCloud: User is unauthenticated, skipping sync");
            return;
        }

        DatabaseReference balanceRef = userRef.child("balance");
        balanceRef.setValue(balance)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "setValue: Cloud balance updated to " + balance);
                    updateLastSyncedAt();
                })
                .addOnFailureListener(e -> Log.e(TAG, "setValue: Failed to update balance", e));
    }

    /**
     * Atomically increments or decrements balance in Realtime Database using runTransaction().
     * Protects against multi-device race conditions.
     *
     * @param deltaMinutes Positive to add minutes, negative to deduct minutes
     */
    public void adjustBalanceInCloud(int deltaMinutes) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        userRef.child("balance").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) {
                    current = 0;
                }
                int newBalance = Math.max(0, current + deltaMinutes);
                currentData.setValue(newBalance);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "adjustBalanceInCloud transaction error: " + error.getMessage());
                } else {
                    Log.d(TAG, "adjustBalanceInCloud transaction committed: " + committed);
                }
                updateLastSyncedAt();
            }
        });
    }

    /**
     * Listens in real-time to balance changes using ValueEventListener.
     */
    public ValueEventListener listenToBalance(@NonNull BalanceListener listener) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) {
            listener.onError("User unauthenticated");
            return null;
        }

        DatabaseReference balanceRef = userRef.child("balance");
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer balance = snapshot.getValue(Integer.class);
                    if (balance != null) {
                        Log.d(TAG, "onDataChange: Live balance received from RTDB: " + balance);
                        listener.onBalanceChanged(balance);
                        return;
                    }
                }
                Log.d(TAG, "onDataChange: Balance node does not exist yet");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Balance listener error: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        balanceRef.addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /**
     * Reads the current balance once using addListenerForSingleValueEvent().
     */
    public void fetchBalanceOnce(@NonNull SingleValueCallback<Integer> callback) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) {
            callback.onError("User unauthenticated");
            return;
        }

        userRef.child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer balance = snapshot.getValue(Integer.class);
                callback.onData(balance != null ? balance : 0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void removeBalanceListener(@Nullable ValueEventListener listener) {
        DatabaseReference userRef = getUserRef();
        if (userRef != null && listener != null) {
            userRef.child("balance").removeEventListener(listener);
        }
    }

    // =========================================================================
    // 2. ACTIVE 50-MIN FOCUS TIMER SYNC (updateChildren, removeValue)
    // =========================================================================

    /**
     * Saves or updates the currently active focus session in Realtime Database.
     */
    public void saveActiveTimer(long taskId, String taskTitle, int totalMinutes,
                                long remainingSeconds, boolean isRunning) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        Map<String, Object> timerData = new HashMap<>();
        timerData.put("taskId", taskId);
        timerData.put("taskTitle", taskTitle);
        timerData.put("totalMinutes", totalMinutes);
        timerData.put("remainingSeconds", remainingSeconds);
        timerData.put("isRunning", isRunning);
        timerData.put("updatedAt", System.currentTimeMillis());

        userRef.child("activeTimer").updateChildren(timerData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "updateChildren: Active timer state updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update active timer", e));
    }

    /**
     * Clears the active timer node when a session finishes using removeValue().
     */
    public void clearActiveTimer() {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        userRef.child("activeTimer").removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "removeValue: Active timer cleared"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to clear active timer", e));
    }

    /**
     * Listens to active focus timer state changes across devices.
     */
    public ValueEventListener listenToActiveTimer(@NonNull ActiveTimerListener listener) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return null;

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long taskId = snapshot.child("taskId").getValue(Long.class);
                    String title = snapshot.child("taskTitle").getValue(String.class);
                    Long remainingSec = snapshot.child("remainingSeconds").getValue(Long.class);
                    Boolean isRunning = snapshot.child("isRunning").getValue(Boolean.class);

                    listener.onTimerUpdated(
                            taskId != null ? taskId : 0,
                            title != null ? title : "",
                            remainingSec != null ? remainingSec : 0,
                            isRunning != null ? isRunning : false
                    );
                } else {
                    listener.onTimerCleared();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Active timer listener cancelled: " + error.getMessage());
            }
        };

        userRef.child("activeTimer").addValueEventListener(eventListener);
        return eventListener;
    }

    // =========================================================================
    // 3. USER PREFERENCES (updateChildren, removeValue)
    // =========================================================================

    /**
     * Updates user preferences atomically using updateChildren().
     */
    public void updatePreferences(@NonNull Map<String, Object> preferences) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        userRef.child("preferences").updateChildren(preferences)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateChildren: Preferences updated");
                    updateLastSyncedAt();
                })
                .addOnFailureListener(e -> Log.e(TAG, "updateChildren: Error updating preferences", e));
    }

    /**
     * Removes a specific preference key using removeValue().
     */
    public void removePreference(@NonNull String key) {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        userRef.child("preferences").child(key).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "removeValue: Removed preference key: " + key))
                .addOnFailureListener(e -> Log.e(TAG, "removeValue: Error removing key: " + key, e));
    }

    // =========================================================================
    // 4. LIVE PRESENCE & SYNC INFO (.info/connected)
    // =========================================================================

    /**
     * Observes live connection state using Firebase Realtime Database ".info/connected".
     */
    public ValueEventListener observeConnectionState(@NonNull ConnectionStateListener listener) {
        if (database == null) {
            listener.onConnectionChanged(false);
            return null;
        }

        DatabaseReference connectedRef = database.getReference(".info/connected");
        ValueEventListener connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                Log.d(TAG, "onDataChange: .info/connected is " + connected);
                listener.onConnectionChanged(connected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Connection listener cancelled: " + error.getMessage());
            }
        };

        connectedRef.addValueEventListener(connectionListener);
        return connectionListener;
    }

    /**
     * Removes the connection state listener.
     */
    public void removeConnectionListener(@Nullable ValueEventListener listener) {
        if (database != null && listener != null) {
            database.getReference(".info/connected").removeEventListener(listener);
        }
    }

    public void updateLastSyncedAt() {
        DatabaseReference userRef = getUserRef();
        if (userRef == null) return;

        long now = System.currentTimeMillis();
        userRef.child("lastSyncTimestamp").setValue(now);

        Map<String, Object> syncMap = new HashMap<>();
        syncMap.put("lastSyncedAt", now);
        syncMap.put("clientVersion", "1.0");

        userRef.child("syncInfo").updateChildren(syncMap);
    }
}
