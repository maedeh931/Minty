package com.minty.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ValueEventListener;
import com.minty.app.databinding.ActivityDevToolsBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Developer Diagnostics screen.
 * Isolated from the standard student Profile interface to preserve production cleanliness
 * while enabling thorough testing of ContentProvider, Realtime Database, and Cloud Firestore.
 */
public class DevToolsActivity extends AppCompatActivity {

    private static final String TAG = "DevToolsActivity";

    private static final String[] SAMPLE_STUDY_TASKS = {
            "Review Algorithm Notes 💻",
            "Database Lab Report 📝",
            "Math Problem Set #4 📐",
            "Read Chapter 5 in Textbook 📖",
            "Operating Systems Quiz Prep ⚡",
            "Software Engineering Diagram 📊",
            "Physics Problem Solving 🔬",
            "English Essay Outline ✍️"
    };
    private static final int[] SAMPLE_MINUTES = {20, 25, 15, 30, 25, 20, 35, 15};
    private static final String[] SAMPLE_CATEGORIES = {"Study", "Assignment", "Study", "Reading", "Exam Prep", "Project", "Assignment", "Writing"};
    private static int sampleTaskCounter = 0;

    private ActivityDevToolsBinding binding;
    private DatabaseHelper dbHelper;
    private ValueEventListener connectionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDevToolsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        // Toolbar navigation back
        binding.toolbarDevTools.setNavigationOnClickListener(v -> finish());

        // Launch Android Syllabus Interactive Lab
        binding.btnOpenSyllabusLab.setOnClickListener(v -> {
            Intent intent = new Intent(DevToolsActivity.this, SyllabusLabActivity.class);
            startActivity(intent);
        });

        // Setup Virtual Garden Showcase Mode Controls
        setupShowcaseControls();

        // 1. ContentProvider: Insert
        binding.btnProviderInsert.setOnClickListener(v -> insertSampleTaskViaProvider());

        // 2. ContentProvider: Query
        binding.btnProviderQuery.setOnClickListener(v -> queryTasksViaProvider());

        // 3. ContentProvider: Update
        binding.btnProviderUpdate.setOnClickListener(v -> updateLastTaskViaProvider());

        // 4. ContentProvider: Delete
        binding.btnProviderDelete.setOnClickListener(v -> deleteLastTaskViaProvider());

        // 5. RTDB Connection State
        connectionListener = RealtimeDbManager.getInstance().observeConnectionState(connected -> {
            binding.tvCloudStatus.setText(connected ? "RTDB Status: Connected (Online 🟢)" : "RTDB Status: Disconnected (Offline 🔴)");
        });

        // 6. RTDB: setValue (Balance increment)
        binding.btnRtdbSetBalance.setOnClickListener(v -> {
            int newBal = dbHelper.getBalance() + 5;
            dbHelper.updateBalanceDirect(newBal);
            RealtimeDbManager.getInstance().syncBalanceToCloud(newBal);
            binding.tvFirebaseResult.setText("RTDB setValue():\nBalance updated to " + newBal + " minutes.\nPath: /users/{uid}/balance");
            Toast.makeText(this, "RTDB: Balance updated (+5)", Toast.LENGTH_SHORT).show();
        });

        // 7. RTDB: getValue (Balance retrieval)
        binding.btnRtdbGetBalance.setOnClickListener(v -> {
            RealtimeDbManager.getInstance().fetchBalanceOnce(new RealtimeDbManager.SingleValueCallback<Integer>() {
                @Override
                public void onData(Integer balance) {
                    binding.tvFirebaseResult.setText("RTDB getValue():\nFetched cloud balance: " + balance + " minutes\nLocal balance: " + dbHelper.getBalance());
                }

                @Override
                public void onError(String error) {
                    binding.tvFirebaseResult.setText("RTDB getValue() ERROR:\n" + error);
                }
            });
        });

        // 8. RTDB: updateChildren (User preferences)
        binding.btnRtdbUpdatePrefs.setOnClickListener(v -> {
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("theme", "aspen_botanical");
            prefs.put("soundEnabled", true);
            prefs.put("updatedAt", System.currentTimeMillis());

            RealtimeDbManager.getInstance().updatePreferences(prefs);
            binding.tvFirebaseResult.setText("RTDB updateChildren():\nPreferences updated.\nPath: /users/{uid}/preferences");
        });

