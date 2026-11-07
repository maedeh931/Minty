package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * MainActivity serves as the Home screen for Minty.
 * Displays the current minty minutes balance and navigation buttons.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvBalance;
    private Button btnAddTask;
    private Button btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");
        setContentView(R.layout.activity_main);

        // Initialize UI components using findViewById
        tvBalance = findViewById(R.id.tvBalance);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnHistory = findViewById(R.id.btnHistory);

        // Explicit Intent to navigate from Home to AddTaskActivity
        btnAddTask.setOnClickListener(v -> {
            Log.d(TAG, "btnAddTask clicked: navigating to AddTaskActivity");
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // Explicit Intent to navigate from Home to HistoryActivity
        btnHistory.setOnClickListener(v -> {
            Log.d(TAG, "btnHistory clicked: navigating to HistoryActivity");
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
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
