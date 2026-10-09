package com.smartshield.wifi.presentation.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.smartshield.wifi.domain.WifiNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Ultra-modern, Cyberpunk/Neo-Glass Radio Spectrum View rendering glowing parabolic
 * frequency curves, gradient fills, dBm level grids, and peak signal indicators.
 */
public class ChannelSpectrumView extends View {

    private final List<WifiNetwork> networks = new ArrayList<>();
    private Paint axisPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint curveStrokePaint;
    private Paint curveFillPaint;
    private Paint glowDotPaint;
    private Paint tagBgPaint;

    private boolean is24GhzBand = true;

    // Futuristic Neon Palette
    private final int[][] NEON_PALETTE = {
            {Color.parseColor("#00F2FE"), Color.parseColor("#4FACFE")}, // Electric Cyan
            {Color.parseColor("#10B981"), Color.parseColor("#34D399")}, // Emerald Neon
            {Color.parseColor("#F59E0B"), Color.parseColor("#FBBF24")}, // Amber Glow
            {Color.parseColor("#EC4899"), Color.parseColor("#F472B6")}, // Neon Pink
            {Color.parseColor("#8B5CF6"), Color.parseColor("#A78BFA")}, // Purple Laser
            {Color.parseColor("#06B6D4"), Color.parseColor("#67E8F9")}  // Cyan Flare
    };

    public ChannelSpectrumView(Context context) {
        super(context);
        init();
    }

