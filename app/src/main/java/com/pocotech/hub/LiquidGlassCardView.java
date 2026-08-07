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

/**
 * Card container with a dark glass-style background, rounded corners,
 * a subtle border, and an optional animated top highlight.
 */
public class LiquidGlassCardView extends LinearLayout {

    private Paint bgPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint shinePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float time = 0f;
    private boolean glassEnabled = true;
    private android.os.Handler handler = new android.os.Handler();
    private RectF bounds = new RectF();

    private static final float RADIUS = 16f * 3; // ~16dp at mdpi

    public void setGlassEnabled(boolean enabled) {
        this.glassEnabled = enabled;
        if (!enabled) handler.removeCallbacks(ticker);
        else handler.post(ticker);
        invalidate();
    }

    private Runnable ticker = new Runnable() {
        @Override public void run() {
            time += 0.02f;
            invalidate();
            handler.postDelayed(this, 40);
        }
    };

    public LiquidGlassCardView(Context context) { super(context); init(); }
    public LiquidGlassCardView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public LiquidGlassCardView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (glassEnabled) handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        bounds.set(0, 0, w, h);

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.argb(255, 22, 27, 34));
        canvas.drawRoundRect(bounds, RADIUS, RADIUS, bgPaint);

        if (glassEnabled) {
            float shineAlpha = (float)(0.04f + Math.abs(Math.sin(time * 0.6f)) * 0.03f);
            LinearGradient shineGrad = new LinearGradient(
                0, 0, 0, h * 0.4f,
                new int[]{
                    Color.argb((int)(shineAlpha * 255), 255, 255, 255),
                    Color.argb(0, 255, 255, 255)
                },
                null, Shader.TileMode.CLAMP
            );
            shinePaint.setShader(shineGrad);
            shinePaint.setStyle(Paint.Style.FILL);
            Path clip = new Path();
            clip.addRoundRect(bounds, RADIUS, RADIUS, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            canvas.drawRect(0, 0, w, h * 0.4f, shinePaint);
            canvas.restore();
            shinePaint.setShader(null);
        }

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);
        borderPaint.setColor(Color.argb(30, 255, 255, 255));
        canvas.drawRoundRect(bounds, RADIUS, RADIUS, borderPaint);

        super.onDraw(canvas);
    }
}
