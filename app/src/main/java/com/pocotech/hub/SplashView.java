package com.pocotech.hub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;

/**
 * Сплеш 2.1.0: фирменный градиент бренда, логотип из иконки лаунчера,
 * слоган CONNECTIVE INNOVATION. Без XP / достижений / таймеров активности.
 */
public class SplashView extends View {

    private static final int[] BRAND = {
        0xFF4F46E5, 0xFF8B5CF6, 0xFF22D3EE, 0xFF14B8A6
    };
    private static final long TOTAL_MS = 1500L;
    private static final long FRAME_MS = 16L;

    private float progress = 0f;
    private long startTime = -1L;
    private final Handler handler = new Handler();
    private final Paint bgPaint = new Paint();
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (startTime < 0) startTime = System.currentTimeMillis();
            progress = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) TOTAL_MS);
            invalidate();
            if (progress < 1f) handler.postDelayed(this, FRAME_MS);
        }
    };

    public SplashView(Context ctx) {
        super(ctx);
        bgPaint.setColor(0xFF080C14);
        handler.post(ticker);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas canvas) {
        final float W = getWidth(), H = getHeight();
        if (W == 0 || H == 0) return;
        canvas.drawRect(0, 0, W, H, bgPaint);

        final float minSide = Math.min(W, H);
        int[][] blobs = {{BRAND[0], 15, 22}, {BRAND[1], 80, 30}, {BRAND[2], 30, 75}, {BRAND[3], 75, 85}};
        for (int[] b : blobs) {
            float bx = W * b[1] / 100f, by = H * b[2] / 100f;
            float r = minSide * 0.55f;
            blobPaint.setShader(new RadialGradient(bx, by, r,
                    new int[]{Color.argb(40, Color.red(b[0]), Color.green(b[0]), Color.blue(b[0])),
                            Color.argb(0,  Color.red(b[0]), Color.green(b[0]), Color.blue(b[0]))},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(bx - r, by - r * 0.72f, bx + r, by + r * 0.72f), blobPaint);
        }
        blobPaint.setShader(null);

        Bitmap icon = null;
        try {
            icon = BitmapFactory.decodeResource(getResources(),
                    getResources().getIdentifier("ic_launcher_foreground", "mipmap", getContext().getPackageName()));
        } catch (Exception ignored) {}
        if (icon == null) {
            try {
                icon = BitmapFactory.decodeResource(getResources(),
                        getResources().getIdentifier("ic_launcher", "mipmap", getContext().getPackageName()));
            } catch (Exception ignored) {}
        }
        if (icon == null) {
            try {
                icon = BitmapFactory.decodeResource(getResources(),
                        getResources().getIdentifier("poco_icon", "drawable", getContext().getPackageName()));
            } catch (Exception ignored) {}
        }

        float p = Math.max(0f, Math.min(1f, progress));
        float alpha = Math.min(1f, p / 0.5f);
        float scale = 0.85f + 0.15f * ease(p);
        float size = minSide * 0.36f;
        if (size < dp(100)) size = dp(120);
        float cx = W / 2f;
        float cy = H / 2f - dp(20);

        iconPaint.setShader(new RadialGradient(cx, cy, size * 0.9f,
                new int[]{Color.argb((int) (130 * alpha), 79, 70, 229),
                        Color.argb(0, 79, 70, 229)},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, size * 0.9f, iconPaint);
        iconPaint.setShader(null);

        iconPaint.setAlpha((int) (255 * alpha));
        if (icon != null) {
            Rect src = new Rect(0, 0, icon.getWidth(), icon.getHeight());
            RectF dst = new RectF(cx - size * scale / 2f, cy - size * scale / 2f,
                    cx + size * scale / 2f, cy + size * scale / 2f);
            try { canvas.drawBitmap(icon, src, dst, iconPaint); } catch (Exception ignored) {}
        } else {
            drawFallbackMark(canvas, cx, cy, size * scale, alpha);
        }
        iconPaint.setAlpha(255);

        float titleY = cy + size * scale / 2f + dp(28);
        titlePaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titlePaint.setTextSize(dp(32));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setLetterSpacing(-0.01f);
        titlePaint.setShader(new LinearGradient(cx - dp(120), titleY, cx + dp(120), titleY,
                BRAND, new float[]{0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        titlePaint.setAlpha((int) (255 * Math.max(0f, Math.min(1f, (p - 0.2f) / 0.6f))));
        canvas.drawText("HYPERHUB", cx, titleY, titlePaint);
        titlePaint.setShader(null);

        subPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        subPaint.setTextSize(dp(13));
        subPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setLetterSpacing(0.18f);
        subPaint.setColor(Color.argb((int) (230 * Math.max(0f, Math.min(1f, (p - 0.35f) / 0.55f))), 200, 215, 240));
        canvas.drawText("CONNECTIVE  INNOVATION", cx, titleY + dp(24), subPaint);
    }

    private float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private void drawFallbackMark(Canvas c, float cx, float cy, float size, float a) {
        iconPaint.setShader(null);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(2.5f));
        iconPaint.setColor(Color.argb((int) (255 * a), 255, 255, 255));
        float r = size / 2f;
        c.drawCircle(cx, cy, r, iconPaint);
        int n = 6;
        for (int i = 0; i < n; i++) {
            double ang = -Math.PI / 2 + i * 2 * Math.PI / n;
            float nx = cx + (float) Math.cos(ang) * r;
            float ny = cy + (float) Math.sin(ang) * r;
            c.drawLine(cx, cy, nx, ny, iconPaint);
            iconPaint.setStyle(Paint.Style.FILL);
            c.drawCircle(nx, ny, dp(4), iconPaint);
            iconPaint.setStyle(Paint.Style.STROKE);
        }
        iconPaint.setStyle(Paint.Style.FILL);
    }
}
