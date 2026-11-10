package com.minty.app;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton manager for Cloud Firestore in Minty.
 * Demonstrates:
 * - Collections, documents, and fields
 * - set() and add() operations
 * - get() for one-shot retrieval and querying
 * - update() for targeted field modifications
 * - delete() for document removal
 * - addSnapshotListener() for real-time document/collection change tracking
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";

    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;

    public interface TaskSyncListener {
        void onTasksSynced(List<Task> remoteTasks);
        void onError(String error);
    }

    public interface HistoryCallback {
        void onHistoryLoaded(List<Task> completedTasks);
        void onError(String error);
    }

    public interface GardenCallback {
        void onProgressLoaded(GardenProgress progress);
        void onError(String error);
    }

    public interface SyncCallback {
        void onComplete(boolean success, @Nullable String error);
    }

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            db.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore offline persistence enabled");
        } catch (Exception e) {
            Log.d(TAG, "Firestore settings already applied: " + e.getMessage());
        }
    }

    public static FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager();
                }
            }
        }
        return instance;
    }

    @Nullable
    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Nullable
    private CollectionReference getTasksCollection() {
        String uid = getCurrentUid();
        if (uid == null) return null;
        return db.collection("users").document(uid).collection("tasks");
    }

    @Nullable
    private CollectionReference getSessionsCollection() {
        String uid = getCurrentUid();
        if (uid == null) return null;
        return db.collection("users").document(uid).collection("sessions");
    }

    @Nullable
    private DocumentReference getGardenDocument() {
        String uid = getCurrentUid();
        if (uid == null) return null;
        return db.collection("users").document(uid).collection("garden").document("status");
    }

    // =========================================================================
    // 1. TASKS SYNCHRONIZATION (set, update, delete, addSnapshotListener)
    // =========================================================================

    /**
     * Adds or updates a task in Firestore using set() with a deterministic document ID.
     */
    public void setTask(@NonNull Task task) {
        setTask(task, (SyncCallback) null);
    }

    public void setTask(@NonNull Task task, @Nullable SyncCallback callback) {
        CollectionReference tasksRef = getTasksCollection();
        if (tasksRef == null) {
            Log.w(TAG, "setTask: User unauthenticated, skipping Firestore sync");
            if (callback != null) callback.onComplete(false, "User unauthenticated");
            return;
        }

        String docId = "task_" + task.getId();
        tasksRef.document(docId).set(task.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "set: Task document saved successfully: " + docId);
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "set: Error saving task: " + docId, e);
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    public void setTask(@NonNull Task task, @Nullable OnSuccessListener<Void> successListener) {
        setTask(task, (success, error) -> {
            if (success && successListener != null) {
                successListener.onSuccess(null);
            }
        });
    }

    /**
     * Updates only task completion status using update().
     */
    public void updateTaskCompletion(long taskId, boolean completed, long completedAt) {
        CollectionReference tasksRef = getTasksCollection();
        if (tasksRef == null) return;

        String docId = "task_" + taskId;
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", completed);
        updates.put("completedAt", completedAt);

        tasksRef.document(docId).update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "update: Task completion updated: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "update: Failed to update completion for " + docId, e));
    }

    /**
     * Deletes a task from Firestore using delete().
     */
    public void deleteTask(long taskId, @Nullable OnSuccessListener<Void> successListener) {
        CollectionReference tasksRef = getTasksCollection();
        if (tasksRef == null) return;

        String docId = "task_" + taskId;
        tasksRef.document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "delete: Task deleted from Firestore: " + docId);
                    if (successListener != null) {
                        successListener.onSuccess(aVoid);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "delete: Failed to delete task: " + docId, e));
    }

    /**
     * Attaches a real-time listener to the tasks collection using addSnapshotListener().
     */
    public ListenerRegistration listenToTasks(@NonNull TaskSyncListener listener) {
        CollectionReference tasksRef = getTasksCollection();
        if (tasksRef == null) {
            listener.onError("User unauthenticated");
            return null;
        }

        return tasksRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "addSnapshotListener: Error listening to tasks", e);
                listener.onError(e.getMessage());
                return;
            }

            if (queryDocumentSnapshots != null) {
                boolean fromCache = queryDocumentSnapshots.getMetadata().isFromCache();
                Log.d(TAG, "addSnapshotListener: Received snapshot (fromCache=" + fromCache + ")");

                List<Task> tasks = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Task task = documentToTask(doc);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                listener.onTasksSynced(tasks);
            }
        });
    }

    /**
     * Parses a complete Task object from a Firestore DocumentSnapshot including all planner fields.
     */
    @Nullable
    public static Task documentToTask(@NonNull DocumentSnapshot doc) {
        Long id = doc.getLong("id");
        String title = doc.getString("title");
        if (id == null || title == null) return null;

        Long minutes = doc.getLong("minutes");
        Boolean completed = doc.getBoolean("completed");
        Long createdAt = doc.getLong("createdAt");
        Long completedAt = doc.getLong("completedAt");
        String description = doc.getString("description");
        Long targetDateMillis = doc.getLong("targetDateMillis");
        Long startTimeMillis = doc.getLong("startTimeMillis");
        Long deadlineMillis = doc.getLong("deadlineMillis");
        Long priority = doc.getLong("priority");
        String category = doc.getString("category");
        Long reminderMinutes = doc.getLong("reminderMinutes");
        Boolean reminderScheduled = doc.getBoolean("reminderScheduled");

        return new Task(
                id,
                title,
                minutes != null ? minutes.intValue() : 0,
                completed != null ? completed : false,
                createdAt != null ? createdAt : 0,
                completedAt != null ? completedAt : 0,
                description != null ? description : "",
                targetDateMillis != null ? targetDateMillis : 0,
                startTimeMillis != null ? startTimeMillis : 0,
                deadlineMillis != null ? deadlineMillis : 0,
                priority != null ? priority.intValue() : Task.PRIORITY_MEDIUM,
                category != null ? category : Task.CATEGORY_GENERAL,
                reminderMinutes != null ? reminderMinutes.intValue() : 0,
                reminderScheduled != null ? reminderScheduled : false
        );
    }

    /**
     * One-shot fetch for completed task history using get().
     */
    public void fetchCompletedHistory(@NonNull HistoryCallback callback) {
        CollectionReference tasksRef = getTasksCollection();
        if (tasksRef == null) {
            callback.onError("User unauthenticated");
            return;
        }

        tasksRef.whereEqualTo("completed", true)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> completed = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = documentToTask(doc);
                        if (task != null) {
                            completed.add(task);
                        }
                    }
                    Log.d(TAG, "get: Loaded " + completed.size() + " completed tasks from Firestore");
                    callback.onHistoryLoaded(completed);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "get: Failed to query completed tasks", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Persists user profile attributes (name, email) in Firestore under users/{uid}.
     */
    public void saveUserProfile(String displayName, String email) {
        String uid = getCurrentUid();
        if (uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName != null ? displayName : "");
        data.put("email", email != null ? email : "");
        data.put("updatedAt", System.currentTimeMillis());
        db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "saveUserProfile: synced to Firestore"))
                .addOnFailureListener(e -> Log.w(TAG, "saveUserProfile: failed", e));
    }

    // =========================================================================
    // 2. FOCUS SESSIONS (add operation)
    // =========================================================================

    /**
     * Records a completed or stopped focus/scroll session using collection().add().
     */
    public void recordFocusSession(@NonNull FocusSession session) {
        recordFocusSession(session, (SyncCallback) null);
    }

    public void recordFocusSession(@NonNull FocusSession session, @Nullable SyncCallback callback) {
        CollectionReference sessionsRef = getSessionsCollection();
        if (sessionsRef == null) {
            Log.w(TAG, "recordFocusSession: User unauthenticated, skipping session log");
            if (callback != null) callback.onComplete(false, "User unauthenticated");
            return;
        }

        sessionsRef.add(session.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "add: Session logged with auto-generated ID: " + docRef.getId());
                    session.setSessionId(docRef.getId());
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "add: Failed to log focus session", e);
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    public void recordFocusSession(@NonNull FocusSession session, @Nullable OnSuccessListener<DocumentReference> listener) {
        CollectionReference sessionsRef = getSessionsCollection();
        if (sessionsRef == null) {
            Log.w(TAG, "recordFocusSession: User unauthenticated, skipping session log");
            return;
        }

        sessionsRef.add(session.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "add: Session logged with auto-generated ID: " + docRef.getId());
                    session.setSessionId(docRef.getId());
                    if (listener != null) {
                        listener.onSuccess(docRef);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "add: Failed to log focus session", e));
    }

    // =========================================================================
    // 3. GARDEN PROGRESS (set and get operations)
    // =========================================================================

    /**
     * Updates garden progress using document().set().
     */
    public void saveGardenProgress(@NonNull GardenProgress progress) {
        saveGardenProgress(progress, (SyncCallback) null);
    }

    public void saveGardenProgress(@NonNull GardenProgress progress, @Nullable SyncCallback callback) {
        DocumentReference gardenRef = getGardenDocument();
        if (gardenRef == null) {
            if (callback != null) callback.onComplete(false, "User unauthenticated");
            return;
        }

        gardenRef.set(progress.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "set: Garden progress updated to " + progress.getStageName());
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "set: Failed to save garden progress", e);
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    /**
     * Fetches garden progress using document().get().
     */
    public void fetchGardenProgress(@NonNull GardenCallback callback) {
        DocumentReference gardenRef = getGardenDocument();
        if (gardenRef == null) {
            callback.onError("User unauthenticated");
            return;
        }

        gardenRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String stage = doc.getString("stageName");
                        Long totalMinutes = doc.getLong("totalMinutesGrown");
                        Long level = doc.getLong("growthLevel");
                        Long lastWatered = doc.getLong("lastWateredAt");

                        GardenProgress progress = new GardenProgress(
                                stage != null ? stage : "Mint Seedling",
                                totalMinutes != null ? totalMinutes.intValue() : 0,
                                level != null ? level.intValue() : 1,
                                lastWatered != null ? lastWatered : System.currentTimeMillis()
                        );
                        callback.onProgressLoaded(progress);
                    } else {
                        // Default progress
                        callback.onProgressLoaded(new GardenProgress("Mint Seedling", 0, 1, System.currentTimeMillis()));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "get: Failed to read garden status", e);
                    callback.onError(e.getMessage());
                });
    }
}
