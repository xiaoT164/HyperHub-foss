package com.pocotech.hub;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

/** UI-хелперы, требуемые другими модулями (в 2.1.0 — без геймификации). */
public final class HomeExtras {

    private HomeExtras() {}

    public static int dp(Activity act, float v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }

    public static TextView title(Activity act, CharSequence text, float sizeSp) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(sizeSp);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView subtitle(Activity act, CharSequence text) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFF9AA6B8);
        t.setTextSize(12f);
        return t;
    }

    public static TextView chip(Activity act, CharSequence text, int bgColor) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(11f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(act, 8));
        bg.setColor(bgColor);
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(act, 6);
        t.setLayoutParams(lp);
        int p = dp(act, 5);
        t.setPadding(p, dp(act, 2), p, dp(act, 2));
        return t;
    }

    public static TextView sectionHeader(Activity act, String s) {
        TextView t = new TextView(act);
        t.setText(s);
        t.setTextColor(0xFF64748B);
        t.setTextSize(10f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        int p = dp(act, 4);
        t.setPadding(p, dp(act, 18), 0, dp(act, 8));
        return t;
    }

    public static TextView actionButton(Activity act, CharSequence text, int bgColor) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(12f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(act, 12));
        bg.setColor(bgColor);
        t.setBackground(bg);
        int p = dp(act, 14);
        t.setPadding(p, dp(act, 8), p, dp(act, 8));
        return t;
    }

    public static TextView ghostButton(Activity act, CharSequence text) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFF9AA6B8);
        t.setTextSize(12f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(act, 12));
        bg.setColor(0x14FFFFFF);
        bg.setStroke(1, 0x22FFFFFF);
        t.setBackground(bg);
        int p = dp(act, 14);
        t.setPadding(p, dp(act, 8), p, dp(act, 8));
        return t;
    }

    public static TextView hint(Activity act, String text) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextColor(0xFF9AA6B8);
        t.setTextSize(11f);
        t.setPadding(dp(act, 4), dp(act, 6), 0, dp(act, 10));
        return t;
    }

    public static ProgressBar thinProgress(Activity act, int value, int max, int color) {
        ProgressBar bar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(max);
        bar.setProgress(value);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 4));
        bar.setLayoutParams(lp);
        try { bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color)); } catch (Exception ignored) {}
        return bar;
    }

    public static void bind(Activity act, View root, HubHost host) {
        // 2.1.0: пустой bind — геймификация удалена.
    }
    public static void refreshStats(Activity act, View root, HubHost host) {
        // 2.1.0: no-op.
    }
}
