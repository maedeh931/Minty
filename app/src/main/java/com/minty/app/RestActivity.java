package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.minty.app.databinding.ActivityRestBinding;

import java.util.Locale;

/**
 * RestActivity displays Minty's peaceful nighttime Rest Mode.
 * Features a moonlit botanical garden, sleeping mascot, fireflies animation,
 * and mindful break controls while safeguarding user streaks and records.
 */
public class RestActivity extends AppCompatActivity {

    private static final String TAG = "RestActivity";

    private ActivityRestBinding binding;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupAnimations();
        setupClickListeners();
        updateRestUi();
    }

    private void setupAnimations() {
        // Firefly 1 gentle floating pulse
        ObjectAnimator firefly1Anim = ObjectAnimator.ofFloat(binding.tvFirefly1, "alpha", 0.3f, 1.0f, 0.4f);
        firefly1Anim.setDuration(2500);
        firefly1Anim.setRepeatCount(ValueAnimator.INFINITE);
        firefly1Anim.setRepeatMode(ValueAnimator.REVERSE);
        firefly1Anim.start();

        // Firefly 2 gentle floating translation
        ObjectAnimator firefly2Anim = ObjectAnimator.ofFloat(binding.tvFirefly2, "translationY", 0f, -12f, 0f);
        firefly2Anim.setDuration(3200);
        firefly2Anim.setRepeatCount(ValueAnimator.INFINITE);
        firefly2Anim.setRepeatMode(ValueAnimator.REVERSE);
        firefly2Anim.start();

        // Zzz gentle scale
        ObjectAnimator zzzAnim = ObjectAnimator.ofFloat(binding.tvZzz, "scaleX", 0.9f, 1.1f);
        zzzAnim.setDuration(2000);
        zzzAnim.setRepeatCount(ValueAnimator.INFINITE);
        zzzAnim.setRepeatMode(ValueAnimator.REVERSE);
        zzzAnim.start();
    }

    private void setupClickListeners() {
        // Option 1: 1 Hour
        binding.cardRest1Hour.setOnClickListener(v -> {
            RestManager.startRest(this, 3600000L, RestManager.TYPE_POWER_REST);
            Toast.makeText(this, "1-Hour Power Rest started. Relax and recharge 🌙", Toast.LENGTH_SHORT).show();
            updateRestUi();
        });

        // Option 2: Until Tomorrow 7:00 AM
        binding.cardRestUntilTomorrow.setOnClickListener(v -> {
            RestManager.startRestUntilTomorrowMorning(this);
            Toast.makeText(this, "Resting until morning (7:00 AM). Sleep well! 🌙", Toast.LENGTH_SHORT).show();
            updateRestUi();
        });

        // Option 3: Custom Duration
        binding.cardRestCustom.setOnClickListener(v -> showCustomDurationPicker());

        // Wake up button
        binding.btnWakeUp.setOnClickListener(v -> {
            RestManager.endRest(this);
            Toast.makeText(this, "Welcome back! Reminders restored 🌱", Toast.LENGTH_SHORT).show();
            updateRestUi();
        });

        // Back to Dashboard
        binding.btnBackHome.setOnClickListener(v -> finish());
    }

    private void showCustomDurationPicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    long totalMillis = (hourOfDay * 3600000L) + (minute * 60000L);
                    if (totalMillis <= 0) {
                        totalMillis = 1800000L; // Default 30 min minimum
                    }
                    RestManager.startRest(this, totalMillis, "Mindful Rest (" + hourOfDay + "h " + minute + "m)");
                    Toast.makeText(this, "Mindful Rest started 🌙", Toast.LENGTH_SHORT).show();
                    updateRestUi();
                },
                1,
                30,
                true
        );
        dialog.setTitle("Select Rest Duration (Hours & Minutes)");
        dialog.show();
    }

    private void updateRestUi() {
        boolean isResting = RestManager.isRestActive(this);

        if (isResting) {
            binding.cardActiveRest.setVisibility(View.VISIBLE);
            binding.btnWakeUp.setVisibility(View.VISIBLE);
            binding.layoutDurationOptions.setVisibility(View.GONE);

            binding.tvActiveRestLabel.setText(RestManager.getRestType(this) + " Active");
            binding.tvRestUntilTime.setText("Resting until " + RestManager.getFormattedUntilTime(this) + " • Optional reminders paused");

            startCountdownTicker();
        } else {
            binding.cardActiveRest.setVisibility(View.GONE);
            binding.btnWakeUp.setVisibility(View.GONE);
            binding.layoutDurationOptions.setVisibility(View.VISIBLE);

            stopCountdownTicker();
        }
    }

    private void startCountdownTicker() {
        stopCountdownTicker();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!RestManager.isRestActive(RestActivity.this)) {
                    updateRestUi();
                    return;
                }

                long remaining = RestManager.getRemainingMillis(RestActivity.this);
                long hours = (remaining / 3600000L);
                long minutes = (remaining % 3600000L) / 60000L;
                long seconds = (remaining % 60000L) / 1000L;

                String formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                binding.tvRestTimerCountdown.setText(formatted);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopCountdownTicker() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRestUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCountdownTicker();
    }
}
