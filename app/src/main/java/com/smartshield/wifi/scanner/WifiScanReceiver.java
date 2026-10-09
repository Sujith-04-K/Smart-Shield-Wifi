package com.smartshield.wifi.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * BroadcastReceiver listening for WifiManager.SCAN_RESULTS_AVAILABLE_ACTION from Android OS.
 * Notifies WifiScanner when updated radio spectrum data is ready for parsing.
 */
public class WifiScanReceiver extends BroadcastReceiver {

    public interface ScanResultCallback {
        void onScanResultsReady(boolean success);
    }

    private final ScanResultCallback callback;

    public WifiScanReceiver(ScanResultCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (callback != null) {
                callback.onScanResultsReady(success);
            }
        }
    }
}
