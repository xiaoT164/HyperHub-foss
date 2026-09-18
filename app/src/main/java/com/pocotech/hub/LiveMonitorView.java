package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

/**
 * Мини-график реального времени (2.0.0): хранит скользящее окно значений 0..1
 * и рисует плавную кривую с градиентной заливкой.
 */
public class LiveMonitorView extends View {

    private static final int CAPACITY = 90;

    private final float[] values = new float[CAPACITY];
    private int count = 0;
    private int head = 0;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private boolean running = false;
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            invalidate();
            handler.postDelayed(this, 120);
        }
    };

    public LiveMonitorView(Context c) { super(c); init(); }
    public LiveMonitorView(Context c, AttributeSet a) { super(c, a); init(); }
    public LiveMonitorView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.6f);
        linePaint.setColor(0xFF58A6FF);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFFFFFFFF);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x14FFFFFF);
    }

    public void push(float normalized) {
        values[head] = Math.max(0f, Math.min(1f, normalized));
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) count++;
        invalidate();
    }

    public void seed(float normalized) {
        for (int i = 0; i < CAPACITY; i++) {
            values[i] = Math.max(0f, Math.min(1f, normalized));
        }
        count = CAPACITY;
        head = 0;
        invalidate();
    }

    public void clear() {
        count = 0;
        head = 0;
        invalidate();
    }

    public void setAccent(int color) {
        linePaint.setColor(color);
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        handler.postDelayed(ticker, 120);
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        RectF area = new RectF(0, 0, w, h);

        // Сетка
        for (int i = 1; i < 4; i++) {
            float y = h * i / 4f;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        if (count < 2) {
            linePaint.setAlpha(90);
            canvas.drawLine(0, h * 0.6f, w, h * 0.6f, linePaint);
            linePaint.setAlpha(255);
            return;
        }

        int n = count;
        float step = w / (float) (n - 1);

        linePath.reset();
        fillPath.reset();

        for (int i = 0; i < n; i++) {
            int idx = (head - n + i + CAPACITY * 2) % CAPACITY;
            float v = values[idx];
            float x = i * step;
            float y = h - (v * (h - 6f)) - 3f;
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, h);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        fillPath.lineTo(w, h);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.argb(120, 88, 166, 255), Color.argb(0, 88, 166, 255)},
                null, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        fillPaint.setShader(null);

        canvas.drawPath(linePath, linePaint);

        // Текущее значение — точка
        int lastIdx = (head - 1 + CAPACITY) % CAPACITY;
        float lastY = h - (values[lastIdx] * (h - 6f)) - 3f;
        canvas.drawCircle(w - 2f, lastY, 3.4f, dotPaint);
    }
}
