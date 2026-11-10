package com.minty.app;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.minty.app.databinding.ActivitySpendBinding;

import java.util.Locale;

/**
 * SpendActivity allows users to spend minty minutes for scrolling time.
 * Supports choosing durations (1, 5, 10, 15 min), triggers CountdownService,
 * displays a live mm:ss countdown via dynamic broadcast receiver,
 * and launches social media via implicit Intent.
 */
public class SpendActivity extends AppCompatActivity {

    private static final String TAG = "SpendActivity";

    private ActivitySpendBinding binding;
    private DatabaseHelper dbHelper;
    private int pendingMinutes = 0;

    // Dynamic BroadcastReceiver for CountdownService TICK broadcasts
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.minty.app.TICK".equals(intent.getAction())) {
                boolean finished = intent.getBooleanExtra(CountdownService.EXTRA_FINISHED, false);
                long seconds = intent.getLongExtra(CountdownService.EXTRA_REMAINING_SECONDS, CountdownService.remainingSeconds);

                if (finished) {
                    updateCountdownDisplay(0);
                    updateUiState(false);
                    updateBalanceDisplay();
                    Toast.makeText(SpendActivity.this, "Time's up! Scroll session finished.", Toast.LENGTH_SHORT).show();
                } else {
                    updateCountdownDisplay(seconds);
                    updateUiState(true);
                }
            }
        }
    };

    // ActivityResultLauncher for POST_NOTIFICATIONS runtime permission on Android 13+ (API 33)
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission denied by user");
                    Toast.makeText(SpendActivity.this, "Notifications are off", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted");
                }
                // Still run the countdown even if permission is denied
                if (pendingMinutes > 0) {
                    int minutesToStart = pendingMinutes;
                    pendingMinutes = 0;
                    startSpendAndCountdown(minutesToStart);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        // Inflate layout using View Binding
        binding = ActivitySpendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Up (back) navigation arrow in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        // Start button listener
        binding.btnStart.setOnClickListener(v -> {
            int selectedMinutes = getSelectedMinutes();
            int currentBalance = dbHelper.getBalance();

            // 1. Check if balance is sufficient
            if (currentBalance < selectedMinutes) {
                Toast.makeText(SpendActivity.this, "Not enough minty minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if timer is already running
            if (CountdownService.isRunning) {
                Toast.makeText(SpendActivity.this, "Timer is already running", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(SpendActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    pendingMinutes = selectedMinutes;
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }

            // Permission already granted or running on API < 33
            startSpendAndCountdown(selectedMinutes);
        });

        // Stop button listener: stops CountdownService without refund
        binding.btnStop.setOnClickListener(v -> {
            Log.d(TAG, "btnStop clicked: stopping countdown service");
            Intent stopIntent = new Intent(SpendActivity.this, CountdownService.class);
            stopService(stopIntent);
            CountdownService.isRunning = false;
            CountdownService.remainingSeconds = 0;
            updateCountdownDisplay(0);
            updateUiState(false);
            updateBalanceDisplay();
        });

        // Implicit Intent to open social media (YouTube) in the browser
        binding.btnOpenSocialMedia.setOnClickListener(v -> {
            Log.d(TAG, "btnOpenSocialMedia clicked: opening YouTube via ACTION_VIEW");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No web browser activity found to open URL", e);
                Toast.makeText(SpendActivity.this, "No browser found to open link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: registering TICK BroadcastReceiver");

        // Register dynamic BroadcastReceiver for TICK events
        IntentFilter filter = new IntentFilter(CountdownService.ACTION_TICK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(tickReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: activity is now interacting with the user");

        // Update balance display
        updateBalanceDisplay();

        // Restore countdown state if service is running
        if (CountdownService.isRunning) {
            updateCountdownDisplay(CountdownService.remainingSeconds);
            updateUiState(true);
        } else {
            updateCountdownDisplay(0);
            updateUiState(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: activity is pausing");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: unregistering TICK BroadcastReceiver");

        // Unregister dynamic BroadcastReceiver
        try {
            unregisterReceiver(tickReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "tickReceiver was not registered", e);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: activity is restarting after being stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Deducts minutes from balance and starts the foreground CountdownService.
     *
     * @param minutes The duration to spend and start
     */
    private void startSpendAndCountdown(int minutes) {
        boolean spent = dbHelper.spendMinutes(minutes);
        if (spent) {
            updateBalanceDisplay();
            // Sync deducted balance to Firebase Realtime Database atomically
            RealtimeDbManager.getInstance().adjustBalanceInCloud(-minutes);

            Intent serviceIntent = new Intent(SpendActivity.this, CountdownService.class);
            serviceIntent.putExtra(CountdownService.EXTRA_MINUTES, minutes);
            serviceIntent.putExtra(CountdownService.EXTRA_SESSION_TYPE, CountdownService.SESSION_TYPE_SCROLL);
            ContextCompat.startForegroundService(SpendActivity.this, serviceIntent);
            updateCountdownDisplay(minutes * 60L);
            updateUiState(true);
            Log.d(TAG, "startSpendAndCountdown: spent " + minutes + " minutes, service started");
        } else {
            Toast.makeText(SpendActivity.this, "Not enough minty minutes", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the selected duration in minutes from the RadioGroup.
     */
    private int getSelectedMinutes() {
        int checkedId = binding.rgDuration.getCheckedRadioButtonId();
        if (checkedId == R.id.rb1Min) {
            return 1;
        } else if (checkedId == R.id.rb10Min) {
            return 10;
        } else if (checkedId == R.id.rb15Min) {
            return 15;
        } else {
            return 5;
        }
    }

    /**
     * Updates the balance display TextView from the database.
     */
    private void updateBalanceDisplay() {
        int balance = dbHelper.getBalance();
        String balanceText = "Balance: " + balance + " minty minutes";
        binding.tvBalance.setText(balanceText);
    }

    /**
     * Formats and updates the mm:ss countdown display TextView.
     */
    private void updateCountdownDisplay(long totalSeconds) {
        long mins = totalSeconds / 60;
        long secs = totalSeconds % 60;
        binding.tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
    }

    /**
     * Enables or disables duration controls and buttons depending on running state.
     */
    private void updateUiState(boolean running) {
        binding.btnStart.setEnabled(!running);
        binding.rb1Min.setEnabled(!running);
        binding.rb5Min.setEnabled(!running);
        binding.rb10Min.setEnabled(!running);
        binding.rb15Min.setEnabled(!running);
        binding.btnStop.setEnabled(running);
    }
}
