package com.smartshield.wifi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OuiVendorLookupTest {

    @Test
    public void testCiscoVendorLookup() {
        String vendor = OuiVendorLookup.getVendorName("00:00:0C:12:34:56");
        assertEquals("Cisco Systems", vendor);
    }

    @Test
    public void testTpLinkVendorLookup() {
        String vendor = OuiVendorLookup.getVendorName("50:C7:BF:AA:BB:CC");
        assertEquals("TP-Link", vendor);
    }

    @Test
    public void testRaspberryPiRogueHardware() {
        String bssid = "B8:27:EB:11:22:33";
        String vendor = OuiVendorLookup.getVendorName(bssid);
        assertEquals("Raspberry Pi Foundation", vendor);
        assertTrue(OuiVendorLookup.isPotentialRogueHardware(bssid));
    }

    @Test
    public void testEspressifRogueHardware() {
        String bssid = "24:0A:C4:DE:FA:01";
        String vendor = OuiVendorLookup.getVendorName(bssid);
        assertEquals("Espressif Systems (ESP8266/ESP32)", vendor);
        assertTrue(OuiVendorLookup.isPotentialRogueHardware(bssid));
    }

    @Test
    public void testStandardRouterNotRogue() {
        String bssid = "00:14:6C:99:88:77";
        assertFalse(OuiVendorLookup.isPotentialRogueHardware(bssid));
    }
}