        // 9. RTDB: removeValue (Remove preference key)
        binding.btnRtdbRemovePref.setOnClickListener(v -> {
            RealtimeDbManager.getInstance().removePreference("soundEnabled");
            binding.tvFirebaseResult.setText("RTDB removeValue():\nRemoved 'soundEnabled' key from preferences.");
        });

        // 10. Firestore: set() (Add or update task)
        binding.btnFirestoreSetTask.setOnClickListener(v -> {
            long newId = System.currentTimeMillis() % 10000;
            Task sampleTask = new Task(newId, "Diagnostic Firestore Task " + newId, 25, false, System.currentTimeMillis(), 0);
            FirestoreManager.getInstance().setTask(sampleTask, (success, error) -> {
                if (success) {
                    binding.tvFirebaseResult.setText("Firestore set():\nTask saved: " + sampleTask.getTitle() + "\nDocument: /users/{uid}/tasks/task_" + newId);
                } else {
                    binding.tvFirebaseResult.setText("Firestore set() ERROR:\n" + error);
                }
            });
        });

        // 11. Firestore: get() (Query completed history)
        binding.btnFirestoreGetHistory.setOnClickListener(v -> {
            FirestoreManager.getInstance().fetchCompletedHistory(new FirestoreManager.HistoryCallback() {
                @Override
                public void onHistoryLoaded(java.util.List<Task> completedTasks) {
                    binding.tvFirebaseResult.setText("Firestore get():\nLoaded " + completedTasks.size() + " completed tasks from Firestore.\nFirst task: "
                            + (completedTasks.isEmpty() ? "None" : completedTasks.get(0).getTitle()));
                }

                @Override
                public void onError(String error) {
                    binding.tvFirebaseResult.setText("Firestore get() ERROR:\n" + error);
                }
            });
        });

        // 12. Firestore: add() (Record focus session)
        binding.btnFirestoreAddSession.setOnClickListener(v -> {
            FocusSession session = new FocusSession(
                    UUID.randomUUID().toString(),
                    101,
                    "Dev Focus Session",
                    25,
                    "focus",
                    System.currentTimeMillis() - 25 * 60000L,
                    System.currentTimeMillis(),
                    true
            );
            FirestoreManager.getInstance().recordFocusSession(session, (success, error) -> {
                if (success) {
                    binding.tvFirebaseResult.setText("Firestore add():\nFocus session recorded with ID: " + session.getSessionId());
                } else {
                    binding.tvFirebaseResult.setText("Firestore add() ERROR:\n" + error);
                }
            });
        });

