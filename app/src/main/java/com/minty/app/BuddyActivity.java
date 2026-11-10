package com.minty.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.ListenerRegistration;
import com.minty.app.databinding.ActivityBuddyBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * BuddyActivity serves as the main interactive Two-Way Accountability Buddy Hub:
 * - Tab 0: Buddies (Connected buddies list, Primary Buddy designation, Dashboard entry)
 * - Tab 1: Find & Requests (Search users by username/code, incoming requests, sent requests)
 * - Tab 2: Challenges (Collaborative study challenges with dual progress bars)
 * - Tab 3: Encouragement (Quick message pills, direct encouragement thread, read receipts)
 */
public class BuddyActivity extends AppCompatActivity {

    private static final String TAG = "BuddyActivity";

    private ActivityBuddyBinding binding;
    private BuddyManager buddyManager;

    // Adapters
    private BuddyAdapter buddyAdapter;
    private BuddyRequestAdapter incomingRequestAdapter;
    private BuddyRequestAdapter sentRequestAdapter;
    private UserSearchAdapter searchAdapter;
    private StudyChallengeAdapter challengeAdapter;
    private BuddyMessageAdapter messageAdapter;

    // Active buddy list & chat target
    private final List<UserProfile> currentBuddies = new ArrayList<>();
    private UserProfile activeChatBuddy;

    // Real-time listener registrations
    private ListenerRegistration buddiesListener;
    private ListenerRegistration incomingRequestsListener;
    private ListenerRegistration sentRequestsListener;
    private ListenerRegistration challengesListener;
    private ListenerRegistration messagesListener;

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuddyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buddyManager = BuddyManager.getInstance(this);

        // Setup toolbar navigation
        binding.toolbarBuddy.setNavigationOnClickListener(v -> finish());

        // Ensure current user's public profile is initialized
        buddyManager.ensureCurrentUserProfile(null);

        // Header info: username & invite code
        binding.tvMyUsername.setText(buddyManager.getMyUsername());
        binding.tvMyInviteCode.setText("Code: " + buddyManager.getMyInvitationCode());

