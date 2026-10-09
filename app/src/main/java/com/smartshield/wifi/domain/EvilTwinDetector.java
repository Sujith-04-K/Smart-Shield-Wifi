package com.smartshield.wifi.domain;

import com.smartshield.wifi.util.StringSimilarityUtil;

import java.util.List;

/**
 * Domain component detecting Evil Twin access points using Levenshtein Distance
 * and security protocol comparison.
 *
 * Rule:
 * If an SSID is similar (Levenshtein distance <= 2) to a nearby access point
 * AND has a mismatched security protocol (e.g. WPA2 vs OPEN),
 * flag as a CRITICAL EVIL TWIN ATTACK (Similarity Score = 0/30).
 */
public class EvilTwinDetector {

    /**
     * Evaluates the SSID Similarity & Evil Twin safety score (out of 30 max).
     *
     * @param target The candidate network being evaluated
     * @param neighbors All nearby scanned Wi-Fi networks in the same radio environment
     * @return 30 (Safe), 15 (Similar SSID warning), or 0 (Critical Evil Twin Risk)
     */
    public static int evaluateSimilarityScore(WifiNetwork target, List<WifiNetwork> neighbors) {
        if (target == null || neighbors == null || neighbors.isEmpty()) {
            return 30; // Default Safe score
        }

        boolean foundSimilarSsid = false;

        for (WifiNetwork neighbor : neighbors) {
            // Ignore self
            if (target.getBssid().equalsIgnoreCase(neighbor.getBssid())) {
                continue;
            }

            String targetSsid = target.getSsid();
            String neighborSsid = neighbor.getSsid();

            if (StringSimilarityUtil.areSsidsSimilar(targetSsid, neighborSsid)) {
                // Check for critical Evil Twin (one OPEN, one SECURE)
                boolean isTargetOpen = "OPEN".equalsIgnoreCase(target.getSecurityType());
                boolean isNeighborOpen = "OPEN".equalsIgnoreCase(neighbor.getSecurityType());

                boolean isTargetSecure = !isTargetOpen && !"UNKNOWN".equalsIgnoreCase(target.getSecurityType());
                boolean isNeighborSecure = !isNeighborOpen && !"UNKNOWN".equalsIgnoreCase(neighbor.getSecurityType());

                if ((isTargetOpen && isNeighborSecure) || (isTargetSecure && isNeighborOpen)) {
                    // CRITICAL RISK: Evil Twin attack (one open, one secure)
                    return 0;
                }

                // Check if SSIDs are exactly the same
                boolean isExactSsidMatch = targetSsid != null && targetSsid.equalsIgnoreCase(neighborSsid);

                if (!isExactSsidMatch) {
                    // Similar but not exact SSID (e.g. Campus_WiFi vs Campus_WiFi_2G)
                    foundSimilarSsid = true;
                }
            }
        }

        // If similar SSIDs were found
        if (foundSimilarSsid) {
            return 15;
        }

        // Absolutely no lookalike SSIDs found
        return 30;
    }

    /**
     * Helper checking if a Critical Evil Twin attack condition is triggered.
     */
    public static boolean isEvilTwinDetected(WifiNetwork target, List<WifiNetwork> neighbors) {
        return evaluateSimilarityScore(target, neighbors) == 0;
    }
}
