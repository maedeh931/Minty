package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Manages daily task completion streaks using SharedPreferences.
 * Rules:
 * - Counts consecutive days with at least 1 completed task.
 * - Does not increment twice on the same day.
 * - Resets to 1 if a day is missed.
 */
public class StreakManager {

    private static final String PREFS_NAME = "minty_streak_prefs";
    private static final String KEY_STREAK_COUNT = "streak_count";
    private static final String KEY_LAST_DATE = "last_completed_date";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Records a completed task and updates the streak count.
     *
     * @param context Application/Activity context
     * @return The updated streak count
     */
    public static int recordCompletion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0);
        String lastDateStr = prefs.getString(KEY_LAST_DATE, null);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        String todayStr = sdf.format(new Date());

        // Do not increase twice on the same day
        if (todayStr.equals(lastDateStr)) {
            return currentStreak > 0 ? currentStreak : 1;
        }

        int newStreak;
        if (lastDateStr == null) {
            newStreak = 1;
        } else {
            try {
                Date lastDate = sdf.parse(lastDateStr);
                Date today = sdf.parse(todayStr);
                long diffMillis = today.getTime() - lastDate.getTime();
                long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

                if (diffDays == 1) {
                    // Consecutive day
                    newStreak = currentStreak + 1;
                } else {
                    // Missed one or more days -> reset to 1
                    newStreak = 1;
                }
            } catch (ParseException e) {
                newStreak = 1;
            }
        }

        prefs.edit()
                .putInt(KEY_STREAK_COUNT, newStreak)
                .putString(KEY_LAST_DATE, todayStr)
                .apply();

        return newStreak;
    }

    /**
     * Returns the current active streak count for display.
     * If yesterday was missed and no task was completed today, returns 0.
     *
     * @param context Application/Activity context
     * @return The active streak count (0 if no streak or expired)
     */
    public static int getDisplayStreak(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int streak = prefs.getInt(KEY_STREAK_COUNT, 0);
        String lastDateStr = prefs.getString(KEY_LAST_DATE, null);

        if (lastDateStr == null || streak == 0) {
            return 0;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date lastDate = sdf.parse(lastDateStr);
            Date today = sdf.parse(sdf.format(new Date()));
            long diffDays = TimeUnit.MILLISECONDS.toDays(today.getTime() - lastDate.getTime());

            // If more than 1 day has passed since last completion, streak is broken
            if (diffDays > 1) {
                return 0;
            }
        } catch (ParseException e) {
            return 0;
        }

        return streak;
    }

    public static int getCurrentStreak(Context context) {
        return getDisplayStreak(context);
    }
}
