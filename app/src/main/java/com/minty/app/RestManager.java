package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * RestManager manages Minty's peaceful Rest Mode.
 * Allows users to take mindful breaks (1 hour, until tomorrow morning, or custom duration).
 * Pauses optional productivity reminders while preserving streaks, garden growth, and task records.
 * NEVER suppresses an active focus timer's completion alert.
 */
public class RestManager {

    private static final String TAG = "RestManager";
    private static final String PREFS_NAME = "minty_rest_prefs";

    private static final String KEY_REST_ACTIVE = "rest_active";
    private static final String KEY_REST_UNTIL_MILLIS = "rest_until_millis";
    private static final String KEY_REST_STARTED_AT = "rest_started_at";
    private static final String KEY_REST_TYPE = "rest_type";

    public static final String TYPE_POWER_REST = "Power Rest (1 Hour)";
    public static final String TYPE_UNTIL_TOMORROW = "Rest Until Morning (7:00 AM)";
    public static final String TYPE_CUSTOM = "Mindful Rest";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Starts Rest Mode for a specified duration in milliseconds.
     */
    public static void startRest(Context context, long durationMillis, String restType) {
        if (context == null || durationMillis <= 0) return;

        long now = System.currentTimeMillis();
        long until = now + durationMillis;

        getPrefs(context).edit()
                .putBoolean(KEY_REST_ACTIVE, true)
                .putLong(KEY_REST_STARTED_AT, now)
                .putLong(KEY_REST_UNTIL_MILLIS, until)
                .putString(KEY_REST_TYPE, restType != null ? restType : TYPE_CUSTOM)
                .apply();

        Log.d(TAG, "startRest: activated until " + until + " (" + restType + ")");
    }

    /**
     * Starts Rest Mode until morning at 7:00 AM (tomorrow, or today if currently before 7:00 AM).
     */
    public static void startRestUntilTomorrowMorning(Context context) {
        Calendar targetMorning = Calendar.getInstance();
        if (targetMorning.get(Calendar.HOUR_OF_DAY) >= 7) {
            targetMorning.add(Calendar.DAY_OF_YEAR, 1);
        }
        targetMorning.set(Calendar.HOUR_OF_DAY, 7);
        targetMorning.set(Calendar.MINUTE, 0);
        targetMorning.set(Calendar.SECOND, 0);
        targetMorning.set(Calendar.MILLISECOND, 0);

        long duration = Math.max(3600000L, targetMorning.getTimeInMillis() - System.currentTimeMillis());
        startRest(context, duration, TYPE_UNTIL_TOMORROW);
    }

    /**
     * Ends Rest Mode, restoring normal task reminders.
     */
    public static void endRest(Context context) {
        if (context == null) return;

        getPrefs(context).edit()
                .putBoolean(KEY_REST_ACTIVE, false)
                .putLong(KEY_REST_UNTIL_MILLIS, 0)
                .apply();

        Log.d(TAG, "endRest: Rest mode deactivated. Restoring task reminder alarms.");
        ReminderManager.rescheduleAllAlarms(context);
    }

    /**
     * Returns true if Rest Mode is currently active and not expired.
     */
    public static boolean isRestActive(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = getPrefs(context);
        boolean active = prefs.getBoolean(KEY_REST_ACTIVE, false);
        if (!active) return false;

        long until = prefs.getLong(KEY_REST_UNTIL_MILLIS, 0);
        if (System.currentTimeMillis() >= until) {
            // Rest mode has expired; automatically clean up and restore reminders
            endRest(context);
            return false;
        }
        return true;
    }

    /**
     * Returns remaining milliseconds of rest, or 0 if inactive.
     */
    public static long getRemainingMillis(Context context) {
        if (!isRestActive(context)) return 0;
        long until = getPrefs(context).getLong(KEY_REST_UNTIL_MILLIS, 0);
        return Math.max(0, until - System.currentTimeMillis());
    }

    public static long getRestUntilMillis(Context context) {
        return getPrefs(context).getLong(KEY_REST_UNTIL_MILLIS, 0);
    }

    public static String getRestType(Context context) {
        return getPrefs(context).getString(KEY_REST_TYPE, TYPE_POWER_REST);
    }

    public static String getFormattedUntilTime(Context context) {
        long until = getRestUntilMillis(context);
        if (until <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(until));
    }
}