        // Native share invite button
        binding.btnShareInvite.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join me on Minty!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, buddyManager.getInvitationShareText());
            startActivity(Intent.createChooser(shareIntent, "Share Minty Invitation"));
        });

        // Privacy settings button
        binding.btnPrivacySettings.setOnClickListener(v -> showPrivacySettingsDialog());

        // Setup Tab Navigation
        setupTabButtons();

        // Setup RecyclerViews
        setupBuddiesTab();
        setupFindRequestsTab();
        setupChallengesTab();
        setupEncouragementTab();

        // Check if an intent requested a specific tab or chat target
        handleIncomingIntent(getIntent());

        // Setup 5-section unified bottom navigation
        NavigationHelper.setupBottomNav(this, binding.bottomNavigationView, R.id.nav_buddies);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        int requestedTab = intent.getIntExtra("open_tab", -1);
        if (requestedTab >= 0 && requestedTab <= 3) {
            selectTab(requestedTab);
        }
        String targetUid = intent.getStringExtra("target_buddy_uid");
        String targetName = intent.getStringExtra("target_buddy_name");
        if (targetUid != null && !targetUid.isEmpty()) {
            activeChatBuddy = new UserProfile(targetUid, targetName != null ? targetName : "Buddy", "", "", "", "", 0);
            updateChatTargetUi();
            attachMessageListener();
        }
    }

    // =========================================================================
    // TAB BAR NAVIGATION
    // =========================================================================

    private void setupTabButtons() {
        binding.tabBuddies.setOnClickListener(v -> selectTab(0));
        binding.tabFindRequests.setOnClickListener(v -> selectTab(1));
        binding.tabChallenges.setOnClickListener(v -> selectTab(2));
        binding.tabEncouragement.setOnClickListener(v -> selectTab(3));
        selectTab(0);
    }

    private void selectTab(int index) {
        currentTab = index;

        // Reset button styles
        resetTabButton(binding.tabBuddies, "Buddies 👥");
        resetTabButton(binding.tabFindRequests, "Find & Requests 🔍");
        resetTabButton(binding.tabChallenges, "Challenges 🎯");
        resetTabButton(binding.tabEncouragement, "Encourage ✨");

        // Hide all containers
        binding.containerTabBuddies.setVisibility(View.GONE);
        binding.containerTabFindRequests.setVisibility(View.GONE);
        binding.containerTabChallenges.setVisibility(View.GONE);
        binding.containerTabEncouragement.setVisibility(View.GONE);

        // Highlight selected tab
        if (index == 0) {
            highlightTabButton(binding.tabBuddies);
            binding.containerTabBuddies.setVisibility(View.VISIBLE);
        } else if (index == 1) {
            highlightTabButton(binding.tabFindRequests);
            binding.containerTabFindRequests.setVisibility(View.VISIBLE);
        } else if (index == 2) {
            highlightTabButton(binding.tabChallenges);
            binding.containerTabChallenges.setVisibility(View.VISIBLE);
        } else if (index == 3) {
            highlightTabButton(binding.tabEncouragement);
            binding.containerTabEncouragement.setVisibility(View.VISIBLE);
            if (activeChatBuddy == null && !currentBuddies.isEmpty()) {
                String primaryUid = buddyManager.getPrimaryBuddyUid();
                activeChatBuddy = currentBuddies.get(0);
                for (UserProfile b : currentBuddies) {
                    if (b.getUid().equals(primaryUid)) {
                        activeChatBuddy = b;
                        break;
                    }
                }
            }
            updateChatTargetUi();
            attachMessageListener();
        }
    }

    private void highlightTabButton(Button btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.leaf));
        btn.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetTabButton(Button btn, String text) {
        btn.setText(text);
        btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.card_white));
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
    }

    // =========================================================================
    // TAB 0: BUDDIES SETUP
    // =========================================================================

    private void setupBuddiesTab() {
        binding.rvBuddies.setLayoutManager(new LinearLayoutManager(this));
        buddyAdapter = new BuddyAdapter(new ArrayList<>(), buddyManager.getPrimaryBuddyUid(), new BuddyAdapter.OnBuddyActionListener() {
            @Override
            public void onViewDashboard(UserProfile buddy) {
                Intent intent = new Intent(BuddyActivity.this, BuddyProfileActivity.class);
                intent.putExtra(BuddyProfileActivity.EXTRA_BUDDY_UID, buddy.getUid());
                intent.putExtra(BuddyProfileActivity.EXTRA_BUDDY_NAME, buddy.getDisplayName());
                intent.putExtra(BuddyProfileActivity.EXTRA_BUDDY_USERNAME, buddy.getUsername());
                intent.putExtra(BuddyProfileActivity.EXTRA_BUDDY_LEVEL, buddy.getTitleLevel());
                startActivity(intent);
            }

            @Override
            public void onSendEncouragement(UserProfile buddy) {
                activeChatBuddy = buddy;
                selectTab(3); // Switch to Encouragement
            }

            @Override
            public void onTogglePrimary(UserProfile buddy, boolean currentlyPrimary) {
                if (currentlyPrimary) {
                    buddyManager.setPrimaryBuddy(null, new BuddyManager.ActionCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                            buddyAdapter.updateData(currentBuddies, null);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    buddyManager.setPrimaryBuddy(buddy.getUid(), new BuddyManager.ActionCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                            buddyAdapter.updateData(currentBuddies, buddy.getUid());
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onRemoveBuddy(UserProfile buddy) {
                new MaterialAlertDialogBuilder(BuddyActivity.this)
                        .setTitle("Remove Buddy")
                        .setMessage("Are you sure you want to remove " + buddy.getDisplayName() + " from your buddies?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            buddyManager.removeBuddyConnection(buddy.getUid(), new BuddyManager.ActionCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.rvBuddies.setAdapter(buddyAdapter);

        binding.btnEmptyFindBuddy.setOnClickListener(v -> selectTab(1));
    }

    // =========================================================================
    // TAB 1: FIND & REQUESTS SETUP
    // =========================================================================

    private void setupFindRequestsTab() {
        // Search results recycler
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new UserSearchAdapter(new ArrayList<>(), user -> {
            buddyManager.sendBuddyRequest(user, new BuddyManager.ActionCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    binding.etSearchUser.setText("");
                    binding.rvSearchResults.setVisibility(View.GONE);
                    binding.tvSearchResultsHeader.setVisibility(View.GONE);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
        binding.rvSearchResults.setAdapter(searchAdapter);

        // Search button
        binding.btnSearchUser.setOnClickListener(v -> {
            String query = binding.etSearchUser.getText() != null ? binding.etSearchUser.getText().toString().trim() : "";
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(this, "Enter a username or code to search", Toast.LENGTH_SHORT).show();
                return;
            }
            buddyManager.searchUsers(query, new BuddyManager.UserSearchCallback() {
                @Override
                public void onResults(List<UserProfile> users) {
                    if (users.isEmpty()) {
                        Toast.makeText(BuddyActivity.this, "No users found matching: " + query, Toast.LENGTH_SHORT).show();
                        binding.rvSearchResults.setVisibility(View.GONE);
                        binding.tvSearchResultsHeader.setVisibility(View.GONE);
                    } else {
                        searchAdapter.setUsers(users);
                        binding.rvSearchResults.setVisibility(View.VISIBLE);
                        binding.tvSearchResultsHeader.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(BuddyActivity.this, "Search error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Incoming requests recycler
        binding.rvIncomingRequests.setLayoutManager(new LinearLayoutManager(this));
        incomingRequestAdapter = new BuddyRequestAdapter(new ArrayList<>(), true, new BuddyRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(BuddyRequest request) {
                buddyManager.acceptBuddyRequest(request, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onReject(BuddyRequest request) {
                buddyManager.rejectBuddyRequest(request, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancel(BuddyRequest request) {}
        });
        binding.rvIncomingRequests.setAdapter(incomingRequestAdapter);

        // Sent requests recycler
        binding.rvSentRequests.setLayoutManager(new LinearLayoutManager(this));
        sentRequestAdapter = new BuddyRequestAdapter(new ArrayList<>(), false, new BuddyRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(BuddyRequest request) {}

            @Override
            public void onReject(BuddyRequest request) {}

            @Override
            public void onCancel(BuddyRequest request) {
                buddyManager.cancelBuddyRequest(request, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        binding.rvSentRequests.setAdapter(sentRequestAdapter);
    }

    // =========================================================================
    // TAB 2: CHALLENGES SETUP
    // =========================================================================

    private void setupChallengesTab() {
        binding.rvChallenges.setLayoutManager(new LinearLayoutManager(this));
        challengeAdapter = new StudyChallengeAdapter(new ArrayList<>(), buddyManager.getCurrentUid(), new StudyChallengeAdapter.OnChallengeActionListener() {
            @Override
            public void onAccept(StudyChallenge challenge) {
                buddyManager.acceptChallenge(challenge, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDecline(StudyChallenge challenge) {
                buddyManager.rejectChallenge(challenge, new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        binding.rvChallenges.setAdapter(challengeAdapter);

        binding.btnCreateChallenge.setOnClickListener(v -> showCreateChallengeDialog());
    }

    private void showCreateChallengeDialog() {
        if (currentBuddies.isEmpty()) {
            Toast.makeText(this, "Connect with at least one buddy before creating a challenge!", Toast.LENGTH_LONG).show();
            selectTab(1); // Switch to Find & Requests
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_challenge, null);
        EditText etTitle = dialogView.findViewById(R.id.etChallengeTitle);
        RadioGroup rgType = dialogView.findViewById(R.id.rgChallengeType);
        EditText etTarget = dialogView.findViewById(R.id.etChallengeTarget);
        EditText etDays = dialogView.findViewById(R.id.etChallengeDays);
        Spinner spnBuddy = dialogView.findViewById(R.id.spnSelectBuddy);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitChallenge);

        // Populate Buddy Spinner
        List<String> buddyNames = new ArrayList<>();
        for (UserProfile b : currentBuddies) {
            buddyNames.add(b.getDisplayName() + " (" + b.getUsername() + ")");
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, buddyNames);
        spnBuddy.setAdapter(spinnerAdapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Please enter a challenge title", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = StudyChallenge.TYPE_FOCUS_MINUTES;
            int checkedId = rgType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbTypeTasks) {
                type = StudyChallenge.TYPE_TASK_COUNT;
            } else if (checkedId == R.id.rbTypeStreak) {
                type = StudyChallenge.TYPE_STREAK;
            }

            int target = 60;
            try {
                target = Integer.parseInt(etTarget.getText().toString().trim());
            } catch (Exception ignored) {}

            int days = 7;
            try {
                days = Integer.parseInt(etDays.getText().toString().trim());
            } catch (Exception ignored) {}

            int selectedBuddyIdx = spnBuddy.getSelectedItemPosition();
            if (selectedBuddyIdx < 0 || selectedBuddyIdx >= currentBuddies.size()) {
                Toast.makeText(this, "Please select a buddy", Toast.LENGTH_SHORT).show();
                return;
            }
            UserProfile invitedBuddy = currentBuddies.get(selectedBuddyIdx);

            buddyManager.createChallenge(title, type, target, days, invitedBuddy, new BuddyManager.ActionCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // =========================================================================
    // TAB 3: ENCOURAGEMENT SETUP
    // =========================================================================

    private void setupEncouragementTab() {
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new BuddyMessageAdapter(new ArrayList<>(), buddyManager.getCurrentUid());
        binding.rvMessages.setAdapter(messageAdapter);

        // Switch buddy button
        binding.btnSwitchChatBuddy.setOnClickListener(v -> showSwitchBuddyDialog());

        // Quick pills click listeners
        setupPillListener(binding.pillPraise1, "Keep growing! 🌱");
        setupPillListener(binding.pillPraise2, "You've got this! 💪");
        setupPillListener(binding.pillPraise3, "Time to focus! ⏱️");
        setupPillListener(binding.pillPraise4, "Proud of your progress! ✨");
        setupPillListener(binding.pillPraise5, "Let's complete goals! 🎯");

        // Custom send button
        binding.btnSendMessage.setOnClickListener(v -> {
            String text = binding.etCustomMessage.getText() != null ? binding.etCustomMessage.getText().toString().trim() : "";
            if (TextUtils.isEmpty(text)) return;
            sendEncouragement(text, BuddyMessage.TYPE_CUSTOM);
            binding.etCustomMessage.setText("");
        });
    }

    private void setupPillListener(View pillView, String text) {
        pillView.setOnClickListener(v -> sendEncouragement(text, BuddyMessage.TYPE_QUICK));
    }

    private void sendEncouragement(String messageText, String type) {
        if (activeChatBuddy == null) {
            Toast.makeText(this, "Please select a buddy to send encouragement", Toast.LENGTH_SHORT).show();
            showSwitchBuddyDialog();
            return;
        }

        buddyManager.sendEncouragementMessage(
                activeChatBuddy.getUid(),
                activeChatBuddy.getDisplayName(),
                messageText,
                type,
                new BuddyManager.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(BuddyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BuddyActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showSwitchBuddyDialog() {
        if (currentBuddies.isEmpty()) {
            Toast.makeText(this, "No connected buddies available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[currentBuddies.size()];
        for (int i = 0; i < currentBuddies.size(); i++) {
            names[i] = currentBuddies.get(i).getDisplayName() + " (" + currentBuddies.get(i).getUsername() + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Buddy to Encourage")
                .setItems(names, (dialog, which) -> {
                    activeChatBuddy = currentBuddies.get(which);
                    updateChatTargetUi();
                    attachMessageListener();
                })
                .show();
    }

    private void updateChatTargetUi() {
        if (activeChatBuddy != null) {
            binding.tvCurrentChatBuddy.setText(activeChatBuddy.getDisplayName() + " (" + activeChatBuddy.getUsername() + ")");
        } else {
            binding.tvCurrentChatBuddy.setText("None selected");
        }
    }

    private void attachMessageListener() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }

        if (activeChatBuddy != null) {
            messagesListener = buddyManager.listenToMessages(activeChatBuddy.getUid(), new BuddyManager.MessageListListener() {
                @Override
                public void onMessagesUpdated(List<BuddyMessage> messages) {
                    messageAdapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Message listener error: " + error);
                }
            });
        }
    }

    // =========================================================================
    // PRIVACY SETTINGS DIALOG
    // =========================================================================

    private void showPrivacySettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_buddy_privacy, null);
        SwitchMaterial swMaster = dialogView.findViewById(R.id.swMasterSharing);
        SwitchMaterial swTasks = dialogView.findViewById(R.id.swShareTasks);
        SwitchMaterial swFocus = dialogView.findViewById(R.id.swShareFocus);
        SwitchMaterial swStreak = dialogView.findViewById(R.id.swShareStreak);
        SwitchMaterial swWeekly = dialogView.findViewById(R.id.swShareWeekly);
        SwitchMaterial swBalance = dialogView.findViewById(R.id.swShareBalance);
        Button btnSave = dialogView.findViewById(R.id.btnSavePrivacy);

        swMaster.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARING_ENABLED, true));
        swTasks.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARE_TASKS, true));
        swFocus.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARE_FOCUS, true));
        swStreak.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARE_STREAK, true));
        swWeekly.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARE_WEEKLY, true));
        swBalance.setChecked(buddyManager.getPrivacySetting(BuddyManager.KEY_SHARE_BALANCE, false));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnSave.setOnClickListener(v -> {
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARING_ENABLED, swMaster.isChecked());
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARE_TASKS, swTasks.isChecked());
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARE_FOCUS, swFocus.isChecked());
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARE_STREAK, swStreak.isChecked());
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARE_WEEKLY, swWeekly.isChecked());
            buddyManager.updatePrivacySetting(BuddyManager.KEY_SHARE_BALANCE, swBalance.isChecked());

            Toast.makeText(this, "Privacy settings saved! 🔒", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // =========================================================================
    // LIFECYCLE LISTENERS
    // =========================================================================

    @Override
    protected void onStart() {
        super.onStart();
        attachFirebaseListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNavigationView != null) {
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_buddies);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachFirebaseListeners();
    }

    private void attachFirebaseListeners() {
        // 1. Connected buddies listener
        if (buddiesListener == null) {
            buddiesListener = buddyManager.listenToConnectedBuddies(new BuddyManager.BuddyListListener() {
                @Override
                public void onBuddiesUpdated(List<UserProfile> buddies, String primaryBuddyUid) {
                    currentBuddies.clear();
                    currentBuddies.addAll(buddies);
                    buddyAdapter.updateData(buddies, primaryBuddyUid);

                    if (buddies.isEmpty()) {
                        binding.rvBuddies.setVisibility(View.GONE);
                        binding.layoutEmptyBuddies.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvBuddies.setVisibility(View.VISIBLE);
                        binding.layoutEmptyBuddies.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Buddies listener error: " + error);
                }
            });
        }

        // 2. Incoming requests listener
        if (incomingRequestsListener == null) {
            incomingRequestsListener = buddyManager.listenToIncomingRequests(new BuddyManager.RequestListener() {
                @Override
                public void onRequestsUpdated(List<BuddyRequest> requests) {
                    incomingRequestAdapter.setRequests(requests);
                    binding.tvEmptyIncoming.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvIncomingRequests.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Incoming requests listener error: " + error);
                }
            });
        }

        // 3. Sent requests listener
        if (sentRequestsListener == null) {
            sentRequestsListener = buddyManager.listenToSentRequests(new BuddyManager.RequestListener() {
                @Override
                public void onRequestsUpdated(List<BuddyRequest> requests) {
                    sentRequestAdapter.setRequests(requests);
                    binding.tvEmptySent.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvSentRequests.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Sent requests listener error: " + error);
                }
            });
        }

        // 4. Challenges listener
        if (challengesListener == null) {
            challengesListener = buddyManager.listenToChallenges(new BuddyManager.ChallengeListListener() {
                @Override
                public void onChallengesUpdated(List<StudyChallenge> activeChallenges, List<StudyChallenge> pendingChallenges) {
                    List<StudyChallenge> combined = new ArrayList<>();
                    combined.addAll(pendingChallenges);
                    combined.addAll(activeChallenges);
                    challengeAdapter.setChallenges(combined);

                    if (combined.isEmpty()) {
                        binding.rvChallenges.setVisibility(View.GONE);
                        binding.layoutEmptyChallenges.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvChallenges.setVisibility(View.VISIBLE);
                        binding.layoutEmptyChallenges.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Challenges listener error: " + error);
                }
            });
        }

        // 5. Messages listener
        if (currentTab == 3 && activeChatBuddy != null) {
            attachMessageListener();
        }
    }

    private void detachFirebaseListeners() {
        if (buddiesListener != null) {
            buddiesListener.remove();
            buddiesListener = null;
        }
        if (incomingRequestsListener != null) {
            incomingRequestsListener.remove();
            incomingRequestsListener = null;
        }
        if (sentRequestsListener != null) {
            sentRequestsListener.remove();
            sentRequestsListener = null;
        }
        if (challengesListener != null) {
            challengesListener.remove();
            challengesListener = null;
        }
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }
}
