package com.smartshield.wifi.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain engine analyzing potential cyber attacks, vulnerabilities, and threat profiles
 * based on the evaluated Weighted MCDA Trust Score and radio capabilities.
 */
public class ThreatAnalysisEngine {

    public static class ThreatProfile {
        public final String threatLevel; // CRITICAL, HIGH, MODERATE, LOW
        public final List<String> possibleAttacks;
        public final List<String> mitigationSteps;

        public ThreatProfile(String threatLevel, List<String> possibleAttacks, List<String> mitigationSteps) {
            this.threatLevel = threatLevel;
            this.possibleAttacks = possibleAttacks;
            this.mitigationSteps = mitigationSteps;
        }
    }

    public static ThreatProfile analyzeThreats(TrustAssessment assessment) {
        List<String> attacks = new ArrayList<>();
        List<String> mitigations = new ArrayList<>();
        WifiNetwork net = assessment.getNetwork();
        String secType = net.getSecurityType() != null ? net.getSecurityType().toUpperCase() : "UNKNOWN";
        String caps = net.getCapabilities() != null ? net.getCapabilities().toUpperCase() : "";

        String threatLevel;

        if (assessment.isEvilTwinDetected()) {
            threatLevel = "CRITICAL THREAT";
            attacks.add("🚨 Evil Twin Rogue AP Attack: Attacker is spoofing a trusted network name to hijack your connection.");
            attacks.add("🚨 Credential Harvesting & Phishing: Fake login pages forcing users to enter passwords or credit card data.");
            attacks.add("🚨 SSL Stripping & Downgrade: Downgrading HTTPS web traffic to unencrypted HTTP to read private traffic.");
            attacks.add("🚨 Man-In-The-Middle (MITM): All mobile network packets are routed through the attacker's hardware.");

            mitigations.add("❌ DO NOT CONNECT to this network under any circumstances!");
            mitigations.add("🛡️ Turn off Wi-Fi auto-connect in Android settings for public networks.");

        } else if ("OPEN".equals(secType) || assessment.getTotalScore() < 50) {
            threatLevel = "HIGH RISK THREAT";
            attacks.add("⚠️ Passive Wireless Packet Sniffing: Unencrypted radio waves allow anyone nearby with Wireshark to capture raw HTTP traffic.");
            attacks.add("⚠️ Man-In-The-Middle (MITM) ARP Spoofing: Attackers on the open Wi-Fi can manipulate network routing tables.");
            attacks.add("⚠️ Session Hijacking / Cookie Theft: Unencrypted session cookies can be stolen to hijack web accounts.");
            attacks.add("⚠️ Rogue Captive Portal Injection: Attacker can inject pop-ups or redirect browser to malware downloads.");

            mitigations.add("🛡️ Use a trustworthy VPN (Virtual Private Network) to encrypt all outbound traffic.");
            mitigations.add("🛡️ Avoid typing passwords, credit cards, or opening banking apps on OPEN Wi-Fi.");
            mitigations.add("🔒 Ensure web URLs start with HTTPS (SSL/TLS).");

        } else if (assessment.getTotalScore() < 80) {
            threatLevel = "MODERATE THREAT";
            if (caps.contains("WPS")) {
                attacks.add("⚠️ WPS PIN Brute-Force Attack (Reaver/Bully): Wi-Fi Protected Setup allows automated PIN cracking within hours.");
            }
            if (caps.contains("TKIP")) {
                attacks.add("⚠️ TKIP Cryptographic Flaw: Legacy TKIP encryption is vulnerable to packet injection attacks.");
            }
            if (!secType.contains("WPA3")) {
                attacks.add("⚠️ WPA2 4-Way Handshake Capture: Attackers can capture WPA2 handshakes to attempt offline password cracking.");
            }

            mitigations.add("🛡️ Verify the router password is 12+ characters long with mixed symbols.");
            mitigations.add("🛡️ Disable WPS (Wi-Fi Protected Setup) in the router admin panel.");

        } else {
            threatLevel = "LOW THREAT (SECURE)";
            attacks.add("✅ Strong Encryption Protection: WPA2-AES / WPA3-SAE prevents passive packet sniffing and eavesdropping.");
            attacks.add("✅ Protected Unicast & Multicast Traffic: Wireless packets are cryptographically isolated per device.");

            mitigations.add("✓ Maintain standard security hygiene and keep device operating system updated.");
        }

        return new ThreatProfile(threatLevel, attacks, mitigations);
    }
}
