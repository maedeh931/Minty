package com.minty.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minty.app.databinding.ActivityFocusBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Focus Timer Activity.
 * Supports Pomodoro (25m), Deep Focus (50m), Sprint (15m), and Custom blocks.
 * Associates tasks with focus sessions and coordinates with CountdownService.
 */
public class FocusActivity extends AppCompatActivity {

    private ActivityFocusBinding binding;
    private DatabaseHelper dbHelper;

    private int selectedDurationMinutes = 25;
    private long selectedTaskId = -1;
    private String selectedTaskTitle = "General Study";

    private final BroadcastReceiver timerTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CountdownService.ACTION_TICK.equals(intent.getAction())) {
                long remainingSeconds = intent.getLongExtra(CountdownService.EXTRA_REMAINING_SECONDS, 0);
                boolean finished = intent.getBooleanExtra(CountdownService.EXTRA_FINISHED, false);

                if (finished) {
                    onSessionCompleted();
                } else {
                    updateTimerUI(remainingSeconds);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFocusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        NavigationHelper.setupBottomNav(this, binding.bottomNavigationView, R.id.nav_focus);

        // Chip selection for duration
        binding.chipPomodoro.setOnClickListener(v -> setDuration(25));
        binding.chipDeepFocus.setOnClickListener(v -> setDuration(50));
        binding.chipQuickFocus.setOnClickListener(v -> setDuration(15));
        binding.chipLongFocus.setOnClickListener(v -> setDuration(60));

        // Task selection spinner
        loadTaskSpinner();

        // Control buttons
        binding.btnStartTimer.setOnClickListener(v -> startFocusTimer());
        binding.btnPauseResumeTimer.setOnClickListener(v -> togglePauseResume());
        binding.btnStopTimer.setOnClickListener(v -> confirmStopTimer());

        binding.btnOpenJournal.setOnClickListener(v -> {
            Intent intent = new Intent(FocusActivity.this, FocusJournalActivity.class);
            startActivity(intent);
        });

        setDuration(25);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(CountdownService.ACTION_TICK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerTickReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timerTickReceiver, filter);
        }
        syncWithRunningService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(timerTickReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigationView.setSelectedItemId(R.id.nav_focus);
        loadTaskSpinner();
        syncWithRunningService();
    }

    private void setDuration(int minutes) {
        this.selectedDurationMinutes = minutes;
        binding.tvTimerCountdown.setText(String.format(Locale.getDefault(), "%02d:00", minutes));
        binding.pbTimerProgress.setMax(minutes * 60);
        binding.pbTimerProgress.setProgress(minutes * 60);
    }

    private void loadTaskSpinner() {
        List<Task> pending = dbHelper.getPendingTasks();
        List<String> taskNames = new ArrayList<>();
        taskNames.add("🌿 General Focus (No specific task)");

        for (Task t : pending) {
            taskNames.add("📝 " + t.getTitle() + " (" + t.getMinutes() + "m)");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, taskNames);
        binding.spinnerTasks.setAdapter(adapter);

        binding.spinnerTasks.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedTaskId = -1;
                    selectedTaskTitle = "General Focus";
                } else {
                    Task chosen = pending.get(position - 1);
                    selectedTaskId = chosen.getId();
                    selectedTaskTitle = chosen.getTitle();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void startFocusTimer() {
        if (CountdownService.isRunning) {
            Toast.makeText(this, "A timer is already running in the background", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, CountdownService.class);
        serviceIntent.putExtra(CountdownService.EXTRA_MINUTES, selectedDurationMinutes);
        serviceIntent.putExtra(CountdownService.EXTRA_SESSION_TYPE, CountdownService.SESSION_TYPE_FOCUS);
        serviceIntent.putExtra(CountdownService.EXTRA_TASK_ID, selectedTaskId);
        serviceIntent.putExtra(CountdownService.EXTRA_TASK_TITLE, selectedTaskTitle);

        ContextCompat.startForegroundService(this, serviceIntent);

        binding.btnStartTimer.setVisibility(View.GONE);
        binding.btnPauseResumeTimer.setVisibility(View.VISIBLE);
        binding.btnPauseResumeTimer.setText("Pause");
        binding.btnStopTimer.setVisibility(View.VISIBLE);
        binding.chipGroupFocusMode.setEnabled(false);

        Toast.makeText(this, "Focus session started! 🌱", Toast.LENGTH_SHORT).show();
    }

    private void togglePauseResume() {
        if (!CountdownService.isRunning) return;

        if (CountdownService.isPaused) {
            Intent resumeIntent = new Intent(this, CountdownService.class);
            resumeIntent.setAction(CountdownService.ACTION_RESUME);
            startService(resumeIntent);
            binding.btnPauseResumeTimer.setText("Pause");
            binding.tvTimerHint.setText("Session in progress...");
        } else {
            Intent pauseIntent = new Intent(this, CountdownService.class);
            pauseIntent.setAction(CountdownService.ACTION_PAUSE);
            startService(pauseIntent);
            binding.btnPauseResumeTimer.setText("Resume");
            binding.tvTimerHint.setText("Session paused ⏸️");
        }
    }

    private void confirmStopTimer() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Focus Session?")
                .setMessage("Stopping early will end the session without earning full growth points.")
                .setPositiveButton("Cancel Session", (dialog, which) -> {
                    Intent stopIntent = new Intent(FocusActivity.this, CountdownService.class);
                    stopIntent.setAction(CountdownService.ACTION_STOP);
                    startService(stopIntent);
                    resetTimerUI();
                    Toast.makeText(FocusActivity.this, "Session cancelled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Keep Focusing", null)
                .show();
    }

    private void updateTimerUI(long remainingSeconds) {
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        binding.tvTimerCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        binding.pbTimerProgress.setProgress((int) remainingSeconds);

        binding.btnStartTimer.setVisibility(View.GONE);
        binding.btnPauseResumeTimer.setVisibility(View.VISIBLE);
        binding.btnPauseResumeTimer.setText(CountdownService.isPaused ? "Resume" : "Pause");
        binding.btnStopTimer.setVisibility(View.VISIBLE);
    }

    private void syncWithRunningService() {
        if (CountdownService.isRunning) {
            binding.btnStartTimer.setVisibility(View.GONE);
            binding.btnPauseResumeTimer.setVisibility(View.VISIBLE);
            binding.btnPauseResumeTimer.setText(CountdownService.isPaused ? "Resume" : "Pause");
            binding.btnStopTimer.setVisibility(View.VISIBLE);
        } else {
            resetTimerUI();
        }
    }

    private void resetTimerUI() {
        binding.btnStartTimer.setVisibility(View.VISIBLE);
        binding.btnPauseResumeTimer.setVisibility(View.GONE);
        binding.btnStopTimer.setVisibility(View.GONE);
        setDuration(selectedDurationMinutes);
        binding.tvTimerHint.setText("Ready to grow your focus garden 🌱");
    }

    private void onSessionCompleted() {
        resetTimerUI();

        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Focus Session Completed!")
                .setMessage("Great work! You focused for " + selectedDurationMinutes + " minutes.\n\n"
                        + "Rewards Earned:\n"
                        + "• +" + selectedDurationMinutes + " Minty Minutes\n"
                        + "• +" + selectedDurationMinutes + " Garden Growth Points 🌱\n"
                        + (selectedTaskId > 0 ? "• Task marked as completed! ✅\n" : "")
                        + "\nYour virtual garden is flourishing!")
                .setPositiveButton("Open Garden 🌱", (dialog, which) -> {
                    Intent intent = new Intent(FocusActivity.this, GardenActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Reflect in Journal 📖", (dialog, which) -> {
                    Intent intent = new Intent(FocusActivity.this, FocusJournalActivity.class);
                    startActivity(intent);
                })
                .show();
    }
}
