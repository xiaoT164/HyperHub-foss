package com.pocotech.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
import java.util.Locale;

/** График истории бенчмарков: столбцы последних прогонов + лучший результат. */
public class BenchTrendView extends View {

    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bestPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private List<HubStore.BenchRecord> data;
    private int accent = 0xFF58A6FF;

    public BenchTrendView(Context c) { super(c); init(); }
    public BenchTrendView(Context c, AttributeSet a) { super(c, a); init(); }
    public BenchTrendView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x14FFFFFF);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(22f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    public void setAccent(int color) {
        accent = color;
        invalidate();
    }

    public void setData(List<HubStore.BenchRecord> history) {
        data = history;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        for (int i = 1; i < 4; i++) {
            float y = h * i / 4f;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        if (data == null || data.isEmpty()) {
            labelPaint.setColor(0x66FFFFFF);
            canvas.drawText("—", w / 2f, h / 2f, labelPaint);
            return;
        }

        int max = 1;
        int bestTotal = 0;
        int bestIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            int t = Math.max(0, data.get(i).total);
            if (t > max) max = t;
            if (t > bestTotal) { bestTotal = t; bestIndex = i; }
        }

        int n = data.size();
        float gap = Math.max(4f, w * 0.02f);
        float barW = Math.max(6f, (w - gap * (n + 1)) / Math.max(1, n));
        float chartBottom = h - 6f;

        for (int i = 0; i < n; i++) {
            HubStore.BenchRecord r = data.get(i);
            float heightRatio = Math.max(0.03f, r.total / (float) max);
            float barH = (chartBottom - 4f) * heightRatio;
            float left = gap + i * (barW + gap);
            rect.set(left, chartBottom - barH, left + barW, chartBottom);

            if (i == bestIndex) {
                bestPaint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                        new int[]{accent, Color.argb(120, 255, 255, 255)},
                        null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, 6f, 6f, bestPaint);
                bestPaint.setShader(null);
            } else {
                barPaint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                        new int[]{Color.argb(210, Color.red(accent), Color.green(accent), Color.blue(accent)),
                                   Color.argb(70, Color.red(accent), Color.green(accent), Color.blue(accent))},
                        null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, 6f, 6f, barPaint);
                barPaint.setShader(null);
            }
        }

        if (bestIndex >= 0) {
            valuePaint.setColor(0xFFFFFFFF);
            float left = gap + bestIndex * (barW + gap);
            canvas.drawText(fmt(bestTotal), left + barW / 2f, 18f, valuePaint);
        }
    }

    private static String fmt(int v) {
        return String.format(Locale.US, "%,d", v).replace(',', ' ');
    }
}
