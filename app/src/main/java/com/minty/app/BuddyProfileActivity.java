package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.minty.app.databinding.ActivityBuddyProfileBinding;

import java.util.Calendar;
import java.util.List;

/**
 * Shared Productivity Dashboard for a connected buddy.
 * Displays real daily focus progress, weekly comparison, tasks completed,
 * daily streak, and privacy-protected balance.
 */
public class BuddyProfileActivity extends AppCompatActivity {

    private static final String TAG = "BuddyProfileActivity";

    public static final String EXTRA_BUDDY_UID = "extra_buddy_uid";
    public static final String EXTRA_BUDDY_NAME = "extra_buddy_name";
    public static final String EXTRA_BUDDY_USERNAME = "extra_buddy_username";
    public static final String EXTRA_BUDDY_LEVEL = "extra_buddy_level";

    private ActivityBuddyProfileBinding binding;
    private BuddyManager buddyManager;
    private DatabaseHelper dbHelper;

    private String buddyUid;
    private String buddyName;
    private String buddyUsername;
    private String buddyLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuddyProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buddyManager = BuddyManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);

        buddyUid = getIntent().getStringExtra(EXTRA_BUDDY_UID);
        buddyName = getIntent().getStringExtra(EXTRA_BUDDY_NAME);
        buddyUsername = getIntent().getStringExtra(EXTRA_BUDDY_USERNAME);
        buddyLevel = getIntent().getStringExtra(EXTRA_BUDDY_LEVEL);

        if (buddyUid == null || buddyUid.isEmpty()) {
            Toast.makeText(this, "Buddy details unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbarBuddyProfile.setNavigationOnClickListener(v -> finish());

        // Header info
        binding.tvDashboardName.setText(buddyName != null ? buddyName : "Minty Friend");
        binding.tvDashboardUsername.setText(buddyUsername != null ? buddyUsername : "@buddy");
        binding.tvDashboardLevel.setText((buddyLevel != null ? buddyLevel : "Focused") + " 🌱");

        updatePrimaryButtonState();

        binding.btnTogglePrimaryFromProfile.setOnClickListener(v -> {
            String currentPrimary = buddyManager.getPrimaryBuddyUid();
            boolean isCurrentlyPrimary = buddyUid.equals(currentPrimary);

            if (isCurrentlyPrimary) {
                buddyManager.setPrimaryBuddy(null, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyProfileActivity.this, "Removed as Primary Buddy", Toast.LENGTH_SHORT).show();
                        updatePrimaryButtonState();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                buddyManager.setPrimaryBuddy(buddyUid, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyProfileActivity.this, "Set as Primary Buddy! 🌟", Toast.LENGTH_SHORT).show();
                        updatePrimaryButtonState();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Quick action: Send encouragement
        binding.btnSendEncouragementFromDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(BuddyProfileActivity.this, BuddyActivity.class);
            intent.putExtra("open_tab", 3); // 3 = Encouragement tab
            intent.putExtra("target_buddy_uid", buddyUid);
            intent.putExtra("target_buddy_name", buddyName);
            startActivity(intent);
            finish();
        });

        loadSharedStats();
    }

    private void updatePrimaryButtonState() {
        String currentPrimary = buddyManager.getPrimaryBuddyUid();
        boolean isPrimary = buddyUid.equals(currentPrimary);
        if (isPrimary) {
            binding.btnTogglePrimaryFromProfile.setText("★ Primary Buddy (Tap to clear)");
            binding.btnTogglePrimaryFromProfile.setBackgroundTintList(
                    getColorStateList(R.color.star_yellow)
            );
        } else {
            binding.btnTogglePrimaryFromProfile.setText("★ Set as Primary Buddy");
            binding.btnTogglePrimaryFromProfile.setBackgroundTintList(
                    getColorStateList(R.color.mint)
            );
        }
    }

    private void loadSharedStats() {
        // Calculate your own stats for comparison
        List<Task> myCompleted = dbHelper.getCompletedTasks();
        int myWeeklyMins = 0;
        Calendar weekCal = Calendar.getInstance();
        weekCal.add(Calendar.DAY_OF_YEAR, -7);
        long startOfWeek = weekCal.getTimeInMillis();
        for (Task t : myCompleted) {
            if (t.getCompletedAt() >= startOfWeek) {
                myWeeklyMins += t.getMinutes();
            }
        }
        final int finalMyWeekly = myWeeklyMins;

        buddyManager.fetchBuddySharedStats(buddyUid, new BuddyManager.StatsCallback() {
            @Override
            public void onStatsLoaded(SharedStats stats) {
                if (isFinishing() || isDestroyed()) return;

                // 1. Daily Focus
                if (stats.isSharingEnabled() && stats.isShareFocusTime()) {
                    int mins = stats.getFocusMinutesToday();
                    binding.tvFocusTodayValue.setText(mins + " min / 120 min goal");
                    int percent = Math.min(100, (int) Math.round((mins / 120.0) * 100));
                    binding.pbFocusToday.setProgress(percent);
                } else {
                    binding.tvFocusTodayValue.setText("Hidden by buddy 🔒");
                    binding.pbFocusToday.setProgress(0);
                }

                // 2. Tasks Today
                if (stats.isSharingEnabled() && stats.isShareCompletedTasks()) {
                    binding.tvTasksTodayValue.setText(String.valueOf(stats.getCompletedTasksToday()));
                } else {
                    binding.tvTasksTodayValue.setText("—");
                }

                // 3. Streak
                if (stats.isSharingEnabled() && stats.isShareDailyStreak()) {
                    binding.tvStreakDaysValue.setText(stats.getStreakDays() + (stats.getStreakDays() == 1 ? " day" : " days"));
                } else {
                    binding.tvStreakDaysValue.setText("—");
                }

                // 4. Weekly Comparison
                if (stats.isSharingEnabled() && stats.isShareWeeklyProgress()) {
                    int buddyWeekly = stats.getWeeklyMinutes();
                    int maxWeekly = Math.max(1, Math.max(finalMyWeekly, buddyWeekly));

                    binding.tvMyWeeklyStats.setText("You: " + finalMyWeekly + " mins focus (" + myCompleted.size() + " tasks)");
                    binding.pbMyWeekly.setProgress((int) Math.round((finalMyWeekly / (double) maxWeekly) * 100));

                    binding.tvBuddyWeeklyStats.setText((buddyName != null ? buddyName : "Buddy") + ": "
                            + buddyWeekly + " mins focus (" + stats.getWeeklyTasksCount() + " tasks)");
                    binding.pbBuddyWeekly.setProgress((int) Math.round((buddyWeekly / (double) maxWeekly) * 100));
                } else {
                    binding.tvMyWeeklyStats.setText("You: " + finalMyWeekly + " mins focus");
                    binding.pbMyWeekly.setProgress(50);
                    binding.tvBuddyWeeklyStats.setText((buddyName != null ? buddyName : "Buddy") + ": Hidden by privacy 🔒");
                    binding.pbBuddyWeekly.setProgress(0);
                }

                // 5. Balance
                if (stats.isSharingEnabled() && stats.isShareBalance() && stats.getBalanceMinutes() >= 0) {
                    binding.tvDashboardBalance.setText(stats.getBalanceMinutes() + " minty minutes ✨");
                } else {
                    binding.tvDashboardBalance.setText("Hidden by buddy's privacy settings 🔒");
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Error loading buddy stats: " + error);
                Toast.makeText(BuddyProfileActivity.this, "Using offline cached buddy stats", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
