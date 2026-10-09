package com.smartshield.wifi;

import org.junit.Test;
import static org.junit.Assert.*;

import com.smartshield.wifi.util.StringSimilarityUtil;

/**
 * Unit tests verifying the Levenshtein Distance string metric algorithms
 * used for detecting lookalike/cloned SSIDs in Evil Twin detection.
 */
public class StringSimilarityUtilTest {

    @Test
    public void testLevenshteinDistance_IdenticalStrings() {
        int distance = StringSimilarityUtil.levenshteinDistance("Campus_WiFi", "Campus_WiFi");
        assertEquals("Identical strings should have edit distance of 0", 0, distance);
    }

    @Test
    public void testLevenshteinDistance_SingleCharacterDifference() {
        int distance = StringSimilarityUtil.levenshteinDistance("Campus_WiFi", "Campus_WiF1");
        assertEquals("Single character substitution should have distance of 1", 1, distance);
    }

    @Test
    public void testLevenshteinDistance_CaseInsensitiveTrimmed() {
        int distance = StringSimilarityUtil.levenshteinDistance(" campus_wifi ", "CAMPUS_WIFI");
        assertEquals("Trimmed case-insensitive comparison should be 0", 0, distance);
    }

    @Test
    public void testAreSsidsSimilar_SimilarDistanceSsid() {
        assertTrue("SSIDs within edit distance 2 should be marked similar",
                StringSimilarityUtil.areSsidsSimilar("Campus_WiFi", "Campus_WiF1"));
    }

    @Test
    public void testAreSsidsSimilar_SubstringContainment() {
        assertTrue("Substring containment (e.g., Campus_WiFi_Free) should be marked similar",
                StringSimilarityUtil.areSsidsSimilar("Campus_WiFi", "Campus_WiFi_Free"));
    }

    @Test
    public void testAreSsidsSimilar_CompletelyDifferentSsids() {
        assertFalse("Completely different SSIDs should not be marked similar",
                StringSimilarityUtil.areSsidsSimilar("Campus_WiFi", "Home_Network_5G"));
    }
}
