package com.smartshield.wifi.presentation.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartshield.wifi.R;
import com.smartshield.wifi.presentation.history.HistoryFragment;
import com.smartshield.wifi.presentation.scanner.ScannerFragment;

/**
 * Host Activity for SmartShield Wi-Fi Capstone Application.
 * Features Material Design 3 Bottom Navigation switching between live Wi-Fi Discovery Scanner
 * and Assessment History / Verified Registry.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbarMain = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbarMain);

        bottomNavView = findViewById(R.id.bottomNavView);
        bottomNavView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_scanner) {
                switchFragment(new ScannerFragment(), "SCANNER");
                return true;
            } else if (itemId == R.id.nav_spectrum) {
                switchFragment(new com.smartshield.wifi.presentation.channel.ChannelAnalyzerFragment(), "SPECTRUM");
                return true;
            } else if (itemId == R.id.nav_history) {
                switchFragment(new HistoryFragment(), "HISTORY");
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavView.setSelectedItemId(R.id.nav_scanner);
        }
    }

    private void switchFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContainer, fragment, tag)
                .commit();
    }
}
