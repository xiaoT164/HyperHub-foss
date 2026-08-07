package com.pocotech.hub;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ToggleView extends View {

    private Paint trackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint thumbPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean checked = true;
    private float animX = -1; // thumb position, -1 = uninitialised
    private android.os.Handler handler = new android.os.Handler();
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onChanged(boolean checked);
    }

    public ToggleView(Context ctx) { super(ctx); init(); }
    public ToggleView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                setChecked(!checked);
            }
        });
    }

    public void setChecked(boolean c) {
        checked = c;
        if (listener != null) listener.onChanged(c);
        animateTo(targetX());
    }

    public boolean isChecked() { return checked; }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) { this.listener = l; }

    private float targetX() {
        float w = getWidth(), h = getHeight();
        float pad = h * 0.12f;
        float thumbD = h - pad * 2;
        return checked ? w - pad - thumbD : pad;
    }

    private void animateTo(final float target) {
        handler.post(new Runnable() {
            @Override public void run() {
                if (Math.abs(animX - target) < 0.5f) { animX = target; invalidate(); return; }
                animX += (target - animX) * 0.18f;
                invalidate();
                handler.postDelayed(this, 16);
            }
        });
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (animX < 0) animX = targetX();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        float pad = h * 0.12f;
        float thumbD = h - pad * 2;

        // Track
        int trackColor = checked
            ? Color.argb(220, 255, 204, 0)
            : Color.argb(80, 255, 255, 255);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, trackPaint);

        // Track shine
        trackPaint.setColor(Color.argb(40, 255, 255, 255));
        canvas.drawRoundRect(new RectF(1, 1, w - 1, h * 0.5f), r, r, trackPaint);

        // Glow under thumb when on
        if (checked) {
            glowPaint.setColor(Color.argb(60, 255, 204, 0));
            glowPaint.setStyle(Paint.Style.FILL);
            float cx = animX + thumbD / 2f;
            float cy = h / 2f;
            canvas.drawCircle(cx, cy, thumbD * 0.9f, glowPaint);
        }

        // Thumb
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(4f, 0, 2f, Color.argb(80, 0, 0, 0));
        canvas.drawCircle(animX + thumbD / 2f, h / 2f, thumbD / 2f, thumbPaint);
        thumbPaint.setShadowLayer(0, 0, 0, 0);
    }
}
