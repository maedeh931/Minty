package com.minty.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.minty.app.databinding.ActivityFeedbackBinding;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * FeedbackActivity demonstrates HTTP POST, PUT, and DELETE operations using Volley JsonObjectRequest
 * against https://jsonplaceholder.typicode.com.
 */
public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";
    private static final String TAG_FEEDBACK = "feedback_request";

    private static final String URL_POSTS = "https://jsonplaceholder.typicode.com/posts";
    private static final String URL_POST_1 = "https://jsonplaceholder.typicode.com/posts/1";

    private ActivityFeedbackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        // Inflate layout using View Binding
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Up (back) navigation arrow in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Send button (POST /posts)
        binding.btnSend.setOnClickListener(v -> {
            Log.d(TAG, "btnSend clicked: executing POST");
            sendFeedbackPost();
        });

        // Update button (PUT /posts/1)
        binding.btnUpdate.setOnClickListener(v -> {
            Log.d(TAG, "btnUpdate clicked: executing PUT");
            updateFeedbackPut();
        });

        // Delete button (DELETE /posts/1)
        binding.btnDelete.setOnClickListener(v -> {
            Log.d(TAG, "btnDelete clicked: executing DELETE");
            deleteFeedbackDelete();
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
        Log.d(TAG, "onResume: activity is interacting with the user");
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
        // Cancel ongoing feedback requests
        ApiClient.getInstance(this).cancelPendingRequests(TAG_FEEDBACK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Executes HTTP POST /posts with title, message (body), and userId: 1.
     */
    private void sendFeedbackPost() {
        String title = getTitleInput();
        String message = getMessageInput();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a feedback title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please enter a feedback message", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            requestBody.put("body", message);
            requestBody.put("userId", 1);
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON body for POST", e);
            return;
        }

        setLoading(true);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL_POSTS,
                requestBody,
                response -> {
                    setLoading(false);
                    int returnedId = response.optInt("id", -1);
                    Toast.makeText(FeedbackActivity.this, "Created post ID: " + returnedId, Toast.LENGTH_SHORT).show();

                    String prettyJson;
                    try {
                        prettyJson = response.toString(2);
                    } catch (JSONException e) {
                        prettyJson = response.toString();
                    }

                    showResult("POST /posts", "Status: Success (Created ID: " + returnedId + ")", prettyJson);
                    Log.d(TAG, "POST /posts success: id=" + returnedId);
                },
                error -> handleError("POST /posts", error)
        );

        request.setRetryPolicy(new DefaultRetryPolicy(8000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApiClient.getInstance(this).addToRequestQueue(request, TAG_FEEDBACK);
    }

    /**
     * Executes HTTP PUT /posts/1 with updated title and message.
     */
    private void updateFeedbackPut() {
        String title = getTitleInput();
        String message = getMessageInput();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a feedback title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please enter a feedback message", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            requestBody.put("body", message);
            requestBody.put("userId", 1);
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON body for PUT", e);
            return;
        }

        setLoading(true);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                URL_POST_1,
                requestBody,
                response -> {
                    setLoading(false);
                    Toast.makeText(FeedbackActivity.this, "Post updated successfully", Toast.LENGTH_SHORT).show();

                    String prettyJson;
                    try {
                        prettyJson = response.toString(2);
                    } catch (JSONException e) {
                        prettyJson = response.toString();
                    }

                    showResult("PUT /posts/1", "Status: Success (Updated post #1)", prettyJson);
                    Log.d(TAG, "PUT /posts/1 success");
                },
                error -> handleError("PUT /posts/1", error)
        );

        request.setRetryPolicy(new DefaultRetryPolicy(8000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApiClient.getInstance(this).addToRequestQueue(request, TAG_FEEDBACK);
    }

    /**
     * Executes HTTP DELETE /posts/1.
     */
    private void deleteFeedbackDelete() {
        setLoading(true);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                URL_POST_1,
                null,
                response -> {
                    setLoading(false);
                    Toast.makeText(FeedbackActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();

                    String prettyJson;
                    try {
                        prettyJson = response != null ? response.toString(2) : "{}";
                    } catch (JSONException e) {
                        prettyJson = "{}";
                    }

                    showResult("DELETE /posts/1", "Status: Success (Deleted post #1)", prettyJson);
                    Log.d(TAG, "DELETE /posts/1 success");
                },
                error -> handleError("DELETE /posts/1", error)
        );

        request.setRetryPolicy(new DefaultRetryPolicy(8000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApiClient.getInstance(this).addToRequestQueue(request, TAG_FEEDBACK);
    }

    /**
     * Handles Volley errors with informative messages.
     */
    private void handleError(String method, VolleyError error) {
        setLoading(false);
        String errorMessage;
        if (error instanceof NoConnectionError) {
            errorMessage = "No internet connection. Please check your network.";
        } else if (error instanceof TimeoutError) {
            errorMessage = "Connection timed out. Server took too long to respond.";
        } else if (error instanceof ServerError) {
            errorMessage = "Server error occurred. Please try again later.";
        } else if (error instanceof NetworkError) {
            errorMessage = "Network error. Please try again.";
        } else if (error.getMessage() != null) {
            errorMessage = "Error: " + error.getMessage();
        } else {
            errorMessage = "Request failed.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        showResult(method, "Status: Error (" + errorMessage + ")", error.toString());
        Log.e(TAG, "Volley error on " + method, error);
    }

    /**
     * Updates the result card with HTTP method, status header, and JSON body.
     */
    private void showResult(String method, String status, String jsonContent) {
        binding.tvResultHeader.setText("HTTP Method: " + method);
        binding.tvResultStatus.setText(status);
        binding.tvResultJson.setText(jsonContent);
    }

    /**
     * Toggles the loading ProgressBar and enables/disables action buttons.
     */
    private void setLoading(boolean loading) {
        binding.pbFeedbackLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSend.setEnabled(!loading);
        binding.btnUpdate.setEnabled(!loading);
        binding.btnDelete.setEnabled(!loading);
    }

    private String getTitleInput() {
        return binding.etFeedbackTitle.getText() != null ? binding.etFeedbackTitle.getText().toString().trim() : "";
    }

    private String getMessageInput() {
        return binding.etFeedbackMessage.getText() != null ? binding.etFeedbackMessage.getText().toString().trim() : "";
    }
}
