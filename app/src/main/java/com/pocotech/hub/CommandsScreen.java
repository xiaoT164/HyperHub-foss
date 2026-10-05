package com.pocotech.hub;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Экран «Команды» — каталог ADB-команд, заменивший экран обновлений. */
public final class CommandsScreen {

    private CommandsScreen() {}

    public static View build(final Activity act, final HubHost host) {
        boolean en = LocaleHelper.isEnglish(act);
        int accent = host.prefs().getAccentColor();

        LinearLayout root = UiKit.column(act, 16);
        root.setPadding(16, 28, 16, 110);

        LinearLayout headerCol = new LinearLayout(act);
        headerCol.setOrientation(LinearLayout.VERTICAL);
        headerCol.addView(UiKit.title(act, en ? "Commands" : "Команды", 28f));
        headerCol.addView(UiKit.subtitle(act,
                en ? "Run via Termux or ADB Shell — copy and paste. All commands are local."
                   : "Выполняйте через Termux или ADB Shell — копируйте и применяйте. Все команды локальные."));
        root.addView(headerCol);

        EditText search = new EditText(act);
        search.setHint(en ? "Filter commands" : "Фильтр команд");
        search.setBackground(UiKit.cardBg(act, 14, true));
        search.setPadding(16, 14, 16, 14);
        search.setTextColor(0xFFFFFFFF);
        search.setHintTextColor(0xFF64748B);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        search.setLayoutParams(slp);
        root.addView(search);

        TextView empty = new TextView(act);
        empty.setText(en ? "No commands match" : "Нет команд по запросу");
        empty.setTextColor(0xFF64748B);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        root.addView(empty);

        for (CommandCatalog.Cmd c : CommandCatalog.all()) {
            root.addView(buildCard(act, host, c, accent));
        }

        UiKit.bindSearch(search, empty, new Runnable() {
            @Override public void run() {
                String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
                int visible = 0;
                for (int i = 0; i < root.getChildCount(); i++) {
                    View v = root.getChildAt(i);
                    Object tag = v.getTag();
                    if (tag instanceof String) {
                        String tokens = (String) tag;
                        boolean match = q.isEmpty() || tokens.toLowerCase(Locale.ROOT).contains(q);
                        v.setVisibility(match ? View.VISIBLE : View.GONE);
                        if (match) visible++;
                    }
                }
                empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
            }
        });
        return UiKit.makeScroll(act, root);
    }

    private static View buildCard(Activity act, HubHost host, final CommandCatalog.Cmd c, int accent) {
        boolean en = LocaleHelper.isEnglish(act);
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.cardBg(act, 16, true));
        int p = UiKit.dp(act, 16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiKit.dp(act, 8);
        card.setLayoutParams(lp);
        card.setTag((c.titleEn + " " + c.titleRu + " " + c.descEn + " " + c.descRu + " " + c.key + " " + c.group).toLowerCase(Locale.ROOT));

        LinearLayout header = new LinearLayout(act);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiKit.title(act, en ? c.titleEn : c.titleRu, 16f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        header.addView(title);
        header.addView(UiKit.chip(act, c.dangerous ? (en ? "advanced" : "продвинутое") : (en ? "safe" : "безопасно"),
                c.dangerous ? 0xFFF59E0B : 0xFF22C55E));
        card.addView(header);

        TextView group = UiKit.subtitle(act, (en ? "Group: " : "Группа: ") + groupLabel(en, c.group));
        group.setPadding(0, 6, 0, 0);
        card.addView(group);

        TextView desc = UiKit.subtitle(act, en ? c.descEn : c.descRu);
        desc.setPadding(0, 8, 0, 12);
        card.addView(desc);

        TextView copy = UiKit.actionBadge(act, en ? "Copy command" : "Скопировать команду", accent);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.runFeature(c.key); }
        });
        card.addView(copy);

        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                boolean added = host.store().toggleFavorite(c.key);
                host.toast(added ? (en ? "Added to favorites" : "Добавлено в избранное")
                        : (en ? "Removed from favorites" : "Удалено из избранного"));
                return true;
            }
        });
        return card;
    }

    private static String groupLabel(boolean en, String key) {
        switch (key) {
            case "lmk":  return "ActivityManager";
            case "perf": return en ? "Performance" : "Производительность";
            case "net":  return en ? "Network" : "Сеть";
            case "batt": return en ? "Battery" : "Батарея";
            case "game": return en ? "Games" : "Игры";
            case "ui":   return en ? "Interface" : "Интерфейс";
            default:     return key;
        }
    }
}
