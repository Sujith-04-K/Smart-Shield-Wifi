package com.smartshield.wifi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Database Entity representing a user-verified trusted Wi-Fi access point.
 * This solves Android 10+ (API 29+) restrictions on reading system saved networks
 * by providing an in-app SmartShield Known Networks Registry.
 */
@Entity(tableName = "known_networks")
public class KnownNetworkEntity {

    @PrimaryKey
    @NonNull
    private String bssid;

    private String ssid;
    private long addedTimestamp;
    private boolean isVerifiedTrusted;

    public KnownNetworkEntity(@NonNull String bssid, String ssid, long addedTimestamp, boolean isVerifiedTrusted) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.addedTimestamp = addedTimestamp;
        this.isVerifiedTrusted = isVerifiedTrusted;
    }

    @NonNull
    public String getBssid() { return bssid; }
    public void setBssid(@NonNull String bssid) { this.bssid = bssid; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public long getAddedTimestamp() { return addedTimestamp; }
    public void setAddedTimestamp(long addedTimestamp) { this.addedTimestamp = addedTimestamp; }

    public boolean isVerifiedTrusted() { return isVerifiedTrusted; }
    public void setVerifiedTrusted(boolean verifiedTrusted) { isVerifiedTrusted = verifiedTrusted; }
}
