package com.smartshield.wifi.presentation.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smartshield.wifi.R;
import com.smartshield.wifi.SmartShieldApp;
import com.smartshield.wifi.data.KnownNetworkEntity;
import com.smartshield.wifi.data.ScanHistoryEntity;
import com.smartshield.wifi.data.SmartShieldDatabase;
import com.smartshield.wifi.data.WifiRepository;
import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.WifiNetwork;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Detail Activity providing Explainable Trust Assessment (XAI) for a specific Wi-Fi access point.
 * Shows criteria score breakdowns ("Why Should I Trust This Network?") and allows users
 * to add/remove access points from their SmartShield Verified Registry.
 */
public class NetworkDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ASSESSMENT = "extra_trust_assessment";

    private WifiRepository repository;
    private TrustAssessment assessment;
    private final AtomicBoolean isVerified = new AtomicBoolean(false);

    private TextView tvDetailSsid;
    private TextView tvDetailBssid;
    private TextView tvDetailScoreBadge;
    private TextView tvDetailFreq;
    private TextView tvDetailConfidence;
    private TextView tvDetailTimestamp;
    private LinearLayout containerReasons;
    private MaterialButton btnToggleVerified;
    private MaterialButton btnSaveHistory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_detail);

        SmartShieldApp app = (SmartShieldApp) getApplication();
        repository = app.getWifiRepository();

        assessment = (TrustAssessment) getIntent().getSerializableExtra(EXTRA_ASSESSMENT);
        if (assessment == null) {
            finish();
            return;
        }

        initViews();
        populateData();
        checkVerifiedStatus();
    }

    private void initViews() {
        Toolbar toolbarDetail = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbarDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbarDetail.setNavigationOnClickListener(v -> onBackPressed());

        tvDetailSsid = findViewById(R.id.tvDetailSsid);
        tvDetailBssid = findViewById(R.id.tvDetailBssid);
        tvDetailScoreBadge = findViewById(R.id.tvDetailScoreBadge);
        tvDetailFreq = findViewById(R.id.tvDetailFreq);
        tvDetailConfidence = findViewById(R.id.tvDetailConfidence);
        tvDetailTimestamp = findViewById(R.id.tvDetailTimestamp);
        containerReasons = findViewById(R.id.containerReasons);
        btnToggleVerified = findViewById(R.id.btnToggleVerified);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
    }

    private void populateData() {
        WifiNetwork net = assessment.getNetwork();
        tvDetailSsid.setText(net.getSsid());
        tvDetailBssid.setText(String.format("BSSID: %s  •  Security: %s", net.getBssid(), net.getSecurityType()));
        tvDetailFreq.setText(String.format("%s (%d MHz)", net.getBandString(), net.getFrequency()));
        tvDetailConfidence.setText("Confidence: " + assessment.getConfidenceLevel().toUpperCase());
        tvDetailTimestamp.setText("Assessment Time: " + assessment.getFormattedTimestamp());

        int score = assessment.getTotalScore();
        String risk = assessment.getRiskLevel();

        if (assessment.isEvilTwinDetected()) {
            tvDetailScoreBadge.setText("EVIL TWIN ALERT • " + score + "/100");
            tvDetailScoreBadge.setBackgroundResource(R.drawable.bg_badge_high);
            tvDetailScoreBadge.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
        } else if ("SAFE".equals(risk)) {
            tvDetailScoreBadge.setText(score + "/100 • SAFE");
            tvDetailScoreBadge.setBackgroundResource(R.drawable.bg_badge_safe);
            tvDetailScoreBadge.setTextColor(ContextCompat.getColor(this, R.color.risk_safe));
        } else if ("MEDIUM_RISK".equals(risk)) {
            tvDetailScoreBadge.setText(score + "/100 • MEDIUM RISK");
            tvDetailScoreBadge.setBackgroundResource(R.drawable.bg_badge_medium);
            tvDetailScoreBadge.setTextColor(ContextCompat.getColor(this, R.color.risk_medium));
        } else {
            tvDetailScoreBadge.setText(score + "/100 • HIGH RISK");
            tvDetailScoreBadge.setBackgroundResource(R.drawable.bg_badge_high);
            tvDetailScoreBadge.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
        }

        // Dynamically populate "Why Should I Trust This Network?" criteria checklist
        containerReasons.removeAllViews();
        List<String> reasons = assessment.getExplainableReasons();
        for (String reason : reasons) {
            TextView tvReason = new TextView(this);
            tvReason.setText(reason);
            tvReason.setTextSize(14f);
            tvReason.setPadding(0, 8, 0, 8);
            if (reason.startsWith("✓")) {
                tvReason.setTextColor(ContextCompat.getColor(this, R.color.risk_safe));
            } else if (reason.startsWith("✗")) {
                tvReason.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
                tvReason.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvReason.setTextColor(ContextCompat.getColor(this, R.color.risk_medium));
            }
            containerReasons.addView(tvReason);
        }

        LinearLayout containerAttacks = findViewById(R.id.containerAttacks);
        if (containerAttacks != null) {
            com.smartshield.wifi.domain.ThreatAnalysisEngine.ThreatProfile profile =
                    com.smartshield.wifi.domain.ThreatAnalysisEngine.analyzeThreats(assessment);

            for (String attack : profile.possibleAttacks) {
                TextView tvAttack = new TextView(this);
                tvAttack.setText(attack);
                tvAttack.setTextSize(13.5f);
                tvAttack.setPadding(0, 8, 0, 8);
                if (attack.startsWith("🚨")) {
                    tvAttack.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
                    tvAttack.setTypeface(null, android.graphics.Typeface.BOLD);
                } else if (attack.startsWith("⚠️")) {
                    tvAttack.setTextColor(ContextCompat.getColor(this, R.color.risk_medium));
                } else {
                    tvAttack.setTextColor(ContextCompat.getColor(this, R.color.risk_safe));
                }
                containerAttacks.addView(tvAttack);
            }
        }

        btnToggleVerified.setOnClickListener(v -> toggleVerifiedNetwork());

        btnSaveHistory.setOnClickListener(v -> saveToHistory());

        MaterialButton btnExportPdf = findViewById(R.id.btnExportPdf);
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                try {
                    java.io.File pdfFile = com.smartshield.wifi.util.PdfReportGenerator.generateSecurityReport(this, assessment);
                    Toast.makeText(this, "PDF Security Report saved: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                    
                    // Share / Open PDF Intent
                    android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            pdfFile
                    );
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, pdfUri);
                    shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(android.content.Intent.createChooser(shareIntent, "Share Security Audit PDF"));
                } catch (Exception e) {
                    Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkVerifiedStatus() {
        String bssid = assessment.getNetwork().getBssid();
        SmartShieldDatabase.databaseWriteExecutor.execute(() -> {
            boolean known = repository.isNetworkKnownSync(bssid);
            isVerified.set(known);
            runOnUiThread(this::updateVerifiedButtonUI);
        });
    }

    private void updateVerifiedButtonUI() {
        if (isVerified.get()) {
            btnToggleVerified.setText(R.string.unmark_verified);
            btnToggleVerified.setIconResource(android.R.drawable.ic_delete);
        } else {
            btnToggleVerified.setText(R.string.mark_as_verified);
            btnToggleVerified.setIconResource(android.R.drawable.ic_menu_save);
        }
    }

    private void toggleVerifiedNetwork() {
        WifiNetwork net = assessment.getNetwork();
        if (isVerified.get()) {
            repository.deleteKnownNetwork(net.getBssid());
            isVerified.set(false);
            Toast.makeText(this, "Removed from SmartShield Verified Registry", Toast.LENGTH_SHORT).show();
        } else {
            KnownNetworkEntity entity = new KnownNetworkEntity(
                    net.getBssid(),
                    net.getSsid(),
                    System.currentTimeMillis(),
                    true
            );
            repository.insertKnownNetwork(entity);
            isVerified.set(true);
            Toast.makeText(this, "Marked as Verified Trusted Network!", Toast.LENGTH_SHORT).show();
        }
        updateVerifiedButtonUI();
    }

    private void saveToHistory() {
        WifiNetwork net = assessment.getNetwork();
        StringBuilder reasonsBuilder = new StringBuilder();
        for (String r : assessment.getExplainableReasons()) {
            reasonsBuilder.append(r).append("; ");
        }

        ScanHistoryEntity history = new ScanHistoryEntity(
                net.getSsid(),
                net.getBssid(),
                assessment.getTotalScore(),
                assessment.getRiskLevel(),
                System.currentTimeMillis(),
                reasonsBuilder.toString(),
                assessment.getSecurityScore(),
                assessment.getSimilarityScore(),
                assessment.getKnownScore(),
                assessment.getSignalScore()
        );
        repository.insertScanHistory(history);
        Toast.makeText(this, "Assessment saved to scan history logs!", Toast.LENGTH_SHORT).show();
    }
}