    public ChannelSpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChannelSpectrumView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#334155"));
        axisPaint.setStrokeWidth(3f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#1E293B"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8, 8}, 0));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTextSize(24f);

        curveStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curveStrokePaint.setStyle(Paint.Style.STROKE);
        curveStrokePaint.setStrokeWidth(5f);

        curveFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curveFillPaint.setStyle(Paint.Style.FILL);

        glowDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowDotPaint.setStyle(Paint.Style.FILL);

        tagBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tagBgPaint.setColor(Color.parseColor("#151D2A"));
        tagBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setNetworks(List<WifiNetwork> newNetworks, boolean is24GhzBand) {
        this.networks.clear();
        this.is24GhzBand = is24GhzBand;
        if (newNetworks != null) {
            for (WifiNetwork net : newNetworks) {
                if (is24GhzBand && "2.4 GHz".equals(net.getBandString())) {
                    this.networks.add(net);
                } else if (!is24GhzBand && !"2.4 GHz".equals(net.getBandString())) {
                    this.networks.add(net);
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        int paddingLeft = 80;
        int paddingBottom = 60;
        int paddingTop = 50;
        int paddingRight = 40;

        int graphWidth = width - paddingLeft - paddingRight;
        int graphHeight = height - paddingTop - paddingBottom;
        float baseLineY = height - paddingBottom;

        // 1. Draw Background & Horizontal dBm Grids
        String[] dbmLabels = {"-30 dBm", "-60 dBm", "-90 dBm"};
        float[] dbmPositions = {paddingTop, paddingTop + graphHeight * 0.5f, baseLineY};

        for (int i = 0; i < dbmLabels.length; i++) {
            float y = dbmPositions[i];
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);

            textPaint.setColor(Color.parseColor("#64748B"));
            textPaint.setTextSize(20f);
            textPaint.setFakeBoldText(false);
            canvas.drawText(dbmLabels[i], 10, y + 6, textPaint);
        }

        // Base Axis Line
        axisPaint.setColor(Color.parseColor("#0284C7"));
        canvas.drawLine(paddingLeft, baseLineY, width - paddingRight, baseLineY, axisPaint);

        // 2. Draw Vertical Channel Grid Lines & Labels
        if (is24GhzBand) {
            for (int ch = 1; ch <= 14; ch++) {
                float x = paddingLeft + ((float) (ch - 1) / 13f) * graphWidth;

                if (ch == 1 || ch == 6 || ch == 11) {
                    textPaint.setColor(Color.parseColor("#38BDF8")); // Electric Cyan for non-overlapping
                    textPaint.setFakeBoldText(true);
                    textPaint.setTextSize(24f);
                } else {
                    textPaint.setColor(Color.parseColor("#475569"));
                    textPaint.setFakeBoldText(false);
                    textPaint.setTextSize(20f);
                }
                canvas.drawText(String.valueOf(ch), x - 6, baseLineY + 36, textPaint);
                canvas.drawLine(x, paddingTop, x, baseLineY, gridPaint);
            }
        } else {
            int[] fiveGhzChannels = {36, 40, 44, 48, 149, 153, 157, 161, 165};
            for (int i = 0; i < fiveGhzChannels.length; i++) {
                int ch = fiveGhzChannels[i];
                float x = paddingLeft + ((float) i / (fiveGhzChannels.length - 1)) * graphWidth;

                textPaint.setColor(Color.parseColor("#38BDF8"));
                textPaint.setFakeBoldText(true);
                textPaint.setTextSize(22f);
                canvas.drawText(String.valueOf(ch), x - 12, baseLineY + 36, textPaint);
                canvas.drawLine(x, paddingTop, x, baseLineY, gridPaint);
            }
        }

        // 3. Draw Parabolic Spectrum Curves with Glowing Gradient Fills & Peak Indicators
        int colorIdx = 0;

        for (WifiNetwork net : networks) {
            int ch = net.getChannelNumber();
            float centerX;

            if (is24GhzBand) {
                int clampedCh = Math.max(1, Math.min(14, ch));
                centerX = paddingLeft + ((float) (clampedCh - 1) / 13f) * graphWidth;
            } else {
                int[] fiveGhzChannels = {36, 40, 44, 48, 149, 153, 157, 161, 165};
                int idx = 0;
                for (int i = 0; i < fiveGhzChannels.length; i++) {
                    if (fiveGhzChannels[i] >= ch) {
                        idx = i;
                        break;
                    }
                }
                centerX = paddingLeft + ((float) idx / (fiveGhzChannels.length - 1)) * graphWidth;
            }

            int rssi = net.getRssi();
            float normalizedHeight = Math.max(0.15f, Math.min(1.0f, (rssi + 100) / 70f));
            float curvePeakY = baseLineY - (normalizedHeight * graphHeight);
            float curveWidthPx = is24GhzBand ? (graphWidth / 13f) * 2.5f : (graphWidth / 8f) * 2.2f;

            int[] colors = NEON_PALETTE[colorIdx % NEON_PALETTE.length];
            int strokeColor = colors[0];

            // Curve Path
            Path path = new Path();
            path.moveTo(centerX - curveWidthPx / 2f, baseLineY);
            path.cubicTo(
                    centerX - curveWidthPx / 4f, curvePeakY,
                    centerX + curveWidthPx / 4f, curvePeakY,
                    centerX + curveWidthPx / 2f, baseLineY
            );

            // Fill Path
            Path fillPath = new Path(path);
            fillPath.lineTo(centerX + curveWidthPx / 2f, baseLineY);
            fillPath.lineTo(centerX - curveWidthPx / 2f, baseLineY);
            fillPath.close();

            // Apply Gradient Fill
            LinearGradient fillShader = new LinearGradient(
                    centerX, curvePeakY, centerX, baseLineY,
                    new int[]{adjustAlpha(strokeColor, 0.45f), adjustAlpha(strokeColor, 0.05f)},
                    null, Shader.TileMode.CLAMP
            );
            curveFillPaint.setShader(fillShader);
            canvas.drawPath(fillPath, curveFillPaint);

            // Draw Neon Stroke Line
            curveStrokePaint.setColor(strokeColor);
            canvas.drawPath(path, curveStrokePaint);

            // Draw Glowing Peak Dot
            glowDotPaint.setColor(adjustAlpha(strokeColor, 0.35f));
            canvas.drawCircle(centerX, curvePeakY, 14f, glowDotPaint);

            glowDotPaint.setColor(strokeColor);
            canvas.drawCircle(centerX, curvePeakY, 7f, glowDotPaint);

            glowDotPaint.setColor(Color.WHITE);
            canvas.drawCircle(centerX, curvePeakY, 3f, glowDotPaint);

            // Draw Floating Tag Badge
            String tagText = net.getSsid() + " (" + rssi + " dBm)";
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(20f);
            textPaint.setFakeBoldText(true);
            float textWidth = textPaint.measureText(tagText);

            float tagLeft = centerX - textWidth / 2f - 10f;
            float tagTop = Math.max(paddingTop + 10f, curvePeakY - 38f);
            float tagRight = tagLeft + textWidth + 20f;
            float tagBottom = tagTop + 28f;

            RectF tagRect = new RectF(tagLeft, tagTop, tagRight, tagBottom);
            tagBgPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawRoundRect(tagRect, 8f, 8f, tagBgPaint);

            canvas.drawText(tagText, tagLeft + 10f, tagTop + 20f, textPaint);

            colorIdx++;
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
