package com.smartshield.wifi.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for resolving MAC Address Organizationally Unique Identifiers (OUI)
 * to router manufacturers and identifying potential Rogue AP / SoftAP hardware
 * (such as Raspberry Pi or Espressif microcontrollers).
 */
public class OuiVendorLookup {

    private static final Map<String, String> OUI_MAP = new HashMap<>();

    static {
        // Cisco / Linksys
        OUI_MAP.put("00:00:0C", "Cisco Systems");
        OUI_MAP.put("00:01:42", "Cisco Systems");
        OUI_MAP.put("00:11:20", "Cisco Systems");
        OUI_MAP.put("00:1A:A1", "Cisco Systems");
        OUI_MAP.put("00:1D:7E", "Cisco Systems");
        OUI_MAP.put("00:25:45", "Cisco Systems");
        OUI_MAP.put("00:12:17", "Linksys (Cisco)");

        // TP-Link
        OUI_MAP.put("00:1D:0F", "TP-Link");
        OUI_MAP.put("14:CF:92", "TP-Link");
        OUI_MAP.put("50:C7:BF", "TP-Link");
        OUI_MAP.put("98:DA:C4", "TP-Link");
        OUI_MAP.put("A4:2B:B0", "TP-Link");
        OUI_MAP.put("C0:25:E9", "TP-Link");
        OUI_MAP.put("E8:48:B8", "TP-Link");
        OUI_MAP.put("F4:EC:38", "TP-Link");

        // Netgear
        OUI_MAP.put("00:09:5B", "Netgear");
        OUI_MAP.put("00:14:6C", "Netgear");
        OUI_MAP.put("00:1B:2F", "Netgear");
        OUI_MAP.put("00:1F:33", "Netgear");
        OUI_MAP.put("20:4E:7F", "Netgear");
        OUI_MAP.put("28:80:88", "Netgear");
        OUI_MAP.put("A4:2B:8C", "Netgear");
        OUI_MAP.put("C4:04:15", "Netgear");

        // Asus
        OUI_MAP.put("00:0E:A6", "ASUSTeK Computer");
        OUI_MAP.put("00:11:D8", "ASUSTeK Computer");
        OUI_MAP.put("00:1E:8C", "ASUSTeK Computer");
        OUI_MAP.put("08:60:6E", "ASUSTeK Computer");
        OUI_MAP.put("10:7B:44", "ASUSTeK Computer");
        OUI_MAP.put("1C:87:2C", "ASUSTeK Computer");
        OUI_MAP.put("30:5A:3A", "ASUSTeK Computer");

        // D-Link
        OUI_MAP.put("00:05:5D", "D-Link");
        OUI_MAP.put("00:0D:88", "D-Link");
        OUI_MAP.put("00:15:E9", "D-Link");
        OUI_MAP.put("00:1E:58", "D-Link");
        OUI_MAP.put("1C:7EE:E6", "D-Link");
        OUI_MAP.put("28:10:7B", "D-Link");

        // Huawei
        OUI_MAP.put("00:0B:45", "Huawei Technologies");
        OUI_MAP.put("00:1E:10", "Huawei Technologies");
        OUI_MAP.put("00:25:9E", "Huawei Technologies");
        OUI_MAP.put("04:25:C6", "Huawei Technologies");
        OUI_MAP.put("10:1B:54", "Huawei Technologies");

        // Apple
        OUI_MAP.put("00:03:93", "Apple Inc.");
        OUI_MAP.put("00:05:02", "Apple Inc.");
        OUI_MAP.put("00:0A:95", "Apple Inc.");
        OUI_MAP.put("00:17:F2", "Apple Inc.");
        OUI_MAP.put("00:1C:B3", "Apple Inc.");
        OUI_MAP.put("04:0C:CE", "Apple Inc.");
        OUI_MAP.put("18:E7:28", "Apple Inc.");

        // Google
        OUI_MAP.put("00:1A:11", "Google LLC");
        OUI_MAP.put("3C:5A:B4", "Google LLC");
        OUI_MAP.put("54:60:09", "Google LLC");
        OUI_MAP.put("94:EB:CD", "Google LLC");
        OUI_MAP.put("D8:3C:99", "Google LLC");

        // Samsung
        OUI_MAP.put("00:07:AB", "Samsung Electronics");
        OUI_MAP.put("00:12:FB", "Samsung Electronics");
        OUI_MAP.put("00:15:99", "Samsung Electronics");
        OUI_MAP.put("50:85:69", "Samsung Electronics");

        // Intel
        OUI_MAP.put("00:02:B3", "Intel Corporation");
        OUI_MAP.put("00:13:E8", "Intel Corporation");
        OUI_MAP.put("00:1B:21", "Intel Corporation");

        // GL.iNet (Popular portable router / pentesting hardware)
        OUI_MAP.put("94:83:C4", "GL Technologies (GL.iNet)");
        OUI_MAP.put("E4:95:6E", "GL Technologies (GL.iNet)");

        // Raspberry Pi (Commonly used for Wi-Fi Pineapple / Rogue SoftAP)
        OUI_MAP.put("B8:27:EB", "Raspberry Pi Foundation");
        OUI_MAP.put("DC:A6:32", "Raspberry Pi Trading");
        OUI_MAP.put("E4:5F:01", "Raspberry Pi Trading");
        OUI_MAP.put("28:CD:C1", "Raspberry Pi Trading");

        // Espressif Systems (ESP8266 / ESP32 IoT / Rogue SoftAP hardware)
        OUI_MAP.put("18:FE:34", "Espressif Systems (ESP8266/ESP32)");
        OUI_MAP.put("24:0A:C4", "Espressif Systems (ESP8266/ESP32)");
        OUI_MAP.put("30:AE:A4", "Espressif Systems (ESP8266/ESP32)");
        OUI_MAP.put("A4:CF:12", "Espressif Systems (ESP8266/ESP32)");
        OUI_MAP.put("84:0D:8E", "Espressif Systems (ESP8266/ESP32)");
        OUI_MAP.put("C4:4F:33", "Espressif Systems (ESP8266/ESP32)");
    }

    /**
     * Look up the vendor name for a given MAC address (BSSID).
     */
    public static String getVendorName(String bssid) {
        if (bssid == null || bssid.trim().isEmpty()) {
            return "Unknown Vendor";
        }
        String cleanBssid = bssid.toUpperCase(Locale.US).replace("-", ":");
        String[] parts = cleanBssid.split(":");
        if (parts.length >= 3) {
            String ouiKey = parts[0] + ":" + parts[1] + ":" + parts[2];
            String vendor = OUI_MAP.get(ouiKey);
            if (vendor != null) {
                return vendor;
            }
        }
        return "Generic / Unknown AP";
    }

    /**
     * Determines whether the BSSID originates from hardware typically used for
     * unauthorized Rogue SoftAPs / Pentesting tools (e.g., Raspberry Pi, ESP32, GL.iNet).
     */
    public static boolean isPotentialRogueHardware(String bssid) {
        String vendor = getVendorName(bssid);
        return vendor.contains("Raspberry Pi")
                || vendor.contains("Espressif")
                || vendor.contains("GL.iNet");
    }
}
