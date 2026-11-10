package com.minty.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * SQLite database helper for Minty.
 * Manages the "tasks" table and the single-row "balance" table offline.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "minty.db";
    private static final int DATABASE_VERSION = 5;

    // Table "tasks" constants
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_TASK_ID = "id";
    public static final String COLUMN_TASK_TITLE = "title";
    public static final String COLUMN_TASK_MINUTES = "minutes";
    public static final String COLUMN_TASK_COMPLETED = "completed";
    public static final String COLUMN_TASK_CREATED_AT = "created_at";
    public static final String COLUMN_TASK_COMPLETED_AT = "completed_at";
    public static final String COLUMN_TASK_DESCRIPTION = "description";
    public static final String COLUMN_TASK_TARGET_DATE = "target_date";
    public static final String COLUMN_TASK_START_TIME = "start_time";
    public static final String COLUMN_TASK_DEADLINE = "deadline";
    public static final String COLUMN_TASK_PRIORITY = "priority";
    public static final String COLUMN_TASK_CATEGORY = "category";
    public static final String COLUMN_TASK_REMINDER_MINS = "reminder_minutes";
    public static final String COLUMN_TASK_REMINDER_SCHED = "reminder_scheduled";

    // Table "balance" constants
    public static final String TABLE_BALANCE = "balance";
    public static final String COLUMN_BALANCE_ID = "id";
    public static final String COLUMN_BALANCE_MINUTES = "minutes";

    // Table "buddies" constants (offline buddy caching)
    public static final String TABLE_BUDDIES = "buddies";
    public static final String COLUMN_BUDDY_UID = "uid";
    public static final String COLUMN_BUDDY_NAME = "display_name";
    public static final String COLUMN_BUDDY_USERNAME = "username";
    public static final String COLUMN_BUDDY_PHOTO = "photo_url";
    public static final String COLUMN_BUDDY_LEVEL = "title_level";
    public static final String COLUMN_BUDDY_IS_PRIMARY = "is_primary";
    public static final String COLUMN_BUDDY_UPDATED_AT = "updated_at";

    // Table "buddy_requests" constants
    public static final String TABLE_BUDDY_REQUESTS = "buddy_requests";
    public static final String COLUMN_REQ_ID = "request_id";
    public static final String COLUMN_REQ_SENDER_UID = "sender_uid";
    public static final String COLUMN_REQ_SENDER_NAME = "sender_name";
    public static final String COLUMN_REQ_SENDER_USERNAME = "sender_username";
    public static final String COLUMN_REQ_RECEIVER_UID = "receiver_uid";
    public static final String COLUMN_REQ_RECEIVER_NAME = "receiver_name";
    public static final String COLUMN_REQ_STATUS = "status";
    public static final String COLUMN_REQ_CREATED_AT = "created_at";
    public static final String COLUMN_REQ_IS_INCOMING = "is_incoming";

    // Table "buddy_messages" constants
    public static final String TABLE_BUDDY_MESSAGES = "buddy_messages";
    public static final String COLUMN_MSG_ID = "message_id";
    public static final String COLUMN_MSG_SENDER_UID = "sender_uid";
    public static final String COLUMN_MSG_SENDER_NAME = "sender_name";
    public static final String COLUMN_MSG_RECEIVER_UID = "receiver_uid";
    public static final String COLUMN_MSG_RECEIVER_NAME = "receiver_name";
    public static final String COLUMN_MSG_TEXT = "message";
    public static final String COLUMN_MSG_TYPE = "message_type";
    public static final String COLUMN_MSG_CREATED_AT = "created_at";
    public static final String COLUMN_MSG_IS_READ = "is_read";

    // Table "challenges" constants
    public static final String TABLE_CHALLENGES = "challenges";
    public static final String COLUMN_CH_ID = "challenge_id";
    public static final String COLUMN_CH_TITLE = "title";
    public static final String COLUMN_CH_TYPE = "type";
    public static final String COLUMN_CH_TARGET = "target";
    public static final String COLUMN_CH_DURATION = "duration_days";
    public static final String COLUMN_CH_START = "start_date";
    public static final String COLUMN_CH_END = "end_date";
    public static final String COLUMN_CH_CREATOR_UID = "creator_uid";
    public static final String COLUMN_CH_CREATOR_NAME = "creator_name";
    public static final String COLUMN_CH_INVITED_UID = "invited_uid";
    public static final String COLUMN_CH_INVITED_NAME = "invited_name";
    public static final String COLUMN_CH_STATUS = "status";
    public static final String COLUMN_CH_CREATOR_PROG = "creator_progress";
    public static final String COLUMN_CH_INVITED_PROG = "invited_progress";
    public static final String COLUMN_CH_CREATED_AT = "created_at";
    public static final String COLUMN_CH_COMPLETED_AT = "completed_at";

    // Table "shared_stats" constants
    public static final String TABLE_SHARED_STATS = "shared_stats";
    public static final String COLUMN_STAT_UID = "uid";
    public static final String COLUMN_STAT_TASKS_TODAY = "tasks_today";
    public static final String COLUMN_STAT_FOCUS_TODAY = "focus_today";
    public static final String COLUMN_STAT_STREAK = "streak_days";
    public static final String COLUMN_STAT_BALANCE = "balance_minutes";
    public static final String COLUMN_STAT_WEEKLY_MINS = "weekly_minutes";
    public static final String COLUMN_STAT_WEEKLY_TASKS = "weekly_tasks";
    public static final String COLUMN_STAT_ACHIEVEMENT = "achievement";
    public static final String COLUMN_STAT_UPDATED_AT = "updated_at";

    // Table "garden_plants" constants
    public static final String TABLE_GARDEN_PLANTS = "garden_plants";
    public static final String COLUMN_PLANT_ID = "plant_id";
    public static final String COLUMN_PLANT_NAME = "name";
    public static final String COLUMN_PLANT_SPECIES = "species";
    public static final String COLUMN_PLANT_POINTS = "growth_points";
    public static final String COLUMN_PLANT_STAGE = "stage";
    public static final String COLUMN_PLANT_PLANTED_AT = "planted_at";
    public static final String COLUMN_PLANT_BLOOMED_AT = "bloomed_at";
    public static final String COLUMN_PLANT_IS_SELECTED = "is_selected";
    public static final String COLUMN_PLANT_IS_UNLOCKED = "is_unlocked";

    // Table "garden_seeds" constants
    public static final String TABLE_GARDEN_SEEDS = "garden_seeds";
    public static final String COLUMN_SEED_SPECIES = "species";
    public static final String COLUMN_SEED_NAME = "display_name";
    public static final String COLUMN_SEED_DESC = "description";
    public static final String COLUMN_SEED_COUNT = "count";
    public static final String COLUMN_SEED_IS_UNLOCKED = "is_unlocked";
    public static final String COLUMN_SEED_REQ = "unlock_requirement";
    public static final String COLUMN_SEED_REQ_LEVEL = "required_level";

    // Table "garden_goals" constants
    public static final String TABLE_GARDEN_GOALS = "garden_goals";
    public static final String COLUMN_GOAL_ID = "goal_id";
    public static final String COLUMN_GOAL_TITLE = "title";
    public static final String COLUMN_GOAL_TYPE = "goal_type";
    public static final String COLUMN_GOAL_TARGET = "target";
    public static final String COLUMN_GOAL_PROGRESS = "current_progress";
    public static final String COLUMN_GOAL_IS_WEEKLY = "is_weekly";
    public static final String COLUMN_GOAL_IS_COMPLETED = "is_completed";
    public static final String COLUMN_GOAL_REWARD_SEED = "reward_seed";
    public static final String COLUMN_GOAL_REWARD_POINTS = "reward_points";

    // Table "garden_achievements" constants
    public static final String TABLE_GARDEN_ACHIEVEMENTS = "garden_achievements";
    public static final String COLUMN_ACH_ID = "achievement_id";
    public static final String COLUMN_ACH_TITLE = "title";
    public static final String COLUMN_ACH_DESC = "description";
    public static final String COLUMN_ACH_ICON = "badge_icon";
    public static final String COLUMN_ACH_IS_UNLOCKED = "is_unlocked";
    public static final String COLUMN_ACH_UNLOCKED_AT = "unlocked_at";
    public static final String COLUMN_ACH_PROGRESS = "current_progress";
    public static final String COLUMN_ACH_TARGET = "target_progress";

    // Table "garden_history" constants
    public static final String TABLE_GARDEN_HISTORY = "garden_history";
    public static final String COLUMN_HIST_ID = "history_id";
    public static final String COLUMN_HIST_TITLE = "title";
    public static final String COLUMN_HIST_DESC = "description";
    public static final String COLUMN_HIST_TIME = "timestamp";
    public static final String COLUMN_HIST_TYPE = "event_type";
    public static final String COLUMN_HIST_SPECIES = "plant_species";

    // Table "garden_processed_sessions" constants (Deduplication engine)
    public static final String TABLE_GARDEN_PROCESSED_SESSIONS = "garden_processed_sessions";
    public static final String COLUMN_PROC_SESSION_ID = "session_id";
    public static final String COLUMN_PROC_TIME = "processed_at";
    public static final String COLUMN_PROC_MINUTES = "minutes";

    // Table "memories" constants (The Little Memory Library)
    public static final String TABLE_MEMORIES = "memories";
    public static final String COLUMN_MEM_ID = "memory_id";
    public static final String COLUMN_MEM_TITLE = "title";
    public static final String COLUMN_MEM_DESC = "description";
    public static final String COLUMN_MEM_CAT = "category";
    public static final String COLUMN_MEM_TIME = "timestamp";
    public static final String COLUMN_MEM_ICON = "icon_emoji";
    public static final String COLUMN_MEM_FAVORITE = "is_favorite";
    public static final String COLUMN_MEM_NOTE = "personal_note";
    public static final String COLUMN_MEM_KEY = "milestone_key";
    public static final String COLUMN_MEM_STATS = "stats_value";
    public static final String COLUMN_MEM_MONTH = "month_year";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create "tasks" table
        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + " ("
                + COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TASK_TITLE + " TEXT NOT NULL, "
                + COLUMN_TASK_MINUTES + " INTEGER NOT NULL, "
                + COLUMN_TASK_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_TASK_CREATED_AT + " INTEGER NOT NULL, "
                + COLUMN_TASK_COMPLETED_AT + " INTEGER DEFAULT 0, "
                + COLUMN_TASK_DESCRIPTION + " TEXT DEFAULT '', "
                + COLUMN_TASK_TARGET_DATE + " INTEGER DEFAULT 0, "
                + COLUMN_TASK_START_TIME + " INTEGER DEFAULT 0, "
                + COLUMN_TASK_DEADLINE + " INTEGER DEFAULT 0, "
                + COLUMN_TASK_PRIORITY + " INTEGER DEFAULT 1, "
                + COLUMN_TASK_CATEGORY + " TEXT DEFAULT 'General', "
                + COLUMN_TASK_REMINDER_MINS + " INTEGER DEFAULT 0, "
                + COLUMN_TASK_REMINDER_SCHED + " INTEGER DEFAULT 0"
                + ");";
        db.execSQL(createTasksTable);

        // Create "balance" table
        String createBalanceTable = "CREATE TABLE " + TABLE_BALANCE + " ("
                + COLUMN_BALANCE_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_BALANCE_MINUTES + " INTEGER NOT NULL DEFAULT 0"
                + ");";
        db.execSQL(createBalanceTable);

        // Insert default initial row: id=1, minutes=0
        ContentValues initialBalance = new ContentValues();
        initialBalance.put(COLUMN_BALANCE_ID, 1);
        initialBalance.put(COLUMN_BALANCE_MINUTES, 0);
        db.insert(TABLE_BALANCE, null, initialBalance);

        // Create buddy system tables
        createBuddyTables(db);

        // Create virtual garden tables
        createGardenTables(db);

        // Create Little Memory Library tables
        createMemoryTables(db);
    }

    private void createBuddyTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDDIES + " ("
                + COLUMN_BUDDY_UID + " TEXT PRIMARY KEY, "
                + COLUMN_BUDDY_NAME + " TEXT NOT NULL, "
                + COLUMN_BUDDY_USERNAME + " TEXT, "
                + COLUMN_BUDDY_PHOTO + " TEXT, "
                + COLUMN_BUDDY_LEVEL + " TEXT, "
                + COLUMN_BUDDY_IS_PRIMARY + " INTEGER DEFAULT 0, "
                + COLUMN_BUDDY_UPDATED_AT + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDDY_REQUESTS + " ("
                + COLUMN_REQ_ID + " TEXT PRIMARY KEY, "
                + COLUMN_REQ_SENDER_UID + " TEXT NOT NULL, "
                + COLUMN_REQ_SENDER_NAME + " TEXT, "
                + COLUMN_REQ_SENDER_USERNAME + " TEXT, "
                + COLUMN_REQ_RECEIVER_UID + " TEXT NOT NULL, "
                + COLUMN_REQ_RECEIVER_NAME + " TEXT, "
                + COLUMN_REQ_STATUS + " TEXT NOT NULL, "
                + COLUMN_REQ_CREATED_AT + " INTEGER, "
                + COLUMN_REQ_IS_INCOMING + " INTEGER DEFAULT 1"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDDY_MESSAGES + " ("
                + COLUMN_MSG_ID + " TEXT PRIMARY KEY, "
                + COLUMN_MSG_SENDER_UID + " TEXT NOT NULL, "
                + COLUMN_MSG_SENDER_NAME + " TEXT, "
                + COLUMN_MSG_RECEIVER_UID + " TEXT NOT NULL, "
                + COLUMN_MSG_RECEIVER_NAME + " TEXT, "
                + COLUMN_MSG_TEXT + " TEXT NOT NULL, "
                + COLUMN_MSG_TYPE + " TEXT, "
                + COLUMN_MSG_CREATED_AT + " INTEGER, "
                + COLUMN_MSG_IS_READ + " INTEGER DEFAULT 0"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CHALLENGES + " ("
                + COLUMN_CH_ID + " TEXT PRIMARY KEY, "
                + COLUMN_CH_TITLE + " TEXT NOT NULL, "
                + COLUMN_CH_TYPE + " TEXT NOT NULL, "
                + COLUMN_CH_TARGET + " INTEGER NOT NULL, "
                + COLUMN_CH_DURATION + " INTEGER, "
                + COLUMN_CH_START + " INTEGER, "
                + COLUMN_CH_END + " INTEGER, "
                + COLUMN_CH_CREATOR_UID + " TEXT NOT NULL, "
                + COLUMN_CH_CREATOR_NAME + " TEXT, "
                + COLUMN_CH_INVITED_UID + " TEXT NOT NULL, "
                + COLUMN_CH_INVITED_NAME + " TEXT, "
                + COLUMN_CH_STATUS + " TEXT NOT NULL, "
                + COLUMN_CH_CREATOR_PROG + " INTEGER DEFAULT 0, "
                + COLUMN_CH_INVITED_PROG + " INTEGER DEFAULT 0, "
                + COLUMN_CH_CREATED_AT + " INTEGER, "
                + COLUMN_CH_COMPLETED_AT + " INTEGER DEFAULT 0"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SHARED_STATS + " ("
                + COLUMN_STAT_UID + " TEXT PRIMARY KEY, "
                + COLUMN_STAT_TASKS_TODAY + " INTEGER DEFAULT 0, "
                + COLUMN_STAT_FOCUS_TODAY + " INTEGER DEFAULT 0, "
                + COLUMN_STAT_STREAK + " INTEGER DEFAULT 0, "
                + COLUMN_STAT_BALANCE + " INTEGER DEFAULT -1, "
                + COLUMN_STAT_WEEKLY_MINS + " INTEGER DEFAULT 0, "
                + COLUMN_STAT_WEEKLY_TASKS + " INTEGER DEFAULT 0, "
                + COLUMN_STAT_ACHIEVEMENT + " TEXT, "
                + COLUMN_STAT_UPDATED_AT + " INTEGER"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createBuddyTables(db);
        }
        if (oldVersion < 3) {
            createGardenTables(db);
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_DESCRIPTION + " TEXT DEFAULT '';");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_TARGET_DATE + " INTEGER DEFAULT 0;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_START_TIME + " INTEGER DEFAULT 0;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_DEADLINE + " INTEGER DEFAULT 0;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_PRIORITY + " INTEGER DEFAULT 1;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_CATEGORY + " TEXT DEFAULT 'General';");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_REMINDER_MINS + " INTEGER DEFAULT 0;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TASK_REMINDER_SCHED + " INTEGER DEFAULT 0;");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            createMemoryTables(db);
        }
    }

    /**
     * Helper to parse a Task model from Cursor row with full schema support.
     */
    public Task cursorToTask(Cursor cursor) {
        if (cursor == null) return null;
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE));
        int minutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_MINUTES));
        boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED)) == 1;
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_CREATED_AT));
        long completedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED_AT));

        int descIdx = cursor.getColumnIndex(COLUMN_TASK_DESCRIPTION);
        String description = descIdx >= 0 ? cursor.getString(descIdx) : "";

        int targetDateIdx = cursor.getColumnIndex(COLUMN_TASK_TARGET_DATE);
        long targetDate = targetDateIdx >= 0 ? cursor.getLong(targetDateIdx) : 0;

        int startTimeIdx = cursor.getColumnIndex(COLUMN_TASK_START_TIME);
        long startTime = startTimeIdx >= 0 ? cursor.getLong(startTimeIdx) : 0;

        int deadlineIdx = cursor.getColumnIndex(COLUMN_TASK_DEADLINE);
        long deadline = deadlineIdx >= 0 ? cursor.getLong(deadlineIdx) : 0;

        int priorityIdx = cursor.getColumnIndex(COLUMN_TASK_PRIORITY);
        int priority = priorityIdx >= 0 ? cursor.getInt(priorityIdx) : Task.PRIORITY_MEDIUM;

        int categoryIdx = cursor.getColumnIndex(COLUMN_TASK_CATEGORY);
        String category = categoryIdx >= 0 ? cursor.getString(categoryIdx) : Task.CATEGORY_GENERAL;

        int reminderMinsIdx = cursor.getColumnIndex(COLUMN_TASK_REMINDER_MINS);
        int reminderMinutes = reminderMinsIdx >= 0 ? cursor.getInt(reminderMinsIdx) : 0;

        int reminderSchedIdx = cursor.getColumnIndex(COLUMN_TASK_REMINDER_SCHED);
        boolean reminderScheduled = reminderSchedIdx >= 0 && cursor.getInt(reminderSchedIdx) == 1;

        return new Task(id, title, minutes, completed, createdAt, completedAt,
                description, targetDate, startTime, deadline, priority, category, reminderMinutes, reminderScheduled);
    }

    /**
     * Adds a new task to the tasks table (backwards compatible).
     *
     * @param title   Title or description of the task
     * @param minutes Amount of minty minutes to earn on completion
     * @return The row ID of the newly inserted task, or -1 on error
     */
    public long addTask(String title, int minutes) {
        return addTask(title, minutes, "", Task.getTodayMidnightMillis(), 0, 0, Task.PRIORITY_MEDIUM, Task.CATEGORY_GENERAL, 0);
    }

    /**
     * Adds a full scheduled task with all planner attributes.
     */
    public long addTask(String title, int minutes, String description, long targetDateMillis,
                        long startTimeMillis, long deadlineMillis, int priority, String category, int reminderMinutes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_TITLE, title);
        values.put(COLUMN_TASK_MINUTES, minutes);
        values.put(COLUMN_TASK_COMPLETED, 0);
        values.put(COLUMN_TASK_CREATED_AT, System.currentTimeMillis());
        values.put(COLUMN_TASK_COMPLETED_AT, 0);
        values.put(COLUMN_TASK_DESCRIPTION, description != null ? description : "");
        values.put(COLUMN_TASK_TARGET_DATE, targetDateMillis > 0 ? targetDateMillis : Task.getTodayMidnightMillis());
        values.put(COLUMN_TASK_START_TIME, startTimeMillis);
        values.put(COLUMN_TASK_DEADLINE, deadlineMillis);
        values.put(COLUMN_TASK_PRIORITY, priority);
        values.put(COLUMN_TASK_CATEGORY, category != null && !category.isEmpty() ? category : Task.CATEGORY_GENERAL);
        values.put(COLUMN_TASK_REMINDER_MINS, reminderMinutes);
        values.put(COLUMN_TASK_REMINDER_SCHED, 0);
        return db.insert(TABLE_TASKS, null, values);
    }

    /**
     * Retrieves all pending tasks (completed == 0), newest first.
     */
    public List<Task> getPendingTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TASKS,
                null,
                COLUMN_TASK_COMPLETED + " = 0",
                null,
                null,
                null,
                COLUMN_TASK_CREATED_AT + " DESC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    taskList.add(cursorToTask(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return taskList;
    }

    /**
     * Retrieves all completed tasks (completed == 1), most recently completed first.
     */
    public List<Task> getCompletedTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TASKS,
                null,
                COLUMN_TASK_COMPLETED + " = 1",
                null,
                null,
                null,
                COLUMN_TASK_COMPLETED_AT + " DESC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    taskList.add(cursorToTask(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return taskList;
    }

    /**
     * Retrieves pending tasks scheduled for Today.
     */
    public List<Task> getTasksForToday() {
        long todayStart = Task.getTodayMidnightMillis();
        long tomorrowStart = todayStart + 86400000L;
        List<Task> list = new ArrayList<>();
        for (Task t : getPendingTasks()) {
            if (t.getTargetDateMillis() >= todayStart && t.getTargetDateMillis() < tomorrowStart) {
                list.add(t);
            } else if (t.getTargetDateMillis() == 0 && t.getCreatedAt() >= todayStart) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Retrieves pending tasks scheduled for Tomorrow.
     */
    public List<Task> getTasksForTomorrow() {
        long tomorrowStart = Task.getTomorrowMidnightMillis();
        long dayAfterTomorrow = tomorrowStart + 86400000L;
        List<Task> list = new ArrayList<>();
        for (Task t : getPendingTasks()) {
            if (t.getTargetDateMillis() >= tomorrowStart && t.getTargetDateMillis() < dayAfterTomorrow) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Retrieves pending tasks scheduled beyond tomorrow.
     */
    public List<Task> getUpcomingTasks() {
        long dayAfterTomorrow = Task.getTomorrowMidnightMillis() + 86400000L;
        List<Task> list = new ArrayList<>();
        for (Task t : getPendingTasks()) {
            if (t.getTargetDateMillis() >= dayAfterTomorrow) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Retrieves pending tasks that were due in the past or have missed deadlines.
     */
    public List<Task> getOverdueTasks() {
        List<Task> list = new ArrayList<>();
        for (Task t : getPendingTasks()) {
            if (t.isOverdue()) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Moves a task to tomorrow (1-tap carry forward).
     */
    public boolean carryForwardTaskToTomorrow(long taskId) {
        return rescheduleTask(taskId, Task.getTomorrowMidnightMillis());
    }

    /**
     * Reschedules a task to a target day timestamp.
     */
    public boolean rescheduleTask(long taskId, long newTargetDateMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_TARGET_DATE, newTargetDateMillis);
        int rows = db.update(TABLE_TASKS, values, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(taskId)});
        return rows > 0;
    }

    /**
     * Updates an existing task with modified values.
     */
    public boolean updateTask(Task task) {
        if (task == null) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_TITLE, task.getTitle());
        values.put(COLUMN_TASK_MINUTES, task.getMinutes());
        values.put(COLUMN_TASK_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_TASK_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TASK_TARGET_DATE, task.getTargetDateMillis());
        values.put(COLUMN_TASK_START_TIME, task.getStartTimeMillis());
        values.put(COLUMN_TASK_DEADLINE, task.getDeadlineMillis());
        values.put(COLUMN_TASK_PRIORITY, task.getPriority());
        values.put(COLUMN_TASK_CATEGORY, task.getCategory());
        values.put(COLUMN_TASK_REMINDER_MINS, task.getReminderMinutes());
        values.put(COLUMN_TASK_REMINDER_SCHED, task.isReminderScheduled() ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(task.getId())});
        return rows > 0;
    }

    /**
     * Renames any legacy sample tasks containing "Provider Task" to realistic student study tasks.
     */
    public void renameLegacyProviderTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] realisticNames = {
            "Review Algorithm Notes 💻",
            "Database Lab Report 📝",
            "Math Problem Set #4 📐",
            "Read Chapter 5 in Textbook 📖"
        };
        try (Cursor c = db.query(TABLE_TASKS, new String[]{COLUMN_TASK_ID},
                COLUMN_TASK_TITLE + " LIKE 'Provider Task%'", null, null, null, COLUMN_TASK_ID + " ASC")) {
            int i = 0;
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String newTitle = realisticNames[i % realisticNames.length];
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_TASK_TITLE, newTitle);
                    cv.put(COLUMN_TASK_CATEGORY, "Study");
                    db.update(TABLE_TASKS, cv, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(id)});
                    i++;
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Marks the task done AND adds its minutes to the balance in one single database transaction.
     *
     * @param id The ID of the task to complete
     * @return true if completion and balance credit succeeded, false otherwise
     */
    public boolean completeTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        Cursor cursor = null;
        try {
            // Find task minutes and current completion status
            cursor = db.query(
                    TABLE_TASKS,
                    new String[]{COLUMN_TASK_MINUTES, COLUMN_TASK_COMPLETED},
                    COLUMN_TASK_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int minutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_MINUTES));
                int completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED));

                // Only complete if not already completed
                if (completed == 0) {
                    ContentValues taskValues = new ContentValues();
                    taskValues.put(COLUMN_TASK_COMPLETED, 1);
                    taskValues.put(COLUMN_TASK_COMPLETED_AT, System.currentTimeMillis());
                    db.update(TABLE_TASKS, taskValues, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(id)});

                    // Add task minutes to the balance row (id=1)
                    db.execSQL("UPDATE " + TABLE_BALANCE + " SET " + COLUMN_BALANCE_MINUTES
                            + " = " + COLUMN_BALANCE_MINUTES + " + ? WHERE " + COLUMN_BALANCE_ID + " = 1",
                            new Object[]{minutes});

                    db.setTransactionSuccessful();
                    success = true;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
        return success;
    }

    /**
     * Returns the current minty minutes balance from the balance table (id=1).
     */
    public int getBalance() {
        int balance = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_BALANCE,
                new String[]{COLUMN_BALANCE_MINUTES},
                COLUMN_BALANCE_ID + " = 1",
                null,
                null,
                null,
                null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    balance = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BALANCE_MINUTES));
                }
            } finally {
                cursor.close();
            }
        }
        return balance;
    }

    /**
     * Subtracts minutes from the balance only if the current balance is sufficient.
     *
     * @param minutes The number of minutes to spend
     * @return true if minutes were successfully spent, false if balance is insufficient
     */
    public boolean spendMinutes(int minutes) {
        if (minutes <= 0) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            int currentBalance = getBalance();
            if (currentBalance >= minutes) {
                db.execSQL("UPDATE " + TABLE_BALANCE + " SET " + COLUMN_BALANCE_MINUTES
                        + " = " + COLUMN_BALANCE_MINUTES + " - ? WHERE " + COLUMN_BALANCE_ID + " = 1",
                        new Object[]{minutes});
                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    /**
     * Calculates the total minty minutes earned across all completed tasks.
     */
    public int getTotalMinutesEarned() {
        int total = 0;
        List<Task> completed = getCompletedTasks();
        for (Task t : completed) {
            total += t.getMinutes();
        }
        return total;
    }

    /**
     * Updates or inserts a task from Cloud Firestore synchronization without
     * double-crediting minutes to the balance.
     *
     * @param task The task retrieved from Cloud Firestore
     * @return true if upsert succeeded
     */
    public boolean upsertTaskFromCloud(Task task) {
        if (task == null) return false;
        SQLiteDatabase db = this.getWritableDatabase();

        // Prevent cloud sync from accidentally overwriting newer local completion data
        Task localTask = getTaskById(task.getId());
        if (localTask != null) {
            // If local task was already completed locally (e.g. offline), do NOT revert to uncompleted
            if (localTask.isCompleted() && !task.isCompleted()) {
                return false;
            }
            // If both are completed, preserve the local completion if local timestamp is newer
            if (localTask.isCompleted() && task.isCompleted() && localTask.getCompletedAt() > task.getCompletedAt()) {
                return false;
            }
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_ID, task.getId());
        values.put(COLUMN_TASK_TITLE, task.getTitle());
        values.put(COLUMN_TASK_MINUTES, task.getMinutes());
        values.put(COLUMN_TASK_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_TASK_CREATED_AT, task.getCreatedAt() > 0 ? task.getCreatedAt() : System.currentTimeMillis());
        values.put(COLUMN_TASK_COMPLETED_AT, task.getCompletedAt());
        values.put(COLUMN_TASK_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TASK_TARGET_DATE, task.getTargetDateMillis());
        values.put(COLUMN_TASK_START_TIME, task.getStartTimeMillis());
        values.put(COLUMN_TASK_DEADLINE, task.getDeadlineMillis());
        values.put(COLUMN_TASK_PRIORITY, task.getPriority());
        values.put(COLUMN_TASK_CATEGORY, task.getCategory());
        values.put(COLUMN_TASK_REMINDER_MINS, task.getReminderMinutes());
        values.put(COLUMN_TASK_REMINDER_SCHED, task.isReminderScheduled() ? 1 : 0);

        long rowId = db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return rowId != -1;
    }

    /**
     * Directly sets the balance in the balance table (id=1) from cloud synchronization.
     *
     * @param newBalance The authoritative balance from Firebase Realtime Database
     */
    public void updateBalanceDirect(int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BALANCE_MINUTES, Math.max(0, newBalance));
        db.update(TABLE_BALANCE, values, COLUMN_BALANCE_ID + " = 1", null);
    }

    /**
     * Deletes a task by its ID.
     */
    public boolean deleteTaskById(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affected = db.delete(TABLE_TASKS, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(id)});
        return affected > 0;
    }

    /**
     * Retrieves a task by its ID.
     */
    public Task getTaskById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Task task = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    task = cursorToTask(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return task;
    }

    // =========================================================================
    // BUDDY SYSTEM OFFLINE CACHING
    // =========================================================================

    public void cacheBuddy(UserProfile buddy, boolean isPrimary) {
        if (buddy == null || buddy.getUid().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_BUDDY_UID, buddy.getUid());
        cv.put(COLUMN_BUDDY_NAME, buddy.getDisplayName());
        cv.put(COLUMN_BUDDY_USERNAME, buddy.getUsername());
        cv.put(COLUMN_BUDDY_PHOTO, buddy.getPhotoUrl());
        cv.put(COLUMN_BUDDY_LEVEL, buddy.getTitleLevel());
        cv.put(COLUMN_BUDDY_IS_PRIMARY, isPrimary ? 1 : 0);
        cv.put(COLUMN_BUDDY_UPDATED_AT, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_BUDDIES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void setPrimaryBuddy(String uid) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Clear old primary
        ContentValues clearCv = new ContentValues();
        clearCv.put(COLUMN_BUDDY_IS_PRIMARY, 0);
        db.update(TABLE_BUDDIES, clearCv, null, null);

        if (uid != null && !uid.isEmpty()) {
            ContentValues setCv = new ContentValues();
            setCv.put(COLUMN_BUDDY_IS_PRIMARY, 1);
            db.update(TABLE_BUDDIES, setCv, COLUMN_BUDDY_UID + " = ?", new String[]{uid});
        }
    }

    public String getPrimaryBuddyUid() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_BUDDIES, new String[]{COLUMN_BUDDY_UID},
                COLUMN_BUDDY_IS_PRIMARY + " = 1", null, null, null, null);
        String uid = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    uid = c.getString(0);
                }
            } finally {
                c.close();
            }
        }
        return uid;
    }

    public List<UserProfile> getCachedBuddies() {
        List<UserProfile> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_BUDDIES, null, null, null, null, null,
                COLUMN_BUDDY_IS_PRIMARY + " DESC, " + COLUMN_BUDDY_NAME + " ASC");
        if (c != null) {
            try {
                int uidIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_UID);
                int nameIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_NAME);
                int usernameIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_USERNAME);
                int photoIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_PHOTO);
                int levelIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_LEVEL);
                int updatedIdx = c.getColumnIndexOrThrow(COLUMN_BUDDY_UPDATED_AT);

                while (c.moveToNext()) {
                    list.add(new UserProfile(
                            c.getString(uidIdx),
                            c.getString(nameIdx),
                            c.getString(usernameIdx),
                            "",
                            c.getString(photoIdx),
                            c.getString(levelIdx),
                            c.getLong(updatedIdx)
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void deleteCachedBuddy(String uid) {
        if (uid == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BUDDIES, COLUMN_BUDDY_UID + " = ?", new String[]{uid});
        db.delete(TABLE_SHARED_STATS, COLUMN_STAT_UID + " = ?", new String[]{uid});
    }

    public void cacheBuddyRequest(BuddyRequest request, boolean isIncoming) {
        if (request == null || request.getRequestId().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_REQ_ID, request.getRequestId());
        cv.put(COLUMN_REQ_SENDER_UID, request.getSenderUid());
        cv.put(COLUMN_REQ_SENDER_NAME, request.getSenderName());
        cv.put(COLUMN_REQ_SENDER_USERNAME, request.getSenderUsername());
        cv.put(COLUMN_REQ_RECEIVER_UID, request.getReceiverUid());
        cv.put(COLUMN_REQ_RECEIVER_NAME, request.getReceiverName());
        cv.put(COLUMN_REQ_STATUS, request.getStatus());
        cv.put(COLUMN_REQ_CREATED_AT, request.getCreatedAt());
        cv.put(COLUMN_REQ_IS_INCOMING, isIncoming ? 1 : 0);

        db.insertWithOnConflict(TABLE_BUDDY_REQUESTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<BuddyRequest> getCachedRequests(boolean incoming) {
        List<BuddyRequest> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_BUDDY_REQUESTS, null,
                COLUMN_REQ_IS_INCOMING + " = ? AND " + COLUMN_REQ_STATUS + " = ?",
                new String[]{incoming ? "1" : "0", BuddyRequest.STATUS_PENDING},
                null, null, COLUMN_REQ_CREATED_AT + " DESC");
        if (c != null) {
            try {
                int idIdx = c.getColumnIndexOrThrow(COLUMN_REQ_ID);
                int sUidIdx = c.getColumnIndexOrThrow(COLUMN_REQ_SENDER_UID);
                int sNameIdx = c.getColumnIndexOrThrow(COLUMN_REQ_SENDER_NAME);
                int sUserIdx = c.getColumnIndexOrThrow(COLUMN_REQ_SENDER_USERNAME);
                int rUidIdx = c.getColumnIndexOrThrow(COLUMN_REQ_RECEIVER_UID);
                int rNameIdx = c.getColumnIndexOrThrow(COLUMN_REQ_RECEIVER_NAME);
                int statusIdx = c.getColumnIndexOrThrow(COLUMN_REQ_STATUS);
                int createdIdx = c.getColumnIndexOrThrow(COLUMN_REQ_CREATED_AT);

                while (c.moveToNext()) {
                    list.add(new BuddyRequest(
                            c.getString(idIdx),
                            c.getString(sUidIdx),
                            c.getString(sNameIdx),
                            c.getString(sUserIdx),
                            c.getString(rUidIdx),
                            c.getString(rNameIdx),
                            c.getString(statusIdx),
                            c.getLong(createdIdx),
                            c.getLong(createdIdx)
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void deleteCachedRequest(String requestId) {
        if (requestId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BUDDY_REQUESTS, COLUMN_REQ_ID + " = ?", new String[]{requestId});
    }

    public void cacheMessage(BuddyMessage message) {
        if (message == null || message.getMessageId().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MSG_ID, message.getMessageId());
        cv.put(COLUMN_MSG_SENDER_UID, message.getSenderUid());
        cv.put(COLUMN_MSG_SENDER_NAME, message.getSenderName());
        cv.put(COLUMN_MSG_RECEIVER_UID, message.getReceiverUid());
        cv.put(COLUMN_MSG_RECEIVER_NAME, message.getReceiverName());
        cv.put(COLUMN_MSG_TEXT, message.getMessage());
        cv.put(COLUMN_MSG_TYPE, message.getMessageType());
        cv.put(COLUMN_MSG_CREATED_AT, message.getCreatedAt());
        cv.put(COLUMN_MSG_IS_READ, message.isRead() ? 1 : 0);

        db.insertWithOnConflict(TABLE_BUDDY_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<BuddyMessage> getCachedMessages(String otherUid) {
        List<BuddyMessage> list = new ArrayList<>();
        if (otherUid == null) return list;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_BUDDY_MESSAGES, null,
                COLUMN_MSG_SENDER_UID + " = ? OR " + COLUMN_MSG_RECEIVER_UID + " = ?",
                new String[]{otherUid, otherUid},
                null, null, COLUMN_MSG_CREATED_AT + " ASC");
        if (c != null) {
            try {
                int idIdx = c.getColumnIndexOrThrow(COLUMN_MSG_ID);
                int sUidIdx = c.getColumnIndexOrThrow(COLUMN_MSG_SENDER_UID);
                int sNameIdx = c.getColumnIndexOrThrow(COLUMN_MSG_SENDER_NAME);
                int rUidIdx = c.getColumnIndexOrThrow(COLUMN_MSG_RECEIVER_UID);
                int rNameIdx = c.getColumnIndexOrThrow(COLUMN_MSG_RECEIVER_NAME);
                int textIdx = c.getColumnIndexOrThrow(COLUMN_MSG_TEXT);
                int typeIdx = c.getColumnIndexOrThrow(COLUMN_MSG_TYPE);
                int createdIdx = c.getColumnIndexOrThrow(COLUMN_MSG_CREATED_AT);
                int readIdx = c.getColumnIndexOrThrow(COLUMN_MSG_IS_READ);

                while (c.moveToNext()) {
                    list.add(new BuddyMessage(
                            c.getString(idIdx),
                            c.getString(sUidIdx),
                            c.getString(sNameIdx),
                            c.getString(rUidIdx),
                            c.getString(rNameIdx),
                            c.getString(textIdx),
                            c.getString(typeIdx),
                            c.getLong(createdIdx),
                            c.getInt(readIdx) == 1
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void markMessageReadLocally(String messageId) {
        if (messageId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MSG_IS_READ, 1);
        db.update(TABLE_BUDDY_MESSAGES, cv, COLUMN_MSG_ID + " = ?", new String[]{messageId});
    }

    public int getUnreadMessageCount(String myUid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_BUDDY_MESSAGES, new String[]{"COUNT(*)"},
                COLUMN_MSG_RECEIVER_UID + " = ? AND " + COLUMN_MSG_IS_READ + " = 0",
                new String[]{myUid != null ? myUid : ""}, null, null, null);
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) count = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return count;
    }

    public void cacheChallenge(StudyChallenge challenge) {
        if (challenge == null || challenge.getChallengeId().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CH_ID, challenge.getChallengeId());
        cv.put(COLUMN_CH_TITLE, challenge.getTitle());
        cv.put(COLUMN_CH_TYPE, challenge.getType());
        cv.put(COLUMN_CH_TARGET, challenge.getTarget());
        cv.put(COLUMN_CH_DURATION, challenge.getDurationDays());
        cv.put(COLUMN_CH_START, challenge.getStartDate());
        cv.put(COLUMN_CH_END, challenge.getEndDate());
        cv.put(COLUMN_CH_CREATOR_UID, challenge.getCreatorUid());
        cv.put(COLUMN_CH_CREATOR_NAME, challenge.getCreatorName());
        cv.put(COLUMN_CH_INVITED_UID, challenge.getInvitedUid());
        cv.put(COLUMN_CH_INVITED_NAME, challenge.getInvitedName());
        cv.put(COLUMN_CH_STATUS, challenge.getStatus());
        cv.put(COLUMN_CH_CREATOR_PROG, challenge.getCreatorProgress());
        cv.put(COLUMN_CH_INVITED_PROG, challenge.getInvitedProgress());
        cv.put(COLUMN_CH_CREATED_AT, challenge.getCreatedAt());
        cv.put(COLUMN_CH_COMPLETED_AT, challenge.getCompletedAt());

        db.insertWithOnConflict(TABLE_CHALLENGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<StudyChallenge> getCachedChallenges() {
        List<StudyChallenge> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CHALLENGES, null, null, null, null, null,
                COLUMN_CH_CREATED_AT + " DESC");
        if (c != null) {
            try {
                int idIdx = c.getColumnIndexOrThrow(COLUMN_CH_ID);
                int titleIdx = c.getColumnIndexOrThrow(COLUMN_CH_TITLE);
                int typeIdx = c.getColumnIndexOrThrow(COLUMN_CH_TYPE);
                int targetIdx = c.getColumnIndexOrThrow(COLUMN_CH_TARGET);
                int durIdx = c.getColumnIndexOrThrow(COLUMN_CH_DURATION);
                int startIdx = c.getColumnIndexOrThrow(COLUMN_CH_START);
                int endIdx = c.getColumnIndexOrThrow(COLUMN_CH_END);
                int cUidIdx = c.getColumnIndexOrThrow(COLUMN_CH_CREATOR_UID);
                int cNameIdx = c.getColumnIndexOrThrow(COLUMN_CH_CREATOR_NAME);
                int iUidIdx = c.getColumnIndexOrThrow(COLUMN_CH_INVITED_UID);
                int iNameIdx = c.getColumnIndexOrThrow(COLUMN_CH_INVITED_NAME);
                int statusIdx = c.getColumnIndexOrThrow(COLUMN_CH_STATUS);
                int cProgIdx = c.getColumnIndexOrThrow(COLUMN_CH_CREATOR_PROG);
                int iProgIdx = c.getColumnIndexOrThrow(COLUMN_CH_INVITED_PROG);
                int createdIdx = c.getColumnIndexOrThrow(COLUMN_CH_CREATED_AT);
                int compIdx = c.getColumnIndexOrThrow(COLUMN_CH_COMPLETED_AT);

                while (c.moveToNext()) {
                    list.add(new StudyChallenge(
                            c.getString(idIdx),
                            c.getString(titleIdx),
                            c.getString(typeIdx),
                            c.getInt(targetIdx),
                            c.getInt(durIdx),
                            c.getLong(startIdx),
                            c.getLong(endIdx),
                            c.getString(cUidIdx),
                            c.getString(cNameIdx),
                            c.getString(iUidIdx),
                            c.getString(iNameIdx),
                            c.getString(statusIdx),
                            c.getInt(cProgIdx),
                            c.getInt(iProgIdx),
                            c.getLong(createdIdx),
                            c.getLong(compIdx)
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void cacheSharedStats(SharedStats stats) {
        if (stats == null || stats.getUid().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_STAT_UID, stats.getUid());
        cv.put(COLUMN_STAT_TASKS_TODAY, stats.getCompletedTasksToday());
        cv.put(COLUMN_STAT_FOCUS_TODAY, stats.getFocusMinutesToday());
        cv.put(COLUMN_STAT_STREAK, stats.getStreakDays());
        cv.put(COLUMN_STAT_BALANCE, stats.getBalanceMinutes());
        cv.put(COLUMN_STAT_WEEKLY_MINS, stats.getWeeklyMinutes());
        cv.put(COLUMN_STAT_WEEKLY_TASKS, stats.getWeeklyTasksCount());
        cv.put(COLUMN_STAT_ACHIEVEMENT, stats.getRecentAchievement());
        cv.put(COLUMN_STAT_UPDATED_AT, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_SHARED_STATS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public SharedStats getCachedSharedStats(String uid) {
        if (uid == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_SHARED_STATS, null, COLUMN_STAT_UID + " = ?",
                new String[]{uid}, null, null, null);
        SharedStats stats = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    stats = new SharedStats(
                            uid,
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_TASKS_TODAY)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_FOCUS_TODAY)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_STREAK)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_BALANCE)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_WEEKLY_MINS)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_STAT_WEEKLY_TASKS)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_STAT_ACHIEVEMENT)),
                            true, true, true, true, true, true,
                            c.getLong(c.getColumnIndexOrThrow(COLUMN_STAT_UPDATED_AT))
                    );
                }
            } finally {
                c.close();
            }
        }
        return stats;
    }

    // =========================================================================
    // VIRTUAL GARDEN SCHEMA & DATABASE METHODS (Stage 2)
    // =========================================================================

    public void createGardenTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_PLANTS + " ("
                + COLUMN_PLANT_ID + " TEXT PRIMARY KEY, "
                + COLUMN_PLANT_NAME + " TEXT NOT NULL, "
                + COLUMN_PLANT_SPECIES + " TEXT NOT NULL, "
                + COLUMN_PLANT_POINTS + " INTEGER DEFAULT 0, "
                + COLUMN_PLANT_STAGE + " INTEGER DEFAULT 0, "
                + COLUMN_PLANT_PLANTED_AT + " INTEGER, "
                + COLUMN_PLANT_BLOOMED_AT + " INTEGER DEFAULT 0, "
                + COLUMN_PLANT_IS_SELECTED + " INTEGER DEFAULT 0, "
                + COLUMN_PLANT_IS_UNLOCKED + " INTEGER DEFAULT 1"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_SEEDS + " ("
                + COLUMN_SEED_SPECIES + " TEXT PRIMARY KEY, "
                + COLUMN_SEED_NAME + " TEXT NOT NULL, "
                + COLUMN_SEED_DESC + " TEXT, "
                + COLUMN_SEED_COUNT + " INTEGER DEFAULT 0, "
                + COLUMN_SEED_IS_UNLOCKED + " INTEGER DEFAULT 0, "
                + COLUMN_SEED_REQ + " TEXT, "
                + COLUMN_SEED_REQ_LEVEL + " INTEGER DEFAULT 1"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_GOALS + " ("
                + COLUMN_GOAL_ID + " TEXT PRIMARY KEY, "
                + COLUMN_GOAL_TITLE + " TEXT NOT NULL, "
                + COLUMN_GOAL_TYPE + " TEXT NOT NULL, "
                + COLUMN_GOAL_TARGET + " INTEGER NOT NULL, "
                + COLUMN_GOAL_PROGRESS + " INTEGER DEFAULT 0, "
                + COLUMN_GOAL_IS_WEEKLY + " INTEGER DEFAULT 0, "
                + COLUMN_GOAL_IS_COMPLETED + " INTEGER DEFAULT 0, "
                + COLUMN_GOAL_REWARD_SEED + " TEXT, "
                + COLUMN_GOAL_REWARD_POINTS + " INTEGER DEFAULT 0"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_ACHIEVEMENTS + " ("
                + COLUMN_ACH_ID + " TEXT PRIMARY KEY, "
                + COLUMN_ACH_TITLE + " TEXT NOT NULL, "
                + COLUMN_ACH_DESC + " TEXT, "
                + COLUMN_ACH_ICON + " TEXT, "
                + COLUMN_ACH_IS_UNLOCKED + " INTEGER DEFAULT 0, "
                + COLUMN_ACH_UNLOCKED_AT + " INTEGER DEFAULT 0, "
                + COLUMN_ACH_PROGRESS + " INTEGER DEFAULT 0, "
                + COLUMN_ACH_TARGET + " INTEGER DEFAULT 1"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_HISTORY + " ("
                + COLUMN_HIST_ID + " TEXT PRIMARY KEY, "
                + COLUMN_HIST_TITLE + " TEXT NOT NULL, "
                + COLUMN_HIST_DESC + " TEXT, "
                + COLUMN_HIST_TIME + " INTEGER NOT NULL, "
                + COLUMN_HIST_TYPE + " TEXT NOT NULL, "
                + COLUMN_HIST_SPECIES + " TEXT"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GARDEN_PROCESSED_SESSIONS + " ("
                + COLUMN_PROC_SESSION_ID + " TEXT PRIMARY KEY, "
                + COLUMN_PROC_TIME + " INTEGER NOT NULL, "
                + COLUMN_PROC_MINUTES + " INTEGER NOT NULL"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries ("
                + "entry_id TEXT PRIMARY KEY, "
                + "timestamp INTEGER NOT NULL, "
                + "topic TEXT, "
                + "takeaway TEXT, "
                + "rating INTEGER DEFAULT 5, "
                + "notes TEXT"
                + ");");

        // Seed default starter plant if none exists
        Cursor cPlant = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_PLANTS, null);
        boolean hasPlants = false;
        if (cPlant != null) {
            if (cPlant.moveToFirst() && cPlant.getInt(0) > 0) hasPlants = true;
            cPlant.close();
        }

        if (!hasPlants) {
            ContentValues starterPlant = new ContentValues();
            starterPlant.put(COLUMN_PLANT_ID, "plant_mint_starter");
            starterPlant.put(COLUMN_PLANT_NAME, "Minty");
            starterPlant.put(COLUMN_PLANT_SPECIES, GardenPlant.SPECIES_MINT);
            starterPlant.put(COLUMN_PLANT_POINTS, 15);
            starterPlant.put(COLUMN_PLANT_STAGE, GardenPlant.STAGE_SPROUT);
            starterPlant.put(COLUMN_PLANT_PLANTED_AT, System.currentTimeMillis());
            starterPlant.put(COLUMN_PLANT_BLOOMED_AT, 0);
            starterPlant.put(COLUMN_PLANT_IS_SELECTED, 1);
            starterPlant.put(COLUMN_PLANT_IS_UNLOCKED, 1);
            db.insert(TABLE_GARDEN_PLANTS, null, starterPlant);
        }

        // Seed default seed varieties if none exist
        Cursor cSeeds = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_SEEDS, null);
        boolean hasSeeds = false;
        if (cSeeds != null) {
            if (cSeeds.moveToFirst() && cSeeds.getInt(0) > 0) hasSeeds = true;
            cSeeds.close();
        }

        if (!hasSeeds) {
            insertSeedInternal(db, GardenPlant.SPECIES_MINT, "Mint Seeds", "The classic aromatic mascot herb. Fast growing and resilient.", 2, 1, "Starter variety", 1);
            insertSeedInternal(db, GardenPlant.SPECIES_SUNFLOWER, "Sunflower Seeds", "Radiant golden bloom that turns toward the morning sun.", 2, 1, "Starter variety", 1);
            insertSeedInternal(db, GardenPlant.SPECIES_DAISY, "Daisy Seeds", "Gentle white and golden petals symbolizing fresh beginnings.", 1, 1, "Starter variety", 1);
            insertSeedInternal(db, GardenPlant.SPECIES_TULIP, "Tulip Bulbs", "Graceful cup-shaped pastel blooms.", 0, 0, "Reach Garden Level 2", 2);
            insertSeedInternal(db, GardenPlant.SPECIES_ROSE, "Rose Seeds", "Classic vibrant petals celebrating deep focus accomplishments.", 0, 0, "Reach Garden Level 3", 3);
            insertSeedInternal(db, GardenPlant.SPECIES_LAVENDER, "Lavender Seeds", "Calming purple stalks providing a soothing study atmosphere.", 0, 0, "Reach Garden Level 4", 4);
            insertSeedInternal(db, GardenPlant.SPECIES_CHERRY_BLOSSOM, "Cherry Blossom", "Rare sakura pink petals celebrating ultimate dedication.", 0, 0, "Reach Garden Level 5", 5);
        }

        // Seed default goals if none exist
        Cursor cGoals = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_GOALS, null);
        boolean hasGoals = false;
        if (cGoals != null) {
            if (cGoals.moveToFirst() && cGoals.getInt(0) > 0) hasGoals = true;
            cGoals.close();
        }

        if (!hasGoals) {
            insertGoalInternal(db, "daily_sessions", "Complete 2 Focus Sessions", GardenGoal.TYPE_SESSIONS, 2, 0, 0, GardenPlant.SPECIES_SUNFLOWER, 20);
            insertGoalInternal(db, "daily_mins", "Focus for 45 Minutes", GardenGoal.TYPE_MINUTES, 45, 0, 0, GardenPlant.SPECIES_DAISY, 25);
            insertGoalInternal(db, "daily_tasks", "Complete 3 Tasks", GardenGoal.TYPE_TASKS, 3, 0, 0, GardenPlant.SPECIES_MINT, 20);
            insertGoalInternal(db, "weekly_mins", "Focus 200 Minutes This Week", GardenGoal.TYPE_MINUTES, 200, 1, 0, GardenPlant.SPECIES_TULIP, 50);
            insertGoalInternal(db, "weekly_streak", "Maintain a 5-Day Streak", GardenGoal.TYPE_STREAK, 5, 1, 0, GardenPlant.SPECIES_ROSE, 75);
        }

        // Seed default achievements if none exist
        Cursor cAch = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_ACHIEVEMENTS, null);
        boolean hasAch = false;
        if (cAch != null) {
            if (cAch.moveToFirst() && cAch.getInt(0) > 0) hasAch = true;
            cAch.close();
        }

        if (!hasAch) {
            insertAchInternal(db, "ach_first_seed", "First Seed", "Plant your first seed in the garden.", "seed", 0, 1);
            insertAchInternal(db, "ach_first_sprout", "First Sprout", "Grow your first plant into a healthy sprout.", "sprout", 0, 1);
            insertAchInternal(db, "ach_first_bloom", "First Bloom", "Bloom your very first full flower.", "bloom", 0, 1);
            insertAchInternal(db, "ach_streak_3", "Consistent Gardener", "Maintain a 3-day focus streak.", "streak", 0, 3);
            insertAchInternal(db, "ach_sessions_10", "Focus Enthusiast", "Complete 10 productive focus sessions.", "timer", 0, 10);
            insertAchInternal(db, "ach_blooms_5", "Flourishing Garden", "Bloom 5 beautiful flowers in your garden.", "flowers", 0, 5);
            insertAchInternal(db, "ach_weekly_goal", "Dedicated Gardener", "Complete a full weekly study goal.", "goal", 0, 1);
        }
    }

    private void insertSeedInternal(SQLiteDatabase db, String species, String name, String desc, int count, int unlocked, String req, int reqLevel) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEED_SPECIES, species);
        cv.put(COLUMN_SEED_NAME, name);
        cv.put(COLUMN_SEED_DESC, desc);
        cv.put(COLUMN_SEED_COUNT, count);
        cv.put(COLUMN_SEED_IS_UNLOCKED, unlocked);
        cv.put(COLUMN_SEED_REQ, req);
        cv.put(COLUMN_SEED_REQ_LEVEL, reqLevel);
        db.insertWithOnConflict(TABLE_GARDEN_SEEDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertGoalInternal(SQLiteDatabase db, String id, String title, String type, int target, int isWeekly, int isDone, String rewardSeed, int rewardPts) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GOAL_ID, id);
        cv.put(COLUMN_GOAL_TITLE, title);
        cv.put(COLUMN_GOAL_TYPE, type);
        cv.put(COLUMN_GOAL_TARGET, target);
        cv.put(COLUMN_GOAL_PROGRESS, 0);
        cv.put(COLUMN_GOAL_IS_WEEKLY, isWeekly);
        cv.put(COLUMN_GOAL_IS_COMPLETED, isDone);
        cv.put(COLUMN_GOAL_REWARD_SEED, rewardSeed);
        cv.put(COLUMN_GOAL_REWARD_POINTS, rewardPts);
        db.insertWithOnConflict(TABLE_GARDEN_GOALS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertAchInternal(SQLiteDatabase db, String id, String title, String desc, String icon, int unlocked, int target) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ACH_ID, id);
        cv.put(COLUMN_ACH_TITLE, title);
        cv.put(COLUMN_ACH_DESC, desc);
        cv.put(COLUMN_ACH_ICON, icon);
        cv.put(COLUMN_ACH_IS_UNLOCKED, unlocked);
        cv.put(COLUMN_ACH_UNLOCKED_AT, 0);
        cv.put(COLUMN_ACH_PROGRESS, 0);
        cv.put(COLUMN_ACH_TARGET, target);
        db.insertWithOnConflict(TABLE_GARDEN_ACHIEVEMENTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // --- Plants Methods ---

    public GardenPlant getActivePlant() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_PLANTS, null, COLUMN_PLANT_IS_SELECTED + " = 1", null, null, null, null, "1");
        GardenPlant plant = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    plant = plantFromCursor(c);
                }
            } finally {
                c.close();
            }
        }
        if (plant == null) {
            List<GardenPlant> all = getAllPlants();
            if (!all.isEmpty()) {
                plant = all.get(0);
                setSelectedPlant(plant.getPlantId());
            }
        }
        return plant;
    }

    public List<GardenPlant> getAllPlants() {
        List<GardenPlant> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_PLANTS, null, null, null, null, null, COLUMN_PLANT_PLANTED_AT + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(plantFromCursor(c));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public GardenPlant getPlantById(String plantId) {
        if (plantId == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_PLANTS, null, COLUMN_PLANT_ID + " = ?", new String[]{plantId}, null, null, null);
        GardenPlant plant = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) plant = plantFromCursor(c);
            } finally {
                c.close();
            }
        }
        return plant;
    }

    public void savePlant(GardenPlant plant) {
        if (plant == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PLANT_ID, plant.getPlantId());
        cv.put(COLUMN_PLANT_NAME, plant.getName());
        cv.put(COLUMN_PLANT_SPECIES, plant.getSpecies());
        cv.put(COLUMN_PLANT_POINTS, plant.getGrowthPoints());
        cv.put(COLUMN_PLANT_STAGE, plant.getStage());
        cv.put(COLUMN_PLANT_PLANTED_AT, plant.getPlantedAt());
        cv.put(COLUMN_PLANT_BLOOMED_AT, plant.getBloomedAt());
        cv.put(COLUMN_PLANT_IS_SELECTED, plant.isSelected() ? 1 : 0);
        cv.put(COLUMN_PLANT_IS_UNLOCKED, plant.isUnlocked() ? 1 : 0);
        db.insertWithOnConflict(TABLE_GARDEN_PLANTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void setSelectedPlant(String plantId) {
        if (plantId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues clear = new ContentValues();
        clear.put(COLUMN_PLANT_IS_SELECTED, 0);
        db.update(TABLE_GARDEN_PLANTS, clear, null, null);

        ContentValues set = new ContentValues();
        set.put(COLUMN_PLANT_IS_SELECTED, 1);
        db.update(TABLE_GARDEN_PLANTS, set, COLUMN_PLANT_ID + " = ?", new String[]{plantId});
    }

    public int getBloomedPlantsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_PLANTS + " WHERE " + COLUMN_PLANT_STAGE + " >= 4 OR " + COLUMN_PLANT_BLOOMED_AT + " > 0", null);
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) count = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return count;
    }

    private GardenPlant plantFromCursor(Cursor c) {
        return new GardenPlant(
                c.getString(c.getColumnIndexOrThrow(COLUMN_PLANT_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_PLANT_NAME)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_PLANT_SPECIES)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_PLANT_POINTS)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_PLANT_STAGE)),
                c.getLong(c.getColumnIndexOrThrow(COLUMN_PLANT_PLANTED_AT)),
                c.getLong(c.getColumnIndexOrThrow(COLUMN_PLANT_BLOOMED_AT)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_PLANT_IS_SELECTED)) == 1,
                c.getInt(c.getColumnIndexOrThrow(COLUMN_PLANT_IS_UNLOCKED)) == 1
        );
    }

    // --- Seeds Methods ---

    public List<GardenSeed> getSeedInventory() {
        List<GardenSeed> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_SEEDS, null, null, null, null, null, COLUMN_SEED_REQ_LEVEL + " ASC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(new GardenSeed(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_SPECIES)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_NAME)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_DESC)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_COUNT)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_IS_UNLOCKED)) == 1,
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_REQ)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_REQ_LEVEL))
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public GardenSeed getSeedBySpecies(String species) {
        if (species == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_SEEDS, null, COLUMN_SEED_SPECIES + " = ?", new String[]{species}, null, null, null);
        GardenSeed seed = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    seed = new GardenSeed(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_SPECIES)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_NAME)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_DESC)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_COUNT)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_IS_UNLOCKED)) == 1,
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SEED_REQ)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_SEED_REQ_LEVEL))
                    );
                }
            } finally {
                c.close();
            }
        }
        return seed;
    }

    public void addSeeds(String species, int count) {
        if (species == null || count <= 0) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_GARDEN_SEEDS + " SET " + COLUMN_SEED_COUNT + " = " + COLUMN_SEED_COUNT + " + " + count
                + ", " + COLUMN_SEED_IS_UNLOCKED + " = 1 WHERE " + COLUMN_SEED_SPECIES + " = ?", new String[]{species});
    }

    public boolean consumeSeed(String species) {
        GardenSeed seed = getSeedBySpecies(species);
        if (seed == null || seed.getCount() <= 0) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_GARDEN_SEEDS + " SET " + COLUMN_SEED_COUNT + " = " + COLUMN_SEED_COUNT + " - 1 WHERE "
                + COLUMN_SEED_SPECIES + " = ? AND " + COLUMN_SEED_COUNT + " > 0", new String[]{species});
        return true;
    }

    public void unlockSeedsForGardenLevel(int gardenLevel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEED_IS_UNLOCKED, 1);
        db.update(TABLE_GARDEN_SEEDS, cv, COLUMN_SEED_REQ_LEVEL + " <= ?", new String[]{String.valueOf(gardenLevel)});
    }

    // --- Goals Methods ---

    public List<GardenGoal> getAllGoals() {
        List<GardenGoal> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_GOALS, null, null, null, null, null, COLUMN_GOAL_IS_WEEKLY + " ASC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(new GardenGoal(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_GOAL_ID)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_GOAL_TITLE)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_GOAL_TYPE)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_GOAL_TARGET)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_GOAL_PROGRESS)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_GOAL_IS_WEEKLY)) == 1,
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_GOAL_IS_COMPLETED)) == 1,
                            c.getString(c.getColumnIndexOrThrow(COLUMN_GOAL_REWARD_SEED)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_GOAL_REWARD_POINTS))
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void updateGoalProgress(String goalId, int progress, boolean completed) {
        if (goalId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GOAL_PROGRESS, progress);
        cv.put(COLUMN_GOAL_IS_COMPLETED, completed ? 1 : 0);
        db.update(TABLE_GARDEN_GOALS, cv, COLUMN_GOAL_ID + " = ?", new String[]{goalId});
    }

    // --- Achievements Methods ---

    public List<GardenAchievement> getAllAchievements() {
        List<GardenAchievement> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_ACHIEVEMENTS, null, null, null, null, null, COLUMN_ACH_IS_UNLOCKED + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(new GardenAchievement(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_ACH_ID)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_ACH_TITLE)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_ACH_DESC)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_ACH_ICON)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_ACH_IS_UNLOCKED)) == 1,
                            c.getLong(c.getColumnIndexOrThrow(COLUMN_ACH_UNLOCKED_AT)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_ACH_PROGRESS)),
                            c.getInt(c.getColumnIndexOrThrow(COLUMN_ACH_TARGET))
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public void unlockAchievement(String achievementId) {
        if (achievementId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ACH_IS_UNLOCKED, 1);
        cv.put(COLUMN_ACH_UNLOCKED_AT, System.currentTimeMillis());
        db.update(TABLE_GARDEN_ACHIEVEMENTS, cv, COLUMN_ACH_ID + " = ?", new String[]{achievementId});
    }

    // --- History Methods ---

    public void addGardenHistory(GardenHistoryItem item) {
        if (item == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_HIST_ID, item.getHistoryId());
        cv.put(COLUMN_HIST_TITLE, item.getTitle());
        cv.put(COLUMN_HIST_DESC, item.getDescription());
        cv.put(COLUMN_HIST_TIME, item.getTimestamp());
        cv.put(COLUMN_HIST_TYPE, item.getEventType());
        cv.put(COLUMN_HIST_SPECIES, item.getPlantSpecies());
        db.insertWithOnConflict(TABLE_GARDEN_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<GardenHistoryItem> getGardenHistory() {
        List<GardenHistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_HISTORY, null, null, null, null, null, COLUMN_HIST_TIME + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(new GardenHistoryItem(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_HIST_ID)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_HIST_TITLE)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_HIST_DESC)),
                            c.getLong(c.getColumnIndexOrThrow(COLUMN_HIST_TIME)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_HIST_TYPE)),
                            c.getString(c.getColumnIndexOrThrow(COLUMN_HIST_SPECIES))
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    // --- Session Deduplication Engine ---

    public boolean isSessionProcessed(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GARDEN_PROCESSED_SESSIONS, new String[]{COLUMN_PROC_SESSION_ID},
                COLUMN_PROC_SESSION_ID + " = ?", new String[]{sessionId}, null, null, null);
        boolean exists = false;
        if (c != null) {
            try {
                exists = c.moveToFirst();
            } finally {
                c.close();
            }
        }
        return exists;
    }

    public void markSessionProcessed(String sessionId, int minutes) {
        if (sessionId == null || sessionId.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PROC_SESSION_ID, sessionId);
        cv.put(COLUMN_PROC_TIME, System.currentTimeMillis());
        cv.put(COLUMN_PROC_MINUTES, minutes);
        db.insertWithOnConflict(TABLE_GARDEN_PROCESSED_SESSIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // --- Focus Journal Methods ---

    public void addJournalEntry(JournalEntry entry) {
        if (entry == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("entry_id", entry.getEntryId());
        cv.put("timestamp", entry.getTimestamp());
        cv.put("topic", entry.getTopicWorkedOn());
        cv.put("takeaway", entry.getKeyTakeaway());
        cv.put("rating", entry.getProductivityRating());
        cv.put("notes", entry.getNotes());
        db.insertWithOnConflict("journal_entries", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<JournalEntry> getAllJournalEntries() {
        List<JournalEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("journal_entries", null, null, null, null, null, "timestamp DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(new JournalEntry(
                            c.getString(c.getColumnIndexOrThrow("entry_id")),
                            c.getLong(c.getColumnIndexOrThrow("timestamp")),
                            c.getString(c.getColumnIndexOrThrow("topic")),
                            c.getString(c.getColumnIndexOrThrow("takeaway")),
                            c.getInt(c.getColumnIndexOrThrow("rating")),
                            c.getString(c.getColumnIndexOrThrow("notes"))
                    ));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    // =========================================================================
    // The Little Memory Library (Scrapbook, Milestones & Monthly Recaps)
    // =========================================================================

    private void createMemoryTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEMORIES + " ("
                + COLUMN_MEM_ID + " TEXT PRIMARY KEY, "
                + COLUMN_MEM_TITLE + " TEXT NOT NULL, "
                + COLUMN_MEM_DESC + " TEXT NOT NULL, "
                + COLUMN_MEM_CAT + " TEXT NOT NULL, "
                + COLUMN_MEM_TIME + " INTEGER NOT NULL, "
                + COLUMN_MEM_ICON + " TEXT, "
                + COLUMN_MEM_FAVORITE + " INTEGER DEFAULT 0, "
                + COLUMN_MEM_NOTE + " TEXT DEFAULT '', "
                + COLUMN_MEM_KEY + " TEXT UNIQUE, "
                + COLUMN_MEM_STATS + " INTEGER DEFAULT 0, "
                + COLUMN_MEM_MONTH + " TEXT"
                + ");");
    }

    public boolean isMemoryExists(String milestoneKey) {
        if (milestoneKey == null || milestoneKey.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEMORIES, new String[]{COLUMN_MEM_ID},
                COLUMN_MEM_KEY + " = ?", new String[]{milestoneKey}, null, null, null);
        boolean exists = false;
        if (cursor != null) {
            try {
                exists = cursor.getCount() > 0;
            } finally {
                cursor.close();
            }
        }
        return exists;
    }

    public boolean addMemory(MemoryItem memory) {
        if (memory == null) return false;
        if (isMemoryExists(memory.getMilestoneKey())) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEM_ID, memory.getId());
        values.put(COLUMN_MEM_TITLE, memory.getTitle());
        values.put(COLUMN_MEM_DESC, memory.getDescription());
        values.put(COLUMN_MEM_CAT, memory.getCategory());
        values.put(COLUMN_MEM_TIME, memory.getTimestamp());
        values.put(COLUMN_MEM_ICON, memory.getIconEmoji());
        values.put(COLUMN_MEM_FAVORITE, memory.isFavorite() ? 1 : 0);
        values.put(COLUMN_MEM_NOTE, memory.getPersonalNote());
        values.put(COLUMN_MEM_KEY, memory.getMilestoneKey());
        values.put(COLUMN_MEM_STATS, memory.getStatsValue());
        values.put(COLUMN_MEM_MONTH, memory.getMonthYear());
        long row = db.insertWithOnConflict(TABLE_MEMORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return row != -1;
    }

    public List<MemoryItem> getAllMemories() {
        List<MemoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_MEMORIES, null, null, null, null, null, COLUMN_MEM_TIME + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(cursorToMemory(c));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public List<MemoryItem> getFavoriteMemories() {
        List<MemoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_MEMORIES, null, COLUMN_MEM_FAVORITE + " = 1", null, null, null, COLUMN_MEM_TIME + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(cursorToMemory(c));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public List<MemoryItem> getMemoriesByCategory(String category) {
        List<MemoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_MEMORIES, null, COLUMN_MEM_CAT + " = ?", new String[]{category}, null, null, COLUMN_MEM_TIME + " DESC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    list.add(cursorToMemory(c));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    public boolean updateMemoryPersonalNote(String memoryId, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MEM_NOTE, note != null ? note : "");
        int affected = db.update(TABLE_MEMORIES, cv, COLUMN_MEM_ID + " = ?", new String[]{memoryId});
        return affected > 0;
    }

    public boolean toggleMemoryFavorite(String memoryId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MEM_FAVORITE, isFavorite ? 1 : 0);
        int affected = db.update(TABLE_MEMORIES, cv, COLUMN_MEM_ID + " = ?", new String[]{memoryId});
        return affected > 0;
    }

    public boolean deleteMemory(String memoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affected = db.delete(TABLE_MEMORIES, COLUMN_MEM_ID + " = ?", new String[]{memoryId});
        return affected > 0;
    }

    private MemoryItem cursorToMemory(Cursor c) {
        return new MemoryItem(
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_TITLE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_DESC)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_CAT)),
                c.getLong(c.getColumnIndexOrThrow(COLUMN_MEM_TIME)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_ICON)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_MEM_FAVORITE)) == 1,
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_NOTE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_MEM_KEY)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_MEM_STATS))
        );
    }

    public int getMonthFocusMinutes(long startMillis, long endMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_PROC_MINUTES + ") FROM " + TABLE_GARDEN_PROCESSED_SESSIONS
                + " WHERE " + COLUMN_PROC_TIME + " BETWEEN ? AND ?", new String[]{String.valueOf(startMillis), String.valueOf(endMillis)});
        int total = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    total = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
        return total;
    }

    public int getMonthCompletedTasksCount(long startMillis, long endMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS
                + " WHERE " + COLUMN_TASK_COMPLETED + " = 1 AND " + COLUMN_TASK_COMPLETED_AT + " BETWEEN ? AND ?",
                new String[]{String.valueOf(startMillis), String.valueOf(endMillis)});
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    count = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int getMonthBloomedFlowersCount(long startMillis, long endMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GARDEN_PLANTS
                + " WHERE " + COLUMN_PLANT_BLOOMED_AT + " BETWEEN ? AND ?",
                new String[]{String.valueOf(startMillis), String.valueOf(endMillis)});
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    count = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int getMonthFocusMinutes(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long end = c.getTimeInMillis();
        return getMonthFocusMinutes(start, end);
    }

    public int getMonthCompletedTasksCount(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long end = c.getTimeInMillis();
        return getMonthCompletedTasksCount(start, end);
    }

    public int getMonthBloomedFlowersCount(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long end = c.getTimeInMillis();
        return getMonthBloomedFlowersCount(start, end);
    }
}
