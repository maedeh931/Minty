package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minty.app.databinding.ActivityHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HistoryActivity displays the list of completed tasks and the total minutes earned.
 * Uses View Binding (ActivityHistoryBinding) and a custom ListView adapter (HistoryAdapter).
 */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private ActivityHistoryBinding binding;
    private DatabaseHelper dbHelper;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        // Inflate layout using View Binding
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Up (back) navigation arrow in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        // Setup custom ListView adapter
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        binding.lvCompletedTasks.setAdapter(historyAdapter);

        // Topic 6: ListView Item Click Listener (AdapterView.OnItemClickListener)
        binding.lvCompletedTasks.setOnItemClickListener((parent, view, position, id) -> {
            Task task = (Task) historyAdapter.getItem(position);
            if (task != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault());
                String dateStr = task.getCompletedAt() > 0 ? sdf.format(new Date(task.getCompletedAt())) : "Recently";
                new MaterialAlertDialogBuilder(HistoryActivity.this)
                        .setTitle("Task Completed 🎉")
                        .setMessage("Title: " + task.getTitle() + "\nDuration: " + task.getMinutes() + " min\nEarned: +" + task.getMinutes() + " minty minutes\nFinished: " + dateStr)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: activity is becoming visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: activity is now interacting with the user");

        // Load completed tasks from SQLite (most recent first)
        List<Task> completedTasks = dbHelper.getCompletedTasks();
        historyAdapter.setTasks(completedTasks);

        // Calculate and display total minutes earned
        int totalEarned = 0;
        for (Task task : completedTasks) {
            totalEarned += task.getMinutes();
        }
        String totalEarnedText = "Total earned: " + totalEarned + " min";
        binding.tvTotalEarned.setText(totalEarnedText);

        // Manage empty state visibility inside FrameLayout
        if (completedTasks.isEmpty()) {
            binding.lvCompletedTasks.setVisibility(View.GONE);
            binding.tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            binding.lvCompletedTasks.setVisibility(View.VISIBLE);
            binding.tvEmptyHistory.setVisibility(View.GONE);
        }

        Log.d(TAG, "onResume: loaded " + completedTasks.size() + " completed tasks, " + totalEarnedText);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: activity is restarting after being stopped");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: activity is pausing");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: activity is no longer visible");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }
}
