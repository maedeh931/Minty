package com.minty.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Dynamic BroadcastReceiver for system events:
 * - Battery low / Battery OK
 * - Airplane mode changes
 * - Power connected / Power disconnected
 */
public class SystemEventReceiver extends BroadcastReceiver {

    private static final String TAG = "SystemEventReceiver";

    /**
     * Listener interface to notify screens or services of system events.
     */
    public interface SystemEventListener {
        void onSystemEvent(String message, boolean warning);
    }

    private final SystemEventListener listener;

    public SystemEventReceiver(SystemEventListener listener) {
        this.listener = listener;
    }

    /**
     * Helper to build an IntentFilter with all handled system actions.
     */
    public static IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "onReceive: action=" + action);

        String message = null;
        boolean warning = false;

        switch (action) {
            case Intent.ACTION_BATTERY_LOW:
                message = "Battery is low. Save your progress and charge soon.";
                warning = true;
                break;
            case Intent.ACTION_BATTERY_OKAY:
                message = "Battery OK";
                warning = false;
                break;
            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                if (isAirplaneModeOn) {
                    message = "Airplane mode is on. Cloud sync will be paused.";
                    warning = true;
                } else {
                    message = "Back online.";
                    warning = false;
                }
                break;
            case Intent.ACTION_POWER_CONNECTED:
                message = "Charging";
                warning = false;
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                message = "Unplugged from power";
                warning = false;
                break;
        }

        if (message != null && listener != null) {
            listener.onSystemEvent(message, warning);
        }
    }
}
