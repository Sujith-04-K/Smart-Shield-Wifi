package com.smartshield.wifi.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.smartshield.wifi.domain.WifiNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Scanner class wrapping Android's WifiManager and supervising OS scan throttling.
 * Android OS restricts foreground apps to 4 Wi-Fi scans per 2-minute window.
 * This class tracks timestamps, provides cooldown countdowns, and parses ScanResult lists.
 */
public class WifiScanner {

    private static final long THROTTLE_WINDOW_MS = 120_000L; // 2 minutes in ms
    private static final int MAX_SCANS_IN_WINDOW = 4;

    private final WifiManager wifiManager;
    private final LinkedList<Long> recentScanTimestamps = new LinkedList<>();
    private WifiScanReceiver receiver;
    private List<WifiNetwork> cachedNetworks = new ArrayList<>();

    public WifiScanner(Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Checks if Android's 4-scans-per-2-minutes throttle limit is currently active.
     */
    public synchronized boolean isThrottled() {
        cleanOldTimestamps();
        return recentScanTimestamps.size() >= MAX_SCANS_IN_WINDOW;
    }

    /**
     * Returns remaining cooldown countdown in seconds when scan throttling is active.
     */
    public synchronized int getRemainingCooldownSeconds() {
        cleanOldTimestamps();
        if (recentScanTimestamps.size() < MAX_SCANS_IN_WINDOW) {
            return 0;
        }
        long oldestInWindow = recentScanTimestamps.getFirst();
        long elapsed = System.currentTimeMillis() - oldestInWindow;
        long remainingMs = THROTTLE_WINDOW_MS - elapsed;
        return (int) Math.max(1, remainingMs / 1000L);
    }

    private synchronized void cleanOldTimestamps() {
        long now = System.currentTimeMillis();
        while (!recentScanTimestamps.isEmpty() && (now - recentScanTimestamps.getFirst() > THROTTLE_WINDOW_MS)) {
            recentScanTimestamps.removeFirst();
        }
    }

    /**
     * Triggers a Wi-Fi scan if not throttled.
     *
     * @return true if scan started successfully, false if throttled or OS denied request
     */
    @SuppressLint("MissingPermission")
    public synchronized boolean startScan() {
        if (wifiManager == null) return false;
        if (isThrottled()) {
            return false;
        }
        boolean success = wifiManager.startScan();
        if (success) {
            recentScanTimestamps.addLast(System.currentTimeMillis());
        }
        return success;
    }

    /**
     * Retrieves and parses discovered access points from Android WifiManager.
     * Compatible with API 21 through API 34+ (including getWifiSsid() compatibility).
     */
    @SuppressLint("MissingPermission")
    public synchronized List<WifiNetwork> getDiscoveredNetworks() {
        if (wifiManager == null) return cachedNetworks;

        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            return cachedNetworks;
        }

        List<WifiNetwork> networks = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ScanResult result : results) {
            if (result == null || result.BSSID == null) continue;

            String ssid = extractSsid(result);
            String capabilities = result.capabilities != null ? result.capabilities : "";
            String securityType = classifySecurityType(capabilities, result);

            WifiNetwork network = new WifiNetwork(
                    ssid,
                    result.BSSID,
                    result.level,
                    result.frequency,
                    securityType,
                    capabilities,
                    now
            );
            networks.add(network);
        }

        // Sort descending by signal strength dBm
        Collections.sort(networks, (o1, o2) -> Integer.compare(o2.getRssi(), o1.getRssi()));
        this.cachedNetworks = networks;
        return networks;
    }

    /**
     * Extracts SSID string across Android API levels (handles API 33 getWifiSsid() vs SSID).
     */
    private String extractSsid(ScanResult result) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (result.getWifiSsid() != null) {
                    return result.getWifiSsid().toString();
                }
            }
            if (result.SSID != null && !result.SSID.isEmpty()) {
                return result.SSID;
            }
        } catch (Exception ignored) {}
        return "Hidden Network";
    }

    /**
     * Classifies security protocol from ScanResult capabilities.
     */
    private String classifySecurityType(String caps, ScanResult result) {
        String upper = caps.toUpperCase();
        if (upper.contains("SAE") || upper.contains("WPA3")) {
            return "WPA3";
        } else if (upper.contains("WPA2") || upper.contains("RSN")) {
            return "WPA2";
        } else if (upper.contains("WPA")) {
            return "WPA";
        } else if (upper.contains("WEP")) {
            return "WEP";
        } else if (upper.contains("ESS") && !upper.contains("PSK") && !upper.contains("EAP") && !upper.contains("SAE")) {
            return "OPEN";
        }
        if (caps.isEmpty() || "OPEN".equalsIgnoreCase(caps)) {
            return "OPEN";
        }
        return "UNKNOWN";
    }

    public void registerScanReceiver(Context context, WifiScanReceiver.ScanResultCallback callback) {
        unregisterScanReceiver(context);
        receiver = new WifiScanReceiver(callback);
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(receiver, filter);
    }

    public void unregisterScanReceiver(Context context) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception ignored) {}
            receiver = null;
        }
    }
}
