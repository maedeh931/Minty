package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

/**
 * AddTaskActivity allows users to enter a task title and minty minutes reward.
 * Currently finishes the activity upon clicking Save (database integration pending).
 */
public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText etTaskTitle;
    private EditText etMinutes;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");
        setContentView(R.layout.activity_add_task);

        // Bind layout views using findViewById
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etMinutes = findViewById(R.id.etMinutes);
        btnSave = findViewById(R.id.btnSave);

        // Save button finishes the activity for now (no database yet)
        btnSave.setOnClickListener(v -> {
            String title = etTaskTitle.getText().toString().trim();
            String minutesStr = etMinutes.getText().toString().trim();
            Log.d(TAG, "btnSave clicked - Title: " + title + ", Minutes: " + minutesStr + " -> finishing activity");
            finish();
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
