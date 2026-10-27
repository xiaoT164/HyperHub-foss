package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;

public class SplashView extends View {

    private static final long TOTAL_MS = 2600L;
    private static final long FRAME_MS = 16L;

    private static final float T_ICON_IN_S  = 0.00f;
    private static final float T_ICON_IN_E  = 0.18f;
    private static final float T_TITLE_IN_S = 0.10f;
    private static final float T_TITLE_IN_E = 0.28f;
    private static final float T_BAR_IN_S   = 0.22f;
    private static final float T_BAR_IN_E   = 0.34f;
    private static final float T_FADE_S     = 0.77f;
    private static final float T_FADE_E     = 1.00f;

    private static final int   BLOB_COUNT  = 4;
    private static final int[] BLOB_COLORS = { 0xFF4A90FF, 0xFF9B59FF, 0xFFFF5E8A, 0xFF00D4FF };
    private static final float[] BLOB_X0   = { 0.15f, 0.75f, 0.35f, 0.85f };
    private static final float[] BLOB_Y0   = { 0.20f, 0.30f, 0.70f, 0.60f };
    private static final float[] BLOB_SPD  = { 0.09f, 0.07f, 0.11f, 0.08f };
    private static final float[] BLOB_PH   = { 0.00f, 1.57f, 3.14f, 4.71f };
    private static final float[] BLOB_RAD  = { 0.55f, 0.50f, 0.48f, 0.45f };
    private static final int[]   BLOB_ALPHA= { 38, 32, 28, 30 };
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 16;

    private float  progress   = 0f;
    private float  auroraTime = 0f;
    private long   startTime  = -1L;

    private final Handler handler = new Handler();

