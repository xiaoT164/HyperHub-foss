package com.pocotech.hub;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Журнал оптимизаций — замена XP/серий/достижений. */
public final class ActionLogScreen {

    private ActionLogScreen() {}

    public static View build(final Activity act, final HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);
        int accent = host.prefs().getAccentColor();

        LinearLayout root = UiKit.column(act, 16);
        root.setPadding(16, 28, 16, 110);

        root.addView(UiKit.title(act, en ? "Journal" : "Журнал", 28f));
        root.addView(UiKit.subtitle(act,
                en ? "Chronological log of applied optimisations and benchmark runs."
                   : "Хронология оптимизаций и прогонов бенчмарка."));

        LinearLayout buttons = new LinearLayout(act);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView clear = new TextView(act);
        clear.setText(en ? "Clear" : "Очистить");
        clear.setBackground(UiKit.cardBg(act, 12, true));
        clear.setPadding(20, 12, 20, 12);
        clear.setTextColor(0xFFEF4444);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new android.app.AlertDialog.Builder(act)
                        .setTitle(en ? "Clear journal?" : "Очистить журнал?")
                        .setMessage(en ? "Local only. Cannot be undone." : "Только локальные данные. Действие необратимо.")
                        .setPositiveButton(en ? "Clear" : "Очистить", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                host.store().clearActions();
                                host.refreshCurrentScreen();
                            }
                        }).setNegativeButton(en ? "Cancel" : "Отмена", null).show();
            }
        });
        buttons.addView(clear);

        TextView export = new TextView(act);
        export.setText(en ? "Export" : "Экспорт");
        export.setBackground(UiKit.cardBg(act, 12, true));
        export.setPadding(20, 12, 20, 12);
        export.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.leftMargin = 12;
        export.setLayoutParams(ll);
        export.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.runFeature("export_log"); }
        });
        buttons.addView(export);
        root.addView(buttons);

        List<HubStore.ActionEntry> log = host.store().actions();
        if (log.isEmpty()) {
            TextView empty = new TextView(act);
            empty.setText(en ? "No entries yet. Apply a command, run a benchmark, or tweak a setting."
                             : "Записей пока нет. Примените команду, запустите бенчмарк или измените настройку.");
            empty.setTextColor(0xFF9AA6B8);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 32, 0, 0);
            root.addView(empty);
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            for (HubStore.ActionEntry e : log) {
                root.addView(entryCard(act, e, fmt, accent));
            }
        }
        return UiKit.makeScroll(act, root);
    }

    private static View entryCard(Activity act, HubStore.ActionEntry e, SimpleDateFormat fmt, int accent) {
        LinearLayout row = UiKit.rowCard(act, 14);
        View dot = new View(act);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(UiKit.dp(act, 10), UiKit.dp(act, 10));
        dot.setLayoutParams(dp);
        dot.setBackground(new android.graphics.drawable.GradientDrawable() {
            { setCornerRadius(UiKit.dp(act, 5)); setColor(accent); }
        });
        row.addView(dot);

        LinearLayout info = new LinearLayout(act);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(lp);
        info.setPadding(UiKit.dp(act, 10), 0, 0, 0);
        info.addView(UiKit.title(act, e.title, 14f));
        info.addView(UiKit.subtitle(act, fmt.format(new Date(e.time))));
        row.addView(info);

        if (HubStore.LMK_FULL_KEY.equals(e.key)) row.addView(UiKit.chip(act, "LMK", 0xFFF59E0B));
        else if ("bench".equals(e.key)) row.addView(UiKit.chip(act, "Bench", 0xFF4F46E5));
        return row;
    }
}
