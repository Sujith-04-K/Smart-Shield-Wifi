package com.smartshield.wifi.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Database Entity representing a historical Wi-Fi trust assessment scan log.
 * Stores the Weighted MCDA category scores (Security 40, Similarity 30, Known 20, Signal 10),
 * the total score (0-100), the risk classification, and serialized explainable reasons.
 */
@Entity(tableName = "scan_history")
public class ScanHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String ssid;
    private String bssid;
    private int totalScore;
    private String riskLevel; // SAFE, MEDIUM_RISK, HIGH_RISK
    private long timestamp;
    private String reasonsJson; // JSON array of explainability checkmarks

    // Weighted Category Score Breakdown (100 total)
    private int securityScore;   // out of 40
    private int similarityScore; // out of 30
    private int knownScore;      // out of 20
    private int signalScore;     // out of 10

    public ScanHistoryEntity(String ssid, String bssid, int totalScore, String riskLevel,
                             long timestamp, String reasonsJson, int securityScore,
                             int similarityScore, int knownScore, int signalScore) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.totalScore = totalScore;
        this.riskLevel = riskLevel;
        this.timestamp = timestamp;
        this.reasonsJson = reasonsJson;
        this.securityScore = securityScore;
        this.similarityScore = similarityScore;
        this.knownScore = knownScore;
        this.signalScore = signalScore;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public String getBssid() { return bssid; }
    public void setBssid(String bssid) { this.bssid = bssid; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReasonsJson() { return reasonsJson; }
    public void setReasonsJson(String reasonsJson) { this.reasonsJson = reasonsJson; }

    public int getSecurityScore() { return securityScore; }
    public void setSecurityScore(int securityScore) { this.securityScore = securityScore; }

    public int getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(int similarityScore) { this.similarityScore = similarityScore; }

    public int getKnownScore() { return knownScore; }
    public void setKnownScore(int knownScore) { this.knownScore = knownScore; }

    public int getSignalScore() { return signalScore; }
    public void setSignalScore(int signalScore) { this.signalScore = signalScore; }
}
