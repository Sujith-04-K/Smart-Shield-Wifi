package com.smartshield.wifi.util;

/**
 * Utility implementing the Levenshtein Distance string comparison algorithm.
 * Used by EvilTwinDetector to identify lookalike or spoofed Wi-Fi SSIDs.
 */
public class StringSimilarityUtil {

    /**
     * Calculates the minimum number of single-character edits (insertions, deletions,
     * or substitutions) required to change string s1 into s2.
     *
     * @param s1 First SSID string
     * @param s2 Second SSID string
     * @return Levenshtein edit distance integer
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        s1 = s1.trim().toLowerCase();
        s2 = s2.trim().toLowerCase();

        int len0 = s1.length() + 1;
        int len1 = s2.length() + 1;

        int[] cost = new int[len0];
        int[] newCost = new int[len0];

        for (int i = 0; i < len0; i++) {
            cost[i] = i;
        }

        for (int j = 1; j < len1; j++) {
            newCost[0] = j;
            for (int i = 1; i < len0; i++) {
                int match = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                int costReplace = cost[i - 1] + match;
                int costInsert = cost[i] + 1;
                int costDelete = newCost[i - 1] + 1;

                newCost[i] = Math.min(Math.min(costInsert, costDelete), costReplace);
            }
            int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }

        return cost[len0 - 1];
    }

    /**
     * Determines if two SSIDs are suspiciously similar (Levenshtein distance <= 2
     * or one is a substring of the other).
     */
    public static boolean areSsidsSimilar(String ssid1, String ssid2) {
        if (ssid1 == null || ssid2 == null) return false;
        String lower1 = ssid1.trim().toLowerCase();
        String lower2 = ssid2.trim().toLowerCase();

        if (lower1.equals(lower2)) return true;
        if (lower1.isEmpty() || lower2.isEmpty()) return false;

        // Check substring containment e.g., "Campus_WiFi" and "Campus_WiFi_Free"
        if (lower1.contains(lower2) || lower2.contains(lower1)) return true;

        int dist = levenshteinDistance(lower1, lower2);
        return dist <= 2;
    }
}
