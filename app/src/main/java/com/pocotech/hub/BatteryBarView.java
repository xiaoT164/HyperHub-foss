package com.pocotech.hub;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class BatteryBarView extends View {
    private Paint bgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float percent = 0f; // 0..1
    private float animPercent = 0f;
    private android.os.Handler handler = new android.os.Handler();

    public BatteryBarView(Context ctx) { super(ctx); }
    public BatteryBarView(Context ctx, AttributeSet a) { super(ctx, a); }

    public void setPercent(float p) {
        this.percent = p;
        animateTo(p);
    }

    private void animateTo(final float target) {
        handler.post(new Runnable() {
            @Override public void run() {
                if (Math.abs(animPercent - target) < 0.005f) { animPercent = target; invalidate(); return; }
                animPercent += (target - animPercent) * 0.08f;
                invalidate();
                handler.postDelayed(this, 16);
            }
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        // Background track
        bgPaint.setColor(Color.argb(40, 255, 255, 255));
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, bgPaint);

        float fillW = w * animPercent;
        if (fillW < h) fillW = h; // min width = capsule

        // Color: green > yellow > red
        int barColor;
        if (animPercent > 0.5f) barColor = Color.rgb(80, 220, 100);
        else if (animPercent > 0.2f) barColor = Color.rgb(255, 200, 30);
        else barColor = Color.rgb(255, 70, 50);

        // Gradient fill
        LinearGradient grad = new LinearGradient(0, 0, fillW, 0,
            new int[]{ barColor, lighten(barColor) }, null, Shader.TileMode.CLAMP);
        barPaint.setShader(grad);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, fillW, h), r, r, barPaint);
        barPaint.setShader(null);

        // Shine on bar
        if (fillW > h * 2) {
            barPaint.setColor(Color.argb(50, 255, 255, 255));
            canvas.drawRoundRect(new RectF(2, 1, fillW - 2, h * 0.45f), r, r, barPaint);
        }
    }

    private int lighten(int color) {
        int r = Math.min(255, Color.red(color) + 60);
        int g = Math.min(255, Color.green(color) + 60);
        int b = Math.min(255, Color.blue(color) + 60);
        return Color.rgb(r, g, b);
    }
}
