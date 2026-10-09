package com.smartshield.wifi.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton Room Database for SmartShield Wi-Fi.
 * Manages tables for ScanHistoryEntity and KnownNetworkEntity.
 * Includes an ExecutorService for non-blocking asynchronous disk writes.
 */
@Database(
    entities = {ScanHistoryEntity.class, KnownNetworkEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class SmartShieldDatabase extends RoomDatabase {

    public abstract WifiDao wifiDao();

    private static volatile SmartShieldDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static SmartShieldDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SmartShieldDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SmartShieldDatabase.class,
                            "smartshield_wifi_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
