package com.smartshield.wifi.util;

import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.TrustAssessmentEngine;
import com.smartshield.wifi.domain.WifiNetwork;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing Wi-Fi QR Code payloads (WIFI:S:...;T:...;P:...;)
 * and generating pre-connection trust assessments.
 */
public class WifiQrParser {

    public static class QrWifiPayload {
        public final String ssid;
        public final String securityType; // "WPA", "WEP", "nopass", "UNKNOWN"
        public final String password;
        public final boolean isHidden;

        public QrWifiPayload(String ssid, String securityType, String password, boolean isHidden) {
            this.ssid = ssid != null ? ssid : "Unknown Network";
            this.securityType = securityType != null ? securityType : "nopass";
            this.password = password != null ? password : "";
            this.isHidden = isHidden;
        }
    }

    /**
     * Parses a Wi-Fi QR Code raw text string.
     */
    public static QrWifiPayload parse(String qrContent) {
        if (qrContent == null || !qrContent.toUpperCase().startsWith("WIFI:")) {
            return null;
        }

        String ssid = extractField(qrContent, "S:");
        String security = extractField(qrContent, "T:");
        String password = extractField(qrContent, "P:");
        String hiddenStr = extractField(qrContent, "H:");
        boolean isHidden = "true".equalsIgnoreCase(hiddenStr);

        if (security == null || security.isEmpty()) {
            security = (password != null && !password.isEmpty()) ? "WPA2" : "nopass";
        }

        return new QrWifiPayload(ssid, security, password, isHidden);
    }

    /**
     * Converts parsed QR Payload into a candidate WifiNetwork and evaluates its Trust Score.
     */
    public static TrustAssessment evaluateQrCode(QrWifiPayload payload) {
        if (payload == null) return null;

        String secType;
        String caps;
        String secUpper = payload.securityType.toUpperCase();

        if (secUpper.contains("WPA3") || secUpper.contains("SAE")) {
            secType = "WPA3";
            caps = "[WPA3-SAE-CCMP]";
        } else if (secUpper.contains("WPA")) {
            secType = "WPA2";
            caps = "[WPA2-PSK-CCMP]";
        } else if (secUpper.contains("WEP")) {
            secType = "WPA";
            caps = "[WEP]";
        } else {
            secType = "OPEN";
            caps = "[ESS]";
        }

        // Generate synthetic BSSID for evaluation
        String syntheticBssid = "QR:00:11:22:33:44";
        WifiNetwork qrNetwork = new WifiNetwork(
                payload.ssid,
                syntheticBssid,
                -55, // Assume clear line of sight when scanning QR code
                2437, // Default 2.4GHz Ch 6
                secType,
                caps,
                System.currentTimeMillis()
        );

        TrustAssessment baseAssessment = TrustAssessmentEngine.evaluate(qrNetwork, Collections.emptyList(), false);

        java.util.List<String> customReasons = new java.util.ArrayList<>(baseAssessment.getExplainableReasons());

        // Add QR Password strength & security specific explanations
        if ("OPEN".equals(secType) || "nopass".equalsIgnoreCase(payload.securityType)) {
            customReasons.add("✗ QR Security Warning: QR code configures an unencrypted OPEN network.");
        } else if (payload.password.length() < 8) {
            customReasons.add("! Short Password Warning: Wi-Fi password in QR code is under 8 characters.");
        } else {
            customReasons.add("✓ Strong Credentials: QR code contains an 8+ character encrypted password.");
        }

        return new TrustAssessment(
                qrNetwork,
                baseAssessment.getSecurityScore(),
                baseAssessment.getSimilarityScore(),
                baseAssessment.getKnownScore(),
                baseAssessment.getSignalScore(),
                baseAssessment.isEvilTwinDetected(),
                customReasons
        );
    }

    private static String extractField(String source, String key) {
        int keyIndex = source.indexOf(key);
        if (keyIndex == -1) return null;

        int startIndex = keyIndex + key.length();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = startIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == ';') {
                break;
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }
}
