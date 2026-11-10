package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.minty.app.databinding.ActivityAddTaskBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * AddTaskActivity allows users to plan a task with title, notes, planned day,
 * scheduled start time, priority, category, duration, and smart reminder.
 */
public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private ActivityAddTaskBinding binding;
    private DatabaseHelper dbHelper;

    private long selectedTargetDateMillis = Task.getTodayMidnightMillis();
    private long selectedStartTimeMillis = 0;
    private int selectedHour = -1;
    private int selectedMinute = -1;
    private long editingTaskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupDateAndDaySelection();
        setupTimePicker();
        setupQuickMinutes();
        setupKeyboardInteractions();

        // Check if editing an existing task
        editingTaskId = getIntent().getLongExtra("extra_edit_task_id", -1);
        if (editingTaskId > 0) {
            loadExistingTaskForEditing(editingTaskId);
        } else if (getIntent().getBooleanExtra("plan_for_tomorrow", false)) {
            // Check if prefilled for tomorrow (e.g. from PlanTomorrowActivity)
            binding.chipTomorrow.setChecked(true);
            selectedTargetDateMillis = Task.getTomorrowMidnightMillis();
        }

        // Save button listener with input validation and SQLite persistence
        binding.btnSave.setOnClickListener(v -> saveTask());
    }

    private void loadExistingTaskForEditing(long taskId) {
        Task task = dbHelper.getTaskById(taskId);
        if (task == null) return;

        binding.etTaskTitle.setText(task.getTitle());
        binding.etTaskDescription.setText(task.getDescription());
        binding.etMinutes.setText(String.valueOf(task.getMinutes()));
        binding.btnSave.setText("Update Task 🌱");

        selectedTargetDateMillis = task.getTargetDateMillis();
        if (task.isForToday()) {
            binding.chipToday.setChecked(true);
        } else if (task.isForTomorrow()) {
            binding.chipTomorrow.setChecked(true);
        } else if (selectedTargetDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            binding.chipPickDate.setText(sdf.format(new Date(selectedTargetDateMillis)));
            binding.chipPickDate.setChecked(true);
        }

        selectedStartTimeMillis = task.getStartTimeMillis();
        if (selectedStartTimeMillis > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedStartTimeMillis);
            selectedHour = c.get(Calendar.HOUR_OF_DAY);
            selectedMinute = c.get(Calendar.MINUTE);
            updateStartTimeDisplay();
        }

        if (task.getPriority() == Task.PRIORITY_LOW) {
            binding.chipPriorityLow.setChecked(true);
        } else if (task.getPriority() == Task.PRIORITY_HIGH) {
            binding.chipPriorityHigh.setChecked(true);
        } else {
            binding.chipPriorityMed.setChecked(true);
        }

        if (Task.CATEGORY_WORK.equalsIgnoreCase(task.getCategory())) {
            binding.chipCatWork.setChecked(true);
        } else if (Task.CATEGORY_PERSONAL.equalsIgnoreCase(task.getCategory())) {
            binding.chipCatPersonal.setChecked(true);
        } else if (Task.CATEGORY_HEALTH.equalsIgnoreCase(task.getCategory())) {
            binding.chipCatHealth.setChecked(true);
        } else if (Task.CATEGORY_GENERAL.equalsIgnoreCase(task.getCategory())) {
            binding.chipCatGeneral.setChecked(true);
        } else {
            binding.chipCatStudy.setChecked(true);
        }

        if (task.getReminderMinutes() == 5) {
            binding.chipRemind5m.setChecked(true);
        } else if (task.getReminderMinutes() == 15) {
            binding.chipRemind15m.setChecked(true);
        } else if (task.getReminderMinutes() == 30) {
            binding.chipRemind30m.setChecked(true);
        } else if (task.getReminderMinutes() == 0) {
            binding.chipRemindNone.setChecked(true);
        } else {
            binding.chipRemind10m.setChecked(true);
        }
    }

    private void setupDateAndDaySelection() {
        binding.chipGroupDay.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) {
                selectedTargetDateMillis = Task.getTodayMidnightMillis();
                updateStartTimeDisplay();
            } else if (checkedId == R.id.chipTomorrow) {
                selectedTargetDateMillis = Task.getTomorrowMidnightMillis();
                updateStartTimeDisplay();
            } else if (checkedId == R.id.chipPickDate) {
                showDatePicker();
            }
        });
    }

    private boolean isUpdatingMinutesProgrammatically = false;

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
            currentFocus.clearFocus();
        }
    }

    private void setupKeyboardInteractions() {
        binding.scrollViewAddTask.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });

        binding.etMinutes.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void showDatePicker() {
        hideKeyboard();
        Calendar cal = Calendar.getInstance();
        if (selectedTargetDateMillis > 0) {
            cal.setTimeInMillis(selectedTargetDateMillis);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                R.style.MintyTimePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 0, 0, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    selectedTargetDateMillis = picked.getTimeInMillis();

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    binding.chipPickDate.setText(sdf.format(new Date(selectedTargetDateMillis)));
                    binding.chipPickDate.setChecked(true);
                    updateStartTimeDisplay();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 86400000L);
        dialog.show();
    }

    private void setupTimePicker() {
        View.OnClickListener openTimePicker = v -> {
            hideKeyboard();
            Calendar now = Calendar.getInstance();
            int initHour = selectedHour >= 0 ? selectedHour : now.get(Calendar.HOUR_OF_DAY);
            int initMin = selectedMinute >= 0 ? selectedMinute : now.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    R.style.MintyTimePickerDialogTheme,
                    (view, hourOfDay, minute) -> {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        updateStartTimeDisplay();
                    },
                    initHour,
                    initMin,
                    false
            );
            dialog.show();
        };

        binding.btnSetStartTime.setOnClickListener(openTimePicker);
        binding.tvSelectedTime.setOnClickListener(openTimePicker);
        binding.layoutStartTimeRow.setOnClickListener(openTimePicker);
    }

    private void updateStartTimeDisplay() {
        if (selectedHour >= 0 && selectedMinute >= 0) {
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTimeInMillis(selectedTargetDateMillis);
            timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
            timeCal.set(Calendar.MINUTE, selectedMinute);
            timeCal.set(Calendar.SECOND, 0);
            timeCal.set(Calendar.MILLISECOND, 0);
            selectedStartTimeMillis = timeCal.getTimeInMillis();

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            binding.tvSelectedTime.setText("Scheduled for " + timeFormat.format(new Date(selectedStartTimeMillis)));
            binding.tvSelectedTime.setTextColor(getColor(R.color.leaf_dark));
            binding.btnSetStartTime.setText("Change Time ⏰");
        } else {
            selectedStartTimeMillis = 0;
            binding.tvSelectedTime.setText("No specific time");
            binding.tvSelectedTime.setTextColor(getColor(R.color.text_muted));
            binding.btnSetStartTime.setText("Set Start Time ⏰");
        }
    }

    private void setupQuickMinutes() {
        binding.chipGroupMinutes.setOnCheckedChangeListener((group, checkedId) -> {
            if (isUpdatingMinutesProgrammatically) return;

            isUpdatingMinutesProgrammatically = true;
            if (checkedId == R.id.chip15m) {
                binding.etMinutes.setText("15");
                binding.etMinutes.setSelection(binding.etMinutes.getText().length());
                hideKeyboard();
            } else if (checkedId == R.id.chip25m) {
                binding.etMinutes.setText("25");
                binding.etMinutes.setSelection(binding.etMinutes.getText().length());
                hideKeyboard();
            } else if (checkedId == R.id.chip45m) {
                binding.etMinutes.setText("45");
                binding.etMinutes.setSelection(binding.etMinutes.getText().length());
                hideKeyboard();
            } else if (checkedId == R.id.chip60m) {
                binding.etMinutes.setText("60");
                binding.etMinutes.setSelection(binding.etMinutes.getText().length());
                hideKeyboard();
            }
            isUpdatingMinutesProgrammatically = false;
        });

        binding.etMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingMinutesProgrammatically) return;

                String val = s != null ? s.toString().trim() : "";
                isUpdatingMinutesProgrammatically = true;
                if ("15".equals(val)) {
                    binding.chip15m.setChecked(true);
                } else if ("25".equals(val)) {
                    binding.chip25m.setChecked(true);
                } else if ("45".equals(val)) {
                    binding.chip45m.setChecked(true);
                } else if ("60".equals(val)) {
                    binding.chip60m.setChecked(true);
                } else {
                    binding.chipGroupMinutes.clearCheck();
                }
                isUpdatingMinutesProgrammatically = false;
            }
        });
    }

    private void saveTask() {
        String title = binding.etTaskTitle.getText() != null
                ? binding.etTaskTitle.getText().toString().trim() : "";
        String description = binding.etTaskDescription.getText() != null
                ? binding.etTaskDescription.getText().toString().trim() : "";
        String minutesStr = binding.etMinutes.getText() != null
                ? binding.etMinutes.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesStr);
        } catch (NumberFormatException e) {
            minutes = 25;
        }

        if (minutes <= 0) {
            Toast.makeText(this, "Please enter a positive duration in minutes", Toast.LENGTH_SHORT).show();
            return;
        }

        // Priority
        int priority = Task.PRIORITY_MEDIUM;
        int checkedPriorityId = binding.chipGroupPriority.getCheckedChipId();
        if (checkedPriorityId == R.id.chipPriorityLow) {
            priority = Task.PRIORITY_LOW;
        } else if (checkedPriorityId == R.id.chipPriorityHigh) {
            priority = Task.PRIORITY_HIGH;
        }

        // Category
        String category = Task.CATEGORY_GENERAL;
        int checkedCatId = binding.chipGroupCategory.getCheckedChipId();
        if (checkedCatId == R.id.chipCatStudy) {
            category = Task.CATEGORY_STUDY;
        } else if (checkedCatId == R.id.chipCatWork) {
            category = Task.CATEGORY_WORK;
        } else if (checkedCatId == R.id.chipCatPersonal) {
            category = Task.CATEGORY_PERSONAL;
        } else if (checkedCatId == R.id.chipCatHealth) {
            category = Task.CATEGORY_HEALTH;
        }

        // Reminder Minutes
        int reminderMinutes = 0;
        int checkedRemindId = binding.chipGroupReminder.getCheckedChipId();
        if (checkedRemindId == R.id.chipRemind5m) {
            reminderMinutes = 5;
        } else if (checkedRemindId == R.id.chipRemind10m) {
            reminderMinutes = 10;
        } else if (checkedRemindId == R.id.chipRemind15m) {
            reminderMinutes = 15;
        } else if (checkedRemindId == R.id.chipRemind30m) {
            reminderMinutes = 30;
        }

        if (editingTaskId > 0) {
            Task updatedTask = new Task(
                    editingTaskId,
                    title,
                    minutes,
                    false,
                    System.currentTimeMillis(),
                    0,
                    description,
                    selectedTargetDateMillis,
                    selectedStartTimeMillis,
                    0,
                    priority,
                    category,
                    reminderMinutes,
                    reminderMinutes > 0
            );
            dbHelper.updateTask(updatedTask);
            ReminderManager.cancelTaskReminder(this, editingTaskId);
            if (reminderMinutes > 0) {
                ReminderManager.scheduleTaskReminder(this, updatedTask);
            }
            FirestoreManager.getInstance().setTask(updatedTask);
            Toast.makeText(this, "Task updated! 🌱", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Save to SQLite
        long insertedId = dbHelper.addTask(
                title,
                minutes,
                description,
                selectedTargetDateMillis,
                selectedStartTimeMillis,
                0,
                priority,
                category,
                reminderMinutes
        );

        if (insertedId > 0) {
            Task newTask = new Task(
                    insertedId,
                    title,
                    minutes,
                    false,
                    System.currentTimeMillis(),
                    0,
                    description,
                    selectedTargetDateMillis,
                    selectedStartTimeMillis,
                    0,
                    priority,
                    category,
                    reminderMinutes,
                    reminderMinutes > 0
            );

            // Schedule exact alarm reminder
            if (reminderMinutes > 0) {
                ReminderManager.scheduleTaskReminder(this, newTask);
            }

            // Sync to Firestore
            FirestoreManager.getInstance().setTask(newTask, (success, error) -> {
                if (success) {
                    Log.d(TAG, "Task synced to Cloud Firestore: " + insertedId);
                } else {
                    Log.w(TAG, "Cloud Firestore task sync pending/failed: " + error);
                }
            });

            Toast.makeText(this, "Task planted in your garden! 🌱", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save task", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
