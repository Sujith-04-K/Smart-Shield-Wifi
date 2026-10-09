package com.smartshield.wifi.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Unified Room Database Access Object (DAO) for SmartShield Wi-Fi.
 * Handles both historical trust assessment logs and user-verified known networks
 * to keep the architecture lean and student-friendly.
 */
@Dao
public interface WifiDao {

    // ==========================================
    // SCAN HISTORY QUERIES
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScanHistory(ScanHistoryEntity entity);

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    LiveData<List<ScanHistoryEntity>> getAllScanHistory();

    @Query("SELECT * FROM scan_history WHERE riskLevel = :level ORDER BY timestamp DESC")
    LiveData<List<ScanHistoryEntity>> getScanHistoryByRisk(String level);

    @Query("DELETE FROM scan_history")
    void clearAllScanHistory();

    // ==========================================
    // KNOWN / VERIFIED NETWORKS QUERIES
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertKnownNetwork(KnownNetworkEntity entity);

    @Query("DELETE FROM known_networks WHERE bssid = :bssid")
    void deleteKnownNetwork(String bssid);

    @Query("SELECT * FROM known_networks WHERE bssid = :bssid LIMIT 1")
    KnownNetworkEntity getKnownNetworkSync(String bssid);

    @Query("SELECT * FROM known_networks ORDER BY addedTimestamp DESC")
    LiveData<List<KnownNetworkEntity>> getAllKnownNetworks();

    @Query("SELECT bssid FROM known_networks")
    List<String> getAllKnownBssidsSync();
}
