package com.smartshield.wifi;

import org.junit.Test;
import static org.junit.Assert.*;

import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.TrustAssessmentEngine;
import com.smartshield.wifi.domain.WifiNetwork;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests verifying the Weighted Multi-Criteria Decision Assessment (MCDA)
 * scoring formula (Security 40% + Similarity 30% + Known 20% + Signal 10% = 100 points)
 * and XAI explainable reason generation ("Why Should I Trust This Network?").
 */
public class TrustAssessmentEngineTest {

    @Test
    public void testEvaluate_PerfectWpa3KnownNetwork_Scores100Safe() {
        WifiNetwork wpa3Network = new WifiNetwork("Enterprise_SAE", "11:22:33:44:55:66", -52, 5240, "WPA3", "[WPA3-SAE]", System.currentTimeMillis());

        TrustAssessment assessment = TrustAssessmentEngine.evaluate(wpa3Network, Collections.emptyList(), true);

        assertEquals("Security score should be 40 for WPA3", 40, assessment.getSecurityScore());
        assertEquals("Similarity score should be 30 for safe SSID", 30, assessment.getSimilarityScore());
        assertEquals("Known score should be 20 for verified network", 20, assessment.getKnownScore());
        assertEquals("Signal score should be 10 for strong RSSI", 10, assessment.getSignalScore());
        assertEquals("Total trust score should be 100", 100, assessment.getTotalScore());
        assertEquals("Risk level should be SAFE", "SAFE", assessment.getRiskLevel());
        assertFalse("Should not detect Evil Twin", assessment.isEvilTwinDetected());

        List<String> reasons = assessment.getExplainableReasons();
        assertFalse("Explainable reasons must not be empty", reasons.isEmpty());
        assertTrue("Should contain WPA3 reason", reasons.get(0).contains("WPA3 Security Protocol"));
    }

    @Test
    public void testEvaluate_StandardWpa2KnownNetwork_Scores90Safe() {
        WifiNetwork wpa2Network = new WifiNetwork("Home_Wifi", "22:33:44:55:66:77", -60, 2412, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());

        TrustAssessment assessment = TrustAssessmentEngine.evaluate(wpa2Network, Collections.emptyList(), true);

        assertEquals("Security score should be 30 for WPA2", 30, assessment.getSecurityScore());
        assertEquals("Total score should be 90", 90, assessment.getTotalScore());
        assertEquals("Risk level should be SAFE", "SAFE", assessment.getRiskLevel());
    }

    @Test
    public void testEvaluate_RogueOpenEvilTwin_ScoresHighRisk() {
        WifiNetwork rogueOpen = new WifiNetwork("Campus_WiFi_Free", "AA:BB:CC:DD:EE:FF", -58, 2412, "OPEN", "[]", System.currentTimeMillis());
        WifiNetwork legitWpa2 = new WifiNetwork("Campus_WiFi", "00:11:22:33:44:55", -65, 5240, "WPA2", "[WPA2-PSK]", System.currentTimeMillis());

        TrustAssessment assessment = TrustAssessmentEngine.evaluate(rogueOpen, Collections.singletonList(legitWpa2), false);

        assertEquals("Security score should be 0 for OPEN network", 0, assessment.getSecurityScore());
        assertEquals("Similarity score should be 0 when Evil Twin is detected", 0, assessment.getSimilarityScore());
        assertEquals("Known score should be 20 for unknown network", 20, assessment.getKnownScore());
        assertTrue("Evil twin flag must be true", assessment.isEvilTwinDetected());
        assertEquals("Risk level must be HIGH_RISK", "HIGH_RISK", assessment.getRiskLevel());

        boolean hasEvilTwinReason = false;
        for (String reason : assessment.getExplainableReasons()) {
            if (reason.contains("Evil Twin Attack Detected")) {
                hasEvilTwinReason = true;
                break;
            }
        }
        assertTrue("Explainable reasons must include Critical Evil Twin alert", hasEvilTwinReason);
    }

    @Test
    public void testEvaluate_Wpa2EnterpriseNetwork_GetsEnterpriseBonus() {
        WifiNetwork enterprise = new WifiNetwork("Corp_Wifi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-EAP-CCMP]", System.currentTimeMillis());
        TrustAssessment assessment = TrustAssessmentEngine.evaluate(enterprise, Collections.emptyList(), true);

        // Base is WPA2 (30) + EAP bonus (5) = 35
        assertEquals("Security score should be 35 due to Enterprise EAP bonus", 35, assessment.getSecurityScore());
        boolean hasEnterpriseReason = false;
        for (String r : assessment.getExplainableReasons()) {
            if (r.contains("Enterprise-Grade Authentication")) {
                hasEnterpriseReason = true;
                break;
            }
        }
        assertTrue("Reasons should explain Enterprise bonus", hasEnterpriseReason);
    }

    @Test
    public void testEvaluate_Wpa2WithLegacyTkip_DeductsPoints() {
        WifiNetwork legacyTkip = new WifiNetwork("Legacy_Wifi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-PSK-TKIP]", System.currentTimeMillis());
        TrustAssessment assessment = TrustAssessmentEngine.evaluate(legacyTkip, Collections.emptyList(), true);

        // Base WPA2 (30) - TKIP penalty (10) = 20
        assertEquals("Security score should be 20 due to legacy TKIP penalty", 20, assessment.getSecurityScore());
    }

    @Test
    public void testEvaluate_Wpa2WithWpsEnabled_DeductsPoints() {
        WifiNetwork wpsNetwork = new WifiNetwork("Wps_Wifi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-PSK-CCMP][WPS]", System.currentTimeMillis());
        TrustAssessment assessment = TrustAssessmentEngine.evaluate(wpsNetwork, Collections.emptyList(), true);

        // Base WPA2 (30) - WPS penalty (5) = 25
        assertEquals("Security score should be 25 due to WPS vulnerability", 25, assessment.getSecurityScore());
    }

    @Test
    public void testEvaluate_AdHocNetwork_DeductsPoints() {
        WifiNetwork adHoc = new WifiNetwork("AdHoc_Wifi", "00:11:22:33:44:55", -55, 5240, "WPA2", "[WPA2-PSK-CCMP][IBSS]", System.currentTimeMillis());
        TrustAssessment assessment = TrustAssessmentEngine.evaluate(adHoc, Collections.emptyList(), true);

        // Base WPA2 (30) - Ad-hoc penalty (15) = 15
        assertEquals("Security score should be 15 due to Ad-Hoc mode penalty", 15, assessment.getSecurityScore());
    }
}
