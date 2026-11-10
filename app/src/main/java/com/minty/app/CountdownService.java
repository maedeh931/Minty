package com.minty.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;
import java.util.UUID;

/**
 * Foreground Service that manages the scroll countdown timer.
 * Posts an ongoing foreground notification, sends TICK broadcasts every second,
 * warns at 30 seconds, and alerts when scroll time has ended.
 */
public class CountdownService extends Service {

    private static final String TAG = "CountdownService";

    public static final String CHANNEL_ID = "minty_timer";
    public static final String CHANNEL_NAME = "Minty timer";

    public static final String ACTION_TICK = "com.minty.app.TICK";
    public static final String ACTION_STOP = "com.minty.app.ACTION_STOP";
    public static final String ACTION_PAUSE = "com.minty.app.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.minty.app.ACTION_RESUME";
    public static final String EXTRA_MINUTES = "extra_minutes";
    public static final String EXTRA_REMAINING_SECONDS = "remaining_seconds";
    public static final String EXTRA_FINISHED = "finished";
    public static final String EXTRA_SESSION_TYPE = "extra_session_type";
    public static final String SESSION_TYPE_SCROLL = "scroll";
    public static final String SESSION_TYPE_FOCUS = "focus";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    private static final int NOTIFICATION_ID_FOREGROUND = 1001;
    private static final int NOTIFICATION_ID_WARNING = 1002;
    private static final int NOTIFICATION_ID_FINISHED = 1003;

    // Public static fields accessible from activities
    public static boolean isRunning = false;
    public static boolean isPaused = false;
    public static long remainingSeconds = 0;
    public static String currentSessionType = SESSION_TYPE_SCROLL;
    public static long currentTaskId = -1;
    public static String currentTaskTitle = "";

