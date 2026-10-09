package com.smartshield.wifi.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable Domain Model representing a nearby discovered Wi-Fi Access Point.
 * Holds radio spectrum metadata and encryption protocol classification.
 */
public class WifiNetwork implements Serializable {

    private final String ssid;
    private final String bssid;
    private final int rssi; // Signal level in dBm
    private final int frequency; // Channel frequency in MHz
    private final String securityType; // "WPA3", "WPA2", "WPA", "OPEN", "UNKNOWN"
    private final String capabilities; // Raw capability string
    private final long timestamp;

    public WifiNetwork(String ssid, String bssid, int rssi, int frequency,
                       String securityType, String capabilities, long timestamp) {
        this.ssid = ssid != null ? ssid : "Hidden Network";
        this.bssid = bssid != null ? bssid : "00:00:00:00:00:00";
        this.rssi = rssi;
        this.frequency = frequency;
        this.securityType = securityType != null ? securityType : "UNKNOWN";
        this.capabilities = capabilities != null ? capabilities : "";
        this.timestamp = timestamp;
    }

    public String getSsid() { return ssid; }
    public String getBssid() { return bssid; }
    public int getRssi() { return rssi; }
    public int getFrequency() { return frequency; }
    public String getSecurityType() { return securityType; }
    public String getCapabilities() { return capabilities; }
    public long getTimestamp() { return timestamp; }

    /**
     * Helper to classify band (2.4 GHz vs 5 GHz / 6 GHz)
     */
    public String getBandString() {
        if (frequency >= 4900 && frequency < 5900) {
            return "5 GHz";
        } else if (frequency >= 5900) {
            return "6 GHz";
        } else {
            return "2.4 GHz";
        }
    }

    /**
     * Calculates the Wi-Fi channel number from frequency in MHz.
     */
    public int getChannelNumber() {
        if (frequency >= 2412 && frequency <= 2484) {
            if (frequency == 2484) return 14;
            return (frequency - 2407) / 5;
        } else if (frequency >= 5170 && frequency <= 5825) {
            return (frequency - 5000) / 5;
        } else if (frequency >= 5955 && frequency <= 7115) {
            return (frequency - 5950) / 5 + 1;
        }
        return 1;
    }

    /**
     * Returns resolved router vendor name based on BSSID MAC OUI.
     */
    public String getVendorName() {
        return com.smartshield.wifi.util.OuiVendorLookup.getVendorName(bssid);
    }

    /**
     * Flags whether the access point hardware is a potential Rogue SoftAP.
     */
    public boolean isPotentialRogueHardware() {
        return com.smartshield.wifi.util.OuiVendorLookup.isPotentialRogueHardware(bssid);
    }

    /**
     * Calculates RSSI percentage (0% to 100%) for UI progress bars
     */
    public int getSignalPercentage() {
        if (rssi >= -50) return 100;
        if (rssi <= -100) return 0;
        return 2 * (rssi + 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiNetwork that = (WifiNetwork) o;
        return Objects.equals(bssid, that.bssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bssid);
    }
}
