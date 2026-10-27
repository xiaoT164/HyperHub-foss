package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class ToggleView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler  = new Handler(Looper.getMainLooper());
    private final Runnable animator = new Runnable() {
        @Override public void run() {
            animScheduled = false;
            if (dragging) {
                invalidate();
                return;
            }
            float target = targetX();
            if (Math.abs(animX - target) < 0.5f) {
                animX = target;
                invalidate();
                return;
            }
            animX += (target - animX) * 0.22f;
            invalidate();
            scheduleAnimation();
        }
    };

    private boolean checked = true;
    private boolean dragging = false;
    private boolean animScheduled = false;
    private float animX = Float.NaN;
    private float downX;
    private float dragOffset;
    private int touchSlop;
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onChanged(boolean checked);
    }

    public ToggleView(Context ctx) {
        super(ctx);
        init();
    }

    public ToggleView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        setChecked(!checked);
        return true;
    }

    public void setChecked(boolean value) {
        setChecked(value, true);
    }

    public void setChecked(boolean value, boolean notifyListener) {
        boolean changed = checked != value;
        checked = value;
        if (Float.isNaN(animX)) {
            animX = targetX();
        }
        if (!dragging) {
            if (hasUsableSize()) {
                scheduleAnimation();
            } else {
                animX = targetX();
                invalidate();
            }
        }
        if (notifyListener && changed && listener != null) {
            listener.onChanged(value);
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
        this.listener = l;
    }

    private boolean hasUsableSize() {
        return getWidth() > 0 && getHeight() > 0;
    }

    private float targetX() {
        if (!hasUsableSize()) {
            return 0f;
        }
        float w = getWidth();
        float h = getHeight();
        float pad = h * 0.12f;
        float thumbD = h - pad * 2f;
        return checked ? (w - pad - thumbD) : pad;
    }

    private float minThumbX() {
        return hasUsableSize() ? getHeight() * 0.12f : 0f;
    }

    private float maxThumbX() {
        if (!hasUsableSize()) return 0f;
        float h = getHeight();
        float pad = h * 0.12f;
        float thumbD = h - pad * 2f;
        return getWidth() - pad - thumbD;
    }

    private float thumbDiameter() {
        if (!hasUsableSize()) return 0f;
        float h = getHeight();
        float pad = h * 0.12f;
        return h - pad * 2f;
    }

    private void scheduleAnimation() {
        if (animScheduled) return;
        animScheduled = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postOnAnimation(animator);
        } else {
            handler.postDelayed(animator, 16L);
        }
    }

    private void stopAnimation() {
        animScheduled = false;
        handler.removeCallbacks(animator);
        removeCallbacks(animator);
    }

    @Override protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!dragging) {
            animX = targetX();
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragging = false;
                downX = event.getX();
                dragOffset = event.getX() - (Float.isNaN(animX) ? targetX() : animX);
                stopAnimation();
                setPressed(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                if (!dragging && Math.abs(dx) > touchSlop) {
                    dragging = true;
                }
                if (dragging && hasUsableSize()) {
                    animX = clamp(event.getX() - dragOffset, minThumbX(), maxThumbX());
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                setPressed(false);
                if (dragging && hasUsableSize()) {
                    dragging = false;
                    float center = animX + thumbDiameter() / 2f;
                    setChecked(center >= getWidth() / 2f, true);
                } else {
                    dragging = false;
                    performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                dragging = false;
                scheduleAnimation();
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (Float.isNaN(animX)) {
            animX = targetX();
        }

        float r = h / 2f;
        float pad = h * 0.12f;
        float thumbD = h - pad * 2f;
        float thumbX = clamp(animX, minThumbX(), maxThumbX());
        float thumbCx = thumbX + thumbD / 2f;
        float cy = h / 2f;

        int trackColor = checked
                ? Color.argb(220, 255, 204, 0)
                : Color.argb(80, 255, 255, 255);
        if (isPressed()) {
            trackColor = checked ? Color.argb(235, 255, 210, 40) : Color.argb(100, 255, 255, 255);
        }

        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, trackPaint);

        trackPaint.setColor(Color.argb(40, 255, 255, 255));
        canvas.drawRoundRect(new RectF(1, 1, w - 1, h * 0.5f), r, r, trackPaint);

        if (checked || dragging) {
            glowPaint.setColor(Color.argb(60, 255, 204, 0));
            glowPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(thumbCx, cy, thumbD * 0.9f, glowPaint);
        }

        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(4f, 0f, 2f, Color.argb(80, 0, 0, 0));
        canvas.drawCircle(thumbCx, cy, thumbD / 2f, thumbPaint);
        thumbPaint.setShadowLayer(0f, 0f, 0f, 0);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
