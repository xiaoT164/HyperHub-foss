package com.pocotech.hub;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GlassSheet {

    // ── public builder API ────────────────────────────────────────────────────
    private final Context ctx;
    private String title   = "";
    private String body    = "";
    private String copyLabel = null;
    private String copyText  = null;
    private TextView activeTitleView = null;
    private TextView activeBodyView  = null;

    public GlassSheet(Context ctx) { this.ctx = ctx; }

    public GlassSheet title(String t)  {
        this.title = t;
        if (activeTitleView != null) activeTitleView.setText(t);
        return this;
    }

    public GlassSheet body(String b)   {
        this.body = b;
        if (activeBodyView != null) activeBodyView.setText(b);
        return this;
    }
    public GlassSheet copyButton(String label, String text) {
        this.copyLabel = label;
        this.copyText  = text;
        return this;
    }

    public void show() {
        final Dialog dialog = new Dialog(ctx, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final String localizedTitle = UiLocalizer.translate(ctx, title);
        final String localizedBody = UiLocalizer.translate(ctx, body);
        final String localizedCopyLabel = copyLabel != null ? UiLocalizer.translate(ctx, copyLabel) : null;

        Window win = dialog.getWindow();
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        win.setGravity(Gravity.BOTTOM);
        win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // ── root: dim overlay + sheet ─────────────────────────────────────────
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        // Dim background
        final View dimView = new View(ctx);
        dimView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        dimView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        root.addView(dimView);

        // ── sheet container (glass view) ──────────────────────────────────────
        boolean glassOn = new AppSettings(ctx).isLiquidGlass();
        final GlassSheetView sheetView = new GlassSheetView(ctx, glassOn);
        FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        sheetParams.gravity = Gravity.BOTTOM;
        sheetView.setLayoutParams(sheetParams);
        sheetView.setPadding(dp(20), dp(28), dp(20), dp(32));

        // ── inner content ─────────────────────────────────────────────────────
        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        // Drag handle
        View handle = new View(ctx);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(40), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(20);
        handle.setLayoutParams(handleParams);
        handle.setBackgroundColor(Color.argb(80, 255, 255, 255));
        setRoundedBg(handle, 2f);
        content.addView(handle);

        // Title
        TextView tvTitle = new TextView(ctx);
        activeTitleView = tvTitle;
        tvTitle.setText(localizedTitle);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(14);
        tvTitle.setLayoutParams(titleParams);
        content.addView(tvTitle);

        // Divider
        View divider = new View(ctx);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackgroundColor(Color.argb(40, 255, 255, 255));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.bottomMargin = dp(14);
        divider.setLayoutParams(divParams);
        content.addView(divider);

        // Body (scrollable)
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        scrollParams.bottomMargin = dp(18);
        scroll.setLayoutParams(scrollParams);
        scroll.setVerticalScrollBarEnabled(false);

        TextView tvBody = new TextView(ctx);
        activeBodyView = tvBody;
        tvBody.setText(localizedBody);
        tvBody.setTextColor(Color.argb(210, 230, 235, 255));
        tvBody.setTextSize(14f);
        tvBody.setLineSpacing(dp(3), 1f);
        tvBody.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        scroll.addView(tvBody);
        content.addView(scroll);

        // ── buttons row ───────────────────────────────────────────────────────
        LinearLayout btnsRow = new LinearLayout(ctx);
        btnsRow.setOrientation(LinearLayout.HORIZONTAL);
        btnsRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        // Close button
        TextView btnClose = makeButton(ctx, UiLocalizer.translate(ctx, "Понятно"), false);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (copyLabel != null) closeParams.rightMargin = dp(8);
        btnClose.setLayoutParams(closeParams);
        btnsRow.addView(btnClose);

        // Copy button (optional)
        if (localizedCopyLabel != null) {
            TextView btnCopy = makeButton(ctx, localizedCopyLabel, true);
            btnCopy.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            final String textToCopy = copyText;
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("cmd", textToCopy));
                    android.widget.Toast.makeText(ctx, UiLocalizer.translate(ctx, "Скопировано!"), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            btnsRow.addView(btnCopy);
        }

        content.addView(btnsRow);
        sheetView.addView(content);
        root.addView(sheetView);

        dialog.setContentView(root);
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface dialogInterface) {
                activeTitleView = null;
                activeBodyView = null;
            }
        });
        dialog.show();
        UiLocalizer.localizeViewTree(root, ctx);

        // ── ANIMATE IN ────────────────────────────────────────────────────────
        // Sheet: slide up from +300dp + fade in
        sheetView.setTranslationY(dp(320));
        sheetView.setAlpha(0f);

        sheetView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(480)
            .setInterpolator(new DecelerateInterpolator(2.4f))
            .start();

        // Dim: fade in
        animateDim(dimView, 0, 185, 380);

        // Float loop after appear
        final Handler floatHandler = new Handler();
        final float[] floatPhase = {0f};
        final Runnable floatTicker = new Runnable() {
            @Override public void run() {
                floatPhase[0] += 0.03f;
                float offset = (float)(Math.sin(floatPhase[0]) * dp(4));
                sheetView.setTranslationY(offset);
                floatHandler.postDelayed(this, 16);
            }
        };
        floatHandler.postDelayed(floatTicker, 500);

        // ── CLOSE handler ─────────────────────────────────────────────────────
        final View.OnClickListener closeListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                floatHandler.removeCallbacks(floatTicker);
                float curY = sheetView.getTranslationY();

                sheetView.animate()
                    .translationY(curY + dp(340))
                    .alpha(0f)
                    .setDuration(380)
                    .setInterpolator(new AccelerateInterpolator(2f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) {
                            dialog.dismiss();
                        }
                    }).start();

                animateDim(dimView, 185, 0, 320);
            }
        };

        btnClose.setOnClickListener(closeListener);

        // Tap outside to close
        dimView.setOnClickListener(closeListener);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int dp(float v) {
        return (int)(v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void animateDim(final View v, int fromA, int toA, int duration) {
        ValueAnimator anim = ValueAnimator.ofInt(fromA, toA);
        anim.setDuration(duration);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                v.setBackgroundColor(Color.argb((int) a.getAnimatedValue(), 0, 0, 0));
            }
        });
        anim.start();
    }

    private void setRoundedBg(View v, float radiusDp) {
        float r = radiusDp * ctx.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.argb(80, 255, 255, 255));
        d.setCornerRadius(r);
        v.setBackground(d);
    }

    private TextView makeButton(Context ctx, String label, boolean accent) {
        TextView btn = new TextView(ctx);
        btn.setText(label);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(13), dp(16), dp(13));
        btn.setTextSize(14f);
        btn.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(14));

        if (accent) {
            bg.setColor(Color.argb(230, 255, 204, 0));
            btn.setTextColor(Color.argb(255, 20, 15, 0));
        } else {
            bg.setColor(Color.argb(45, 255, 255, 255));
            bg.setStroke(1, Color.argb(60, 255, 255, 255));
            btn.setTextColor(Color.argb(220, 240, 240, 255));
        }
        btn.setBackground(bg);
        return btn;
    }

    // ── GlassSheetView ────────────────────────────────────────────────────────

    static class GlassSheetView extends FrameLayout {

        private boolean glassEnabled = true;

        private Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint shinePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path  clipPath    = new Path();

        private float time = 0f;
        private Handler handler = new Handler();
        private Runnable ticker = new Runnable() {
            @Override public void run() {
                time += 0.018f;
                invalidate();
                handler.postDelayed(this, 32);
            }
        };

        public GlassSheetView(Context ctx) {
            super(ctx);
            setWillNotDraw(false);
        }
        public GlassSheetView(Context ctx, boolean glassEnabled) {
            super(ctx);
            this.glassEnabled = glassEnabled;
            setWillNotDraw(false);
            if (!glassEnabled) setBackgroundColor(android.graphics.Color.argb(220, 30, 30, 35));
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (glassEnabled) handler.post(ticker);
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            handler.removeCallbacks(ticker);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!glassEnabled) { super.onDraw(canvas); return; }
            float w = getWidth();
            float h = getHeight();
            float r = dp(28);   // top corners only

            RectF rect = new RectF(0, 0, w, h);

            // top-rounded clip
            clipPath.reset();
            clipPath.moveTo(0, r);
            clipPath.quadTo(0, 0, r, 0);
            clipPath.lineTo(w - r, 0);
            clipPath.quadTo(w, 0, w, r);
            clipPath.lineTo(w, h);
            clipPath.lineTo(0, h);
            clipPath.close();
            canvas.clipPath(clipPath);

            // ── 1. Glass body ─────────────────────────────────────────────────
            LinearGradient bodyGrad = new LinearGradient(0, 0, 0, h,
                new int[]{
                    Color.argb(75, 255, 255, 255),
                    Color.argb(30, 180, 200, 255),
                    Color.argb(50, 200, 215, 255)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
            fillPaint.setShader(bodyGrad);
            fillPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(rect, fillPaint);
            fillPaint.setShader(null);

            // ── 2. Drifting caustic blob ──────────────────────────────────────
            float bx = w * (0.35f + (float)Math.sin(time * 0.6f) * 0.22f);
            float by = h * (0.28f + (float)Math.cos(time * 0.45f) * 0.12f);
            int bAlpha = (int)(22 + Math.abs(Math.sin(time * 0.7f)) * 16);
            fillPaint.setColor(Color.argb(bAlpha, 255, 255, 255));
            fillPaint.setStyle(Paint.Style.FILL);
            canvas.drawOval(new RectF(bx - w*0.35f, by - h*0.18f,
                                      bx + w*0.35f, by + h*0.18f), fillPaint);

            // ── 3. Top shine strip ────────────────────────────────────────────
            LinearGradient shineGrad = new LinearGradient(0, 0, 0, h * 0.20f,
                new int[]{Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)},
                null, Shader.TileMode.CLAMP);
            shinePaint.setShader(shineGrad);
            shinePaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(new RectF(0, 0, w, h * 0.20f), shinePaint);
            shinePaint.setShader(null);

            // ── 4. Subtle bottom reflection ───────────────────────────────────
            LinearGradient bottomGrad = new LinearGradient(0, h*0.78f, 0, h,
                new int[]{Color.argb(0, 150, 180, 255), Color.argb(25, 150, 180, 255)},
                null, Shader.TileMode.CLAMP);
            fillPaint.setShader(bottomGrad);
            canvas.drawRect(new RectF(0, h*0.78f, w, h), fillPaint);
            fillPaint.setShader(null);

            // ── 5. Top border glow (pulsing) ──────────────────────────────────
            float pulse = (float)(Math.sin(time * 1.2f) * 0.5f + 0.5f);
            int borderAlpha = (int)(55 + pulse * 45);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1.5f);
            borderPaint.setColor(Color.argb(borderAlpha, 210, 225, 255));
            canvas.drawPath(clipPath, borderPaint);

            // side faint lines
            borderPaint.setAlpha(borderAlpha / 3);
            canvas.drawLine(0, r, 0, h, borderPaint);
            canvas.drawLine(w, r, w, h, borderPaint);

            super.onDraw(canvas);
        }

        private int dp(float v) {
            return (int)(v * getContext().getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
