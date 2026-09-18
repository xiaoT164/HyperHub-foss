package com.pocotech.hub;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Экран «Профили» (2.0.0): готовые сценарии оптимизации и пользовательские наборы твиков.
 */
public final class ProfilesScreen {

    private ProfilesScreen() {}

    private static final class Preset {
        final String nameRu, nameEn, descRu, descEn;
        final int accent;
        final List<String> keys;

        Preset(String nameRu, String nameEn, String descRu, String descEn, int accent, List<String> keys) {
            this.nameRu = nameRu;
            this.nameEn = nameEn;
            this.descRu = descRu;
            this.descEn = descEn;
            this.accent = accent;
            this.keys = keys;
        }
    }

    private static final List<Preset> PRESETS = Arrays.asList(
        new Preset("Игровой", "Gaming",
                "FPS-буст, Game Turbo, пинг, производительность и память",
                "FPS boost, Game Turbo, ping, performance and memory",
                0xFFFF6B6B, Arrays.asList("game", "fps", "net", "perf", "ram")),
        new Preset("Батарея", "Battery",
                "Энергорежим, оптимизация фона и дисплея",
                "Power tuning, background and display optimization",
                0xFF29D3A0, Arrays.asList("batt", "ram", "display")),
        new Preset("Чистый интерфейс", "Clean UI",
                "Флагманские анимации, скрытые подписи, регион",
                "Flagship animations, hidden labels, region",
                0xFF9B6BFF, Arrays.asList("anim", "labels", "region")),
        new Preset("Диагностика", "Diagnostics",
                "CIT-тесты, Radio Info, инженерные коды, разработчик",
                "CIT tests, Radio Info, engineering codes, developer",
                0xFF58A6FF, Arrays.asList("cit", "radio", "debug", "dev")),
        new Preset("Приватность", "Privacy",
                "Реклама, bloatware, клонирование, шрифты",
                "Ads, bloatware, cloning, fonts",
                0xFFFFCC00, Arrays.asList("ads", "bloat", "space", "font"))
    );

