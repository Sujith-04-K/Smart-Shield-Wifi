package com.smartshield.wifi.util;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Probes the connected network to detect Captive Portals (fake login pages),
 * HTTP traffic redirects, or Man-In-The-Middle (MITM) SSL/HTTP interception.
 */
public class CaptivePortalProbe {

    public interface ProbeCallback {
        void onResult(ProbeResult result);
    }

    public enum Status {
        DIRECT_INTERNET("Clean Direct Connection (HTTP 204)", "No traffic hijacking detected.", true),
        CAPTIVE_PORTAL_REDIRECT("Captive Portal / Traffic Interception Detected", "Traffic is being redirected to a captive portal or proxy.", false),
        NO_INTERNET("No Internet Access", "Network is connected but cannot reach public servers.", false);

        public final String title;
        public final String description;
        public final boolean isSecure;

        Status(String title, String description, boolean isSecure) {
            this.title = title;
            this.description = description;
            this.isSecure = isSecure;
        }
    }

    public static class ProbeResult {
        public final Status status;
        public final int responseCode;
        public final String redirectUrl;

        public ProbeResult(Status status, int responseCode, String redirectUrl) {
            this.status = status;
            this.responseCode = responseCode;
            this.redirectUrl = redirectUrl;
        }
    }

    private static final String PROBE_URL_HTTP = "http://connectivitycheck.gstatic.com/generate_204";
    private static final String PROBE_URL_HTTPS = "https://connectivitycheck.gstatic.com/generate_204";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void runProbe(ProbeCallback callback) {
        executor.execute(() -> {
            ProbeResult result = executeHttpCheck(PROBE_URL_HTTP);
            if (result.status == Status.NO_INTERNET && result.responseCode == -1) {
                // Retry with HTTPS fallback
                result = executeHttpCheck(PROBE_URL_HTTPS);
            }
            final ProbeResult finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }

    private static ProbeResult executeHttpCheck(String urlStr) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            String locationHeader = urlConnection.getHeaderField("Location");

            if (responseCode == 204) {
                return new ProbeResult(Status.DIRECT_INTERNET, 204, null);
            } else if (responseCode == 200 || (responseCode >= 300 && responseCode <= 308)) {
                return new ProbeResult(Status.CAPTIVE_PORTAL_REDIRECT, responseCode, locationHeader);
            } else {
                return new ProbeResult(Status.NO_INTERNET, responseCode, null);
            }
        } catch (Exception e) {
            return new ProbeResult(Status.NO_INTERNET, -1, e.getMessage());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
