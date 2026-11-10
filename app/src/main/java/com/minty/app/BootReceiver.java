package com.minty.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Static BroadcastReceiver triggered on system boot (BOOT_COMPLETED).
 * Posts an encouraging reminder notification to launch MainActivity.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    public static final String CHANNEL_ID = "minty_reminders";
    public static final String CHANNEL_NAME = "Minty reminders";
    private static final int NOTIFICATION_ID_BOOT = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "onReceive: BOOT_COMPLETED received");

        // Reschedule all exact task alarms
        try {
            ReminderManager.rescheduleAllAlarms(context);
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling task alarms on boot", e);
        }

        // Skip posting if POST_NOTIFICATIONS permission is not granted on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS not granted on boot; skipping notification");
                return;
            }
        }

        // If peaceful Rest Mode is currently active, do not disturb user with boot encouragement reminder
        if (RestManager.isRestActive(context)) {
            Log.d(TAG, "Rest Mode active on boot; skipping boot encouragement reminder");
            return;
        }

        // Create notification channel for reminders on API 26+
        createReminderChannel(context);

        // PendingIntent to reopen MainActivity
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Minty")
                .setContentText("Ready to earn some minty minutes today?")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BOOT, builder.build());
            Log.d(TAG, "Boot reminder notification posted successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while posting boot notification", e);
        }
    }

    /**
     * Creates the reminder notification channel on API 26+.
     */
    private void createReminderChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Daily reminders to earn and spend minty minutes");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