    public static View build(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final int accent = host.prefs().getAccentColor() != 0 ? host.prefs().getAccentColor() : 0xFF4F8CFF;

        Frame(act);

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

        TextView header = HomeExtras.title(act, en ? "Profiles" : "Профили", 32f);
        header.setGravity(android.view.Gravity.CENTER);
        box.addView(header);
        TextView sub = HomeExtras.subtitle(act, en
                ? "One tap applies a scenario: the app shows every step"
                : "Один тап запускает сценарий: приложение показывает все шаги");
        sub.setGravity(android.view.Gravity.CENTER);
        box.addView(sub);

        LinearLayout levelBox = new LinearLayout(act);
        levelBox.setOrientation(LinearLayout.HORIZONTAL);
        levelBox.setPadding(0, 0, 0, HomeExtras.dp(act, 12));
        levelBox.addView(HomeExtras.chip(act, (en ? "Level " : "Уровень ") + host.store().getLevel(), accent));
        levelBox.addView(spacer(act, 8));
        levelBox.addView(HomeExtras.chip(act,
                (en ? "Profiles applied: " : "Профилей применено: ") + host.store().profileUses(), 0xFF44DD88));
        box.addView(levelBox);

        for (final Preset p : PRESETS) {
            box.addView(presetCard(act, host, en, p));
        }

        // ── Пользовательские профили ─────────────────────────────────────────
        TextView customHeader = new TextView(act);
        customHeader.setText(en ? "CUSTOM PROFILES" : "ПОЛЬЗОВАТЕЛЬСКИЕ ПРОФИЛИ");
        customHeader.setTextColor(0xFF506070);
        customHeader.setTextSize(10f);
        customHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        customHeader.setPadding(HomeExtras.dp(act, 4), HomeExtras.dp(act, 14), 0, HomeExtras.dp(act, 8));
        box.addView(customHeader);

        final LinearLayout customBox = new LinearLayout(act);
        customBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(customBox);

        renderCustom(act, host, customBox, accent);

        TextView create = HomeExtras.actionButton(act, en ? "+ New profile" : "+ Новый профиль", accent);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.topMargin = HomeExtras.dp(act, 12);
        create.setLayoutParams(cp);
        create.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                askNewProfile(act, host, customBox, accent);
            }
        });
        box.addView(create);

        return scroll;
    }

    private static void renderCustom(final Activity act, final HubHost host, LinearLayout box, final int accent) {
        boolean en = LocaleHelper.isEnglish(act);
        box.removeAllViews();
        List<HubStore.Profile> list = host.store().profiles();
        if (list.isEmpty()) {
            box.addView(HomeExtras.hint(act, en
                    ? "No custom profiles yet — combine any tools you like"
                    : "Пользовательских профилей пока нет — соберите свои комбинации"));
            return;
        }
        for (final HubStore.Profile p : list) {
            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(HomeExtras.dp(act, 14), HomeExtras.dp(act, 12),
                    HomeExtras.dp(act, 14), HomeExtras.dp(act, 12));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(HomeExtras.dp(act, 16));
            bg.setColor(0x1EFFFFFF);
            bg.setStroke(1, 0x28FFFFFF);
            card.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = HomeExtras.dp(act, 8);
            card.setLayoutParams(lp);

            TextView name = new TextView(act);
            name.setText(p.name);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(15f);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            card.addView(name);

            StringBuilder sb = new StringBuilder();
            for (String k : p.keys) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(Features.title(k, en));
            }
            TextView desc = new TextView(act);
            desc.setText(sb.toString());
            desc.setTextColor(0xFF8A96A8);
            desc.setTextSize(11f);
            card.addView(desc);

            LinearLayout actions = new LinearLayout(act);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, HomeExtras.dp(act, 10), 0, 0);

            TextView open = HomeExtras.ghostButton(act, en ? "Show steps" : "Показать шаги");
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.vibrate();
                    showSteps(act, host, p.name, p.keys);
                }
            });
            actions.addView(open);
            actions.addView(spacer(act, 8));

            TextView del = HomeExtras.ghostButton(act, en ? "Delete" : "Удалить");
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.vibrate();
                    host.store().removeProfile(p.name);
                    host.toast(LocaleHelper.isEnglish(act) ? "Profile deleted" : "Профиль удалён");
                    host.refreshCurrentScreen();
                }
            });
            actions.addView(del);
            card.addView(actions);

            box.addView(card);
        }
    }

    private static View presetCard(final Activity act, final HubHost host, final boolean en, final Preset p) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(HomeExtras.dp(act, 16), HomeExtras.dp(act, 14),
                HomeExtras.dp(act, 16), HomeExtras.dp(act, 14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(HomeExtras.dp(act, 20));
        bg.setColor(0x22FFFFFF);
        bg.setStroke(1, 0x30FFFFFF);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = HomeExtras.dp(act, 10);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);

        View dot = new View(act);
        android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
        dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dotBg.setColor(p.accent);
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(HomeExtras.dp(act, 10), HomeExtras.dp(act, 10));
        dp.rightMargin = HomeExtras.dp(act, 10);
        dot.setLayoutParams(dp);
        top.addView(dot);

        TextView name = new TextView(act);
        name.setText(en ? p.nameEn : p.nameRu);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(16f);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(name);

        TextView count = HomeExtras.chip(act, p.keys.size() + (en ? " steps" : " шагов"), p.accent);
        top.addView(count);
        card.addView(top);

        TextView desc = new TextView(act);
        desc.setText(en ? p.descEn : p.descRu);
        desc.setTextColor(0xFF9AA6B8);
        desc.setTextSize(12f);
        desc.setPadding(0, HomeExtras.dp(act, 6), 0, HomeExtras.dp(act, 10));
        card.addView(desc);

        LinearLayout actions = new LinearLayout(act);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView apply = HomeExtras.actionButton(act, en ? "Apply" : "Применить", p.accent);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                host.store().countProfileUse();
                host.store().addXp(HubStore.XP_PROFILE);
                showSteps(act, host, en ? p.nameEn : p.nameRu, p.keys);
                host.refreshCurrentScreen();
            }
        });
        actions.addView(apply);
        actions.addView(spacer(act, 8));

        TextView save = HomeExtras.ghostButton(act, en ? "Save as mine" : "Сохранить себе");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                String name = (en ? p.nameEn : p.nameRu) + (en ? " (my)" : " (мой)");
                host.store().saveProfile(name, p.keys);
                host.toast(en ? "Saved to custom profiles" : "Сохранено в пользовательские профили");
                host.refreshCurrentScreen();
            }
        });
        actions.addView(save);
        card.addView(actions);

        return card;
    }

    private static void showSteps(final Activity act, final HubHost host, String name, List<String> keys) {
        boolean en = LocaleHelper.isEnglish(act);
        StringBuilder sb = new StringBuilder();
        sb.append(en ? "Open any step below after closing this sheet:\n\n" : "Откройте любой шаг после закрытия листа:\n\n");
        for (int i = 0; i < keys.size(); i++) {
            sb.append(i + 1).append(". ").append(Features.title(keys.get(i), en)).append("\n");
        }
        sb.append(en
                ? "\nSteps open the Home cards with full instructions and copyable commands."
                : "\nШаги открывают карточки главного экрана с инструкциями и готовыми командами для копирования.");
        host.showSheet(name, sb.toString());
        for (String k : keys) host.store().pushRecent(k);
        host.refreshCurrentScreen();
    }

    private static void askNewProfile(final Activity act, final HubHost host,
                                      final LinearLayout target, final int accent) {
        final boolean en = LocaleHelper.isEnglish(act);
        final LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = HomeExtras.dp(act, 18);
        box.setPadding(pad, pad, pad, pad);

        final EditText nameInput = new EditText(act);
        nameInput.setHint(en ? "Profile name" : "Название профиля");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInput.setTextColor(0xFFFFFFFF);
        nameInput.setHintTextColor(0xFF5A6675);
        box.addView(nameInput);

        final List<String> selected = new ArrayList<>();
        for (Features.F f : Features.ALL) {
            final TextView row = new TextView(act);
            row.setText((selected.contains(f.key) ? "☑ " : "☐ ") + Features.title(f.key, en));
            row.setTextColor(0xFFD6DEEF);
            row.setTextSize(13f);
            row.setPadding(0, HomeExtras.dp(act, 7), 0, HomeExtras.dp(act, 7));
            final Features.F feat = f;
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (selected.contains(feat.key)) selected.remove(feat.key);
                    else selected.add(feat.key);
                    row.setText((selected.contains(feat.key) ? "☑ " : "☐ ") + Features.title(feat.key, en));
                }
            });
            box.addView(row);
        }

        ScrollView scroll = new ScrollView(act);
        scroll.addView(box);

        final android.app.Dialog dialog = new android.app.Dialog(act);
        LinearLayout shell = new LinearLayout(act);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(0xF00E131C);
        LinearLayout.LayoutParams shellP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, HomeExtras.dp(act, 420));
        shellP.gravity = android.view.Gravity.BOTTOM;
        shell.setLayoutParams(shellP);
        shell.setPadding(HomeExtras.dp(act, 16), HomeExtras.dp(act, 16),
                HomeExtras.dp(act, 16), HomeExtras.dp(act, 20));

        TextView t = HomeExtras.title(act, en ? "New profile" : "Новый профиль", 17f);
        shell.addView(t);
        shell.addView(scroll);

        TextView ok = HomeExtras.actionButton(act, en ? "Save" : "Сохранить", accent);
        LinearLayout.LayoutParams okP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        okP.topMargin = HomeExtras.dp(act, 10);
        ok.setLayoutParams(okP);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    host.toast(LocaleHelper.isEnglish(act) ? "Enter a name" : "Введите название");
                    return;
                }
                if (selected.isEmpty()) {
                    host.toast(LocaleHelper.isEnglish(act) ? "Pick at least one tool" : "Выберите хотя бы один инструмент");
                    return;
                }
                host.store().saveProfile(name, selected);
                host.toast(LocaleHelper.isEnglish(act) ? "Profile saved" : "Профиль сохранён");
                dialog.dismiss();
                host.refreshCurrentScreen();
            }
        });
        shell.addView(ok);

        dialog.setContentView(shell);
        dialog.show();
    }

    private static View spacer(Activity act, int dp) {
        View v = new View(act);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(HomeExtras.dp(act, dp), 1);
        v.setLayoutParams(p);
        return v;
    }

    private static void Frame(Activity act) { /* маркер намерения: экран строится программно */ }
}
