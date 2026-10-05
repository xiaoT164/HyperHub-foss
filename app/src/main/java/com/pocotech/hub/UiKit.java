package com.pocotech.hub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Общий набор UI-примитивов для экранов HyperHub 2.1.0. */
public final class UiKit {

    private UiKit() {}

    public static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable cardBg(Context ctx, int radiusDp, boolean glass) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                glass ? new int[]{0x33FFFFFF, 0x14FFFFFF}
                      : new int[]{0xCC111827, 0xCC0B1020});
        g.setCornerRadius(dp(ctx, radiusDp));
        g.setStroke(1, 0x22FFFFFF);
        return g;
    }

    public static TextView title(Context ctx, CharSequence text, float sizeSp) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(sizeSp);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView subtitle(Context ctx, CharSequence text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(0xFF9AA6B8);
        t.setTextSize(12f);
        return t;
    }

    public static TextView sectionHeader(Context ctx, String s) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextColor(0xFF64748B);
        t.setTextSize(10f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        int p = dp(ctx, 4);
        t.setPadding(p, dp(ctx, 18), 0, dp(ctx, 8));
        return t;
    }

    public static LinearLayout column(Context ctx, int padDp) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(ctx, padDp);
        l.setPadding(p, 0, p, dp(ctx, 110));
        return l;
    }

    public static LinearLayout rowCard(Context ctx, int radiusDp) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(cardBg(ctx, radiusDp, true));
        int p = dp(ctx, 16);
        card.setPadding(p, dp(ctx, 14), p, dp(ctx, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        card.setLayoutParams(lp);
        card.setGravity(Gravity.CENTER_VERTICAL);
        return card;
    }

    public static ScrollView makeScroll(Context ctx, View content) {
        ScrollView s = new ScrollView(ctx);
        s.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        s.setBackgroundColor(0xFF080C14);
        s.setVerticalScrollBarEnabled(false);
        s.addView(content);
        return s;
    }

    public static void copyToClipboard(Context ctx, String text, String message) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("hyperhub", text));
        if (message != null) Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    public static TextView chip(Context ctx, String text, int bgColor) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(11f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 8));
        bg.setColor(bgColor);
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(ctx, 6);
        t.setLayoutParams(lp);
        int p = dp(ctx, 5);
        t.setPadding(p, dp(ctx, 2), p, dp(ctx, 2));
        return t;
    }

    public static TextView actionBadge(Context ctx, String text, int accent) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(11f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 10));
        bg.setColor(accent);
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 6);
        t.setLayoutParams(lp);
        int p = dp(ctx, 8);
        t.setPadding(p, dp(ctx, 6), p, dp(ctx, 6));
        return t;
    }

    public static void bindSearch(EditText search, TextView empty, Runnable filter) {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filter.run(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        filter.run();
    }

    public static void makeSheetCommand(Context ctx, CommandCatalog.Cmd cmd, HubHost host) {
        new GlassSheet(ctx)
                .title(LocaleHelper.isEnglish(ctx) ? cmd.titleEn : cmd.titleRu)
                .body((LocaleHelper.isEnglish(ctx) ? cmd.descEn : cmd.descRu)
                        + "\n\n" + cmd.command)
                .copyButton(LocaleHelper.isEnglish(ctx) ? "Copy command" : "Скопировать команду", cmd.command)
                .show();
    }
}
