package com.smartshield.wifi;

import org.junit.Test;
import static org.junit.Assert.*;

import com.smartshield.wifi.domain.EvilTwinDetector;
import com.smartshield.wifi.domain.WifiNetwork;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for EvilTwinDetector verifying detection of Rogue Access Points
 * via Levenshtein Distance and encryption protocol mismatch rules.
 */
public class EvilTwinDetectorTest {

    @Test
    public void testEvaluateSimilarityScore_NoSimilarNetworks_ReturnsSafe30() {
        WifiNetwork target = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -55, 5240, "WPA3", "[WPA3-SAE]", System.currentTimeMillis());
        WifiNetwork unrelated = new WifiNetwork("CoffeeShop_Guest", "66:77:88:99:AA:BB", -70, 2412, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());

        List<WifiNetwork> neighbors = Collections.singletonList(unrelated);
        int score = EvilTwinDetector.evaluateSimilarityScore(target, neighbors);

        assertEquals("Should return 30 (Safe) when no similar SSIDs exist", 30, score);
        assertFalse("Evil Twin should not be detected", EvilTwinDetector.isEvilTwinDetected(target, neighbors));
    }

    @Test
    public void testEvaluateSimilarityScore_SimilarSsidSameSecurity_ReturnsWarning15() {
        WifiNetwork target = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());
        WifiNetwork duplicate = new WifiNetwork("Campus_WiFi_2G", "66:77:88:99:AA:BB", -65, 2412, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());

        List<WifiNetwork> neighbors = Collections.singletonList(duplicate);
        int score = EvilTwinDetector.evaluateSimilarityScore(target, neighbors);

        assertEquals("Should return 15 (Warning) when similar SSID shares matching security", 15, score);
        assertFalse("Evil Twin should not be flagged when encryption matches", EvilTwinDetector.isEvilTwinDetected(target, neighbors));
    }

    @Test
    public void testEvaluateSimilarityScore_SimilarSsidOpenRogue_ReturnsZeroCriticalRisk() {
        // Legitimate network is WPA2, Rogue AP is broadcasting "Campus_WiFi_Free" with OPEN security
        WifiNetwork rogueTarget = new WifiNetwork("Campus_WiFi_Free", "99:88:77:66:55:44", -48, 2412, "OPEN", "[]", System.currentTimeMillis());
        WifiNetwork legitimate = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -62, 5240, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());

        List<WifiNetwork> neighbors = Collections.singletonList(legitimate);
        int score = EvilTwinDetector.evaluateSimilarityScore(rogueTarget, neighbors);

        assertEquals("Should return 0 (Critical Risk) when similar SSID has mismatched OPEN security", 0, score);
        assertTrue("Evil Twin attack must be flagged!", EvilTwinDetector.isEvilTwinDetected(rogueTarget, neighbors));
    }

    @Test
    public void testEvaluateSimilarityScore_TransitionalSecureNetwork_ReturnsSafe30() {
        WifiNetwork target = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());
        WifiNetwork neighbor = new WifiNetwork("Campus_WiFi", "66:77:88:99:AA:BB", -60, 2412, "WPA3", "[WPA3-SAE]", System.currentTimeMillis());

        List<WifiNetwork> neighbors = Collections.singletonList(neighbor);
        int score = EvilTwinDetector.evaluateSimilarityScore(target, neighbors);

        assertEquals("Should return 30 (Safe) for identical SSID and secure protocols", 30, score);
        assertFalse("Should not flag transitional networks as Evil Twin", EvilTwinDetector.isEvilTwinDetected(target, neighbors));
    }

    @Test
    public void testEvaluateSimilarityScore_EmptyNeighbors_ReturnsSafe30() {
        WifiNetwork target = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -55, 5240, "WPA3", "[WPA3-SAE]", System.currentTimeMillis());
        int score = EvilTwinDetector.evaluateSimilarityScore(target, Collections.emptyList());

        assertEquals("Should return 30 (Safe) when neighbor list is empty", 30, score);
    }
}
