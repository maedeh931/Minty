package com.minty.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minty.app.databinding.ActivityFocusJournalBinding;

import java.util.List;
import java.util.UUID;

/**
 * Focus Journal and Reflection Activity.
 * Provides private, post-session learning reflections to reinforce study habits.
 */
public class FocusJournalActivity extends AppCompatActivity {

    private ActivityFocusJournalBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFocusJournalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbarJournal.setNavigationOnClickListener(v -> finish());
        binding.rvJournalEntries.setLayoutManager(new LinearLayoutManager(this));

        binding.btnSaveEntry.setOnClickListener(v -> saveJournalEntry());

        loadJournalEntries();
    }

    private void saveJournalEntry() {
        String topic = binding.etTopic.getText().toString().trim();
        String takeaway = binding.etTakeaway.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();

        if (topic.isEmpty()) {
            binding.etTopic.setError("Please describe what you focused on");
            return;
        }

        if (takeaway.isEmpty()) {
            binding.etTakeaway.setError("Please enter a key takeaway or accomplishment");
            return;
        }

        JournalEntry entry = new JournalEntry(
                "journal_" + UUID.randomUUID().toString().substring(0, 8),
                System.currentTimeMillis(),
                topic,
                takeaway,
                5,
                notes
        );

        dbHelper.addJournalEntry(entry);

        binding.etTopic.setText("");
        binding.etTakeaway.setText("");
        binding.etNotes.setText("");

        Toast.makeText(this, "Reflection saved! ✨", Toast.LENGTH_SHORT).show();
        loadJournalEntries();
    }

    private void loadJournalEntries() {
        List<JournalEntry> list = dbHelper.getAllJournalEntries();
        if (list.isEmpty()) {
            binding.rvJournalEntries.setVisibility(View.GONE);
            binding.tvEmptyJournal.setVisibility(View.VISIBLE);
        } else {
            binding.rvJournalEntries.setVisibility(View.VISIBLE);
            binding.tvEmptyJournal.setVisibility(View.GONE);
            JournalAdapter adapter = new JournalAdapter(list);
            binding.rvJournalEntries.setAdapter(adapter);
        }
    }
}
