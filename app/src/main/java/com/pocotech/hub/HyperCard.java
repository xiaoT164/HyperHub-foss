package com.pocotech.hub;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

public class HyperCard extends LinearLayout {

    private static final float CORNER_DP = 28f;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private boolean glassEnabled = true;
    private float corner;
    private float pressAlpha = 0f;

    public HyperCard(Context c) { super(c); init(); }
    public HyperCard(Context c, AttributeSet a) { super(c, a); init(); }
    public HyperCard(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setClipToPadding(false);
        float dp = getResources().getDisplayMetrics().density;
        corner = CORNER_DP * dp;
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    public void setGlassEnabled(boolean enabled) {
        glassEnabled = enabled;
        invalidate();
    }

    private boolean isInteractive() {
        return isClickable() || hasOnClickListeners();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isInteractive()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animatePress(true);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    animatePress(false);
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void animatePress(boolean down) {
        float targetAlpha = down ? 1f : 0f;
        float targetScale = down ? 0.986f : 1f;
        ValueAnimator animator = ValueAnimator.ofFloat(pressAlpha, targetAlpha);
        animator.setDuration(down ? 80 : 180);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            pressAlpha = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();

        animate().scaleX(targetScale).scaleY(targetScale)
                .setDuration(down ? 80 : 180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
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

        drawSurface(canvas, w, h);

        if (pressAlpha > 0f) {
            pressPaint.setColor(Color.argb((int) (28f * pressAlpha), 255, 255, 255));
            canvas.drawRect(bounds, pressPaint);
        }

        canvas.restore();
        drawBorder(canvas, w, h);
        super.onDraw(canvas);
    }

    private void drawSurface(Canvas canvas, float w, float h) {
        if (glassEnabled) {
            LinearGradient body = new LinearGradient(0, 0, 0, h,
                    new int[]{
                            Color.argb(230, 28, 33, 45),
                            Color.argb(244, 20, 24, 34)
                    }, null, Shader.TileMode.CLAMP);
            bgPaint.setShader(body);
        } else {
            bgPaint.setShader(null);
            bgPaint.setColor(Color.argb(245, 24, 28, 36));
        }
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bounds, corner, corner, bgPaint);
        bgPaint.setShader(null);

        LinearGradient topGlow = new LinearGradient(0, 0, 0, h * 0.48f,
                new int[]{
                        Color.argb(glassEnabled ? 54 : 22, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                }, null, Shader.TileMode.CLAMP);
        highlightPaint.setShader(topGlow);
        highlightPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, w, h * 0.48f, highlightPaint);

        LinearGradient sideTint = new LinearGradient(0, 0, w, h,
                new int[]{
                        Color.argb(glassEnabled ? 42 : 18, 90, 150, 255),
                        Color.argb(0, 90, 150, 255)
                }, null, Shader.TileMode.CLAMP);
        highlightPaint.setShader(sideTint);
        canvas.drawRect(0, 0, w, h, highlightPaint);
        highlightPaint.setShader(null);
    }

    private void drawBorder(Canvas canvas, float w, float h) {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.4f);
        borderPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{
                        Color.argb(86, 255, 255, 255),
                        Color.argb(52, 140, 190, 255)
                }, null, Shader.TileMode.CLAMP));
        RectF inset = new RectF(0.9f, 0.9f, w - 0.9f, h - 0.9f);
        canvas.drawRoundRect(inset, corner, corner, borderPaint);
        borderPaint.setShader(null);
    }
}
