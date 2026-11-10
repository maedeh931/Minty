package com.minty.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minty.app.databinding.ActivityMemoryLibraryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity displaying The Little Memory Library: an illustrated scrapbook chronicling
 * personal milestones, monthly garden recap, bloom records, and weekly reflections.
 */
public class MemoryLibraryActivity extends AppCompatActivity implements MemoryAdapter.OnMemoryInteractionListener {

    private ActivityMemoryLibraryBinding binding;
    private DatabaseHelper dbHelper;
    private MemoryAdapter adapter;
    private List<MemoryItem> currentMemories = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemoryLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        // Toolbar setup
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding.toolbarMemoryLibrary.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new MemoryAdapter(new ArrayList<>(), this);
        binding.rvMemories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMemories.setAdapter(adapter);

        // Setup Reflection Button
        binding.btnWriteReflection.setOnClickListener(v -> showWeeklyReflectionDialog());

        // Setup Filter Chips
        setupFilterChips();

        // Load Monthly Recap & Memories
        loadMonthlyRecap();
        loadMemories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMonthlyRecap();
        loadMemories();
    }

    private void setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFavorites) {
                currentFilter = "favorites";
            } else if (checkedId == R.id.chipBlooms) {
                currentFilter = MemoryItem.CATEGORY_BLOOM;
            } else if (checkedId == R.id.chipMilestones) {
                currentFilter = MemoryItem.CATEGORY_FOCUS;
            } else if (checkedId == R.id.chipDailyPlans) {
                currentFilter = "plans";
            } else if (checkedId == R.id.chipReflections) {
                currentFilter = MemoryItem.CATEGORY_REFLECTION;
            } else {
                currentFilter = "all";
            }
            loadMemories();
        });
    }

    private void loadMonthlyRecap() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthYearFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthName = monthYearFmt.format(cal.getTime());

        binding.tvRecapMonthTitle.setText("🌿 Monthly Growth Recap • " + monthName);

        int focusMinutes = dbHelper.getMonthFocusMinutes(cal);
        int completedTasks = dbHelper.getMonthCompletedTasksCount(cal);
        int bloomedCount = dbHelper.getMonthBloomedFlowersCount(cal);

        binding.tvRecapFocusMinutes.setText(focusMinutes + " m");
        binding.tvRecapTasksDone.setText(String.valueOf(completedTasks));
        binding.tvRecapBloomedFlowers.setText(bloomedCount + " 🌸");
    }

    private void loadMemories() {
        if ("favorites".equals(currentFilter)) {
            currentMemories = dbHelper.getFavoriteMemories();
        } else if ("plans".equals(currentFilter)) {
            List<MemoryItem> plans = dbHelper.getMemoriesByCategory(MemoryItem.CATEGORY_DAILY_PLAN);
            List<MemoryItem> streaks = dbHelper.getMemoriesByCategory(MemoryItem.CATEGORY_STREAK);
            currentMemories = new ArrayList<>(plans);
            currentMemories.addAll(streaks);
        } else if (!"all".equals(currentFilter)) {
            currentMemories = dbHelper.getMemoriesByCategory(currentFilter);
        } else {
            currentMemories = dbHelper.getAllMemories();
        }

        adapter.updateList(currentMemories);

        if (currentMemories.isEmpty()) {
            binding.rvMemories.setVisibility(View.GONE);
            binding.layoutEmptyMemories.setVisibility(View.VISIBLE);
        } else {
            binding.rvMemories.setVisibility(View.VISIBLE);
            binding.layoutEmptyMemories.setVisibility(View.GONE);
        }
    }

    private void showWeeklyReflectionDialog() {
        final EditText input = new EditText(this);
        input.setHint("Write a peaceful thought or achievement...");
        input.setMinLines(3);
        input.setPadding(32, 24, 32, 24);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Weekly Reflection 💭")
                .setMessage("What went well for you this week? What are you grateful or proud of?")
                .setView(input)
                .setPositiveButton("Save Memory", (dialog, which) -> {
                    String reflection = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(reflection)) {
                        MemoryManager.getInstance(MemoryLibraryActivity.this).addWeeklyReflection(reflection);
                        Toast.makeText(MemoryLibraryActivity.this, "Reflection chronicled in your library! 🌱", Toast.LENGTH_SHORT).show();
                        loadMemories();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onFavoriteToggle(MemoryItem memory) {
        boolean newStatus = !memory.isFavorite();
        memory.setFavorite(newStatus);
        MemoryManager.getInstance(this).toggleFavorite(memory.getMemoryId(), newStatus);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEditNote(MemoryItem memory) {
        final EditText input = new EditText(this);
        input.setHint("Add personal thoughts, feelings or study context...");
        input.setText(memory.getPersonalNote() != null ? memory.getPersonalNote() : "");
        input.setMinLines(2);
        input.setPadding(32, 24, 32, 24);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reflection Note 📝")
                .setMessage("Add a personal story to \"" + memory.getTitle() + "\":")
                .setView(input)
                .setPositiveButton("Save Note", (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    memory.setPersonalNote(note);
                    MemoryManager.getInstance(MemoryLibraryActivity.this).updatePersonalNote(memory.getMemoryId(), note);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MemoryLibraryActivity.this, "Note saved! 🌿", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onShare(MemoryItem memory) {
        if (!TextUtils.isEmpty(memory.getPersonalNote())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Share Memory Milestone 🌿")
                    .setMessage("Would you like to include your private reflection note in this share card?")
                    .setPositiveButton("Include Note", (dialog, which) -> executeShare(memory, true))
                    .setNegativeButton("Share Without Note", (dialog, which) -> executeShare(memory, false))
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            executeShare(memory, false);
        }
    }

    private void executeShare(MemoryItem memory, boolean includePersonalNote) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(memory.getTimestamp()));

        StringBuilder sb = new StringBuilder();
        sb.append(memory.getEmojiIcon()).append(" Minty Memory Milestone\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(memory.getTitle()).append("\n\n");
        sb.append(memory.getDescription()).append("\n\n");
        if (includePersonalNote && !TextUtils.isEmpty(memory.getPersonalNote())) {
            sb.append("Personal Note: \"").append(memory.getPersonalNote()).append("\"\n\n");
        }
        sb.append("Recorded on ").append(dateStr).append("\n");
        sb.append("Minty — Focus a little. Grow a lot. 🌱");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, memory.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Memory Milestone"));
    }
}
