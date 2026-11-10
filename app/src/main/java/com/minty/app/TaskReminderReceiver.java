package com.minty.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled task reminder fires.
 * Displays a friendly high-priority notification with direct focus action buttons.
 */
public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskReminderReceiver";
    public static final String CHANNEL_ID = "minty_task_reminders";
    public static final String CHANNEL_NAME = "Minty Task Reminders";

    public static final String ACTION_REMINDER = "com.minty.app.ACTION_TASK_REMINDER";
    public static final String ACTION_SNOOZE = "com.minty.app.ACTION_TASK_SNOOZE";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = "Scheduled Task";
        }

        Log.d(TAG, "onReceive: action=" + action + ", taskId=" + taskId + ", title=" + taskTitle);

        if (ACTION_SNOOZE.equals(action)) {
            // Dismiss notification and snooze for 10 minutes
            NotificationManagerCompat.from(context).cancel((int) taskId);
            ReminderManager.scheduleSnooze(context, taskId, taskTitle, 10);
            return;
        }

        // Pause optional task reminders if peaceful Rest Mode is active
        if (RestManager.isRestActive(context)) {
            Log.d(TAG, "Rest Mode active; pausing optional reminder for taskId=" + taskId);
            return;
        }

        createNotificationChannel(context);
        postReminderNotification(context, taskId, taskTitle);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for planned and upcoming tasks in Minty");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void postReminderNotification(Context context, long taskId, String taskTitle) {
        // Main intent opens MainActivity
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                (int) taskId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Snooze intent
        Intent snoozeIntent = new Intent(context, TaskReminderReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
        snoozeIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (taskId + 100000),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Upcoming Task: " + taskTitle + " 🌱")
                .setContentText("It's almost time for your focus session. Ready to grow?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_media_play, "Start Focus", contentPendingIntent)
                .addAction(android.R.drawable.ic_popup_reminder, "Snooze 10m", snoozePendingIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) taskId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted for task reminder", e);
        }
    }
}
