package com.smartshield.wifi.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Enterprise Rule-Based Expert System (XAI) calculating Weighted Multi-Criteria Trust Scores
 * and generating explainable "Why Should I Trust This Network?" audit trails.
 *
 * Total Score = Security (40%) + Similarity (30%) + Known (20%) + Signal (10%) = 100 max.
 */
public class TrustAssessmentEngine {

    /**
     * Evaluates a target Wi-Fi network against its radio environment and known registry.
     *
     * @param target The candidate access point to evaluate
     * @param neighbors All scanned nearby access points
     * @param isKnown True if this BSSID is saved in the user's SmartShield Verified Registry
     * @return Complete explainable TrustAssessment
     */
    public static TrustAssessment evaluate(WifiNetwork target, List<WifiNetwork> neighbors, boolean isKnown) {
        List<String> reasons = new ArrayList<>();

        // 1. Security Protocol Score (max 40)
        int securityScore = 0;
        String secType = target.getSecurityType() != null ? target.getSecurityType().toUpperCase() : "UNKNOWN";
        String caps = target.getCapabilities() != null ? target.getCapabilities().toUpperCase() : "";

        if (secType.contains("WPA3")) {
            securityScore = 40;
            reasons.add("✓ WPA3 Security Protocol (+40/40): Uses latest SAE wireless encryption.");
        } else if (secType.contains("WPA2")) {
            securityScore = 30;
            reasons.add("✓ WPA2 Security Protocol (+30/40): Standard AES encryption enabled.");
        } else if (secType.contains("WPA")) {
            securityScore = 15;
            reasons.add("! WPA Legacy Security (+15/40): Older TKIP/CCMP encryption; upgrade recommended.");
        } else if ("OPEN".equals(secType)) {
            securityScore = 0;
            reasons.add("✗ OPEN Network - Unencrypted (+0/40): No wireless encryption; traffic can be intercepted.");
        } else {
            securityScore = 5;
            reasons.add("! Unknown Encryption Protocol (+5/40): Unable to verify security cipher.");
        }

        // Apply advanced security cipher checks to refine Security Score
        if (!"OPEN".equals(secType) && !"UNKNOWN".equals(secType)) {
            // Enterprise (EAP) check
            if (caps.contains("EAP")) {
                securityScore = Math.min(40, securityScore + 5);
                reasons.add("✓ Enterprise-Grade Authentication (802.1X): Uses secure individual credentials.");
            }

            // TKIP Legacy vulnerability check
            if (caps.contains("TKIP") && !caps.contains("CCMP") && !caps.contains("GCMP")) {
                securityScore = Math.max(0, securityScore - 10);
                reasons.add("✗ Insecure Cipher (TKIP) Detected (-10/40): Cryptographically weak TKIP cipher; vulnerable to spoofing.");
            }

            // WPS PIN vulnerability check
            if (caps.contains("WPS")) {
                securityScore = Math.max(0, securityScore - 5);
                reasons.add("! WPS PIN Brute-Force Risk (-5/40): Wi-Fi Protected Setup is enabled; vulnerable to automated pin brute-forcing.");
            }

            // Ad-hoc network check
            if (caps.contains("IBSS")) {
                securityScore = Math.max(0, securityScore - 15);
                reasons.add("✗ Ad-Hoc Mode Detected (-15/40): Peer-to-peer connection instead of standard Access Point.");
            }
        }

        // 2. SSID Similarity & Evil Twin Safety (max 30)
        int similarityScore = EvilTwinDetector.evaluateSimilarityScore(target, neighbors);
        boolean isEvilTwin = (similarityScore == 0);
        if (similarityScore == 30) {
            reasons.add("✓ Unique SSID (+30/30): Single physical access point broadcasting this network name.");
        } else if (similarityScore == 15) {
            reasons.add("✓ Multi-AP Enterprise Network (+15/30): Multiple physical routers broadcast '" + target.getSsid() + "' for seamless campus roaming. BSSID (" + target.getBssid() + ") on Ch " + target.getChannelNumber() + ".");
        } else {
            reasons.add("✗ CRITICAL ALERT: Evil Twin Attack Detected (+0/30): SSID matches a nearby network but has mismatched security protocols!");
        }

        // 3. Known Network Status & Hardware Vendor (max 20)
        int knownScore = 20;
        String vendor = target.getVendorName();
        if (target.isPotentialRogueHardware()) {
            knownScore = Math.max(0, knownScore - 10);
            reasons.add("✗ Rogue Hardware Indicator (-10/20): BSSID MAC OUI (" + vendor + ") matches softAP/pentesting hardware.");
        } else if (isKnown) {
            reasons.add("✓ Previously Verified in Registry (+20/20): Marked trusted. Vendor: " + vendor + ".");
        } else {
            reasons.add("✓ Safe Baseline Configuration (+20/20): Clean trust profile. Vendor: " + vendor + ".");
        }

        // 4. Signal Strength Score (max 10)
        int signalScore = 3;
        int rssi = target.getRssi();
        if (rssi > -65) {
            signalScore = 10;
            reasons.add("✓ Strong Signal Quality (+10/10): Excellent signal strength (" + rssi + " dBm) ensures stable transmission.");
        } else if (rssi >= -80) {
            signalScore = 7;
            reasons.add("✓ Moderate Signal Quality (+7/10): Good signal strength (" + rssi + " dBm).");
        } else {
            reasons.add("✗ Weak Signal Quality (+3/10): Low RSSI (" + rssi + " dBm) may cause packet loss.");
        }

        return new TrustAssessment(target, securityScore, similarityScore, knownScore, signalScore, isEvilTwin, reasons);
    }

    /**
     * Batch evaluates a list of discovered networks.
     */
    public static List<TrustAssessment> evaluateAll(List<WifiNetwork> candidates, Set<String> knownBssids) {
        List<TrustAssessment> results = new ArrayList<>();
        if (candidates == null) return results;

        for (WifiNetwork network : candidates) {
            boolean isKnown = knownBssids != null && knownBssids.contains(network.getBssid());
            results.add(evaluate(network, candidates, isKnown));
        }
        return results;
    }
}
