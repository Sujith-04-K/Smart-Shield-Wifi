package com.smartshield.wifi.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.WifiNetwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for generating professional PDF Wi-Fi Security Audit Reports using native Android PdfDocument API.
 */
public class PdfReportGenerator {

    public static File generateSecurityReport(Context context, TrustAssessment assessment) throws IOException {
        WifiNetwork network = assessment.getNetwork();

        // 1. Create a PDF document (Standard A4 Page: 595 x 842 points)
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int startX = 40;
        int y = 50;

        // Header Background
        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.parseColor("#0F172A")); // Slate 900
        canvas.drawRect(0, 0, 595, 100, headerBgPaint);

        // Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SmartShield Wi-Fi Audit Report", startX, y, paint);

        // Subtitle & Timestamp
        y += 24;
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(Color.parseColor("#94A3B8"));
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        canvas.drawText("Generated on: " + dateStr + " | Mobile Computing Security Framework", startX, y, paint);

        // Reset Y for body
        y = 130;

        // Network Information Box
        Paint boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(1.5f);
        boxPaint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawRoundRect(startX, y, 555, y + 120, 8, 8, boxPaint);

        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Target Network Metadata", startX + 15, y + 25, paint);

        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(Color.parseColor("#334155"));

        canvas.drawText("SSID: " + network.getSsid(), startX + 15, y + 50, paint);
        canvas.drawText("BSSID: " + network.getBssid(), startX + 15, y + 68, paint);
        canvas.drawText("Security: " + network.getSecurityType(), startX + 15, y + 86, paint);
        canvas.drawText("Hardware Vendor: " + network.getVendorName(), startX + 15, y + 104, paint);

        canvas.drawText("Signal (RSSI): " + network.getRssi() + " dBm (" + network.getSignalPercentage() + "%)", startX + 280, y + 50, paint);
        canvas.drawText("Frequency: " + network.getFrequency() + " MHz (" + network.getBandString() + ")", startX + 280, y + 68, paint);
        canvas.drawText("Channel: Ch " + network.getChannelNumber(), startX + 280, y + 86, paint);

        y += 145;

        // Trust Assessment Badge & Overall Score
        int score = assessment.getTotalScore();
        Paint scoreBgPaint = new Paint();
        if (score >= 80) {
            scoreBgPaint.setColor(Color.parseColor("#166534")); // Dark Green
        } else if (score >= 50) {
            scoreBgPaint.setColor(Color.parseColor("#854D0E")); // Dark Yellow
        } else {
            scoreBgPaint.setColor(Color.parseColor("#991B1B")); // Dark Red
        }
        canvas.drawRoundRect(startX, y, 555, y + 60, 8, 8, scoreBgPaint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Trust Score: " + score + " / 100 (" + assessment.getRiskLevel() + ")", startX + 20, y + 36, paint);

        if (assessment.isEvilTwinDetected()) {
            paint.setTextSize(12);
            paint.setColor(Color.YELLOW);
            canvas.drawText("⚠️ EVIL TWIN ROGUE AP DETECTED", startX + 320, y + 36, paint);
        }

        y += 85;

        // Explainable Audit Checklist Header
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Multi-Criteria Explainable Security Audit", startX, y, paint);
        y += 15;

        // Audit Items Table
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        for (String reason : assessment.getExplainableReasons()) {
            if (reason.startsWith("✓")) {
                paint.setColor(Color.parseColor("#15803D")); // Green
            } else if (reason.startsWith("!")) {
                paint.setColor(Color.parseColor("#B45309")); // Amber
            } else {
                paint.setColor(Color.parseColor("#B91C1C")); // Red
            }

            // Word wrap long text
            if (reason.length() > 80) {
                canvas.drawText(reason.substring(0, 80), startX, y, paint);
                y += 16;
                canvas.drawText("    " + reason.substring(80), startX, y, paint);
            } else {
                canvas.drawText(reason, startX, y, paint);
            }
            y += 20;

            if (y > 770) break; // Prevent page overflow
        }

        y += 15;
        // Footer Note
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawLine(startX, y, 555, y, linePaint);

        y += 20;
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("SmartShield Wi-Fi Framework - Final-Year Mobile Computing Security Capstone", startX, y, paint);

        pdfDocument.finishPage(page);

        // Write PDF file to External Files or Cache Directory
        File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SmartShield_Reports");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        String fileName = "SmartShield_Report_" + network.getSsid().replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(pdfDir, fileName);

        FileOutputStream fos = new FileOutputStream(file);
        pdfDocument.writeTo(fos);
        pdfDocument.close();
        fos.close();

        return file;
    }
}
