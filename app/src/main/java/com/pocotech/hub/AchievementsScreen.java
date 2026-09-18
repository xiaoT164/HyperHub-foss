package com.pocotech.hub;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Экран «Достижения» (2.0.0): уровень, XP, серия дней и 12 целей с прогрессом. */
public final class AchievementsScreen {

    private AchievementsScreen() {}

    public static View build(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final int accent = host.prefs().getAccentColor() != 0 ? host.prefs().getAccentColor() : 0xFF4F8CFF;
        final HubStore store = host.store();

        ScrollView scroll = new ScrollView(act);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        scroll.setBackgroundColor(0xFF080C14);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = HomeExtras.dp(act, 16);
        box.setPadding(pad, HomeExtras.dp(act, 28), pad, HomeExtras.dp(act, 110));
        scroll.addView(box);

        TextView header = HomeExtras.title(act, en ? "Achievements" : "Достижения", 32f);
        header.setGravity(android.view.Gravity.CENTER);
        box.addView(header);
        TextView sub = HomeExtras.subtitle(act, en
                ? "Progress is stored locally on your device"
                : "Прогресс хранится локально на устройстве");
        sub.setGravity(android.view.Gravity.CENTER);
        box.addView(sub);

        // ── Карточка уровня ──────────────────────────────────────────────────
        LinearLayout lvl = new LinearLayout(act);
        lvl.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable lvlBg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{0x33FFFFFF, 0x14FFFFFF});
        lvlBg.setCornerRadius(HomeExtras.dp(act, 22));
        lvlBg.setStroke(1, 0x30FFFFFF);
        lvl.setBackground(lvlBg);
        lvl.setPadding(HomeExtras.dp(act, 18), HomeExtras.dp(act, 18),
                HomeExtras.dp(act, 18), HomeExtras.dp(act, 18));
        box.addView(lvl);

        TextView lvlTitle = HomeExtras.title(act,
                (en ? "Level " : "Уровень ") + store.getLevel() + " · " + store.getLevelTitle(en), 20f);
        lvl.addView(lvlTitle);

        TextView xpText = new TextView(act);
        xpText.setText(store.getXp() + " XP · " + (en ? "next level in " : "до следующего уровня ")
                + (100 - store.getLevelProgress()) + " XP");
        xpText.setTextColor(0xFF9AA6B8);
        xpText.setTextSize(12f);
        xpText.setPadding(0, HomeExtras.dp(act, 4), 0, HomeExtras.dp(act, 10));
        lvl.addView(xpText);

        ProgressBar bar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(store.getLevelProgress());
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, HomeExtras.dp(act, 6)));
        try { bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent)); } catch (Exception ignored) {}
        lvl.addView(bar);

        LinearLayout chips = new LinearLayout(act);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, HomeExtras.dp(act, 12), 0, 0);
        chips.addView(HomeExtras.chip(act, (store.isTodayActive() ? "🔥 " : "") +
                (en ? "Streak " : "Серия ") + store.streak(), 0xFFFFCC00));
        chips.addView(HomeExtras.chip(act, (en ? "Best " : "Рекорд ") + store.bestStreak(), 0xFF58A6FF));
        chips.addView(HomeExtras.chip(act, (en ? "Days " : "Дней ") + store.totalDays(), 0xFF44DD88));
        lvl.addView(chips);

        // ── Календарь активности ─────────────────────────────────────────────
        TextView calHeader = new TextView(act);
        calHeader.setText(en ? "LAST 14 DAYS" : "ПОСЛЕДНИЕ 14 ДНЕЙ");
        calHeader.setTextColor(0xFF506070);
        calHeader.setTextSize(10f);
        calHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        calHeader.setPadding(HomeExtras.dp(act, 4), HomeExtras.dp(act, 18), 0, HomeExtras.dp(act, 8));
        box.addView(calHeader);

        List<String> days = store.activeDays();
        LinearLayout cal = new LinearLayout(act);
        cal.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 13; i >= 0; i--) {
            String key = HubStore.dayKey(-i);
            boolean active = days.contains(key);
            View cell = new View(act);
            android.graphics.drawable.GradientDrawable cb = new android.graphics.drawable.GradientDrawable();
            cb.setCornerRadius(HomeExtras.dp(act, 5));
            cb.setColor(active ? accent : 0x1EFFFFFF);
            cell.setBackground(cb);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, HomeExtras.dp(act, 22), 1f);
            cp.rightMargin = HomeExtras.dp(act, 3);
            cell.setLayoutParams(cp);
            cal.addView(cell);
        }
        box.addView(cal);

        // ── Достижения ───────────────────────────────────────────────────────
        TextView achHeader = new TextView(act);
        achHeader.setText(en ? "GOALS" : "ЦЕЛИ");
        achHeader.setTextColor(0xFF506070);
        achHeader.setTextSize(10f);
        achHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        achHeader.setPadding(HomeExtras.dp(act, 4), HomeExtras.dp(act, 20), 0, HomeExtras.dp(act, 8));
        box.addView(achHeader);

        int done = 0;
        for (HubStore.Achievement a : store.achievements(en)) {
            if (a.done) done++;
            box.addView(achievementCard(act, a, accent));
        }

        TextView footer = new TextView(act);
        footer.setText((en ? "Completed: " : "Выполнено: ") + done + "/12 · "
                + (en ? "tools launched: " : "запусков инструментов: ") + store.totalUses());
        footer.setTextColor(0xFF6A7A8C);
        footer.setTextSize(11f);
        footer.setGravity(android.view.Gravity.CENTER);
        footer.setPadding(0, HomeExtras.dp(act, 12), 0, 0);
        box.addView(footer);

        return scroll;
    }

    private static View achievementCard(Activity act, HubStore.Achievement a, int accent) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(HomeExtras.dp(act, 14), HomeExtras.dp(act, 12),
                HomeExtras.dp(act, 14), HomeExtras.dp(act, 12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(HomeExtras.dp(act, 16));
        bg.setColor(a.done ? 0x2444DD88 : 0x1AFFFFFF);
        bg.setStroke(1, a.done ? 0x5544DD88 : 0x24FFFFFF);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = HomeExtras.dp(act, 8);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView name = new TextView(act);
        name.setText((a.done ? "★ " : "☆ ") + a.title);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(14f);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(name);

        TextView pct = HomeExtras.chip(act, a.progress + "%", a.done ? 0xFF44DD88 : accent);
        top.addView(pct);
        card.addView(top);

        TextView desc = new TextView(act);
        desc.setText(a.desc);
        desc.setTextColor(0xFF9AA6B8);
        desc.setTextSize(11f);
        desc.setPadding(0, HomeExtras.dp(act, 4), 0, HomeExtras.dp(act, 8));
        card.addView(desc);

        ProgressBar bar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(a.progress);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, HomeExtras.dp(act, 4)));
        try {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(a.done ? 0xFF44DD88 : accent));
        } catch (Exception ignored) {}
        card.addView(bar);

        return card;
    }
}
