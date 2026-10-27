package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class HyperNavBar extends LinearLayout {

    private static final float CORNER_DP = 32f;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private boolean glassEnabled = true;
    private float corner;

    public HyperNavBar(Context c) { super(c); init(); }
    public HyperNavBar(Context c, AttributeSet a) { super(c, a); init(); }
    public HyperNavBar(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        float dp = getResources().getDisplayMetrics().density;
        corner = CORNER_DP * dp;
    }

    public void setGlassEnabled(boolean enabled) {
        glassEnabled = enabled;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas);
            return;
        }
        bounds.set(0, 0, w, h);

        Path clip = new Path();
        clip.addRoundRect(bounds, corner, corner, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        drawBackground(canvas, w, h);

        canvas.restore();
        drawBorder(canvas, w, h);
        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        if (glassEnabled) {
            bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{
                            Color.argb(235, 24, 29, 40),
                            Color.argb(248, 18, 22, 30)
                    }, null, Shader.TileMode.CLAMP));
        } else {
            bgPaint.setShader(null);
            bgPaint.setColor(Color.argb(244, 22, 26, 34));
        }
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bounds, corner, corner, bgPaint);
        bgPaint.setShader(null);

        glowPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{
                        Color.argb(glassEnabled ? 54 : 20, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                }, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h * 0.55f, glowPaint);
        glowPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{
                        Color.argb(glassEnabled ? 40 : 14, 86, 148, 255),
                        Color.argb(0, 86, 148, 255)
                }, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawBorder(Canvas canvas, float w, float h) {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.4f);
        borderPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{
                        Color.argb(95, 255, 255, 255),
                        Color.argb(58, 129, 182, 255)
                }, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0.8f, 0.8f, w - 0.8f, h - 0.8f), corner, corner, borderPaint);
        borderPaint.setShader(null);
    }
}
