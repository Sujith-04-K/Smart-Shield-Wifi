package com.smartshield.wifi.presentation.qr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.smartshield.wifi.R;
import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.util.WifiQrParser;

import java.io.InputStream;

/**
 * Activity for scanning live Wi-Fi QR Codes via camera, picking QR image from gallery,
 * or entering QR payload text manually to present immediate security evaluations.
 */
public class QrScannerActivity extends AppCompatActivity {

    private EditText etQrInput;
    private MaterialButton btnEvaluateQr;
    private MaterialButton btnLaunchCamera;
    private MaterialButton btnPickGallery;
    private MaterialCardView cardQrResult;
    private TextView tvQrResultTitle;
    private TextView tvQrScore;
    private TextView tvQrReasons;

    // ZXing Barcode Camera Launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    etQrInput.setText(result.getContents());
                    evaluateInput();
                } else {
                    Toast.makeText(this, "Camera scan cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Gallery Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    decodeQrFromUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        Toolbar toolbarQr = findViewById(R.id.toolbarQr);
        setSupportActionBar(toolbarQr);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbarQr.setNavigationOnClickListener(v -> finish());

        etQrInput = findViewById(R.id.etQrInput);
        btnEvaluateQr = findViewById(R.id.btnEvaluateQr);
        btnLaunchCamera = findViewById(R.id.btnLaunchCamera);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        cardQrResult = findViewById(R.id.cardQrResult);
        tvQrResultTitle = findViewById(R.id.tvQrResultTitle);
        tvQrScore = findViewById(R.id.tvQrScore);
        tvQrReasons = findViewById(R.id.tvQrReasons);

        // DO NOT pre-fill dummy data! Start clean.
        etQrInput.setText("");

        btnLaunchCamera.setOnClickListener(v -> launchCameraScanner());
        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnEvaluateQr.setOnClickListener(v -> evaluateInput());

        // Automatically launch camera scanner on screen start
        if (savedInstanceState == null) {
            launchCameraScanner();
        }
    }

    private void launchCameraScanner() {
        ScanOptions options = new ScanOptions();
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setOrientationLocked(true);
        options.setPrompt("Align Wi-Fi QR Code within the frame");
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        barcodeLauncher.launch(options);
    }

    private void decodeQrFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                return;
            }

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(binaryBitmap);
            if (result != null && result.getText() != null) {
                etQrInput.setText(result.getText());
                evaluateInput();
                Toast.makeText(this, "QR Code detected from image!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No QR Code found in selected image", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Could not decode QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void evaluateInput() {
        String input = etQrInput.getText() != null ? etQrInput.getText().toString().trim() : "";
        if (input.isEmpty()) {
            Toast.makeText(this, "Please scan or enter a Wi-Fi QR payload string", Toast.LENGTH_SHORT).show();
            return;
        }

        WifiQrParser.QrWifiPayload payload = WifiQrParser.parse(input);
        if (payload == null) {
            Toast.makeText(this, "Invalid Wi-Fi QR payload. Format should start with WIFI:S:...", Toast.LENGTH_LONG).show();
            return;
        }

        TrustAssessment assessment = WifiQrParser.evaluateQrCode(payload);
        if (assessment != null) {
            cardQrResult.setVisibility(View.VISIBLE);
            tvQrScore.setText("Trust Score: " + assessment.getTotalScore() + " / 100 (" + assessment.getRiskLevel() + ")");

            StringBuilder sb = new StringBuilder();
            sb.append("Target Network: ").append(payload.ssid).append("\n");
            sb.append("Security Cipher: ").append(payload.securityType).append("\n\n");

            sb.append("📋 Explainable Audit Rules:\n");
            for (String reason : assessment.getExplainableReasons()) {
                sb.append("• ").append(reason).append("\n");
            }

            com.smartshield.wifi.domain.ThreatAnalysisEngine.ThreatProfile profile =
                    com.smartshield.wifi.domain.ThreatAnalysisEngine.analyzeThreats(assessment);

            sb.append("\n🚨 Potential Security Attacks & Vulnerabilities:\n");
            for (String attack : profile.possibleAttacks) {
                sb.append("• ").append(attack).append("\n");
            }

            sb.append("\n🛡️ Security Mitigation:\n");
            for (String mit : profile.mitigationSteps) {
                sb.append("• ").append(mit).append("\n");
            }

            tvQrReasons.setText(sb.toString());
        }
    }
}
