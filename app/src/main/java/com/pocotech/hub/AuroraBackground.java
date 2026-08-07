package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class AuroraBackground extends View {

    private static final int[] COLORS = {
            0xFF63A4FF,
            0xFF9D7CFF,
            0xFF71D5FF
    };

    private final Paint bgPaint = new Paint();
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler();

    private float time = 0f;
    private int width;
    private int height;
    private boolean running;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            time += 0.0115f;
            invalidate();
            if (running) handler.postDelayed(this, 16);
        }
    };

    public AuroraBackground(Context c) { super(c); init(); }
    public AuroraBackground(Context c, AttributeSet a) { super(c, a); init(); }
    public AuroraBackground(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        bgPaint.setColor(0xFF090E16);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (width <= 0 || height <= 0) return;

        canvas.drawRect(0, 0, width, height, bgPaint);
        drawBlob(canvas, 0.20f, 0.22f, 0.54f, COLORS[0], 0.90f, 0.74f, 0.0f);
        drawBlob(canvas, 0.78f, 0.30f, 0.48f, COLORS[1], 0.72f, 0.58f, 1.7f);
        drawBlob(canvas, 0.48f, 0.74f, 0.58f, COLORS[2], 0.58f, 0.80f, 3.1f);

        Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
        veil.setColor(Color.argb(112, 6, 10, 18));
        canvas.drawRect(0, 0, width, height, veil);
    }

    private void drawBlob(Canvas canvas, float xBase, float yBase, float radiusScale,
                          int color, float xAmp, float yAmp, float phase) {
        float minSide = Math.min(width, height);
        float cx = width * xBase + (float) Math.sin(time * 0.85f + phase) * (minSide * 0.085f * xAmp);
        float cy = height * yBase + (float) Math.cos(time * 0.66f + phase) * (minSide * 0.075f * yAmp);
        float radius = minSide * radiusScale;

        int alpha = 42 + (int) (Math.abs(Math.sin(time * 0.40f + phase)) * 18f);
        RadialGradient gradient = new RadialGradient(
                cx, cy, radius,
                new int[]{
                        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        blobPaint.setShader(gradient);
        RectF oval = new RectF(cx - radius, cy - radius * 0.70f, cx + radius, cy + radius * 0.70f);
        canvas.drawOval(oval, blobPaint);
        blobPaint.setShader(null);
    }
}
