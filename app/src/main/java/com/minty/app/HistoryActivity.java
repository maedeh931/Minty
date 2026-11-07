package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * HistoryActivity displays the list of completed tasks.
 * Currently displays placeholder text indicating no tasks have been completed.
 */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private TextView tvHistoryTitle;
    private TextView tvPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");
        setContentView(R.layout.activity_history);

        // Bind layout views using findViewById
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }
}
