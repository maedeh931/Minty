package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DailyQuestManager manages generation, evaluation, progress tracking, and reward claiming
 * for Minty's rotating daily quests.
 */
public class DailyQuestManager {

    private static final String TAG = "DailyQuestManager";
    private static final String PREF_NAME = "minty_daily_quests_prefs";
    private static final String KEY_QUESTS_JSON = "saved_quests_json";
    private static final String KEY_LAST_DATE = "last_quest_date";

    public static final String TYPE_FOCUS = "quest_focus";
    public static final String TYPE_TASKS = "quest_tasks";
    public static final String TYPE_GARDEN = "quest_garden";

    private static DailyQuestManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DailyQuestManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized DailyQuestManager getInstance(Context context) {
        if (instance == null) {
            instance = new DailyQuestManager(context);
        }
        return instance;
    }

    /**
     * Retrieves today's 3 daily quests. Automatically refreshes at local midnight.
     */
    public synchronized List<DailyQuest> getTodayQuests() {
        String todayStr = dateFormat.format(new Date());
        String savedDate = prefs.getString(KEY_LAST_DATE, "");

        if (!todayStr.equals(savedDate)) {
            Log.d(TAG, "New day detected (" + todayStr + "). Generating 3 fresh daily quests.");
            List<DailyQuest> freshQuests = generateQuestsForDate(todayStr);
            saveQuests(freshQuests, todayStr);
            return freshQuests;
        }

        String jsonStr = prefs.getString(KEY_QUESTS_JSON, null);
        if (jsonStr != null) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                List<DailyQuest> quests = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    quests.add(DailyQuest.fromJson(arr.getJSONObject(i)));
                }
                return quests;
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing saved daily quests", e);
            }
        }

        List<DailyQuest> fresh = generateQuestsForDate(todayStr);
        saveQuests(fresh, todayStr);
        return fresh;
    }

    private List<DailyQuest> generateQuestsForDate(String dateKey) {
        List<DailyQuest> list = new ArrayList<>();
        list.add(new DailyQuest(
                TYPE_FOCUS,
                "Deep Work Sprout",
                "Complete at least 1 focus session today",
                "⏳",
                0,
                1,
                15,
                false,
                dateKey
        ));

        list.add(new DailyQuest(
                TYPE_TASKS,
                "Task Bloom",
                "Complete 2 study tasks from your planner",
                "✅",
                0,
                2,
                10,
                false,
                dateKey
        ));

        list.add(new DailyQuest(
                TYPE_GARDEN,
                "Garden Hydration",
                "Water a plant in your botanical garden",
                "💧",
                0,
                1,
                10,
                false,
                dateKey
        ));
        return list;
    }

    private void saveQuests(List<DailyQuest> quests, String dateKey) {
        try {
            JSONArray arr = new JSONArray();
            for (DailyQuest q : quests) {
                arr.put(q.toJson());
            }
            prefs.edit()
                    .putString(KEY_QUESTS_JSON, arr.toString())
                    .putString(KEY_LAST_DATE, dateKey)
                    .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing quests", e);
        }
    }

    /**
     * Increments progress for a specific quest type.
     */
    public synchronized void recordProgress(String questType, int amount) {
        List<DailyQuest> quests = getTodayQuests();
        boolean changed = false;

        for (DailyQuest q : quests) {
            if (q.getId().equals(questType) && !q.isClaimed()) {
                int newProg = Math.min(q.getTargetCount(), q.getCurrentProgress() + amount);
                if (newProg != q.getCurrentProgress()) {
                    q.setCurrentProgress(newProg);
                    changed = true;
                    Log.d(TAG, "Quest " + q.getTitle() + " progress updated to " + newProg + "/" + q.getTargetCount());
                }
            }
        }

        if (changed) {
            String todayStr = dateFormat.format(new Date());
            saveQuests(quests, todayStr);
        }
    }

    /**
     * Claims the reward for a completed quest.
     * @return Reward message if successfully claimed, or null if cannot claim.
     */
    public synchronized String claimReward(String questId) {
        List<DailyQuest> quests = getTodayQuests();
        for (DailyQuest q : quests) {
            if (q.getId().equals(questId)) {
                if (!q.isCompleted()) {
                    return null; // Not ready yet
                }
                if (q.isClaimed()) {
                    return null; // Already claimed
                }

                q.setClaimed(true);
                String todayStr = dateFormat.format(new Date());
                saveQuests(quests, todayStr);

                // Add reward minutes to SQLite and sync to cloud
                DatabaseHelper db = new DatabaseHelper(appContext);
                int oldBal = db.getBalance();
                int newBal = oldBal + q.getRewardMinutes();
                db.updateBalanceDirect(newBal);

                RealtimeDbManager.getInstance().syncBalanceToCloud(newBal);

                Log.d(TAG, "Claimed reward for " + q.getTitle() + ": +" + q.getRewardMinutes() + " minutes. New balance: " + newBal);
                return "Claimed +" + q.getRewardMinutes() + " Minty Minutes! 🎉";
            }
        }
        return null;
    }
}
