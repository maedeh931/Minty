package com.minty.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.minty.app.databinding.ActivityProgressBinding;

import java.util.Calendar;
import java.util.List;

/**
 * Progress and Analytics Dashboard.
 * Displays daily focus duration, completed tasks ratio, weekly focus chart,
 * streak metrics, and botanical milestones calculated from genuine SQLite productivity records.
 */
public class ProgressActivity extends AppCompatActivity {

    private ActivityProgressBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        NavigationHelper.setupBottomNav(this, binding.bottomNavigationView, R.id.nav_progress);

        loadAnalyticsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigationView.setSelectedItemId(R.id.nav_progress);
        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        List<Task> completedTasks = dbHelper.getCompletedTasks();

        // 1. Calculate Today's Stats
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        long startOfToday = todayCal.getTimeInMillis();

        int todayMins = 0;
        int todayTasksCount = 0;

        for (Task t : completedTasks) {
            if (t.getCompletedAt() >= startOfToday) {
                todayMins += t.getMinutes();
                todayTasksCount++;
            }
        }

        binding.tvTodayFocusMinutes.setText(todayMins + "m");
        binding.tvTodayTasksCount.setText(String.valueOf(todayTasksCount));

        // Goal rate (based on 45m daily study benchmark)
        int rate = Math.min(100, (todayMins * 100) / 45);
        binding.tvProductivityRatio.setText(rate + "%");

        // 2. Weekly 7-Day Trend
        Calendar weekCal = Calendar.getInstance();
        weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekCal.set(Calendar.HOUR_OF_DAY, 0);
        weekCal.set(Calendar.MINUTE, 0);
        weekCal.set(Calendar.SECOND, 0);

        int[] dayMins = new int[7]; // 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
        int weeklyTotal = 0;

        for (Task t : completedTasks) {
            long compTime = t.getCompletedAt();
            if (compTime >= weekCal.getTimeInMillis()) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(compTime);
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                int idx;
                switch (dayOfWeek) {
                    case Calendar.MONDAY: idx = 0; break;
                    case Calendar.TUESDAY: idx = 1; break;
                    case Calendar.WEDNESDAY: idx = 2; break;
                    case Calendar.THURSDAY: idx = 3; break;
                    case Calendar.FRIDAY: idx = 4; break;
                    case Calendar.SATURDAY: idx = 5; break;
                    case Calendar.SUNDAY:
                    default: idx = 6; break;
                }
                dayMins[idx] += t.getMinutes();
                weeklyTotal += t.getMinutes();
            }
        }

        int avgPerDay = weeklyTotal / 7;
        binding.tvWeeklyTotalMins.setText("Total: " + weeklyTotal + " mins this week (avg " + avgPerDay + "m/day)");

        // Set bar heights proportionally (max height 100dp)
        int maxDay = 1;
        for (int m : dayMins) if (m > maxDay) maxDay = m;

        setBarHeight(binding.barMon, binding.tvBarMonVal, dayMins[0], maxDay);
        setBarHeight(binding.barTue, binding.tvBarTueVal, dayMins[1], maxDay);
        setBarHeight(binding.barWed, binding.tvBarWedVal, dayMins[2], maxDay);
        setBarHeight(binding.barThu, binding.tvBarThuVal, dayMins[3], maxDay);
        setBarHeight(binding.barFri, binding.tvBarFriVal, dayMins[4], maxDay);
        setBarHeight(binding.barSat, binding.tvBarSatVal, dayMins[5], maxDay);
        setBarHeight(binding.barSun, binding.tvBarSunVal, dayMins[6], maxDay);

        // 3. Consistency & Botanical Milestones
        int streak = StreakManager.getDisplayStreak(this);
        int totalEarned = dbHelper.getTotalMinutesEarned();
        int bloomedCount = dbHelper.getBloomedPlantsCount();
        int level = Math.max(1, Math.min(5, totalEarned / 120 + 1));

        String levelTitle;
        switch (level) {
            case 1: levelTitle = "Little Seed (Level 1)"; break;
            case 2: levelTitle = "Tiny Sprout (Level 2)"; break;
            case 3: levelTitle = "Growing Garden (Level 3)"; break;
            case 4: levelTitle = "Blooming Garden (Level 4)"; break;
            case 5:
            default: levelTitle = "Flourishing Garden (Level 5)"; break;
        }

        binding.tvConsistencySummary.setText("• Daily Streak: " + streak + (streak == 1 ? " Day\n" : " Days\n")
                + "• Garden Progression: " + levelTitle + "\n"
                + "• Flowers Bloomed in Collection: " + bloomedCount + "\n"
                + "• Lifetime Productivity: " + totalEarned + " focus minutes earned");
    }

    private void setBarHeight(View bar, TextView valView, int minutes, int maxMinutes) {
        valView.setText(minutes > 0 ? minutes + "m" : "0m");
        float scale = getResources().getDisplayMetrics().density;
        int minHeightPx = (int) (12 * scale);
        int maxHeightPx = (int) (90 * scale);

        int calculatedHeight = minHeightPx + (int) (((float) minutes / maxMinutes) * (maxHeightPx - minHeightPx));

        ViewGroup.LayoutParams params = bar.getLayoutParams();
        params.height = Math.max(minHeightPx, calculatedHeight);
        bar.setLayoutParams(params);
    }
}
