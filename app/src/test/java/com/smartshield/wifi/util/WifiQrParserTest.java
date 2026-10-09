package com.smartshield.wifi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.smartshield.wifi.domain.TrustAssessment;

import org.junit.Test;

public class WifiQrParserTest {

    @Test
    public void testParseWpa2QrString() {
        String qrContent = "WIFI:S:Campus_WiFi;T:WPA;P:SecretPass123;H:false;;";
        WifiQrParser.QrWifiPayload payload = WifiQrParser.parse(qrContent);

        assertNotNull(payload);
        assertEquals("Campus_WiFi", payload.ssid);
        assertEquals("WPA", payload.securityType);
        assertEquals("SecretPass123", payload.password);

        TrustAssessment assessment = WifiQrParser.evaluateQrCode(payload);
        assertNotNull(assessment);
        assertTrue(assessment.getTotalScore() >= 50);
    }

    @Test
    public void testParseOpenQrString() {
        String qrContent = "WIFI:S:Free_Airport_WiFi;T:nopass;P:;;";
        WifiQrParser.QrWifiPayload payload = WifiQrParser.parse(qrContent);

        assertNotNull(payload);
        assertEquals("Free_Airport_WiFi", payload.ssid);

        TrustAssessment assessment = WifiQrParser.evaluateQrCode(payload);
        assertNotNull(assessment);

        // Verify Open network security deduction
        boolean foundWarning = false;
        for (String reason : assessment.getExplainableReasons()) {
            if (reason.contains("OPEN")) {
                foundWarning = true;
                break;
            }
        }
        assertTrue(foundWarning);
    }
}
