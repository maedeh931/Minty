package com.minty.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * BotanicalMaintenanceService demonstrates a background Service lifecycle (onCreate, onStartCommand, onDestroy)
 * complying with modern Android execution restrictions by executing a lightweight background maintenance job
 * and immediately self-terminating via stopSelf().
 */
public class BotanicalMaintenanceService extends Service {

    public static final String ACTION_MAINTENANCE_STATUS = "com.minty.app.ACTION_MAINTENANCE_STATUS";
    public static final String EXTRA_STATUS_MESSAGE = "extra_status_message";
    private static final String TAG = "BotanicalMaintService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: BotanicalMaintenanceService initialized");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Maintenance service started with startId " + startId);

        // Perform background maintenance on a separate worker thread
        new Thread(() -> {
            try {
                // Simulate background garden cache integrity check
                Thread.sleep(1500);

                DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                int activeTasks = db.getPendingTasks().size();
                int balance = db.getBalance();

                String summary = "Garden Maintenance Completed: " + activeTasks + " tasks verified, " + balance + " balance synchronized.";
                Log.d(TAG, summary);

                // Broadcast completion result back to UI
                Intent broadcast = new Intent(ACTION_MAINTENANCE_STATUS);
                broadcast.putExtra(EXTRA_STATUS_MESSAGE, summary);
                sendBroadcast(broadcast);

            } catch (InterruptedException e) {
                Log.e(TAG, "Maintenance thread interrupted", e);
            } finally {
                // Stop service once task is complete
                stopSelf(startId);
                Log.d(TAG, "stopSelf(" + startId + ") called - background service terminated");
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Pure background service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: BotanicalMaintenanceService destroyed");
    }
}
