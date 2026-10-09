package com.smartshield.wifi;

import android.app.Application;

import com.smartshield.wifi.data.WifiRepository;

/**
 * Main Android Application class for SmartShield Wi-Fi.
 * Initializes global dependencies including Room Database and WifiRepository.
 */
public class SmartShieldApp extends Application {

    private WifiRepository wifiRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Repository & Room Database early
        wifiRepository = new WifiRepository(this);
    }

    public WifiRepository getWifiRepository() {
        return wifiRepository;
    }
}