        // 13. Firestore: Garden status set() and get()
        binding.btnFirestoreGarden.setOnClickListener(v -> {
            GardenProgress garden = GardenProgress.fromMinutes(dbHelper.getBalance());
            FirestoreManager.getInstance().saveGardenProgress(garden, (success, error) -> {
                if (success) {
                    FirestoreManager.getInstance().fetchGardenProgress(new FirestoreManager.GardenCallback() {
                        @Override
                        public void onProgressLoaded(GardenProgress progress) {
                            binding.tvFirebaseResult.setText("Firestore Garden Status:\nStage: " + progress.getStageName()
                                    + "\nMinutes: " + progress.getTotalMinutesGrown()
                                    + "\nLevel: " + progress.getGrowthLevel());
                        }

                        @Override
                        public void onError(String error) {
                            binding.tvFirebaseResult.setText("Firestore Garden get() ERROR:\n" + error);
                        }
                    });
                } else {
                    binding.tvFirebaseResult.setText("Firestore Garden set() ERROR:\n" + error);
                }
            });
        });
    }

    private void insertSampleTaskViaProvider() {
        int idx = Math.abs(sampleTaskCounter++) % SAMPLE_STUDY_TASKS.length;
        String taskTitle = SAMPLE_STUDY_TASKS[idx];
        int mins = SAMPLE_MINUTES[idx];
        String cat = SAMPLE_CATEGORIES[idx];

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TASK_TITLE, taskTitle);
        values.put(DatabaseHelper.COLUMN_TASK_MINUTES, mins);
        values.put(DatabaseHelper.COLUMN_TASK_COMPLETED, 0);
        values.put(DatabaseHelper.COLUMN_TASK_CREATED_AT, System.currentTimeMillis());
        values.put(DatabaseHelper.COLUMN_TASK_CATEGORY, cat);
        values.put(DatabaseHelper.COLUMN_TASK_PRIORITY, Task.PRIORITY_MEDIUM);
        values.put(DatabaseHelper.COLUMN_TASK_TARGET_DATE, Task.getTodayMidnightMillis());

        try {
            Uri insertedUri = getContentResolver().insert(TaskProvider.CONTENT_URI, values);
            if (insertedUri != null) {
                long newTaskId = ContentUris.parseId(insertedUri);
                Task task = new Task(newTaskId, taskTitle, mins, false, System.currentTimeMillis(), 0);
                task.setCategory(cat);
                task.setPriority(Task.PRIORITY_MEDIUM);
                task.setTargetDateMillis(Task.getTodayMidnightMillis());
                FirestoreManager.getInstance().setTask(task);

                binding.tvProviderResult.setText("✅ INSERT SUCCESSFUL!\n"
                        + "• Task ID: #" + newTaskId + "\n"
                        + "• Title: \"" + taskTitle + "\" (" + mins + " min)\n"
                        + "• Category: " + cat + "\n"
                        + "• Saved to: SQLite & Cloud Firestore\n"
                        + "• Content URI: " + insertedUri);
                Toast.makeText(this, "Task: \"" + taskTitle + "\" created via ContentProvider!", Toast.LENGTH_SHORT).show();
            } else {
                binding.tvProviderResult.setText("❌ INSERT FAILED: TaskProvider returned null URI");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting task via Provider", e);
            binding.tvProviderResult.setText("❌ INSERT ERROR:\n" + e.getMessage());
        }
    }

    private void queryTasksViaProvider() {
        try (Cursor cursor = getContentResolver().query(TaskProvider.CONTENT_URI, null, null, null, DatabaseHelper.COLUMN_TASK_ID + " DESC")) {
            if (cursor != null) {
                int totalCount = cursor.getCount();
                if (totalCount == 0) {
                    binding.tvProviderResult.setText("📋 QUERY RESULT:\nNo tasks found in SQLite database.\nTap INSERT to create a sample task!");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📋 QUERY RESULT (Total: ").append(totalCount).append(" tasks):\n");
                int displayed = 0;
                while (cursor.moveToNext() && displayed < 4) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_TITLE));
                    int mins = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_MINUTES));
                    int completed = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_COMPLETED));
                    sb.append("• [ID #").append(id).append("] ")
                            .append(title)
                            .append(" (").append(mins).append("m) — ")
                            .append(completed == 1 ? "DONE ✅" : "PENDING ⏳")
                            .append("\n");
                    displayed++;
                }
                if (totalCount > 4) {
                    sb.append("... and ").append(totalCount - 4).append(" older tasks.");
                }
                binding.tvProviderResult.setText(sb.toString().trim());
                Toast.makeText(this, "Queried " + totalCount + " tasks from TaskProvider", Toast.LENGTH_SHORT).show();
            } else {
                binding.tvProviderResult.setText("❌ QUERY RESULT: Returned null cursor");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying tasks via Provider", e);
            binding.tvProviderResult.setText("❌ QUERY ERROR:\n" + e.getMessage());
        }
    }

    private void updateLastTaskViaProvider() {
        try (Cursor cursor = getContentResolver().query(TaskProvider.CONTENT_URI,
                new String[]{DatabaseHelper.COLUMN_TASK_ID, DatabaseHelper.COLUMN_TASK_TITLE},
                null, null, DatabaseHelper.COLUMN_TASK_ID + " DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                long lastId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_ID));
                String oldTitle = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_TITLE));
                Uri itemUri = ContentUris.withAppendedId(TaskProvider.CONTENT_URI, lastId);

                String updatedTitle = oldTitle.contains("⭐") ? oldTitle : oldTitle + " (Revised ⭐)";
                ContentValues updateValues = new ContentValues();
                updateValues.put(DatabaseHelper.COLUMN_TASK_TITLE, updatedTitle);

                int count = getContentResolver().update(itemUri, updateValues, null, null);
                if (count > 0) {
                    Task updatedTask = dbHelper.getTaskById(lastId);
                    if (updatedTask != null) {
                        FirestoreManager.getInstance().setTask(updatedTask);
                    }
                    binding.tvProviderResult.setText("✅ UPDATE SUCCESSFUL!\n"
                            + "• Task ID: #" + lastId + "\n"
                            + "• New Title: \"" + updatedTitle + "\"\n"
                            + "• Rows Modified in SQLite: " + count + "\n"
                            + "• Synced to Cloud Firestore");
                    Toast.makeText(this, "Task #" + lastId + " updated!", Toast.LENGTH_SHORT).show();
                } else {
                    binding.tvProviderResult.setText("⚠️ UPDATE: 0 rows modified for Task ID #" + lastId);
                }
            } else {
                binding.tvProviderResult.setText("⚠️ UPDATE RESULT:\nNo tasks available in database to update.\nTap INSERT first!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating task via Provider", e);
            binding.tvProviderResult.setText("❌ UPDATE ERROR:\n" + e.getMessage());
        }
    }

    private void deleteLastTaskViaProvider() {
        try (Cursor cursor = getContentResolver().query(TaskProvider.CONTENT_URI,
                new String[]{DatabaseHelper.COLUMN_TASK_ID, DatabaseHelper.COLUMN_TASK_TITLE},
                null, null, DatabaseHelper.COLUMN_TASK_ID + " DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                long lastId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_ID));
                String oldTitle = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_TITLE));
                Uri itemUri = ContentUris.withAppendedId(TaskProvider.CONTENT_URI, lastId);

                int count = getContentResolver().delete(itemUri, null, null);
                if (count > 0) {
                    FirestoreManager.getInstance().deleteTask(lastId, null);
                    binding.tvProviderResult.setText("🗑️ DELETE SUCCESSFUL!\n"
                            + "• Deleted Task ID: #" + lastId + "\n"
                            + "• Deleted Title: \"" + oldTitle + "\"\n"
                            + "• Rows Removed from SQLite: " + count + "\n"
                            + "• Removed from Cloud Firestore");
                    Toast.makeText(this, "Task #" + lastId + " deleted!", Toast.LENGTH_SHORT).show();
                } else {
                    binding.tvProviderResult.setText("⚠️ DELETE: 0 rows removed for Task ID #" + lastId);
                }
            } else {
                binding.tvProviderResult.setText("⚠️ DELETE RESULT:\nNo tasks available in database to delete.\nTap INSERT first!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting task via Provider", e);
            binding.tvProviderResult.setText("❌ DELETE ERROR:\n" + e.getMessage());
        }
    }

    private void setupShowcaseControls() {
        boolean isEnabled = ShowcaseManager.isDemoModeEnabled(this);
        binding.switchShowcaseMode.setChecked(isEnabled);

        int currentSpeed = ShowcaseManager.getGrowthSpeedSeconds(this);
        if (currentSpeed == ShowcaseManager.SPEED_ULTRA_FAST) {
            binding.rbSpeed5s.setChecked(true);
        } else if (currentSpeed == ShowcaseManager.SPEED_PACED) {
            binding.rbSpeed30s.setChecked(true);
        } else {
            binding.rbSpeed15s.setChecked(true);
        }

        binding.switchShowcaseMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ShowcaseManager.setDemoModeEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "Showcase Mode ENABLED 🌱" : "Showcase Mode DISABLED (Normal Mode Active)", Toast.LENGTH_SHORT).show();
        });

        binding.rgShowcaseSpeed.setOnCheckedChangeListener((group, checkedId) -> {
            int speed = ShowcaseManager.SPEED_STANDARD;
            if (checkedId == R.id.rbSpeed5s) {
                speed = ShowcaseManager.SPEED_ULTRA_FAST;
            } else if (checkedId == R.id.rbSpeed30s) {
                speed = ShowcaseManager.SPEED_PACED;
            }
            ShowcaseManager.setGrowthSpeedSeconds(this, speed);
            Toast.makeText(this, "Demo Growth Speed: " + speed + " seconds", Toast.LENGTH_SHORT).show();
        });

        binding.btnOpenGardenShowcase.setOnClickListener(v -> {
            if (!ShowcaseManager.isDemoModeEnabled(this)) {
                ShowcaseManager.setDemoModeEnabled(this, true);
                binding.switchShowcaseMode.setChecked(true);
            }
            Intent intent = new Intent(DevToolsActivity.this, GardenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        binding.btnResetDemoShowcase.setOnClickListener(v -> {
            ShowcaseManager.resetDemoGarden(this);
            Toast.makeText(this, "Demo Garden Reset to Seed! (Real data untouched) 🔄", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionListener != null) {
            RealtimeDbManager.getInstance().removeConnectionListener(connectionListener);
        }
    }
}
