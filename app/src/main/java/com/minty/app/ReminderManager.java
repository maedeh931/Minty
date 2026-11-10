package com.minty.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Manages exact task reminder alarms using Android's AlarmManager.
 * Automatically respects reminder preferences and handles API version compatibility.
 */
public class ReminderManager {

    private static final String TAG = "ReminderManager";
    private static final String PREFS_NAME = "minty_profile_prefs";
    private static final String KEY_REMINDERS = "reminders_enabled";

    /**
     * Schedules a task reminder alarm.
     *
     * @param context         Android context
     * @param task            The Task model
     */
    public static void scheduleTaskReminder(Context context, Task task) {
        if (context == null || task == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_REMINDERS, true);
        if (!enabled) {
            Log.d(TAG, "Task reminders disabled in preferences; skipping taskId=" + task.getId());
            return;
        }

        if (task.getReminderMinutes() <= 0) {
            return; // No reminder requested
        }

        long triggerTime = 0;
        if (task.getStartTimeMillis() > 0) {
            triggerTime = task.getStartTimeMillis() - (task.getReminderMinutes() * 60000L);
        } else if (task.getTargetDateMillis() > 0) {
            // Default morning trigger at 09:00 AM of target date
            triggerTime = task.getTargetDateMillis() + (9 * 3600000L) - (task.getReminderMinutes() * 60000L);
        }

        // Only schedule if trigger time is in the future
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Trigger time is in the past; skipping taskId=" + task.getId());
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction(TaskReminderReceiver.ACTION_REMINDER);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                intent,
                flags
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Scheduled reminder for task " + task.getId() + " at " + triggerTime);
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm permission missing, fallback to standard alarm", e);
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    /**
     * Cancels an existing task reminder alarm.
     */
    public static void cancelTaskReminder(Context context, long taskId) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction(TaskReminderReceiver.ACTION_REMINDER);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) taskId, intent, flags);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled reminder for task " + taskId);
        }
    }

    /**
     * Reschedules a snooze alarm for X minutes from now.
     */
    public static void scheduleSnooze(Context context, long taskId, String taskTitle, int minutes) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long triggerTime = System.currentTimeMillis() + (minutes * 60000L);
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction(TaskReminderReceiver.ACTION_REMINDER);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, taskTitle);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) taskId, intent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Snoozed task " + taskId + " for " + minutes + " min");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling snooze alarm", e);
        }
    }

    /**
     * Reschedules all pending tasks with reminders, e.g. on system boot.
     */
    public static void rescheduleAllAlarms(Context context) {
        if (context == null) return;
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Task> pending = dbHelper.getPendingTasks();
        for (Task t : pending) {
            if (t.getReminderMinutes() > 0) {
                scheduleTaskReminder(context, t);
            }
        }
        Log.d(TAG, "Rescheduled " + pending.size() + " task alarms on boot");
    }
}
