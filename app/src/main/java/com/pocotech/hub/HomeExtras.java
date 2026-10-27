package com.pocotech.hub;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Домашние виджеты v2.1.0: избранное, недавние и базовые помощники UI.
 * Геймификация (серия/уровень/достижения), индекс здоровья и живой монитор
 * удалены начиная с 2.1.0.
 */
public final class HomeExtras {

    private HomeExtras() {}

    public static void bind(final Activity act, final View root, final HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);

        LinearLayout favs = (LinearLayout) root.findViewById(R.id.home_favorites_container);
        if (favs != null) buildFavorites(act, favs, host);

        LinearLayout recs = (LinearLayout) root.findViewById(R.id.home_recents_container);
        if (recs != null) buildRecents(act, recs, host);
    }

    public static void refreshStats(Activity act, View root, HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);

        LinearLayout favs = (LinearLayout) root.findViewById(R.id.home_favorites_container);
        if (favs != null) buildFavorites(act, favs, host);
        LinearLayout recs = (LinearLayout) root.findViewById(R.id.home_recents_container);
        if (recs != null) buildRecents(act, recs, host);
    }

    private static void buildFavorites(final Activity act, LinearLayout container, final HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);
        container.removeAllViews();
        HubStore store = host.store();
        java.util.List<String> favs = store.favorites();

        TextView header = headerView(act, en ? "FAVORITES (tap = open, long tap = unpin)" : "ИЗБРАННОЕ (тап — открыть, долгий тап — убрать)");
        container.addView(header);

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(row);

        if (favs.isEmpty()) {
            row.addView(hint(act, en
                    ? "Pin tools with a long tap on any card below"
                    : "Закрепите инструменты долгим тапом по карточке ниже"));
            return;
        }

        for (final String key : favs) {
            TextView chip = chip(act, Features.title(key, en), 0xFF4F8CFF);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.vibrate();
                    host.runFeature(key);
                }
            });
            chip.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    host.vibrate();
                    host.store().toggleFavorite(key);
                    host.toast(LocaleHelper.isEnglish(act) ? "Removed from favorites" : "Убрано из избранного");
                    host.refreshCurrentScreen();
                    return true;
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.rightMargin = dp(act, 8);
            p.bottomMargin = dp(act, 8);
            chip.setLayoutParams(p);
            row.addView(chip);
        }
    }

    private static void buildRecents(final Activity act, LinearLayout container, final HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);
        container.removeAllViews();
        java.util.List<String> rec = host.store().recents();
        if (rec.isEmpty()) return;

        container.addView(headerView(act, en ? "RECENT" : "НЕДАВНИЕ"));

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(row);

        int shown = 0;
        for (final String key : rec) {
            if (shown++ >= 4) break;
            TextView chip = chip(act, Features.title(key, en), 0xFF8A8A8F);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.vibrate();
                    host.runFeature(key);
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.rightMargin = dp(act, 8);
            p.bottomMargin = dp(act, 8);
            chip.setLayoutParams(p);
            row.addView(chip);
        }
    }

    // ── мелкие помощники ─────────────────────────────────────────────────────

    static TextView chip(Activity act, String text, int accent) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(accent);
        tv.setPadding(dp(act, 12), dp(act, 7), dp(act, 12), dp(act, 7));
        tv.setBackground(act.getResources().getDrawable(R.drawable.chip_bg));
        return tv;
    }

    static TextView headerView(Activity act, String text) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextColor(0xFF506070);
        tv.setTextSize(10f);
        tv.setAllCaps(false);
        tv.setPadding(dp(act, 4), dp(act, 4), 0, dp(act, 8));
        return tv;
    }

    static TextView hint(Activity act, String text) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextColor(0xFF6A7A8C);
        tv.setTextSize(11f);
        return tv;
    }

    static int dp(Activity act, float value) {
        return (int) (value * act.getResources().getDisplayMetrics().density + 0.5f);
    }

    static TextView title(Activity act, String text, float size) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(size);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return tv;
    }

    static TextView subtitle(Activity act, String text) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextColor(0xFF8FB6FF);
        tv.setTextSize(11f);
        tv.setPadding(0, dp(act, 2), 0, dp(act, 10));
        return tv;
    }

    static TextView actionButton(Activity act, String text, int accent) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(0xFF0B0F16);
        tv.setTextSize(13f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(act, 12));
        bg.setColor(accent);
        tv.setBackground(bg);
        return tv;
    }

    static TextView ghostButton(Activity act, String text) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(0xFFE0E8F5);
        tv.setTextSize(13f);
        tv.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(act, 12));
        bg.setColor(0x28FFFFFF);
        bg.setStroke(1, 0x3CFFFFFF);
        tv.setBackground(bg);
        return tv;
    }
}
