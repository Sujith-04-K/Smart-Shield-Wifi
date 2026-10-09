package com.smartshield.wifi.presentation.scanner;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartshield.wifi.SmartShieldApp;
import com.smartshield.wifi.data.SmartShieldDatabase;
import com.smartshield.wifi.data.WifiRepository;
import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.TrustAssessmentEngine;
import com.smartshield.wifi.domain.WifiNetwork;
import com.smartshield.wifi.scanner.WifiScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ViewModel for ScannerFragment.
 * Manages Wi-Fi scanning, scan throttling countdown, filter chips, and Trust Score evaluation.
 */
public class ScannerViewModel extends AndroidViewModel {

    private final WifiRepository repository;
    private final WifiScanner wifiScanner;

    private final MutableLiveData<List<TrustAssessment>> _scanResults = new MutableLiveData<>();
    private final MutableLiveData<Integer> _cooldownSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> _isScanning = new MutableLiveData<>(false);

    private List<TrustAssessment> allAssessments = new ArrayList<>();
    private String currentRiskFilter = "ALL";

    public ScannerViewModel(@NonNull Application application) {
        super(application);
        SmartShieldApp app = (SmartShieldApp) application;
        this.repository = app.getWifiRepository();
        this.wifiScanner = new WifiScanner(application);
    }

    private final MutableLiveData<List<WifiNetwork>> _discoveredNetworks = new MutableLiveData<>();

    public LiveData<List<TrustAssessment>> getScanResults() { return _scanResults; }
    public LiveData<List<WifiNetwork>> getDiscoveredNetworks() { return _discoveredNetworks; }
    public LiveData<Integer> getCooldownSeconds() { return _cooldownSeconds; }
    public LiveData<Boolean> getIsScanning() { return _isScanning; }

    /**
     * Triggers a scan or loads cached results if throttled.
     */
    public void refreshScan() {
        if (wifiScanner.isThrottled()) {
            _cooldownSeconds.setValue(wifiScanner.getRemainingCooldownSeconds());
            _isScanning.setValue(false);
            processDiscoveredNetworks(wifiScanner.getDiscoveredNetworks());
            return;
        }

        _isScanning.setValue(true);
        boolean started = wifiScanner.startScan();
        if (!started) {
            _isScanning.setValue(false);
            processDiscoveredNetworks(wifiScanner.getDiscoveredNetworks());
        }
    }

    /**
     * Called when Wi-Fi scan results broadcast is received.
     */
    public void onScanBroadcastReceived() {
        _isScanning.setValue(false);
        _cooldownSeconds.setValue(wifiScanner.getRemainingCooldownSeconds());
        processDiscoveredNetworks(wifiScanner.getDiscoveredNetworks());
    }

    /**
     * Evaluates TrustScores in background thread and updates LiveData.
     */
    private void processDiscoveredNetworks(List<WifiNetwork> networks) {
        _discoveredNetworks.postValue(networks);
        SmartShieldDatabase.databaseWriteExecutor.execute(() -> {
            Set<String> knownBssids = repository.getAllKnownBssidsSync();
            List<TrustAssessment> results = TrustAssessmentEngine.evaluateAll(networks, knownBssids);
            this.allAssessments = results;
            applyFilter(currentRiskFilter);
        });
    }

    private boolean isGroupedBySsid = true; // Default ON for clean UI!

    public boolean isGroupedBySsid() { return isGroupedBySsid; }

    public void setGroupedBySsid(boolean group) {
        this.isGroupedBySsid = group;
        applyFilter(currentRiskFilter);
    }

    public void setRiskFilter(String filter) {
        this.currentRiskFilter = filter;
        applyFilter(filter);
    }

    private void applyFilter(String filter) {
        List<TrustAssessment> filtered = new ArrayList<>();
        if ("ALL".equalsIgnoreCase(filter)) {
            filtered.addAll(allAssessments);
        } else {
            for (TrustAssessment item : allAssessments) {
                if (item.getRiskLevel().equalsIgnoreCase(filter)) {
                    filtered.add(item);
                }
            }
        }

        if (isGroupedBySsid && !filtered.isEmpty()) {
            java.util.Map<String, List<TrustAssessment>> groupedMap = new java.util.LinkedHashMap<>();
            for (TrustAssessment ta : filtered) {
                String ssid = ta.getNetwork().getSsid();
                if (!groupedMap.containsKey(ssid)) {
                    groupedMap.put(ssid, new ArrayList<>());
                }
                groupedMap.get(ssid).add(ta);
            }

            List<TrustAssessment> consolidatedList = new ArrayList<>();
            for (List<TrustAssessment> group : groupedMap.values()) {
                if (group.isEmpty()) continue;
                // Pick best assessment in group
                TrustAssessment best = group.get(0);
                for (TrustAssessment item : group) {
                    if (item.getTotalScore() > best.getTotalScore()) {
                        best = item;
                    }
                }

                if (group.size() > 1) {
                    List<String> explainableReasons = new ArrayList<>(best.getExplainableReasons());
                    explainableReasons.add(0, "✓ Enterprise Roaming Network: Consolidates " + group.size() + " physical router access points (BSSIDs).");
                    TrustAssessment consolidated = new TrustAssessment(
                            best.getNetwork(),
                            best.getSecurityScore(),
                            best.getSimilarityScore(),
                            best.getKnownScore(),
                            best.getSignalScore(),
                            best.isEvilTwinDetected(),
                            explainableReasons,
                            group.size()
                    );
                    consolidatedList.add(consolidated);
                } else {
                    consolidatedList.add(best);
                }
            }
            _scanResults.postValue(consolidatedList);
        } else {
            _scanResults.postValue(filtered);
        }
    }

    public WifiScanner getWifiScanner() {
        return wifiScanner;
    }
}
