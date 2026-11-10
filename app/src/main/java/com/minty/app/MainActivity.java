package com.minty.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Calendar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.minty.app.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * MainActivity serves as the Home screen for Minty.
 * Displays the minty minutes balance, streak count, user title level,
 * pending tasks list in a RecyclerView, and navigation buttons.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String TAG_QUOTE = "quote_request";
    private static final String ZENQUOTES_URL = "https://zenquotes.io/api/random";

    // Cheerful praise messages for completed tasks
    private static final String[] PRAISE_MESSAGES = {
            "Well done! 🌟",
            "You crushed it! 💪",
            "Great job, keep going! ✨",
            "Super productive today! 🎉",
            "Awesome effort! 🚀",
            "Proud of you! 💖",
            "One step closer to your goals! 🌸",
            "Look at you go! 🔥",
            "Consistency is your superpower! 🏆"
    };

    // Fallback motivational quotes for offline mode / network errors (at least 6)
    private static final String[][] FALLBACK_QUOTES = {
            {"The secret of getting ahead is getting started.", "Mark Twain"},
            {"Small daily improvements over time lead to stunning results.", "Robin Sharma"},
            {"Do what you can, with what you have, where you are.", "Theodore Roosevelt"},
            {"Action is the foundational key to all success.", "Pablo Picasso"},
            {"Focus on being productive instead of busy.", "Tim Ferriss"},
            {"You don't have to be extreme, just consistent.", "Anonymous"},
            {"Believe you can and you're halfway there.", "Theodore Roosevelt"},
            {"Step by step and the thing is done.", "Charles Atlas"}
    };

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private TaskAdapter taskAdapter;
    private final Random random = new Random();

    private SystemEventReceiver systemEventReceiver;
    private ValueEventListener cloudBalanceListener;
    private ListenerRegistration tasksFirestoreListener;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideBannerRunnable = () -> {
        if (binding != null && binding.cardStatusBanner != null) {
            binding.cardStatusBanner.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        // Inflate layout using View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        // Setup RecyclerView with LinearLayoutManager and TaskAdapter
        binding.rvPendingTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(new ArrayList<>(), task -> {
            completeTaskDirectly(task);
        });
        taskAdapter.setOnTaskClickListener(task -> {
            showTaskFocusDialog(task);
        });
        taskAdapter.setOnTaskMenuActionListener(new TaskAdapter.OnTaskMenuActionListener() {
            @Override
            public void onEdit(Task task) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                intent.putExtra("extra_edit_task_id", task.getId());
                startActivity(intent);
            }

            @Override
            public void onMoveToTomorrow(Task task) {
                dbHelper.carryForwardTaskToTomorrow(task.getId());
                Task updated = dbHelper.getTaskById(task.getId());
                if (updated != null) {
                    FirestoreManager.getInstance().setTask(updated);
                    ReminderManager.scheduleTaskReminder(MainActivity.this, updated);
                }
                Toast.makeText(MainActivity.this, "Moved to tomorrow! 🌙", Toast.LENGTH_SHORT).show();
                loadPendingTasksAndBalance();
            }

            @Override
            public void onReschedule(Task task) {
                showRescheduleDatePicker(task);
            }

            @Override
            public void onFocusRecovery(Task task) {
                showFocusRecoveryDialog(task);
            }

            @Override
            public void onDelete(Task task) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Delete Task")
                        .setMessage("Are you sure you want to delete \"" + task.getTitle() + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteTaskById(task.getId());
                            ReminderManager.cancelTaskReminder(MainActivity.this, task.getId());
                            FirestoreManager.getInstance().deleteTask(task.getId(), null);
                            Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                            loadPendingTasksAndBalance();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.rvPendingTasks.setAdapter(taskAdapter);

        // Plan Tomorrow card
        binding.cardPlanTomorrow.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlanTomorrowActivity.class);
            startActivity(intent);
        });

        // Rest Mode card
        binding.cardRestMode.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RestActivity.class);
            startActivity(intent);
        });

        // The Little Memory Library card
        binding.cardMemoryLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MemoryLibraryActivity.class);
            startActivity(intent);
        });

        // Achievement Scrapbook card
        binding.cardScrapbook.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScrapbookActivity.class);
            startActivity(intent);
        });

        // Planner section filter chips
        binding.chipGroupTaskFilters.setOnCheckedChangeListener((group, checkedId) -> {
            loadPendingTasksAndBalance();
        });

        // Explicit Intent to navigate from Home to BuddyActivity
        binding.btnBuddyHub.setOnClickListener(v -> {
            Log.d(TAG, "btnBuddyHub clicked: navigating to BuddyActivity");
            Intent intent = new Intent(MainActivity.this, BuddyActivity.class);
            startActivity(intent);
        });

        // Explicit Intent to navigate from Home to SpendActivity
        binding.btnStartScrolling.setOnClickListener(v -> {
            Log.d(TAG, "btnStartScrolling clicked: navigating to SpendActivity");
            Intent intent = new Intent(MainActivity.this, SpendActivity.class);
            startActivity(intent);
        });

        // Explicit Intent to navigate from Home to AddTaskActivity
        binding.btnAddTask.setOnClickListener(v -> {
            Log.d(TAG, "btnAddTask clicked: navigating to AddTaskActivity");
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // Explicit Intent to navigate from Home to HistoryActivity
        binding.btnHistory.setOnClickListener(v -> {
            Log.d(TAG, "btnHistory clicked: navigating to HistoryActivity");
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Refresh quote button listener
        binding.btnRefreshQuote.setOnClickListener(v -> {
            Log.d(TAG, "btnRefreshQuote clicked: refreshing quote");
            fetchQuoteOfTheDay();
        });

        // Phase 5: A Little Note from Minty
        binding.btnRefreshNote.setOnClickListener(v -> refreshMintyNote());
        binding.cardLittleNote.setOnClickListener(v -> refreshMintyNote());
        updateMintyNote();

        // Explicit Intent to navigate from Home to FeedbackActivity
        binding.btnFeedback.setOnClickListener(v -> {
            Log.d(TAG, "btnFeedback clicked: navigating to FeedbackActivity");
            Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
            startActivity(intent);
        });

        // Initial quote fetch
        fetchQuoteOfTheDay();

        // Setup user greeting display
        setupUserGreeting();

        // Tap greeting row or mascot card to open ProfileActivity
        View.OnClickListener openProfileListener = v -> {
            Log.d(TAG, "Greeting tapped: navigating to ProfileActivity");
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        };
        binding.rowUserGreeting.setOnClickListener(openProfileListener);
        binding.cardGreeting.setOnClickListener(openProfileListener);

        // Logout button listener
        binding.btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "btnLogout clicked: logging out");
            performLogout();
        });

        // Setup 5-section unified bottom navigation
        NavigationHelper.setupBottomNav(this, binding.bottomNavigationView, R.id.nav_home);

        // Garden preview card click navigates to GardenActivity
        binding.cardGardenPreview.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GardenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: activity is becoming visible");

        // Dynamic registration of SystemEventReceiver
        if (systemEventReceiver == null) {
            systemEventReceiver = new SystemEventReceiver((message, warning) -> {
                showStatusBanner(message, warning);
            });
        }
        IntentFilter filter = SystemEventReceiver.createIntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(systemEventReceiver, filter);
        }

        // Check current airplane mode state on start
        boolean isAirplaneModeOn = Settings.Global.getInt(
                getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,
                0
        ) != 0;
        if (isAirplaneModeOn) {
            showStatusBanner("Airplane mode is on. Cloud sync will be paused.", true);
        }

        // Attach Realtime Database live balance listener
        if (cloudBalanceListener == null) {
            cloudBalanceListener = RealtimeDbManager.getInstance().listenToBalance(new RealtimeDbManager.BalanceListener() {
                @Override
                public void onBalanceChanged(int balance) {
                    dbHelper.updateBalanceDirect(balance);
                    runOnUiThread(() -> loadPendingTasksAndBalance());
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "cloudBalanceListener error: " + message);
                }
            });
        }

        // Attach Cloud Firestore live tasks listener
        if (tasksFirestoreListener == null) {
            tasksFirestoreListener = FirestoreManager.getInstance().listenToTasks(new FirestoreManager.TaskSyncListener() {
                @Override
                public void onTasksSynced(List<Task> remoteTasks) {
                    for (Task cloudTask : remoteTasks) {
                        dbHelper.upsertTaskFromCloud(cloudTask);
                    }
                    runOnUiThread(() -> loadPendingTasksAndBalance());
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "tasksFirestoreListener error: " + error);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: activity is now interacting with the user");

        // Keep BottomNavigationView item in sync
        binding.bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // Refresh user greeting and reload pending tasks, balance, streak, and level
        setupUserGreeting();
        loadPendingTasksAndBalance();
        loadGardenPreview();
        updateMintyNote();

        // Update Rest Mode card state
        if (RestManager.isRestActive(this)) {
            binding.tvRestCardIcon.setText("🌙");
            binding.tvRestCardTitle.setText("Minty is Resting");
            binding.tvRestCardSubtitle.setText("Active until " + RestManager.getFormattedUntilTime(this) + " • Tap to view or resume");
        } else {
            binding.tvRestCardIcon.setText("✨");
            binding.tvRestCardTitle.setText("Peaceful Rest Mode");
            binding.tvRestCardSubtitle.setText("Recharge under the moonlit garden • Streaks preserved");
        }

        // Welcome back memory milestone check
        try {
            SharedPreferences profilePrefs = getSharedPreferences("minty_profile_prefs", MODE_PRIVATE);
            long lastActive = profilePrefs.getLong("last_active_time", 0);
            if (lastActive > 0) {
                MemoryManager.getInstance(this).checkWelcomeBack(lastActive);
            }
            profilePrefs.edit().putLong("last_active_time", System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error checking welcome back memory: " + e.getMessage());
        }
    }

    /**
     * Loads the active growing plant and updates the Home garden preview card.
     */
    private void loadGardenPreview() {
        try {
            boolean isDemo = ShowcaseManager.isDemoModeEnabled(this);
            GardenPlant activePlant = isDemo ? ShowcaseManager.getDemoPlant(this) : dbHelper.getActivePlant();
            if (activePlant != null) {
                binding.cardGardenPreview.setVisibility(View.VISIBLE);
                binding.tvGardenPreviewPlantName.setText(activePlant.getName() + (isDemo ? " (Demo)" : ""));
                binding.tvGardenPreviewStage.setText(activePlant.getStageName() + (isDemo ? " • Demo" : ""));
                binding.ivGardenPreviewPlant.setImageResource(
                        PlantDrawableHelper.getPlantDrawable(activePlant.getSpecies(), activePlant.getStage())
                );

                int nextThreshold = activePlant.getNextStageThreshold();
                int currentPoints = activePlant.getGrowthPoints();

                if (activePlant.isBloomed()) {
                    binding.pbGardenPreview.setMax(100);
                    binding.pbGardenPreview.setProgress(100);
                    binding.tvGardenPreviewPoints.setText("Fully Bloomed! 🌸 Complete");
                } else {
                    int prevThreshold = activePlant.getCurrentStageBasePoints();
                    int stageProgress = Math.max(0, currentPoints - prevThreshold);
                    int stageTotal = Math.max(1, nextThreshold - prevThreshold);
                    int percent = Math.min(100, (int) (((float) stageProgress / stageTotal) * 100));

                    binding.pbGardenPreview.setMax(100);
                    binding.pbGardenPreview.setProgress(percent);
                    binding.tvGardenPreviewPoints.setText(currentPoints + " / " + nextThreshold + " pts to next stage");
                }
            } else {
                binding.cardGardenPreview.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading garden preview", e);
        }
    }

    /**
     * Updates the greeting banner below the mascot card with a warm time-of-day greeting
     * using the user's name (never raw email address).
     */
    private void setupUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = "";
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            name = user.getDisplayName();
        }
        if (TextUtils.isEmpty(name)) {
            name = getSharedPreferences("minty_user_profile", MODE_PRIVATE)
                    .getString("key_display_name", "");
        }
        if (TextUtils.isEmpty(name) && user != null && !TextUtils.isEmpty(user.getEmail())) {
            name = LoginActivity.deriveFriendlyName(user.getEmail());
        }
        if (TextUtils.isEmpty(name)) {
            name = "Minty Friend";
        }

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greetingPrefix;
        if (hour >= 5 && hour < 12) {
            greetingPrefix = "Good morning, ";
        } else if (hour >= 12 && hour < 17) {
            greetingPrefix = "Good afternoon, ";
        } else if (hour >= 17 && hour < 22) {
            greetingPrefix = "Good evening, ";
        } else {
            greetingPrefix = "Sweet dreams, ";
        }

        binding.tvUserGreeting.setText(greetingPrefix + name);
        ProfileImageHelper.loadProfilePhotoCircular(this, binding.ivProfileSmall);
    }

    /**
     * Signs out of Firebase and Google Sign-In, clears back stack, and redirects to LoginActivity.
     */
    private void performLogout() {
        Log.d(TAG, "performLogout: signing out of Firebase and Google");
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "GoogleSignInClient signed out");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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

        // Cancel ongoing quote network request
        ApiClient.getInstance(this).cancelPendingRequests(TAG_QUOTE);

        bannerHandler.removeCallbacks(hideBannerRunnable);
        if (systemEventReceiver != null) {
            try {
                unregisterReceiver(systemEventReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "systemEventReceiver was not registered", e);
            }
            systemEventReceiver = null;
        }

        // Detach Firebase listeners
        if (cloudBalanceListener != null) {
            RealtimeDbManager.getInstance().removeBalanceListener(cloudBalanceListener);
            cloudBalanceListener = null;
        }
        if (tasksFirestoreListener != null) {
            tasksFirestoreListener.remove();
            tasksFirestoreListener = null;
        }
    }

    /**
     * Phase 5: Updates the Home screen "A Little Note from Minty" card with context-aware dialogue.
     */
    private void updateMintyNote() {
        if (binding == null) return;
        MintyPersonalityManager.MintyNote note = MintyPersonalityManager.getInstance(this).getNoteForHomeDashboard();
        if (note != null) {
            binding.tvNoteEmoji.setText(note.getEmoji());
            binding.tvNoteHeadline.setText(note.getHeadline());
            binding.tvNoteCategoryPill.setText(note.getCategory());
            binding.tvNoteMessage.setText(note.getMessage());
        }
    }

    /**
     * Manually refreshes Minty's note when the card or sync button is tapped.
     */
    private void refreshMintyNote() {
        if (binding == null) return;
        MintyPersonalityManager.MintyNote note = MintyPersonalityManager.getInstance(this).refreshHomeNote();
        if (note != null) {
            binding.tvNoteEmoji.setText(note.getEmoji());
            binding.tvNoteHeadline.setText(note.getHeadline());
            binding.tvNoteCategoryPill.setText(note.getCategory());
            binding.tvNoteMessage.setText(note.getMessage());
            Toast.makeText(this, "A fresh thought from Minty 🌱", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Fetches Quote of the Day from zenquotes.io with an 8-second timeout and 0 retries.
     * Displays a ProgressBar while loading, and falls back to offline quotes on failure.
     */
    private void fetchQuoteOfTheDay() {
        binding.pbQuoteLoading.setVisibility(View.VISIBLE);
        binding.btnRefreshQuote.setEnabled(false);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                ZENQUOTES_URL,
                null,
                response -> {
                    binding.pbQuoteLoading.setVisibility(View.GONE);
                    binding.btnRefreshQuote.setEnabled(true);
                    try {
                        if (response != null && response.length() > 0) {
                            JSONObject quoteObj = response.getJSONObject(0);
                            String quote = quoteObj.getString("q");
                            String author = quoteObj.getString("a");
                            binding.tvQuoteText.setText("\"" + quote + "\"");
                            binding.tvQuoteAuthor.setText("- " + author);
                            Log.d(TAG, "fetchQuoteOfTheDay: quote loaded successfully");
                        } else {
                            displayFallbackQuote();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for quote", e);
                        displayFallbackQuote();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching quote", error);
                    binding.pbQuoteLoading.setVisibility(View.GONE);
                    binding.btnRefreshQuote.setEnabled(true);
                    displayFallbackQuote();
                    Toast.makeText(MainActivity.this, "Showing offline quote", Toast.LENGTH_SHORT).show();
                }
        );

        // 8-second timeout and no retries
        request.setRetryPolicy(new DefaultRetryPolicy(
                8000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        ApiClient.getInstance(this).addToRequestQueue(request, TAG_QUOTE);
    }

    /**
     * Selects and displays a random fallback motivational quote when offline or on network error.
     */
    private void displayFallbackQuote() {
        int index = random.nextInt(FALLBACK_QUOTES.length);
        String quote = FALLBACK_QUOTES[index][0];
        String author = FALLBACK_QUOTES[index][1];
        binding.tvQuoteText.setText("\"" + quote + "\"");
        binding.tvQuoteAuthor.setText("- " + author);
    }

    /**
     * Shows the status banner with custom warning or info styling.
     * Soft mint banner auto-hides after 4 seconds.
     */
    private void showStatusBanner(String message, boolean warning) {
        bannerHandler.removeCallbacks(hideBannerRunnable);

        binding.tvStatusBanner.setText(message);
        if (warning) {
            binding.cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.star_yellow));
            binding.tvStatusBanner.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        } else {
            binding.cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mint));
            binding.tvStatusBanner.setTextColor(ContextCompat.getColor(this, R.color.leaf_dark));
            // Soft mint color hides itself after 4 seconds
            bannerHandler.postDelayed(hideBannerRunnable, 4000);
        }
        binding.cardStatusBanner.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }

    /**
     * Queries the database for pending tasks, current balance, streak, and title level,
     * updates the adapter and views, and manages empty state visibility.
     */
    private void loadPendingTasksAndBalance() {
        // Automatically ensure sample/provider tasks have realistic student titles
        dbHelper.renameLegacyProviderTasks();

        // 1. Refresh balance
        int currentBalance = dbHelper.getBalance();
        String balanceText = "Balance: " + currentBalance + " minty minutes";
        binding.tvBalance.setText(balanceText);

        // 2. Refresh streak display
        int streak = StreakManager.getDisplayStreak(this);
        String streakText = "Streak: " + streak + (streak == 1 ? " day" : " days");
        binding.tvStreak.setText(streakText);

        // 3. Calculate title level from total lifetime minutes earned
        int totalMinutesEarned = dbHelper.getTotalMinutesEarned();
        String titleLevel;
        if (totalMinutesEarned < 60) {
            titleLevel = "Beginner";
        } else if (totalMinutesEarned < 300) {
            titleLevel = "Focused";
        } else {
            titleLevel = "Champion";
        }
        binding.tvUserLevel.setText(titleLevel);

        // 4. Fetch tasks from SQLite based on selected filter
        List<Task> filteredTasks;
        int checkedFilterId = binding.chipGroupTaskFilters.getCheckedChipId();
        if (checkedFilterId == R.id.chipFilterTomorrow) {
            filteredTasks = dbHelper.getTasksForTomorrow();
            binding.tvPlannerSectionTitle.setText("Tomorrow's Plan");
        } else if (checkedFilterId == R.id.chipFilterUpcoming) {
            filteredTasks = dbHelper.getUpcomingTasks();
            binding.tvPlannerSectionTitle.setText("Upcoming Tasks");
        } else if (checkedFilterId == R.id.chipFilterOverdue) {
            filteredTasks = dbHelper.getOverdueTasks();
            binding.tvPlannerSectionTitle.setText("Overdue Tasks");
        } else if (checkedFilterId == R.id.chipFilterAll) {
            filteredTasks = dbHelper.getPendingTasks();
            binding.tvPlannerSectionTitle.setText("All Pending Tasks");
        } else {
            // Default Today
            filteredTasks = dbHelper.getTasksForToday();
            binding.tvPlannerSectionTitle.setText("Today's Focus Plan");
        }

        taskAdapter.setTasks(filteredTasks);
        binding.tvPlannerTaskCount.setText(filteredTasks.size() + " tasks");

        // Update Overdue badge count
        List<Task> overdueList = dbHelper.getOverdueTasks();
        if (!overdueList.isEmpty()) {
            binding.chipFilterOverdue.setText("Overdue (" + overdueList.size() + ") ⚠️");
        } else {
            binding.chipFilterOverdue.setText("Overdue ⚠️");
        }

        // 5. Manage empty state inside FrameLayout
        if (filteredTasks.isEmpty()) {
            binding.rvPendingTasks.setVisibility(View.GONE);
            binding.tvEmptyPending.setVisibility(View.VISIBLE);
        } else {
            binding.rvPendingTasks.setVisibility(View.VISIBLE);
            binding.tvEmptyPending.setVisibility(View.GONE);
        }

        Log.d(TAG, "loadPendingTasksAndBalance: balance=" + currentBalance
                + ", streak=" + streak + ", level=" + titleLevel + ", filtered=" + filteredTasks.size());
    }

    /**
     * Completes a task in SQLite and pushes changes across Cloud Firestore and Realtime Database.
     */
    private void completeTaskDirectly(Task task) {
        boolean success = dbHelper.completeTask(task.getId());
        if (success) {
            Log.d(TAG, "Task completed directly: id=" + task.getId() + " - " + task.getTitle());

            // Cancel any pending reminder for this task
            ReminderManager.cancelTaskReminder(MainActivity.this, task.getId());

            // 1. Sync updated balance to Firebase Realtime Database atomically
            int newBalance = dbHelper.getBalance();
            RealtimeDbManager.getInstance().adjustBalanceInCloud(task.getMinutes());

            // 2. Sync task completion to Cloud Firestore
            FirestoreManager.getInstance().updateTaskCompletion(task.getId(), true, System.currentTimeMillis());

            // 3. Record completed focus session in Cloud Firestore
            FocusSession session = new FocusSession(
                    UUID.randomUUID().toString(),
                    task.getId(),
                    task.getTitle(),
                    task.getMinutes(),
                    "focus",
                    System.currentTimeMillis() - task.getMinutes() * 60000L,
                    System.currentTimeMillis(),
                    true
            );
            FirestoreManager.getInstance().recordFocusSession(session);

            // 4. Update and award growth points to Virtual Garden
            GardenManager.getInstance(MainActivity.this).onTaskCompletedDirectly(task);

            // 5. Trigger short haptic feedback (100 ms)
            triggerVibration();

            // 6. Update consecutive day streak
            StreakManager.recordCompletion(MainActivity.this);

            // Chronicle daily plan / streak milestones in The Little Memory Library
            try {
                int currentStreak = StreakManager.getCurrentStreak(MainActivity.this);
                MemoryManager.getInstance(MainActivity.this).checkStreakMilestone(currentStreak);

                long todayStart = Task.getTodayMidnightMillis();
                long tomorrowStart = todayStart + 86400000L;
                int remainingTodayCount = 0;
                for (Task t : dbHelper.getPendingTasks()) {
                    if ((t.getTargetDateMillis() >= todayStart && t.getTargetDateMillis() < tomorrowStart) ||
                        (t.getTargetDateMillis() == 0 && t.getCreatedAt() >= todayStart)) {
                        remainingTodayCount++;
                    }
                }
                if (remainingTodayCount == 0) {
                    int completedToday = 0;
                    for (Task t : dbHelper.getCompletedTasks()) {
                        if (t.getCompletedAt() >= todayStart) completedToday++;
                    }
                    if (completedToday > 0) {
                        MemoryManager.getInstance(MainActivity.this).onDailyPlanCompleted(completedToday);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error recording task memory: " + e.getMessage());
            }

            // Record Daily Quest progress for task completion
            try {
                DailyQuestManager.getInstance(MainActivity.this).recordProgress(DailyQuestManager.TYPE_TASKS, 1);
            } catch (Exception e) {
                Log.e(TAG, "Error recording daily quest task progress: " + e.getMessage());
            }

            // 7. Show praise dialog
            showPraiseDialog(task.getMinutes());

            // 8. Animate balance TextView
            animateBalanceText();

            // 9. Notify BuddyManager for collaborative challenge progress & shared stats
            BuddyManager.getInstance(MainActivity.this).onTaskCompleted(task);
        }

        loadPendingTasksAndBalance();
    }

    /**
     * Opens a dialog allowing the user to start a live focus countdown timer (e.g. 50 min)
     * or complete the task immediately.
     */
    private void showTaskFocusDialog(Task task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Focus Task: " + task.getTitle())
                .setMessage("Duration: " + task.getMinutes() + " minutes\nReward: +" + task.getMinutes() + " minty minutes\n\nChoose an action:")
                .setPositiveButton("Start Focus Timer", (dialog, which) -> {
                    if (CountdownService.isRunning) {
                        Toast.makeText(MainActivity.this, "A timer is already running", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent serviceIntent = new Intent(MainActivity.this, CountdownService.class);
                    serviceIntent.putExtra(CountdownService.EXTRA_MINUTES, task.getMinutes());
                    serviceIntent.putExtra(CountdownService.EXTRA_SESSION_TYPE, CountdownService.SESSION_TYPE_FOCUS);
                    serviceIntent.putExtra(CountdownService.EXTRA_TASK_ID, task.getId());
                    serviceIntent.putExtra(CountdownService.EXTRA_TASK_TITLE, task.getTitle());
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                    Toast.makeText(MainActivity.this, "Focus timer started for " + task.getMinutes() + " minutes!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Mark Done Now", (dialog, which) -> completeTaskDirectly(task))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows a cheerful praise dialog celebrating task completion and minutes earned.
     *
     * @param minutesEarned Amount of minty minutes earned from the completed task
     */
    private void showPraiseDialog(int minutesEarned) {
        String randomPraise = PRAISE_MESSAGES[random.nextInt(PRAISE_MESSAGES.length)];
        String message = "+" + minutesEarned + " minty minutes added to your balance!";

        new MaterialAlertDialogBuilder(this)
                .setTitle(randomPraise)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * Scales up and down the balance text over ~400ms to celebrate balance increase.
     */
    private void animateBalanceText() {
        binding.tvBalance.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(200)
                .withEndAction(() -> binding.tvBalance.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start())
                .start();
    }

    /**
     * Triggers a short 100 ms haptic feedback vibration if available on the device.
     */
    private void triggerVibration() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "triggerVibration error", e);
        }
    }

    private void showRescheduleDatePicker(Task task) {
        Calendar cal = Calendar.getInstance();
        if (task.getTargetDateMillis() > 0) {
            cal.setTimeInMillis(task.getTargetDateMillis());
        }
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 0, 0, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    dbHelper.rescheduleTask(task.getId(), picked.getTimeInMillis());
                    task.setTargetDateMillis(picked.getTimeInMillis());
                    ReminderManager.scheduleTaskReminder(this, task);
                    FirestoreManager.getInstance().setTask(task);
                    Toast.makeText(this, "Task rescheduled! 📅", Toast.LENGTH_SHORT).show();
                    loadPendingTasksAndBalance();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 86400000L);
        dialog.show();
    }

    private void showFocusRecoveryDialog(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_focus_recovery, null);
        TextView tvTaskTitle = dialogView.findViewById(R.id.tvRecoveryTaskTitle);
        tvTaskTitle.setText(task.getTitle());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        // Option 1: 10-Minute Micro-Focus
        dialogView.findViewById(R.id.cardMicroFocus).setOnClickListener(v -> {
            dialog.dismiss();
            if (CountdownService.isRunning) {
                Toast.makeText(this, "A timer is already running", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent serviceIntent = new Intent(this, CountdownService.class);
            serviceIntent.putExtra(CountdownService.EXTRA_MINUTES, 10);
            serviceIntent.putExtra(CountdownService.EXTRA_SESSION_TYPE, CountdownService.SESSION_TYPE_FOCUS);
            serviceIntent.putExtra(CountdownService.EXTRA_TASK_ID, task.getId());
            serviceIntent.putExtra(CountdownService.EXTRA_TASK_TITLE, "Micro: " + task.getTitle());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "10-Minute Micro-Focus started! Let's get momentum 🌱", Toast.LENGTH_LONG).show();
        });

        // Option 2: Break into 2 Subtasks
        dialogView.findViewById(R.id.cardBreakSubtasks).setOnClickListener(v -> {
            dialog.dismiss();
            int halfMins = Math.max(10, task.getMinutes() / 2);
            long id1 = dbHelper.addTask(task.getTitle() + " (Part 1)", halfMins, "Step 1 of split task",
                    Task.getTodayMidnightMillis(), 0, 0, Task.PRIORITY_MEDIUM, task.getCategory(), 5);
            long id2 = dbHelper.addTask(task.getTitle() + " (Part 2)", halfMins, "Step 2 of split task",
                    Task.getTomorrowMidnightMillis(), 0, 0, Task.PRIORITY_LOW, task.getCategory(), 0);
            dbHelper.deleteTaskById(task.getId());
            Toast.makeText(this, "Task split into 2 bite-sized parts! ✂️", Toast.LENGTH_LONG).show();
            loadPendingTasksAndBalance();
        });

        // Option 3: Move to Tomorrow
        dialogView.findViewById(R.id.cardRescheduleTomorrow).setOnClickListener(v -> {
            dialog.dismiss();
            dbHelper.carryForwardTaskToTomorrow(task.getId());
            Toast.makeText(this, "Moved to tomorrow! Have a restful evening 🌙", Toast.LENGTH_SHORT).show();
            loadPendingTasksAndBalance();
        });

        dialogView.findViewById(R.id.btnCloseRecovery).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
