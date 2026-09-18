package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Круговой индикатор (0–100) для индекса здоровья устройства. */
public class RingProgressView extends View {

    private final Paint bgPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int progress = 0;
    private int accent = 0xFF4F8CFF;
    private String label = "";

    public RingProgressView(Context c) { super(c); init(); }
    public RingProgressView(Context c, AttributeSet a) { super(c, a); init(); }
    public RingProgressView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(0x22FFFFFF);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(accent);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        labelPaint.setColor(0x99FFFFFF);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(100, value));
        invalidate();
    }

    public void setAccent(int color) {
        accent = color;
        arcPaint.setColor(color);
        invalidate();
    }

    public void setLabel(String text) {
        label = text == null ? "" : text;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float stroke = Math.max(6f, h * 0.085f);
        bgPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);

        float inset = stroke / 2f + 2f;
        float size = Math.min(w, h) - inset * 2f;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        oval.set(left, top, left + size, top + size);

        canvas.drawArc(oval, -90f, 360f, false, bgPaint);
        if (progress > 0) {
            canvas.drawArc(oval, -90f, 360f * progress / 100f, false, arcPaint);
        }

        textPaint.setTextSize(size * 0.34f);
        float cy = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(progress), w / 2f, cy, textPaint);

        if (!label.isEmpty()) {
            labelPaint.setTextSize(size * 0.12f);
            canvas.drawText(label, w / 2f, cy + size * 0.24f, labelPaint);
        }
    }
}
