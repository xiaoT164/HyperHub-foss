package com.pocotech.hub;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class UsageBarView extends View {
    private Paint bgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float percent = 0f;
    private float animPercent = 0f;
    private android.os.Handler handler = new android.os.Handler();

    public UsageBarView(Context ctx) { super(ctx); }
    public UsageBarView(Context ctx, AttributeSet a) { super(ctx, a); }

    public void setPercent(float p) {
        this.percent = p;
        handler.post(new Runnable() {
            @Override public void run() {
                if (Math.abs(animPercent - percent) < 0.005f) { animPercent = percent; invalidate(); return; }
                animPercent += (percent - animPercent) * 0.08f;
                invalidate();
                handler.postDelayed(this, 16);
            }
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        bgPaint.setColor(Color.argb(40, 255, 255, 255));
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, bgPaint);

        float fillW = Math.max(h, w * animPercent);
        // Blue-purple gradient
        LinearGradient grad = new LinearGradient(0, 0, fillW, 0,
            new int[]{ Color.rgb(60, 130, 255), Color.rgb(160, 80, 255) },
            null, Shader.TileMode.CLAMP);
        barPaint.setShader(grad);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, fillW, h), r, r, barPaint);
        barPaint.setShader(null);

        // Shine
        if (fillW > h * 2) {
            barPaint.setColor(Color.argb(45, 255, 255, 255));
            canvas.drawRoundRect(new RectF(2, 1, fillW - 2, h * 0.45f), r, r, barPaint);
        }
    }
}
