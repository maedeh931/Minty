package com.minty.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.minty.app.databinding.ActivityProfileBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Clean student-facing Profile & Settings activity.
 * Developer diagnostics have been cleanly isolated to DevToolsActivity,
 * accessible by tapping the app version 7 times.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private static final String PREFS_NAME = "minty_profile_prefs";
    private static final String KEY_BUDDY_NAME = "buddy_name";
    private static final String KEY_BUDDY_PHONE = "buddy_phone";
    private static final String KEY_DEV_MODE = "dev_mode_unlocked";
    private static final String KEY_REMINDERS = "reminders_enabled";
    private static final String KEY_HAPTICS = "haptics_enabled";
    private static final String KEY_SOUND = "sound_enabled";

    private ActivityProfileBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private int devTapCount = 0;

    // Contact picker launcher (Contract: PickContact)
    private final ActivityResultLauncher<Void> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.PickContact(), uri -> {
                if (uri != null) {
                    handleContactPicked(uri);
                }
            });

    // Permission launcher for READ_CONTACTS
    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchContactPicker();
                } else {
                    Toast.makeText(this, "Contacts permission is required to choose a buddy", Toast.LENGTH_SHORT).show();
                }
            });

    // Photo picker launcher (Circular avatar)
    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String savedPath = ProfileImageHelper.saveProfilePhoto(this, uri);
                    if (savedPath != null) {
                        ProfileImageHelper.loadProfilePhotoCircular(this, binding.ivProfilePhoto);
                        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update profile photo", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Toolbar navigation back
        binding.toolbarProfile.setNavigationOnClickListener(v -> finish());

        // 1. User details from Firebase Auth
        loadUserDetails();
        binding.btnEditProfileName.setOnClickListener(v -> showEditNameDialog());
        binding.tvProfileName.setOnClickListener(v -> showEditNameDialog());

        // 2. Profile photo circular loader
        ProfileImageHelper.loadProfilePhotoCircular(this, binding.ivProfilePhoto);
        binding.btnChoosePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        // 3. Productivity statistics from SQLite
        loadProductivityStats();

        // 4. Contact buddy picker & SMS sharing
        loadBuddyFromPreferences();
        binding.btnChooseBuddy.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                launchContactPicker();
            } else {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
            }
        });
        binding.btnSendProgress.setOnClickListener(v -> sendProgressSms());

        // 5. Two-Way Buddy Hub navigation
        binding.btnOpenBuddyNetwork.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, BuddyActivity.class);
            startActivity(intent);
        });

        // 5b. The Little Memory Library navigation
        binding.cardOpenMemoryLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MemoryLibraryActivity.class);
            startActivity(intent);
        });

        // 6. Settings Switches
        binding.switchDailyReminder.setChecked(prefs.getBoolean(KEY_REMINDERS, true));
        binding.switchDailyReminder.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_REMINDERS, isChecked).apply();
            Toast.makeText(this, isChecked ? "Daily reminders enabled" : "Daily reminders disabled", Toast.LENGTH_SHORT).show();
        });

        binding.switchHaptics.setChecked(prefs.getBoolean(KEY_HAPTICS, true));
        binding.switchHaptics.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_HAPTICS, isChecked).apply();
        });

        binding.switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        binding.switchSound.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();
        });

        // 7. Developer Mode Unlock (Tap version 7 times)
        boolean devModeUnlocked = prefs.getBoolean(KEY_DEV_MODE, false);
        binding.btnOpenDevTools.setVisibility(devModeUnlocked ? View.VISIBLE : View.GONE);
        binding.btnOpenDevTools.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, DevToolsActivity.class);
            startActivity(intent);
        });

        binding.tvAppVersion.setOnClickListener(v -> {
            if (prefs.getBoolean(KEY_DEV_MODE, false)) {
                binding.btnOpenDevTools.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Developer mode is already active 🛠️", Toast.LENGTH_SHORT).show();
                return;
            }
            devTapCount++;
            if (devTapCount >= 7) {
                prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
                binding.btnOpenDevTools.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Developer mode unlocked! 🛠️", Toast.LENGTH_LONG).show();
            } else if (devTapCount >= 3) {
                int remaining = 7 - devTapCount;
                Toast.makeText(this, "Tap " + remaining + " more time" + (remaining == 1 ? "" : "s") + " for developer tools", Toast.LENGTH_SHORT).show();
            }
        });

        // 8. Sign Out
        binding.btnLogout.setOnClickListener(v -> performLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductivityStats();
    }

    private void loadUserDetails() {
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
            name = "Focus Gardener";
        }
        binding.tvProfileName.setText(name);

        if (user != null) {
            String email = user.getEmail();
            long creationTimestamp = user.getMetadata() != null ? user.getMetadata().getCreationTimestamp() : System.currentTimeMillis();
            binding.tvProfileEmail.setText(!TextUtils.isEmpty(email) ? email : "student@minty.app");

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            binding.tvJoinDate.setText("Member since " + sdf.format(new Date(creationTimestamp)));
        } else {
            binding.tvProfileEmail.setText("Offline mode");
            binding.tvJoinDate.setText("Guest user");
        }
    }

    private void showEditNameDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(binding.tvProfileName.getText());
        input.setSelection(input.getText().length());
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(paddingPx, (int)(8 * getResources().getDisplayMetrics().density), paddingPx, 0);
        container.addView(input);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Edit Display Name")
                .setMessage("Enter the name you'd like Minty to call you:")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName)) {
                        getSharedPreferences("minty_user_profile", MODE_PRIVATE)
                                .edit()
                                .putString("key_display_name", newName)
                                .apply();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                                    new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName(newName)
                                            .build();
                            user.updateProfile(profileUpdates);
                            FirestoreManager.getInstance().saveUserProfile(newName, user.getEmail());
                        } else {
                            FirestoreManager.getInstance().saveUserProfile(newName, "");
                        }
                        binding.tvProfileName.setText(newName);
                        Toast.makeText(this, "Profile name updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProductivityStats() {
        int balance = dbHelper.getBalance();
        int totalEarned = dbHelper.getTotalMinutesEarned();
        int tasksDone = dbHelper.getCompletedTasks().size();
        int streak = StreakManager.getDisplayStreak(this);

        binding.tvStatTotalFocus.setText(totalEarned + " min");
        binding.tvStatTasksDone.setText(String.valueOf(tasksDone));
        binding.tvStatBalance.setText(String.valueOf(balance));
        binding.tvStreakBadge.setText("🔥 " + streak + (streak == 1 ? " Day" : " Days"));

        // Garden Level Badge calculation
        int level = Math.max(1, Math.min(5, totalEarned / 120 + 1));
        String levelName;
        switch (level) {
            case 1: levelName = "🌱 Little Seed"; break;
            case 2: levelName = "🌿 Tiny Sprout"; break;
            case 3: levelName = "🪴 Growing Garden"; break;
            case 4: levelName = "🌸 Blooming Garden"; break;
            case 5:
            default: levelName = "🌳 Flourishing Garden"; break;
        }
        binding.tvGardenLevelBadge.setText(levelName);
    }

    private void launchContactPicker() {
        try {
            contactPickerLauncher.launch(null);
        } catch (Exception e) {
            Log.e(TAG, "Error launching contact picker", e);
            Toast.makeText(this, "Could not open contacts", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleContactPicked(Uri contactUri) {
        String contactName = null;
        String contactId = null;
        String contactPhone = null;

        try (Cursor cursor = getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                if (idIndex != -1) contactId = cursor.getString(idIndex);
                if (nameIndex != -1) contactName = cursor.getString(nameIndex);
                boolean hasPhone = hasPhoneIndex != -1 && cursor.getInt(hasPhoneIndex) > 0;

                if (hasPhone && contactId != null) {
                    try (Cursor phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId}, null)) {

                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                            int phoneIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            if (phoneIdx != -1) contactPhone = phoneCursor.getString(phoneIdx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving contact", e);
        }

        if (contactName != null) {
            saveBuddyToPreferences(contactName, contactPhone != null ? contactPhone : "");
            displayBuddy(contactName, contactPhone != null ? contactPhone : "");
            Toast.makeText(this, "Buddy updated: " + contactName, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBuddyToPreferences(String name, String phone) {
        prefs.edit()
                .putString(KEY_BUDDY_NAME, name)
                .putString(KEY_BUDDY_PHONE, phone)
                .apply();
    }

    private void loadBuddyFromPreferences() {
        String name = prefs.getString(KEY_BUDDY_NAME, null);
        String phone = prefs.getString(KEY_BUDDY_PHONE, null);
        displayBuddy(name, phone);
    }

    private void displayBuddy(String name, String phone) {
        if (!TextUtils.isEmpty(name)) {
            binding.tvBuddyName.setText("Phone Contact Buddy: " + name);
            binding.tvBuddyPhone.setText(!TextUtils.isEmpty(phone) ? phone : "No phone number available");
        } else {
            binding.tvBuddyName.setText("Phone Contact Buddy: None selected");
            binding.tvBuddyPhone.setText("");
        }
    }

    private void sendProgressSms() {
        String phone = prefs.getString(KEY_BUDDY_PHONE, null);
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please choose a buddy with a phone number first", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentBalance = dbHelper.getBalance();
        int streak = StreakManager.getDisplayStreak(this);
        String message = "Hey! In Minty, I've earned " + currentBalance + " minty minutes with a "
                + streak + "-day streak! Focus a little. Grow a lot. 🌱";

        Uri smsUri = Uri.parse("smsto:" + Uri.encode(phone));
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, smsUri);
        smsIntent.putExtra("sms_body", message);

        try {
            startActivity(smsIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No SMS application installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