    private final Paint bgPaint   = new Paint();
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (startTime < 0) startTime = System.currentTimeMillis();
            long elapsed = System.currentTimeMillis() - startTime;
            progress    = Math.min(1f, elapsed / (float) TOTAL_MS);
            auroraTime += 0.012f;
            invalidate();
            if (progress < 1f) handler.postDelayed(this, FRAME_MS);
        }
    };

    public SplashView(Context context) {
        super(context);
        bgPaint.setColor(0xFF080C14);
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static float norm(float t, float s, float e) {
        if (t <= s) return 0f;
        if (t >= e) return 1f;
        return (t - s) / (e - s);
    }
    private static float ease(float t)    { return t * t * (3f - 2f * t); }
    private static float easeOut(float t) { return 1f - (1f - t) * (1f - t); }
    private float dp(float v) {
        return v * getContext().getResources().getDisplayMetrics().density;
    }

    // ── onDraw ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        final float W  = getWidth();
        final float H  = getHeight();
        if (W == 0) return;

        final float p  = progress;
        final float cx = W / 2f;
        final float cy = H / 2f;

        final float fadeAlpha = 1f - ease(norm(p, T_FADE_S, T_FADE_E));

        canvas.drawRect(0, 0, W, H, bgPaint);

        // 2. Aurora blobs
        float minSide = Math.min(W, H);
        for (int i = 0; i < BLOB_COUNT; i++) {
            float bx = W * (BLOB_X0[i] + 0.18f * (float) Math.sin(auroraTime * BLOB_SPD[i] + BLOB_PH[i]));
            float by = H * (BLOB_Y0[i] + 0.14f * (float) Math.cos(auroraTime * BLOB_SPD[i] * 0.77f + BLOB_PH[i] + 0.5f));
            float r  = minSide * BLOB_RAD[i];
            int   col = BLOB_COLORS[i];
            float pulse = (float)(Math.sin(auroraTime * 0.5f + i * 1.3f) * 0.20f + 0.80f);
            int   alpha = (int)(BLOB_ALPHA[i] * pulse * fadeAlpha);
            blobPaint.setShader(new RadialGradient(bx, by, r,
                new int[]{ Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col)),
                           Color.argb(0, Color.red(col), Color.green(col), Color.blue(col)) },
                new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(bx - r, by - r * 0.72f, bx + r, by + r * 0.72f), blobPaint);
        }
        blobPaint.setShader(null);

        drawGrid(canvas, W, H, fadeAlpha);

        float iconRaw   = ease(norm(p, T_ICON_IN_S, T_ICON_IN_E));
        float iconAlpha = iconRaw * fadeAlpha;
        float iconRise  = easeOut(iconRaw) * dp(12);
        float iconR     = minSide * 0.13f;
        float iconCy    = cy * 0.80f - iconRise;
        drawHHIcon(canvas, cx, iconCy, iconR, iconAlpha, auroraTime);

        float titleAlpha = ease(norm(p, T_TITLE_IN_S, T_TITLE_IN_E)) * fadeAlpha;
        if (titleAlpha > 0f) drawTitle(canvas, cx, iconCy + iconR + dp(28), W, titleAlpha);

        float barAlpha = ease(norm(p, T_BAR_IN_S, T_BAR_IN_E)) * fadeAlpha;
        if (barAlpha > 0f) drawProgressBar(canvas, cx, H * 0.87f, W * 0.52f, p, barAlpha);
    }

    // ── Aurora Grid ───────────────────────────────────────────────────────────

    private void drawGrid(Canvas canvas, float W, float H, float a) {
        float cellW = W / GRID_COLS;
        float cellH = H / GRID_ROWS;
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.7f);
        for (int c = 0; c <= GRID_COLS; c++) {
            float x = c * cellW;
            int alpha = (int)((7 + 6 * Math.abs(Math.sin(auroraTime * 0.3f + c * 0.35f))) * a);
            gridPaint.setColor(Color.argb(alpha, 120, 180, 255));
            canvas.drawLine(x, 0, x, H, gridPaint);
        }
        for (int r = 0; r <= GRID_ROWS; r++) {
            float y = r * cellH;
            int alpha = (int)((7 + 5 * Math.abs(Math.sin(auroraTime * 0.25f + r * 0.28f))) * a);
            gridPaint.setColor(Color.argb(alpha, 120, 180, 255));
            canvas.drawLine(0, y, W, y, gridPaint);
        }
        nodePaint.setStyle(Paint.Style.FILL);
        for (int c = 0; c <= GRID_COLS; c++) {
            for (int r = 0; r <= GRID_ROWS; r++) {
                float bright = (float)(Math.sin(auroraTime * 0.6f + c * 0.9f + r * 0.7f) * 0.5f + 0.5f);
                int alpha = (int)((8 + bright * 22) * a);
                if (alpha < 4) continue;
                nodePaint.setColor(Color.argb(alpha, 160, 210, 255));
                canvas.drawCircle(c * cellW, r * cellH, 0.9f + bright * 1.2f, nodePaint);
            }
        }
    }

    private void drawHHIcon(Canvas canvas, float cx, float cy, float r, float alpha, float t) {
        if (alpha <= 0f) return;

        glowPaint.setShader(new RadialGradient(cx, cy, r * 2.2f,
            new int[]{ Color.argb((int)(55 * alpha), 74, 144, 255),
                       Color.argb(0, 74, 144, 255) },
            new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 2.2f, glowPaint);
        glowPaint.setShader(null);

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setShader(new RadialGradient(cx, cy - r * 0.3f, r * 1.4f,
            new int[]{ Color.argb((int)(80 * alpha), 100, 160, 255),
                       Color.argb((int)(30 * alpha), 20,  40,  90) },
            new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, circlePaint);
        circlePaint.setShader(null);

        float strokeW = dp(1.5f);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(strokeW);
        circlePaint.setShader(new android.graphics.SweepGradient(cx, cy,
            new int[]{ Color.argb((int)(200 * alpha), 74, 144, 255),
                       Color.argb((int)(200 * alpha), 0, 212, 255),
                       Color.argb((int)(200 * alpha), 155, 89, 255),
                       Color.argb((int)(200 * alpha), 74, 144, 255) },
            null));
        canvas.drawCircle(cx, cy, r - strokeW / 2f, circlePaint);
        circlePaint.setShader(null);

        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setShader(new RadialGradient(
            cx - r * 0.28f, cy - r * 0.35f, r * 0.55f,
            new int[]{ Color.argb((int)(60 * alpha), 255, 255, 255),
                       Color.argb(0, 255, 255, 255) },
            new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.35f, r * 0.55f, highlightPaint);

        float textSize = r * 0.90f;
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setLetterSpacing(-0.05f);

        float gradShift = (float) Math.sin(t * 1.2f) * dp(10);
        textPaint.setShader(new LinearGradient(
            cx - r + gradShift, cy,
            cx + r + gradShift, cy,
            new int[]{ 0xFF4A90FF, 0xFF00D4FF, 0xFF9B59FF },
            new float[]{ 0f, 0.5f, 1f },
            Shader.TileMode.CLAMP));
        textPaint.setAlpha((int)(255 * alpha));
        canvas.drawText("HH", cx, cy + textSize * 0.36f, textPaint);
        textPaint.setShader(null);
    }

    private void drawTitle(Canvas canvas, float cx, float y, float W, float alpha) {
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        textPaint.setTextSize(dp(32));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setLetterSpacing(-0.02f);
        textPaint.setShader(new LinearGradient(
            cx - dp(80), y, cx + dp(80), y,
            new int[]{ 0xFF4A90FF, 0xFF00D4FF, 0xFF9B59FF },
            new float[]{ 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
        textPaint.setAlpha((int)(255 * alpha));
        canvas.drawText("HyperHub", cx, y, textPaint);
        textPaint.setShader(null);

        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setTextSize(dp(12));
        textPaint.setLetterSpacing(0.06f);
        textPaint.setColor(Color.argb((int)(140 * alpha), 160, 185, 215));
        canvas.drawText("Утилиты оптимизации HyperOS", cx, y + dp(22), textPaint);
    }

    private void drawProgressBar(Canvas canvas, float cx, float y,
                                  float halfW, float p, float alpha) {
        float fill  = ease(norm(p, T_BAR_IN_E, T_FADE_S));
        float pulse = (float)(Math.sin(auroraTime * 4f) * 0.15f + 0.85f);

        float left  = cx - halfW;
        float right = cx + halfW;
        float r     = dp(1f);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setShader(null);
        barPaint.setColor(Color.argb((int)(30 * alpha), 255, 255, 255));
        canvas.drawRoundRect(new RectF(left, y - r, right, y + r), r, r, barPaint);

        if (fill > 0.01f) {
            float fillRight = left + (right - left) * fill;

            barPaint.setShader(new LinearGradient(left, y, fillRight, y,
                new int[]{ 0xFF4A90FF, 0xFF00D4FF }, null, Shader.TileMode.CLAMP));
            barPaint.setAlpha((int)(220 * alpha * pulse));
            canvas.drawRoundRect(new RectF(left, y - r, fillRight, y + r), r, r, barPaint);
            barPaint.setShader(null);

            glowPaint.setShader(new RadialGradient(fillRight, y, dp(8),
                new int[]{ Color.argb((int)(180 * alpha * pulse), 0, 212, 255),
                           Color.argb(0, 0, 212, 255) },
                new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
            canvas.drawCircle(fillRight, y, dp(8), glowPaint);
            glowPaint.setShader(null);
        }
    }
}
