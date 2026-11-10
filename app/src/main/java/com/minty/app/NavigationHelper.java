package com.minty.app;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Centralized navigation manager that connects the unified 5-section bottom navigation:
 * 1. Home (MainActivity)
 * 2. Focus (FocusActivity)
 * 3. Garden (GardenActivity)
 * 4. Buddies (BuddyActivity)
 * 5. Progress (ProgressActivity)
 *
 * Uses FLAG_ACTIVITY_REORDER_TO_FRONT and smooth transitions to avoid unnecessary Activity recreation.
 */
public class NavigationHelper {

    public static void setupBottomNav(@NonNull Activity activity,
                                      @NonNull BottomNavigationView bottomNav,
                                      @IdRes int currentNavId) {

        bottomNav.setSelectedItemId(currentNavId);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentNavId) {
                return true; // Already on this tab
            }

            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(activity, MainActivity.class);
            } else if (itemId == R.id.nav_focus) {
                intent = new Intent(activity, FocusActivity.class);
            } else if (itemId == R.id.nav_garden) {
                intent = new Intent(activity, GardenActivity.class);
            } else if (itemId == R.id.nav_buddies) {
                intent = new Intent(activity, BuddyActivity.class);
            } else if (itemId == R.id.nav_progress) {
                intent = new Intent(activity, ProgressActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // Seamless instant tab switch
                return true;
            }

            return false;
        });
    }
}
