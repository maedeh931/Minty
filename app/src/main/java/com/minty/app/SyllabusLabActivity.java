package com.minty.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ActivitySyllabusLabBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Android Syllabus Interactive Lab & Diagnostic Activity.
 * Formally demonstrates and tests:
 * 1. Activity Lifecycle: Full demonstration and on-screen logging of all 7 lifecycle callbacks.
 * 2. Button Events: Programmatic setOnClickListener vs XML android:onClick attribute.
 * 3. Services: Bound Service (bindService, unbindService, LocalBinder) and Background Service.
 * 4. UI Layouts: TableLayout, WebView, RelativeLayout, LinearLayout, ConstraintLayout.
 * 5. Adapters: GridView, SimpleAdapter, ArrayAdapter, BaseAdapter, RecyclerView Adapter + ViewHolder.
 * 6. Dynamic BroadcastReceiver: IntentFilter, dynamic registration in onResume, unregistration in onStop.
 */
public class SyllabusLabActivity extends AppCompatActivity {

    public static final String ACTION_DYNAMIC_PING = "com.minty.app.ACTION_DYNAMIC_PING";
    private static final String TAG = "SyllabusLabActivity";

    private ActivitySyllabusLabBinding binding;
    private final StringBuilder lifecycleLog = new StringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    // Bound Service state
    private CountdownService.CountdownBinder countdownBinder;
    private boolean isServiceBound = false;

    // ServiceConnection for Bound Service demonstration
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            countdownBinder = (CountdownService.CountdownBinder) service;
            isServiceBound = true;
            updateBoundServiceUi();
            Toast.makeText(SyllabusLabActivity.this, "Bound to CountdownService successfully!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceConnected: Connected to CountdownService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            countdownBinder = null;
            isServiceBound = false;
            updateBoundServiceUi();
            Toast.makeText(SyllabusLabActivity.this, "Disconnected from CountdownService", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceDisconnected: Disconnected from CountdownService");
        }
    };

