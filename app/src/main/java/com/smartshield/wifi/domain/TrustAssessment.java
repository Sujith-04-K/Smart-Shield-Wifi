package com.smartshield.wifi.domain;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Domain Model storing the Explainable Weighted MCDA trust assessment for a Wi-Fi network.
 * Weights:
 * - Security Protocol: 40% (40 max)
 * - SSID Similarity & Evil Twin: 30% (30 max)
 * - Verified Known Status: 20% (20 max)
 * - Signal Quality: 10% (10 max)
 * Total: 100 points
 */
public class TrustAssessment implements Serializable {

    private final WifiNetwork network;
    private final int securityScore;
    private final int similarityScore;
    private final int knownScore;
    private final int signalScore;
    private final int totalScore;
    private final String riskLevel;
    private final boolean isEvilTwinDetected;
    private final List<String> explainableReasons;
    private final int apCount;

    public TrustAssessment(WifiNetwork network, int securityScore, int similarityScore,
                           int knownScore, int signalScore, boolean isEvilTwinDetected,
                           List<String> explainableReasons, int apCount) {
        this.network = network;
        this.securityScore = Math.max(0, Math.min(40, securityScore));
        this.similarityScore = Math.max(0, Math.min(30, similarityScore));
        this.knownScore = Math.max(0, Math.min(20, knownScore));
        this.signalScore = Math.max(0, Math.min(10, signalScore));
        this.isEvilTwinDetected = isEvilTwinDetected;
        this.apCount = Math.max(1, apCount);

        this.totalScore = this.securityScore + this.similarityScore + this.knownScore + this.signalScore;

        if (this.isEvilTwinDetected || this.totalScore < 50) {
            this.riskLevel = "HIGH_RISK";
        } else if (this.totalScore < 80) {
            this.riskLevel = "MEDIUM_RISK";
        } else {
            this.riskLevel = "SAFE";
        }

        this.explainableReasons = explainableReasons != null ?
                new ArrayList<>(explainableReasons) : new ArrayList<>();
    }

    public TrustAssessment(WifiNetwork network, int securityScore, int similarityScore,
                           int knownScore, int signalScore, boolean isEvilTwinDetected,
                           List<String> explainableReasons) {
        this(network, securityScore, similarityScore, knownScore, signalScore, isEvilTwinDetected, explainableReasons, 1);
    }

    public WifiNetwork getNetwork() { return network; }
    public int getSecurityScore() { return securityScore; }
    public int getSimilarityScore() { return similarityScore; }
    public int getKnownScore() { return knownScore; }
    public int getSignalScore() { return signalScore; }
    public int getTotalScore() { return totalScore; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isEvilTwinDetected() { return isEvilTwinDetected; }
    public int getApCount() { return apCount; }
    public List<String> getExplainableReasons() { return Collections.unmodifiableList(explainableReasons); }

    public boolean isSafe() {
        return "SAFE".equals(riskLevel);
    }

    /**
     * Calculates the Assessment Confidence Level (High, Medium, or Low)
     * based on observable data completeness, signal quality, and verification status.
     */
    public String getConfidenceLevel() {
        if (isEvilTwinDetected || securityScore == 0 || signalScore <= 3) {
            return "Low";
        } else if (knownScore == 20 || (securityScore >= 30 && signalScore >= 7)) {
            return "High";
        } else {
            return "Medium";
        }
    }

    /**
     * Formats the scan assessment timestamp for UI display (e.g., "27 Jul 2026, 2:45 PM").
     */
    public String getFormattedTimestamp() {
        if (network == null || network.getTimestamp() <= 0) {
            return "Unknown Time";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        return sdf.format(new Date(network.getTimestamp()));
    }
}
