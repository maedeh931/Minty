package com.minty.app;

import android.content.Context;

import java.util.Random;

/**
 * GardenSpiritManager manages the animated mascot interactions, moods,
 * and conversational encouragement for Minty the Garden Spirit.
 */
public class GardenSpiritManager {

    public static final String MOOD_HAPPY = "HAPPY";
    public static final String MOOD_SLEEPY = "SLEEPY";
    public static final String MOOD_ENCOURAGING = "ENCOURAGING";
    public static final String MOOD_HYDRATED = "HYDRATED";

    private static final String[] ENCOURAGING_WHISPERS = {
            "Small steps each day lead to the most beautiful blossoms! 🌱",
            "Root yourself in the present moment. You've got this! ✨",
            "Focus a little. Grow a lot. Every minute of study counts. 🌿",
            "Even mighty oak trees started as tiny acorns dreaming big! 🌳",
            "Deep breath in... and let your productivity bloom. 🌸"
    };

    private static final String[] SLEEPY_WHISPERS = {
            "Shh... Minty is fast asleep in the botanical greenhouse 🌙",
            "Rest mode is active. Quiet time gives plants room to recharge... zZz",
            "Soft moonbeams and restful thoughts. Sweet dreams! 🌟"
    };

    private static final String[] HAPPY_WHISPERS = {
            "Look at that vibrant foliage! You're accomplishing great things! 🌺",
            "Your garden is thriving with every task you complete! 🍃",
            "Such lovely energy today! Let's keep blossoming! 🌼"
    };

    private static GardenSpiritManager instance;
    private final Random random = new Random();

    private GardenSpiritManager() {}

    public static synchronized GardenSpiritManager getInstance() {
        if (instance == null) {
            instance = new GardenSpiritManager();
        }
        return instance;
    }

    public String getCurrentMood(Context context) {
        if (RestManager.isRestActive(context)) {
            return MOOD_SLEEPY;
        }
        DatabaseHelper db = new DatabaseHelper(context);
        int balance = db.getBalance();
        if (balance >= 50) {
            return MOOD_HAPPY;
        }
        return MOOD_ENCOURAGING;
    }

    public String getMascotGreeting(Context context) {
        String mood = getCurrentMood(context);
        switch (mood) {
            case MOOD_SLEEPY:
                return SLEEPY_WHISPERS[random.nextInt(SLEEPY_WHISPERS.length)];
            case MOOD_HAPPY:
                return HAPPY_WHISPERS[random.nextInt(HAPPY_WHISPERS.length)];
            case MOOD_ENCOURAGING:
            default:
                return ENCOURAGING_WHISPERS[random.nextInt(ENCOURAGING_WHISPERS.length)];
        }
    }

    public String getMoodEmoji(Context context) {
        String mood = getCurrentMood(context);
        switch (mood) {
            case MOOD_SLEEPY:
                return "😴";
            case MOOD_HAPPY:
                return "🌸";
            case MOOD_ENCOURAGING:
            default:
                return "🌱";
        }
    }
}
