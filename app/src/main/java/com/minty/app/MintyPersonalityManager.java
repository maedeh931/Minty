package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * MintyPersonalityManager provides a consistent, warm, gentle, and occasionally playful voice
 * for Minty the botanical garden mascot.
 * Fully offline-first, context-aware, anti-repetitive, with zero guilt-tripping.
 */
public class MintyPersonalityManager {

    private static final String TAG = "MintyPersonality";
    private static final String PREFS_NAME = "minty_personality_prefs";
    private static final String KEY_RECENT_INDICES = "recent_note_indices";
    private static final String KEY_LAST_REFRESH_TIME = "last_note_refresh_time";

    private static volatile MintyPersonalityManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Random random = new Random();

    /**
     * Immutable model representing a single friendly note from Minty.
     */
    public static class MintyNote {
        private final String headline;
        private final String message;
        private final String emoji;
        private final String category;

        public MintyNote(String headline, String message, String emoji, String category) {
            this.headline = headline;
            this.message = message;
            this.emoji = emoji;
            this.category = category;
        }

        public String getHeadline() { return headline; }
        public String getMessage() { return message; }
        public String getEmoji() { return emoji; }
        public String getCategory() { return category; }
    }

    private MintyPersonalityManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static MintyPersonalityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MintyPersonalityManager.class) {
                if (instance == null) {
                    instance = new MintyPersonalityManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Returns a context-aware note for the Home screen card.
     */
    public MintyNote getNoteForHomeDashboard() {
        // Priority 1: Peaceful Rest Mode active
        if (RestManager.isRestActive(appContext)) {
            return getRestModeNote();
        }

        // Priority 2: Daily plan completely finished
        DatabaseHelper db = new DatabaseHelper(appContext);
        List<Task> pendingTasks = db.getPendingTasks();
        List<Task> completed = db.getCompletedTasks();

        Calendar todayMidnight = Calendar.getInstance();
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);
        long startOfToday = todayMidnight.getTimeInMillis();

        int completedToday = 0;
        for (Task t : completed) {
            if (t.getCompletedAt() >= startOfToday) {
                completedToday++;
            }
        }

        if (completedToday > 0 && pendingTasks.isEmpty()) {
            return new MintyNote(
                    "All Clear for Today! 🌟",
                    "You finished everything on your focus plan. Your garden is flourishing and your mind deserves a gentle rest 🍃",
                    "🌸",
                    "Celebration"
            );
        }

        // Priority 3: Current streak celebration
        int currentStreak = StreakManager.getCurrentStreak(appContext);
        if (currentStreak >= 7) {
            return new MintyNote(
                    "Deep Roots Formed! 🌲",
                    "A " + currentStreak + "-day streak! Your roots run deep and strong like an ancient willow. Keep flourishing 🌱",
                    "✨",
                    "Milestone"
            );
        }

        // Priority 4: Time-of-day pool with anti-repetition filter
        return getCuratedTimeOfDayNote();
    }

    private MintyNote getRestModeNote() {
        List<MintyNote> restNotes = new ArrayList<>();
        restNotes.add(new MintyNote(
                "Shh... Minty is Sleeping 🌙",
                "Rest is how deep roots grow. Your garden and streaks are completely safe tonight.",
                "🌙",
                "Rest Mode"
        ));
        restNotes.add(new MintyNote(
                "Under the Moonlit Canopy ✨",
                "Fireflies are dancing softly over the garden. Rest up so we can bloom again tomorrow.",
                "✨",
                "Rest Mode"
        ));
        restNotes.add(new MintyNote(
                "Quiet Night Thoughts 🍃",
                "No alarms, no rush. Let your mind recharge in the peaceful stillness.",
                "💤",
                "Rest Mode"
        ));
        return restNotes.get(random.nextInt(restNotes.size()));
    }

    private MintyNote getCuratedTimeOfDayNote() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        List<MintyNote> pool = new ArrayList<>();

        if (hour >= 5 && hour < 12) {
            // Morning Pool
            pool.add(new MintyNote("A Little Note from Minty 🍃", "Good morning! The soil is fresh and the day is full of possibilities. One gentle step at a time 🌱", "🌱", "Morning Calm"));
            pool.add(new MintyNote("Fresh Morning Sunlight ☀️", "Even the tallest sunflower started with just a tiny sprout. Let's water your first goal today! ☕", "☕", "Encouragement"));
            pool.add(new MintyNote("A Little Note from Minty 🍃", "Breathe in the calm morning air. You don't have to conquer everything, just begin gently.", "🌿", "Mindfulness"));
            pool.add(new MintyNote("Sprout Thoughts 🌱", "Have you stretched yet today? A little water for you, and a little focus for your flowers!", "💧", "Playful"));
        } else if (hour >= 12 && hour < 17) {
            // Afternoon Pool
            pool.add(new MintyNote("Afternoon Breeze 🍃", "Midday can feel heavy. Remember that trees don't rush their leaves—they take their time to grow.", "🍃", "Calm"));
            pool.add(new MintyNote("A Little Note from Minty 🍃", "Drop your shoulders and take a slow, deep breath. You're doing better than you think!", "🌸", "Encouragement"));
            pool.add(new MintyNote("Watering Reminder 💧", "Time for a sip of cool water! Both you and your little garden need hydration to thrive.", "💧", "Mindfulness"));
            pool.add(new MintyNote("Playful Garden Whisper 🌼", "Minty just inspected your garden bed: looking very promising today! Keep going!", "🌼", "Playful"));
        } else if (hour >= 17 && hour < 22) {
            // Evening Pool
            pool.add(new MintyNote("Golden Evening Light 🌅", "The sun is setting on your hard work. Celebrate every small win you watered today.", "🌅", "Encouragement"));
            pool.add(new MintyNote("A Little Note from Minty 🍃", "Wind down gently tonight. Growth happens quietly in the soil while we rest.", "🍵", "Calm"));
            pool.add(new MintyNote("Proud of You 🌿", "Showing up is the hardest part, and you did it today. Take pride in your dedication.", "✨", "Reflection"));
            pool.add(new MintyNote("Garden Wisdom 🌾", "A day well spent brings peaceful dreams. Let tomorrow's tasks wait for tomorrow's dawn.", "🌾", "Mindfulness"));
        } else {
            // Late Night Pool
            pool.add(new MintyNote("Late Night Glow 🌙", "It's late, little seed! Sleep is the sacred fertilizer of brilliant ideas. Close your eyes soon.", "🌙", "Sleepy"));
            pool.add(new MintyNote("A Little Note from Minty 🍃", "The garden is fast asleep under the stars. Give your mind permission to rest completely.", "✨", "Calm"));
            pool.add(new MintyNote("Rest Easy 💤", "You did plenty today. Recharge your energy for tomorrow's blossoms.", "💤", "Rest"));
        }

        // Anti-repetition: filter out recent indices
        Set<String> recentSet = prefs.getStringSet(KEY_RECENT_INDICES, new HashSet<>());
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!recentSet.contains(String.valueOf(i))) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            recentSet.clear();
            for (int i = 0; i < pool.size(); i++) availableIndices.add(i);
        }

        int chosenIndex = availableIndices.get(random.nextInt(availableIndices.size()));
        recentSet.add(String.valueOf(chosenIndex));
        prefs.edit().putStringSet(KEY_RECENT_INDICES, recentSet).apply();

        return pool.get(chosenIndex);
    }

    /**
     * Manually advances to another note (e.g. when user taps refresh).
     */
    public MintyNote refreshHomeNote() {
        return getCuratedTimeOfDayNote();
    }

    /**
     * Encouraging cheer right after completing a focus session.
     */
    public String getPostFocusCheer(int minutesCompleted) {
        String[] cheers = new String[]{
                "Incredible dedication! You just nurtured " + minutesCompleted + " minutes of deep growth 🌱",
                "Your little plant drank every drop of those " + minutesCompleted + " focused minutes! 🌸",
                "That was brilliant concentration. Be proud of yourself! ✨",
                "Deep focus completed! Look at your botanical garden flourishing 🌿"
        };
        return cheers[random.nextInt(cheers.length)];
    }

    /**
     * Encouraging cheer when checking off a planned task.
     */
    public String getTaskCompletedCheer(String taskTitle) {
        String[] cheers = new String[]{
                "Check! \"" + taskTitle + "\" is done and dusted! 🎯",
                "Another seed blossomed: \"" + taskTitle + "\" complete! 🌸",
                "Calm and steady progress on \"" + taskTitle + "\". Proud of you! 🌿"
        };
        return cheers[random.nextInt(cheers.length)];
    }
}
