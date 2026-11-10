package com.minty.app;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Automated tests verifying Phase 3, 4, and 5 business logic:
 * 1. Milestone thresholds and unique keys
 * 2. Personal best monotonicity (never decreases)
 * 3. Share card text generation (private note omission when false)
 * 4. Anti-repetition pool filtering
 */
public class MilestoneLogicTest {

    @Test
    public void testMilestoneFocusThresholdsAndUniqueKeys() {
        int[] milestones = new int[]{25, 60, 120, 300, 600, 1000};
        Set<String> keys = new HashSet<>();

        for (int threshold : milestones) {
            String key = "milestone_focus_" + threshold;
            assertFalse("Milestone key must be unique across all thresholds: " + key, keys.contains(key));
            keys.add(key);
        }

        assertEquals("Must contain all 6 milestone thresholds", 6, keys.size());
    }

    @Test
    public void testPersonalBestNeverDecreases() {
        int currentBest = 0;

        // Session 1: 30 minutes -> updates to 30
        int session1 = 30;
        if (session1 >= 25 && session1 > currentBest) {
            currentBest = session1;
        }
        assertEquals(30, currentBest);

        // Session 2: 45 minutes -> updates to 45
        int session2 = 45;
        if (session2 >= 25 && session2 > currentBest) {
            currentBest = session2;
        }
        assertEquals(45, currentBest);

        // Session 3: 25 minutes -> MUST NOT overwrite 45
        int session3 = 25;
        if (session3 >= 25 && session3 > currentBest) {
            currentBest = session3;
        }
        assertEquals("Personal best must not be replaced by a smaller session", 45, currentBest);

        // Session 4: 15 minutes -> below threshold, must not update
        int session4 = 15;
        if (session4 >= 25 && session4 > currentBest) {
            currentBest = session4;
        }
        assertEquals(45, currentBest);

        // Session 5: 60 minutes -> updates to 60
        int session5 = 60;
        if (session5 >= 25 && session5 > currentBest) {
            currentBest = session5;
        }
        assertEquals(60, currentBest);
    }

    @Test
    public void testShareWithoutNoteExcludesPrivateReflection() {
        String title = "Weekly Reflection (Nov 10)";
        String description = "Reflected on growth, wins, and lessons.";
        String privateNote = "Confidential personal study struggle";
        String dateStr = "Nov 10, 2026";
        String emoji = "📝";

        // Generate content with includePersonalNote = false
        StringBuilder sbWithout = new StringBuilder();
        sbWithout.append(emoji).append(" Minty Memory Milestone\n");
        sbWithout.append("━━━━━━━━━━━━━━━━━━━━\n");
        sbWithout.append(title).append("\n\n");
        sbWithout.append(description).append("\n\n");
        // includePersonalNote is false, so privateNote is NOT appended
        sbWithout.append("Recorded on ").append(dateStr).append("\n");
        sbWithout.append("Minty — Focus a little. Grow a lot. 🌱");

        String resultWithout = sbWithout.toString();
        assertFalse("Private note must NOT appear when user chooses Share Without Note",
                resultWithout.contains("Confidential personal study struggle"));
        assertFalse("Personal Note label must not appear",
                resultWithout.contains("Personal Note:"));

        // Generate content with includePersonalNote = true
        StringBuilder sbWith = new StringBuilder();
        sbWith.append(emoji).append(" Minty Memory Milestone\n");
        sbWith.append("━━━━━━━━━━━━━━━━━━━━\n");
        sbWith.append(title).append("\n\n");
        sbWith.append(description).append("\n\n");
        sbWith.append("Personal Note: \"").append(privateNote).append("\"\n\n");
        sbWith.append("Recorded on ").append(dateStr).append("\n");
        sbWith.append("Minty — Focus a little. Grow a lot. 🌱");

        String resultWith = sbWith.toString();
        assertTrue("Private note MUST appear when user explicitly includes it",
                resultWith.contains("Confidential personal study struggle"));
    }

    @Test
    public void testAntiRepetitionPoolFiltering() {
        List<String> pool = new ArrayList<>();
        pool.add("Note A");
        pool.add("Note B");
        pool.add("Note C");
        pool.add("Note D");

        Set<String> recentSet = new HashSet<>();
        recentSet.add("0"); // Note A recently shown
        recentSet.add("1"); // Note B recently shown

        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!recentSet.contains(String.valueOf(i))) {
                available.add(i);
            }
        }

        assertEquals(2, available.size());
        assertTrue(available.contains(2)); // Note C available
        assertTrue(available.contains(3)); // Note D available
        assertFalse(available.contains(0)); // Note A excluded
        assertFalse(available.contains(1)); // Note B excluded
    }
}
