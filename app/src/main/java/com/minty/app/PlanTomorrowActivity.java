package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.minty.app.databinding.ActivityPlanTomorrowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * PlanTomorrowActivity provides a calm evening ritual to:
 * 1. Review today's unfinished tasks with 1-tap carry-forward to tomorrow.
 * 2. Set tomorrow's Top 3 priorities.
 * 3. Review everything already lined up for tomorrow.
 */
public class PlanTomorrowActivity extends AppCompatActivity {

    private static final String TAG = "PlanTomorrowActivity";

    private ActivityPlanTomorrowBinding binding;
    private DatabaseHelper dbHelper;
    private List<Task> unfinishedTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlanTomorrowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbarPlanTomorrow.setNavigationOnClickListener(v -> finish());

        loadUnfinishedTasks();
        loadTomorrowScheduled();

        // Carry All Unfinished button
        binding.btnCarryAll.setOnClickListener(v -> {
            for (Task task : unfinishedTasks) {
                dbHelper.carryForwardTaskToTomorrow(task.getId());
            }
            Toast.makeText(this, "Carried all tasks to tomorrow! 🌙", Toast.LENGTH_SHORT).show();
            loadUnfinishedTasks();
            loadTomorrowScheduled();
        });

        // Save Top 3 Goals
        binding.btnSaveTomorrowGoals.setOnClickListener(v -> saveTopGoals());
    }

    private void loadUnfinishedTasks() {
        binding.layoutUnfinishedTasks.removeAllViews();
        unfinishedTasks.clear();

        List<Task> pending = dbHelper.getPendingTasks();
        for (Task t : pending) {
            if (t.isForToday() || t.isOverdue()) {
                unfinishedTasks.add(t);
            }
        }

        if (unfinishedTasks.isEmpty()) {
            binding.tvNoUnfinishedTasks.setVisibility(View.VISIBLE);
            binding.btnCarryAll.setVisibility(View.GONE);
            return;
        }

        binding.tvNoUnfinishedTasks.setVisibility(View.GONE);
        binding.btnCarryAll.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Task task : unfinishedTasks) {
            View taskRow = inflater.inflate(R.layout.task_item, binding.layoutUnfinishedTasks, false);

            TextView tvTitle = taskRow.findViewById(R.id.tvTitle);
            TextView tvMinutes = taskRow.findViewById(R.id.tvMinutes);
            TextView tvTimeBadge = taskRow.findViewById(R.id.tvTimeBadge);
            View cbComplete = taskRow.findViewById(R.id.cbComplete);
            View btnMenu = taskRow.findViewById(R.id.btnTaskMenu);

            tvTitle.setText(task.getTitle());
            tvMinutes.setText("+" + task.getMinutes() + " min");
            tvTimeBadge.setText(task.isOverdue() ? "Overdue" : "Today");

            // Hide normal checkbox & menu; clicking card or time badge carries forward
            cbComplete.setVisibility(View.GONE);
            btnMenu.setVisibility(View.GONE);

            // Add carry forward button dynamically or use card click
            taskRow.setOnClickListener(v -> {
                dbHelper.carryForwardTaskToTomorrow(task.getId());
                Toast.makeText(this, "Carried to tomorrow: " + task.getTitle() + " 🌙", Toast.LENGTH_SHORT).show();
                loadUnfinishedTasks();
                loadTomorrowScheduled();
            });

            binding.layoutUnfinishedTasks.addView(taskRow);
        }
    }

    private void loadTomorrowScheduled() {
        binding.layoutTomorrowScheduled.removeAllViews();
        List<Task> tomorrowTasks = dbHelper.getTasksForTomorrow();

        if (tomorrowTasks.isEmpty()) {
            binding.tvNoTomorrowScheduled.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvNoTomorrowScheduled.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Task t : tomorrowTasks) {
            View taskRow = inflater.inflate(R.layout.task_item, binding.layoutTomorrowScheduled, false);

            TextView tvTitle = taskRow.findViewById(R.id.tvTitle);
            TextView tvMinutes = taskRow.findViewById(R.id.tvMinutes);
            TextView tvTimeBadge = taskRow.findViewById(R.id.tvTimeBadge);
            View cbComplete = taskRow.findViewById(R.id.cbComplete);
            View btnMenu = taskRow.findViewById(R.id.btnTaskMenu);

            tvTitle.setText(t.getTitle());
            tvMinutes.setText("+" + t.getMinutes() + " min");
            tvTimeBadge.setText(t.getCategory());
            cbComplete.setVisibility(View.GONE);
            btnMenu.setVisibility(View.GONE);

            binding.layoutTomorrowScheduled.addView(taskRow);
        }
    }

    private void saveTopGoals() {
        String g1 = binding.etGoal1.getText() != null ? binding.etGoal1.getText().toString().trim() : "";
        String g2 = binding.etGoal2.getText() != null ? binding.etGoal2.getText().toString().trim() : "";
        String g3 = binding.etGoal3.getText() != null ? binding.etGoal3.getText().toString().trim() : "";

        if (TextUtils.isEmpty(g1) && TextUtils.isEmpty(g2) && TextUtils.isEmpty(g3)) {
            Toast.makeText(this, "Please enter at least one goal for tomorrow", Toast.LENGTH_SHORT).show();
            return;
        }

        long tomorrowMidnight = Task.getTomorrowMidnightMillis();
        int savedCount = 0;

        if (!TextUtils.isEmpty(g1)) {
            long id1 = dbHelper.addTask(g1, 25, "Priority 1 Goal", tomorrowMidnight, 0, 0, Task.PRIORITY_HIGH, Task.CATEGORY_STUDY, 10);
            if (id1 > 0) savedCount++;
        }
        if (!TextUtils.isEmpty(g2)) {
            long id2 = dbHelper.addTask(g2, 25, "Priority 2 Goal", tomorrowMidnight, 0, 0, Task.PRIORITY_MEDIUM, Task.CATEGORY_STUDY, 10);
            if (id2 > 0) savedCount++;
        }
        if (!TextUtils.isEmpty(g3)) {
            long id3 = dbHelper.addTask(g3, 25, "Priority 3 Goal", tomorrowMidnight, 0, 0, Task.PRIORITY_MEDIUM, Task.CATEGORY_PERSONAL, 10);
            if (id3 > 0) savedCount++;
        }

        binding.etGoal1.setText("");
        binding.etGoal2.setText("");
        binding.etGoal3.setText("");

        Toast.makeText(this, "Saved " + savedCount + " goals for tomorrow! 🌱", Toast.LENGTH_SHORT).show();
        loadTomorrowScheduled();
    }
}
