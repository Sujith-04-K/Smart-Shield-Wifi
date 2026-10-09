package com.smartshield.wifi.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Single Repository coordinating local Room persistence and known-network lookups.
 * Keeps the capstone architecture clean and decoupled from UI ViewModels.
 */
public class WifiRepository {

    private final WifiDao wifiDao;
    private final LiveData<List<ScanHistoryEntity>> allScanHistory;
    private final LiveData<List<KnownNetworkEntity>> allKnownNetworks;

    public WifiRepository(Application application) {
        SmartShieldDatabase database = SmartShieldDatabase.getDatabase(application);
        this.wifiDao = database.wifiDao();
        this.allScanHistory = wifiDao.getAllScanHistory();
        this.allKnownNetworks = wifiDao.getAllKnownNetworks();
    }

    // ==========================================
    // SCAN HISTORY METHODS
    // ==========================================

    public LiveData<List<ScanHistoryEntity>> getAllScanHistory() {
        return allScanHistory;
    }

    public LiveData<List<ScanHistoryEntity>> getScanHistoryByRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isEmpty() || "ALL".equalsIgnoreCase(riskLevel)) {
            return allScanHistory;
        }
        return wifiDao.getScanHistoryByRisk(riskLevel);
    }

    public void insertScanHistory(ScanHistoryEntity entity) {
        SmartShieldDatabase.databaseWriteExecutor.execute(() -> {
            wifiDao.insertScanHistory(entity);
        });
    }

    public void clearAllScanHistory() {
        SmartShieldDatabase.databaseWriteExecutor.execute(wifiDao::clearAllScanHistory);
    }

    // ==========================================
    // KNOWN / VERIFIED NETWORK METHODS
    // ==========================================

    public LiveData<List<KnownNetworkEntity>> getAllKnownNetworks() {
        return allKnownNetworks;
    }

    public void insertKnownNetwork(KnownNetworkEntity entity) {
        SmartShieldDatabase.databaseWriteExecutor.execute(() -> {
            wifiDao.insertKnownNetwork(entity);
        });
    }

    public void deleteKnownNetwork(String bssid) {
        SmartShieldDatabase.databaseWriteExecutor.execute(() -> {
            wifiDao.deleteKnownNetwork(bssid);
        });
    }

    /**
     * Synchronously checks if a BSSID is in the user's verified known network registry.
     * Called by TrustAssessmentEngine on a background thread.
     */
    public boolean isNetworkKnownSync(String bssid) {
        if (bssid == null || bssid.isEmpty()) return false;
        KnownNetworkEntity entity = wifiDao.getKnownNetworkSync(bssid);
        return entity != null && entity.isVerifiedTrusted();
    }

    /**
     * Returns a set of all known BSSIDs synchronously for batch trust evaluations.
     */
    public Set<String> getAllKnownBssidsSync() {
        List<String> list = wifiDao.getAllKnownBssidsSync();
        return new HashSet<>(list);
    }
}