    // Dynamic BroadcastReceiver demonstration
    private final BroadcastReceiver dynamicPingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DYNAMIC_PING.equals(intent.getAction())) {
                String payload = intent.getStringExtra("ping_payload");
                binding.tvBroadcastStatus.setText("Broadcast Received at " + timeFormat.format(new Date()) + "\nPayload: " + payload);
                Toast.makeText(context, "Dynamic Receiver captured: " + payload, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "dynamicPingReceiver: received " + payload);
            } else if (BotanicalMaintenanceService.ACTION_MAINTENANCE_STATUS.equals(intent.getAction())) {
                String status = intent.getStringExtra(BotanicalMaintenanceService.EXTRA_STATUS_MESSAGE);
                binding.tvServiceStatus.setText("Background Service Finished:\n" + status);
                Toast.makeText(context, "Background Service Completed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "dynamicPingReceiver: Maintenance finished: " + status);
            }
        }
    };

    // ==========================================
    // 1. ALL 7 ACTIVITY LIFECYCLE METHODS
    // ==========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySyllabusLabBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        recordLifecycle("onCreate()");

        // Action bar navigation back
        binding.toolbarSyllabusLab.setNavigationOnClickListener(v -> finish());

        // Setup UI Sections
        setupButtonClicks();
        setupBoundAndBackgroundServices();
        setupGridViewSimpleAdapter();
        setupListViewAndRecyclerViewComparison();
        setupWebView();
        setupDynamicBroadcastButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        recordLifecycle("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        recordLifecycle("onResume()");

        // Register dynamic BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DYNAMIC_PING);
        filter.addAction(BotanicalMaintenanceService.ACTION_MAINTENANCE_STATUS);
        registerReceiver(dynamicPingReceiver, filter);
        Log.d(TAG, "onResume: dynamic receiver registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordLifecycle("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        recordLifecycle("onStop()");

        // Unregister dynamic BroadcastReceiver
        try {
            unregisterReceiver(dynamicPingReceiver);
            Log.d(TAG, "onStop: dynamic receiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver already unregistered", e);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recordLifecycle("onRestart()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordLifecycle("onDestroy()");

        // Unbind service if bound to prevent ServiceConnection leak
        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
                isServiceBound = false;
                Log.d(TAG, "onDestroy: safely unbound from CountdownService");
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding in onDestroy", e);
            }
        }
    }

    private void recordLifecycle(String method) {
        String entry = "[" + timeFormat.format(new Date()) + "] " + method + "\n";
        lifecycleLog.append(entry);
        Log.d(TAG, "Lifecycle Callback: " + method);
        if (binding != null) {
            binding.tvLifecycleLog.setText(lifecycleLog.toString().trim());
        }
    }

    // ==========================================
    // 2. BUTTON CLICKS & XML ONCLICK DEMO
    // ==========================================

    private void setupButtonClicks() {
        binding.btnProgrammaticClick.setOnClickListener(v -> {
            String msg = "Programmatic setOnClickListener() triggered at " + timeFormat.format(new Date());
            binding.tvClickResult.setText(msg);
            Toast.makeText(this, "Programmatic Click Event Handled!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Demonstrated XML onClick attribute requirement:
     * Declared as android:onClick="onSyllabusXmlButtonClick" in activity_syllabus_lab.xml.
     */
    public void onSyllabusXmlButtonClick(View view) {
        String msg = "XML android:onClick attribute triggered at " + timeFormat.format(new Date());
        binding.tvClickResult.setText(msg);
        Toast.makeText(this, "XML android:onClick Handled Successfully!", Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // 3. BOUND & BACKGROUND SERVICES
    // ==========================================

    private void setupBoundAndBackgroundServices() {
        binding.btnBindService.setOnClickListener(v -> {
            if (!isServiceBound) {
                Intent bindIntent = new Intent(this, CountdownService.class);
                bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Toast.makeText(this, "Already bound to service", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnUnbindService.setOnClickListener(v -> {
            if (isServiceBound) {
                unbindService(serviceConnection);
                isServiceBound = false;
                updateBoundServiceUi();
                Toast.makeText(this, "Unbound from service", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Service is not currently bound", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnStartBgService.setOnClickListener(v -> {
            Intent bgIntent = new Intent(this, BotanicalMaintenanceService.class);
            startService(bgIntent);
            binding.tvServiceStatus.setText("Background Service Started: Running diagnostic checks...");
            Toast.makeText(this, "Started BotanicalMaintenanceService in background", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateBoundServiceUi() {
        if (isServiceBound && countdownBinder != null) {
            boolean running = countdownBinder.isTimerRunning();
            long secs = countdownBinder.getRemainingSeconds();
            String session = countdownBinder.getSessionType();
            binding.tvServiceStatus.setText("Bound Status: CONNECTED 🟢\n"
                    + "Service Running: " + (running ? "ACTIVE" : "IDLE") + "\n"
                    + "Session: " + session + " | Remaining: " + secs + "s");
        } else {
            binding.tvServiceStatus.setText("Bound Status: DISCONNECTED 🔴\nTap bindService() to connect.");
        }
    }

    // ==========================================
    // 4. GRIDVIEW & SIMPLEADAPTER DEMO
    // ==========================================

    private void setupGridViewSimpleAdapter() {
        // Prepare data for SimpleAdapter (List of Maps)
        List<Map<String, Object>> badgeList = new ArrayList<>();

        String[] badgeNames = {"Mint 🌱", "Cherry 🌸", "Daisy 🌼", "Lavender 🪻", "Sunflower 🌻", "Rose 🌹"};
        int[] badgeIcons = {
                R.drawable.mascot_minty,
                R.drawable.ic_flower_cherry,
                R.drawable.ic_flower_daisy,
                R.drawable.ic_flower_lavender,
                R.drawable.ic_flower_sunflower,
                R.drawable.ic_flower_rose
        };

        for (int i = 0; i < badgeNames.length; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", badgeNames[i]);
            map.put("icon", badgeIcons[i]);
            badgeList.add(map);
        }

        String[] from = {"name", "icon"};
        int[] to = {R.id.tvBadgeName, R.id.ivBadgeIcon};

        SimpleAdapter simpleAdapter = new SimpleAdapter(
                this,
                badgeList,
                R.layout.item_grid_badge,
                from,
                to
        );

        binding.gvBotanicalBadges.setAdapter(simpleAdapter);

        binding.gvBotanicalBadges.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> item = badgeList.get(position);
            Toast.makeText(this, "Badge: " + item.get("name"), Toast.LENGTH_SHORT).show();
        });
    }

    // ==========================================
    // 5. LISTVIEW VS RECYCLERVIEW COMPARISON
    // ==========================================

    private void setupListViewAndRecyclerViewComparison() {
        List<String> topics = new ArrayList<>();
        topics.add("1. Android Activity Lifecycle & State Preservation");
        topics.add("2. Explicit & Implicit Intents with Bundle Extras");
        topics.add("3. Bound Services (LocalBinder, ServiceConnection)");
        topics.add("4. Foreground Services & Ongoing Notifications");
        topics.add("5. Static (Manifest) & Dynamic Broadcast Receivers");
        topics.add("6. Custom ContentProvider with SQLite Backend");
        topics.add("7. Cloud Firestore & Realtime Database Synchronization");
        topics.add("8. Volley HTTP Networking & REST JSON Handling");

        // 1. ListView setup with ArrayAdapter
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                topics
        );
        binding.lvDemoList.setAdapter(arrayAdapter);

        // 2. RecyclerView setup with RecyclerView.Adapter & ViewHolder
        binding.rvDemoRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDemoRecycler.setAdapter(new SimpleRecyclerDemoAdapter(topics));

        // Selector radio group
        binding.rgListSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbShowListView) {
                binding.lvDemoList.setVisibility(View.VISIBLE);
                binding.rvDemoRecycler.setVisibility(View.GONE);
                binding.tvListComparisonNotes.setText("ListView Mode: AdapterView consuming ArrayAdapter. Recycles views via convertView parameter.");
            } else {
                binding.lvDemoList.setVisibility(View.GONE);
                binding.rvDemoRecycler.setVisibility(View.VISIBLE);
                binding.tvListComparisonNotes.setText("RecyclerView Mode: Flexible LayoutManager + mandatory ViewHolder pattern with item animation support.");
            }
        });
    }

    // ==========================================
    // 6. WEBVIEW DEMO
    // ==========================================

    private void setupWebView() {
        WebView webView = binding.wvBotanicalGuide;
        String htmlContent = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>"
                + "body { font-family: -apple-system, sans-serif; background-color: #FFF8EC; color: #39443B; margin: 0; padding: 12px; }"
                + "h3 { color: #5B8C51; margin-top: 0; font-size: 15px; }"
                + "p { font-size: 12px; line-height: 1.5; color: #5F6E5E; }"
                + ".card { background: #FFFFFF; border: 1px solid #D2E3C8; border-radius: 8px; padding: 10px; margin-top: 6px; }"
                + "code { background: #EAF2E7; padding: 2px 4px; border-radius: 4px; color: #2E5325; font-size: 11px; }"
                + "</style></head><body>"
                + "<h3>🌱 Minty Android Lab &amp; Syllabus Reference</h3>"
                + "<p>This <code>WebView</code> demonstrates embedded web rendering inside native Android application UI.</p>"
                + "<div class='card'><b>Architectural Stack:</b> 100% Java, XML ViewBinding, SQLite <code>minty.db</code>, Firebase Realtime Database &amp; Cloud Firestore.</div>"
                + "</body></html>";

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    // ==========================================
    // 7. DYNAMIC BROADCAST SENDER
    // ==========================================

    private void setupDynamicBroadcastButton() {
        binding.btnSendCustomBroadcast.setOnClickListener(v -> {
            Intent pingIntent = new Intent(ACTION_DYNAMIC_PING);
            pingIntent.putExtra("ping_payload", "Syllabus Ping at " + timeFormat.format(new Date()));
            sendBroadcast(pingIntent);
        });
    }

    // ==========================================
    // HELPER: RECYCLER VIEW ADAPTER & VIEWHOLDER
    // ==========================================

    private static class SimpleRecyclerDemoAdapter extends RecyclerView.Adapter<SimpleRecyclerDemoAdapter.ViewHolder> {
        private final List<String> items;

        SimpleRecyclerDemoAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(items.get(position));
            holder.textView.setTextSize(12f);
            holder.textView.setTextColor(0xFF39443B);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
