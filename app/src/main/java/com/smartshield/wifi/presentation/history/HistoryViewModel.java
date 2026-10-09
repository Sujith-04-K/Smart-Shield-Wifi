package com.smartshield.wifi.presentation.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.smartshield.wifi.SmartShieldApp;
import com.smartshield.wifi.data.KnownNetworkEntity;
import com.smartshield.wifi.data.ScanHistoryEntity;
import com.smartshield.wifi.data.WifiRepository;

import java.util.List;

/**
 * ViewModel for HistoryFragment.
 * Manages Scan Assessment Logs and Verified Trusted Networks in Room Database.
 */
public class HistoryViewModel extends AndroidViewModel {

    private final WifiRepository repository;
    private final LiveData<List<ScanHistoryEntity>> allScanHistory;
    private final LiveData<List<KnownNetworkEntity>> allKnownNetworks;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        SmartShieldApp app = (SmartShieldApp) application;
        this.repository = app.getWifiRepository();
        this.allScanHistory = repository.getAllScanHistory();
        this.allKnownNetworks = repository.getAllKnownNetworks();
    }

    public LiveData<List<ScanHistoryEntity>> getAllScanHistory() {
        return allScanHistory;
    }

    public LiveData<List<KnownNetworkEntity>> getAllKnownNetworks() {
        return allKnownNetworks;
    }

    public void clearAllScanHistory() {
        repository.clearAllScanHistory();
    }

    public void deleteKnownNetwork(String bssid) {
        repository.deleteKnownNetwork(bssid);
    }
}