    private CountDownTimer countDownTimer;
    private boolean hasWarned30Seconds = false;
    private boolean hasVibratedOnFinish = false;
    private SystemEventReceiver systemEventReceiver;
    private boolean isBatteryLow = false;
    private int totalMinutes = 0;
    private long sessionStartTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: service created");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: service starting");
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopTimerAndSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_PAUSE.equals(intent.getAction())) {
            pauseTimer();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_RESUME.equals(intent.getAction())) {
            resumeTimer();
            return START_NOT_STICKY;
        }

        int minutes = intent != null ? intent.getIntExtra(EXTRA_MINUTES, 1) : 1;
        currentSessionType = (intent != null && intent.hasExtra(EXTRA_SESSION_TYPE))
                ? intent.getStringExtra(EXTRA_SESSION_TYPE) : SESSION_TYPE_SCROLL;
        currentTaskId = intent != null ? intent.getLongExtra(EXTRA_TASK_ID, -1) : -1;
        currentTaskTitle = (intent != null && intent.hasExtra(EXTRA_TASK_TITLE))
                ? intent.getStringExtra(EXTRA_TASK_TITLE) : "";

        startCountdown(minutes);
        return START_NOT_STICKY;
    }

    public class CountdownBinder extends Binder {
        public CountdownService getService() {
            return CountdownService.this;
        }

        public boolean isTimerRunning() {
            return CountdownService.isRunning;
        }

        public long getRemainingSeconds() {
            return CountdownService.this.remainingSeconds;
        }

        public String getSessionType() {
            return CountdownService.this.currentSessionType;
        }

        public String getTaskTitle() {
            return CountdownService.this.currentTaskTitle;
        }
    }

    private final IBinder binder = new CountdownBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: client bound to CountdownService");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: client unbound from CountdownService");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: service being destroyed");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isRunning = false;
        isPaused = false;
        remainingSeconds = 0;
        isBatteryLow = false;

        // Unregister dynamic SystemEventReceiver
        if (systemEventReceiver != null) {
            try {
                unregisterReceiver(systemEventReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "systemEventReceiver not registered", e);
            }
            systemEventReceiver = null;
        }

        try {
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground", e);
        }
    }

    /**
     * Initializes the CountDownTimer and starts foreground execution immediately.
     *
     * @param minutes Total duration in minutes
     */
    private void startCountdown(int minutes) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        totalMinutes = minutes;
        remainingSeconds = minutes * 60L;
        isRunning = true;
        isPaused = false;
        hasWarned30Seconds = false;
        hasVibratedOnFinish = false;
        sessionStartTime = System.currentTimeMillis();

        // Push initial active timer state to Firebase Realtime Database
        RealtimeDbManager.getInstance().saveActiveTimer(currentTaskId, currentTaskTitle, totalMinutes, remainingSeconds, true);

        // Register dynamic SystemEventReceiver while timer runs
        if (systemEventReceiver == null) {
            systemEventReceiver = new SystemEventReceiver((message, warning) -> {
                if ("Battery is low. Save your progress and charge soon.".equals(message)) {
                    isBatteryLow = true;
                    updateForegroundNotification(formatTime(remainingSeconds));
                    Toast.makeText(CountdownService.this, "Battery low", Toast.LENGTH_SHORT).show();
                } else if ("Battery OK".equals(message)) {
                    isBatteryLow = false;
                    updateForegroundNotification(formatTime(remainingSeconds));
                } else if (message.startsWith("Airplane mode") || "Back online.".equals(message)) {
                    Toast.makeText(CountdownService.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            IntentFilter filter = SystemEventReceiver.createIntentFilter();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(systemEventReceiver, filter);
            }
        }

        // Build and display ongoing foreground notification immediately
        Notification initialNotification = buildForegroundNotification(formatTime(remainingSeconds));
        startForeground(NOTIFICATION_ID_FOREGROUND, initialNotification);

        // Initial broadcast
        sendTickBroadcast(remainingSeconds, false);

        startTimerWithMillis(remainingSeconds * 1000L);
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isPaused = true;
        updateForegroundNotification(formatTime(remainingSeconds) + " (Paused)");
    }

    private void resumeTimer() {
        if (!isPaused || remainingSeconds <= 0) return;
        isPaused = false;
        startTimerWithMillis(remainingSeconds * 1000L);
    }

    private void startTimerWithMillis(long totalMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = Math.round(millisUntilFinished / 1000.0);
                updateForegroundNotification(formatTime(remainingSeconds));
                sendTickBroadcast(remainingSeconds, false);

                // Sync active timer to Realtime Database periodically (every 5 seconds or final 5 seconds)
                if (remainingSeconds % 5 == 0 || remainingSeconds <= 5) {
                    RealtimeDbManager.getInstance().saveActiveTimer(currentTaskId, currentTaskTitle, totalMinutes, remainingSeconds, true);
                }

                // Post a separate high-importance "Time almost up" alert when 30 seconds are left (once only)
                if (remainingSeconds <= 30 && remainingSeconds > 0 && !hasWarned30Seconds) {
                    hasWarned30Seconds = true;
                    postWarningNotification();
                }
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish: countdown completed for " + currentSessionType);
                remainingSeconds = 0;
                isRunning = false;
                isPaused = false;
                sendTickBroadcast(0, true);
                triggerCompletionAlert();

                // Clear active timer in Firebase Realtime Database
                RealtimeDbManager.getInstance().clearActiveTimer();

                if (SESSION_TYPE_FOCUS.equals(currentSessionType)) {
                    // Complete task in SQLite and sync to Firestore & RTDB
                    DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
                    if (currentTaskId > 0) {
                        boolean completed = dbHelper.completeTask(currentTaskId);
                        if (completed) {
                            RealtimeDbManager.getInstance().adjustBalanceInCloud(totalMinutes);
                            FirestoreManager.getInstance().updateTaskCompletion(currentTaskId, true, System.currentTimeMillis());
                        }
                    } else {
                        int currentBal = dbHelper.getBalance();
                        dbHelper.updateBalanceDirect(currentBal + totalMinutes);
                        RealtimeDbManager.getInstance().adjustBalanceInCloud(totalMinutes);
                    }
                    int newBalance = dbHelper.getBalance();

                    // Record focus session in Firestore
                    FocusSession focusSession = new FocusSession(
                            UUID.randomUUID().toString(),
                            currentTaskId,
                            currentTaskTitle.isEmpty() ? "Focus Task" : currentTaskTitle,
                            totalMinutes,
                            SESSION_TYPE_FOCUS,
                            sessionStartTime,
                            System.currentTimeMillis(),
                            true
                    );
                    FirestoreManager.getInstance().recordFocusSession(focusSession);

                    // Award verified focus minutes to Virtual Garden (with session deduplication)
                    GardenManager.getInstance(getApplicationContext()).onFocusSessionCompleted(
                            focusSession.getSessionId(),
                            totalMinutes,
                            currentTaskTitle.isEmpty() ? "Focus Session" : currentTaskTitle
                    );

                    StreakManager.recordCompletion(getApplicationContext());

                    // Chronicle milestones in The Little Memory Library
                    try {
                        MemoryManager memoryMgr = MemoryManager.getInstance(getApplicationContext());
                        memoryMgr.onFirstFocusSession(totalMinutes);
                        memoryMgr.checkPersonalBestSession(totalMinutes);
                        int totalEarned = dbHelper.getTotalMinutesEarned();
                        memoryMgr.checkFocusMilestone(totalEarned);
                        int currentStreak = StreakManager.getCurrentStreak(getApplicationContext());
                        memoryMgr.checkStreakMilestone(currentStreak);
                    } catch (Exception e) {
                        Log.e(TAG, "Error recording memory milestone: " + e.getMessage());
                    }

                    // Record Daily Quest progress
                    try {
                        DailyQuestManager.getInstance(getApplicationContext()).recordProgress(DailyQuestManager.TYPE_FOCUS, 1);
                    } catch (Exception e) {
                        Log.e(TAG, "Error recording focus daily quest: " + e.getMessage());
                    }

                    // Notify BuddyManager for collaborative challenge progress & shared stats
                    BuddyManager.getInstance(getApplicationContext()).onFocusSessionFinished(totalMinutes);
                    if (currentTaskId > 0) {
                        Task completedTask = dbHelper.getTaskById(currentTaskId);
                        if (completedTask != null) {
                            BuddyManager.getInstance(getApplicationContext()).onTaskCompleted(completedTask);
                        }
                    }

                    postFocusCompletedNotification();
                } else {
                    // Scroll session finished
                    FocusSession scrollSession = new FocusSession(
                            UUID.randomUUID().toString(),
                            -1,
                            "Scroll Session",
                            totalMinutes,
                            SESSION_TYPE_SCROLL,
                            sessionStartTime,
                            System.currentTimeMillis(),
                            true
                    );
                    FirestoreManager.getInstance().recordFocusSession(scrollSession);
                    postFinishedNotification();
                }

                try {
                    stopForeground(true);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping foreground on finish", e);
                }
                stopSelf();
            }
        }.start();
    }

    /**
     * Stops the timer and cancels foreground notification.
     */
    private void stopTimerAndSelf() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isRunning = false;
        isPaused = false;
        remainingSeconds = 0;
        RealtimeDbManager.getInstance().clearActiveTimer();
        try {
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground", e);
        }
        stopSelf();
    }

    /**
     * Broadcasts the TICK action with remaining seconds and finished flag.
     */
    private void sendTickBroadcast(long seconds, boolean finished) {
        Intent tickIntent = new Intent(ACTION_TICK);
        tickIntent.setPackage(getPackageName());
        tickIntent.putExtra(EXTRA_REMAINING_SECONDS, seconds);
        tickIntent.putExtra(EXTRA_FINISHED, finished);
        sendBroadcast(tickIntent);
    }

    /**
     * Creates the NotificationChannel on API 26+.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for Minty scroll countdown timer");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Builds the ongoing foreground notification with appropriate Activity PendingIntent.
     */
    private Notification buildForegroundNotification(String timeText) {
        PendingIntent pendingIntent = createTargetActivityPendingIntent(0);

        String title;
        String contentText;
        if (SESSION_TYPE_FOCUS.equals(currentSessionType)) {
            title = "Minty Focus Timer";
            contentText = "Focusing: " + (currentTaskTitle.isEmpty() ? "Task" : currentTaskTitle) + " (" + timeText + ")";
        } else {
            title = "Minty Scroll Timer";
            contentText = "Remaining time: " + timeText;
        }

        if (isBatteryLow) {
            contentText += " - Battery low";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Updates the ongoing foreground notification with current remaining time.
     */
    private void updateForegroundNotification(String timeText) {
        Notification notification = buildForegroundNotification(timeText);
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_FOREGROUND, notification);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException updating notification", e);
        }
    }

    /**
     * Posts a separate high-importance "Time almost up" alert when 30 seconds remain.
     */
    private void postWarningNotification() {
        Log.d(TAG, "postWarningNotification: posting 30 seconds warning alert");
        PendingIntent pendingIntent = createTargetActivityPendingIntent(1);

        String warningText = SESSION_TYPE_FOCUS.equals(currentSessionType)
                ? "Only 30 seconds of focus left! Finish strong!"
                : "Only 30 seconds of scroll time left!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time almost up")
                .setContentText(warningText)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_WARNING, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException posting warning notification", e);
        }
    }

    /**
     * Posts a high-importance "Time's up" notification when the countdown finishes.
     */
    private void postFinishedNotification() {
        Log.d(TAG, "postFinishedNotification: posting finished notification");
        PendingIntent pendingIntent = createTargetActivityPendingIntent(2);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time's up")
                .setContentText("Your scroll time has finished. Back to growing!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_FINISHED, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException posting finished notification", e);
        }
    }

    /**
     * Posts a high-importance notification when a focus task completes successfully.
     */
    private void postFocusCompletedNotification() {
        Log.d(TAG, "postFocusCompletedNotification: posting focus task completed alert");
        PendingIntent pendingIntent = createTargetActivityPendingIntent(3);

        String title = "Focus Session Completed! 🌟";
        String message = "Great job! +" + totalMinutes + " minty minutes credited to your balance.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_FINISHED, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException posting focus completed notification", e);
        }
    }

    /**
     * Triggers vibration and audible completion chime when timer reaches zero,
     * respecting user preferences with duplicate-alert prevention.
     */
    private void triggerCompletionAlert() {
        if (hasVibratedOnFinish) {
            return; // Duplicate-alert prevention
        }
        hasVibratedOnFinish = true;

        SharedPreferences prefs = getSharedPreferences("minty_profile_prefs", Context.MODE_PRIVATE);
        boolean hapticsEnabled = prefs.getBoolean("haptics_enabled", true);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);

        // 1. Completion Vibration (Android 12+ VibratorManager, Android 8+ VibrationEffect, and legacy Vibrator)
        if (hapticsEnabled) {
            try {
                Vibrator vibrator = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    if (vibratorManager != null) {
                        vibrator = vibratorManager.getDefaultVibrator();
                    }
                }
                if (vibrator == null) {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    long[] pattern = new long[]{0, 500, 200, 500};
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                        vibrator.vibrate(effect);
                    } else {
                        vibrator.vibrate(pattern, -1);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error triggering completion vibration", e);
            }
        }

        // 2. Completion Ringtone / Chime
        if (soundEnabled) {
            try {
                Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                }
                if (alertUri != null) {
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alertUri);
                    if (r != null) {
                        r.play();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing completion sound", e);
            }
        }
    }

    /**
     * Creates an immutable PendingIntent to reopen either MainActivity (for focus) or SpendActivity (for scroll).
     */
    private PendingIntent createTargetActivityPendingIntent(int requestCode) {
        Class<?> targetClass = SESSION_TYPE_FOCUS.equals(currentSessionType) ? MainActivity.class : SpendActivity.class;
        Intent intent = new Intent(this, targetClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, requestCode, intent, flags);
    }

    /**
     * Formats seconds into mm:ss display format.
     */
    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }
}
