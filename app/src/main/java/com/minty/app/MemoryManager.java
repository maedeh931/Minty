package com.minty.app;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Singleton manager that monitors productivity and automatically chronicles
 * meaningful milestones into The Little Memory Library scrapbook.
 * Completely deduplicated and offline-first with Cloud Firestore sync.
 */
public class MemoryManager {

    private static final String TAG = "MemoryManager";
    private static volatile MemoryManager instance;

    private final Context appContext;
    private final DatabaseHelper dbHelper;

    private MemoryManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(appContext);
    }

    public static MemoryManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MemoryManager.class) {
                if (instance == null) {
                    instance = new MemoryManager(context);
                }
            }
        }
        return instance;
    }

    private CollectionReference getFirestoreMemoriesRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        return FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("memories");
    }

    private void saveAndSyncMemory(MemoryItem memory) {
        boolean added = dbHelper.addMemory(memory);
        if (added) {
            Log.d(TAG, "saveAndSyncMemory: Milestone chronicled: " + memory.getTitle());
            CollectionReference ref = getFirestoreMemoriesRef();
            if (ref != null) {
                ref.document(memory.getId()).set(memory.toMap(), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore sync memory: " + memory.getId()))
                        .addOnFailureListener(e -> Log.w(TAG, "Firestore sync memory failed", e));
            }
        }
    }

    public void updatePersonalNote(String memoryId, String note) {
        dbHelper.updateMemoryPersonalNote(memoryId, note);
        CollectionReference ref = getFirestoreMemoriesRef();
        if (ref != null) {
            ref.document(memoryId).update("personalNote", note);
        }
    }

    public void toggleFavorite(String memoryId, boolean isFavorite) {
        dbHelper.toggleMemoryFavorite(memoryId, isFavorite);
        CollectionReference ref = getFirestoreMemoriesRef();
        if (ref != null) {
            ref.document(memoryId).update("isFavorite", isFavorite);
        }
    }

    public void deleteMemory(String memoryId) {
        dbHelper.deleteMemory(memoryId);
        CollectionReference ref = getFirestoreMemoriesRef();
        if (ref != null) {
            ref.document(memoryId).delete();
        }
    }

    /**
     * Chronicles the user's very first focus session.
     */
    public void onFirstFocusSession(int minutes) {
        String key = "focus_first";
        if (dbHelper.isMemoryExists(key)) return;

        MemoryItem item = new MemoryItem(
                UUID.randomUUID().toString(),
                "First Focus Session ⏳",
                "Planted your very first session of " + minutes + " focused minutes in Minty. The journey of a thousand blossoms began right here.",
                MemoryItem.CAT_FOCUS,
                System.currentTimeMillis(),
                "⏳",
                true,
                "The very first step of my productivity journey 🌱",
                key,
                minutes
        );
        saveAndSyncMemory(item);
    }

    /**
     * Chronicles a flower blooming into full maturity.
     */
    public void onFlowerBloomed(String plantName, String speciesDisplayName) {
        String key = "bloom_" + plantName + "_" + speciesDisplayName;
        if (dbHelper.isMemoryExists(key)) return;

        MemoryItem item = new MemoryItem(
                UUID.randomUUID().toString(),
                plantName + " in Full Bloom! 🌸",
                "Your consistent daily focus nurtured " + plantName + " into a blooming " + speciesDisplayName + " flower.",
                MemoryItem.CAT_GARDEN,
                System.currentTimeMillis(),
                "🌸",
                true,
                "A living symbol of my dedication 💐",
                key,
                100
        );
        saveAndSyncMemory(item);
    }

    /**
     * Checks lifetime focus milestones: 25, 60, 120, 300, 600, and 1000 minutes.
     */
    public void checkFocusMilestone(int totalLifetimeMinutes) {
        int[] milestones = new int[]{25, 60, 120, 300, 600, 1000};
        String[] titles = new String[]{
                "Pomodoro Milestone (25m) ⏳",
                "First Hour of Deep Focus (60m) ⏳",
                "Two Hours of Deep Focus (120m) 🌿",
                "300 Minutes of Dedication 🌟",
                "10 Hours Deep Work Master (600m) 🏆",
                "1,000 Minutes Master Gardener 👑"
        };

        for (int i = 0; i < milestones.length; i++) {
            int threshold = milestones[i];
            if (totalLifetimeMinutes >= threshold) {
                String key = "milestone_focus_" + threshold;
                if (!dbHelper.isMemoryExists(key)) {
                    MemoryItem item = new MemoryItem(
                            UUID.randomUUID().toString(),
                            titles[i],
                            "Surpassed " + threshold + " minutes of genuine focused productivity in Minty.",
                            MemoryItem.CAT_FOCUS,
                            System.currentTimeMillis(),
                            "🏆",
                            threshold >= 120,
                            "",
                            key,
                            threshold
                    );
                    saveAndSyncMemory(item);
                }
            }
        }
    }

    /**
     * Checks if this session is a new single-session personal best.
     */
    public void checkPersonalBestSession(int sessionMinutes) {
        if (sessionMinutes < 25) return;
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("minty_stats_prefs", Context.MODE_PRIVATE);
        int currentBest = prefs.getInt("personal_best_session_minutes", 0);
        if (sessionMinutes > currentBest) {
            prefs.edit().putInt("personal_best_session_minutes", sessionMinutes).apply();
            String key = "personal_best_" + sessionMinutes;
            if (!dbHelper.isMemoryExists(key)) {
                MemoryItem item = new MemoryItem(
                        UUID.randomUUID().toString(),
                        "New Personal Best: " + sessionMinutes + "m! 🚀",
                        "Completed your longest single focus session of " + sessionMinutes + " minutes without distraction.",
                        MemoryItem.CAT_FOCUS,
                        System.currentTimeMillis(),
                        "🚀",
                        true,
                        "",
                        key,
                        sessionMinutes
                );
                saveAndSyncMemory(item);
            }
        }
    }

    /**
     * Chronicles completing an entire daily plan without leaving tasks behind.
     */
    public void onDailyPlanCompleted(int tasksCount) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateKey = sdf.format(new Date());
        String key = "daily_plan_complete_" + dateKey;
        if (dbHelper.isMemoryExists(key)) return;

        SimpleDateFormat friendlyFmt = new SimpleDateFormat("MMMM d", Locale.getDefault());
        String friendlyDate = friendlyFmt.format(new Date());

        MemoryItem item = new MemoryItem(
                UUID.randomUUID().toString(),
                "Flawless Daily Plan 🎯",
                "Accomplished all " + tasksCount + " planned goals for " + friendlyDate + " with calm determination.",
                MemoryItem.CAT_PLAN,
                System.currentTimeMillis(),
                "🎯",
                false,
                "",
                key,
                tasksCount
        );
        saveAndSyncMemory(item);
    }

    /**
     * Chronicles streak milestones: 3, 7, 14, 30 days.
     */
    public void checkStreakMilestone(int streakDays) {
        int[] milestones = new int[]{3, 7, 14, 30};
        String[] titles = new String[]{
                "3-Day Momentum Spark ✨",
                "7-Day Habit Formed! 🔥",
                "Two Weeks of Resilience 🌿",
                "30 Days of Habit Mastery 🌟"
        };

        for (int i = 0; i < milestones.length; i++) {
            int threshold = milestones[i];
            if (streakDays >= threshold) {
                String key = "streak_milestone_" + threshold;
                if (!dbHelper.isMemoryExists(key)) {
                    MemoryItem item = new MemoryItem(
                            UUID.randomUUID().toString(),
                            titles[i],
                            "Maintained an unbroken " + threshold + "-day streak of showing up for personal growth.",
                            MemoryItem.CAT_STREAK,
                            System.currentTimeMillis(),
                            "🔥",
                            threshold >= 7,
                            "",
                            key,
                            threshold
                    );
                    saveAndSyncMemory(item);
                }
            }
        }
    }

    /**
     * Chronicles completing a buddy challenge.
     */
    public void onChallengeCompleted(String challengeTitle) {
        String key = "challenge_" + challengeTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        if (dbHelper.isMemoryExists(key)) return;

        MemoryItem item = new MemoryItem(
                UUID.randomUUID().toString(),
                "Challenge Conquered: " + challengeTitle + " 🤝",
                "Collaborated and finished this accountability challenge with flying colors.",
                MemoryItem.CAT_CHALLENGE,
                System.currentTimeMillis(),
                "🤝",
                false,
                "",
                key,
                1
        );
        saveAndSyncMemory(item);
    }

    /**
     * Chronicles returning after an absence (3+ days).
     */
    public void checkWelcomeBack(long lastActiveTimeMillis) {
        if (lastActiveTimeMillis <= 0) return;
        long diff = System.currentTimeMillis() - lastActiveTimeMillis;
        if (diff >= 3 * 86400000L) { // 3+ days
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String dateKey = sdf.format(new Date());
            String key = "welcome_back_" + dateKey;
            if (!dbHelper.isMemoryExists(key)) {
                MemoryItem item = new MemoryItem(
                        UUID.randomUUID().toString(),
                        "Welcome Back to Your Garden 🌱",
                        "Returning after a break is an act of gentle resilience. Your garden is always waiting for you.",
                        MemoryItem.CAT_REFLECTION,
                        System.currentTimeMillis(),
                        "🌱",
                        false,
                        "Starting fresh today.",
                        key,
                        0
                );
                saveAndSyncMemory(item);
            }
        }
    }

    /**
     * Adds a user-authored weekly reflection card.
     */
    public void addWeeklyReflection(@NonNull String note) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_ww", Locale.getDefault());
        String weekKey = "weekly_reflection_" + sdf.format(new Date());

        SimpleDateFormat friendlyFmt = new SimpleDateFormat("MMMM d", Locale.getDefault());
        String friendlyDate = friendlyFmt.format(new Date());

        MemoryItem item = new MemoryItem(
                UUID.randomUUID().toString(),
                "Weekly Reflection (" + friendlyDate + ") 📝",
                "Reflected on growth, wins, and lessons for the week.",
                MemoryItem.CAT_REFLECTION,
                System.currentTimeMillis(),
                "📝",
                true,
                note,
                weekKey,
                0
        );
        saveAndSyncMemory(item);
    }
}
