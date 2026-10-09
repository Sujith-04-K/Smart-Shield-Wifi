package com.smartshield.wifi.util;

import android.content.Context;
import android.os.Environment;

import com.smartshield.wifi.data.KnownNetworkEntity;
import com.smartshield.wifi.data.ScanHistoryEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility for exporting Room Database records (Scan History & Known Networks)
 * to CSV and JSON formatted backup files.
 */
public class DataExportManager {

    /**
     * Exports Scan History logs to CSV file.
     */
    public static File exportHistoryToCsv(Context context, List<ScanHistoryEntity> historyList) throws IOException {
        File exportDir = getExportDir(context);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File file = new File(exportDir, "SmartShield_ScanHistory_" + timestamp + ".csv");

        FileWriter writer = new FileWriter(file);
        writer.append("ID,SSID,BSSID,TrustScore,RiskLevel,Timestamp,FormattedDate\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        for (ScanHistoryEntity entity : historyList) {
            String dateStr = dateFormat.format(new Date(entity.getTimestamp()));
            writer.append(String.valueOf(entity.getId())).append(",")
                    .append(escapeCsv(entity.getSsid())).append(",")
                    .append(escapeCsv(entity.getBssid())).append(",")
                    .append(String.valueOf(entity.getTotalScore())).append(",")
                    .append(escapeCsv(entity.getRiskLevel())).append(",")
                    .append(String.valueOf(entity.getTimestamp())).append(",")
                    .append(escapeCsv(dateStr)).append("\n");
        }

        writer.flush();
        writer.close();
        return file;
    }

    /**
     * Exports Scan History logs to JSON file.
     */
    public static File exportHistoryToJson(Context context, List<ScanHistoryEntity> historyList) throws IOException {
        File exportDir = getExportDir(context);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File file = new File(exportDir, "SmartShield_ScanHistory_" + timestamp + ".json");

        FileWriter writer = new FileWriter(file);
        writer.append("[\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        for (int i = 0; i < historyList.size(); i++) {
            ScanHistoryEntity entity = historyList.get(i);
            String dateStr = dateFormat.format(new Date(entity.getTimestamp()));

            writer.append("  {\n")
                    .append("    \"id\": ").append(String.valueOf(entity.getId())).append(",\n")
                    .append("    \"ssid\": \"").append(escapeJson(entity.getSsid())).append("\",\n")
                    .append("    \"bssid\": \"").append(escapeJson(entity.getBssid())).append("\",\n")
                    .append("    \"totalScore\": ").append(String.valueOf(entity.getTotalScore())).append(",\n")
                    .append("    \"riskLevel\": \"").append(escapeJson(entity.getRiskLevel())).append("\",\n")
                    .append("    \"timestamp\": ").append(String.valueOf(entity.getTimestamp())).append(",\n")
                    .append("    \"date\": \"").append(escapeJson(dateStr)).append("\"\n")
                    .append("  }");

            if (i < historyList.size() - 1) {
                writer.append(",");
            }
            writer.append("\n");
        }

        writer.append("]\n");
        writer.flush();
        writer.close();
        return file;
    }

    private static File getExportDir(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SmartShield_Exports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static String escapeCsv(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
