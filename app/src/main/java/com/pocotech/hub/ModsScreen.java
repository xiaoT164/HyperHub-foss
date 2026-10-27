package com.pocotech.hub;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.List;

/**
 * Экран «Моды» (v2.1.0): установка, просмотр, включение/выключение и удаление
 * сторонних модификаций в формате .hhopt.
 */
public final class ModsScreen {

    public static final int PICK_REQUEST = 42021;

    private ModsScreen() {}

    public static View build(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final int accent = host.prefs().getAccentColor() != 0 ? host.prefs().getAccentColor() : 0xFF4F8CFF;
        int dp8 = HomeExtras.dp(act, 8);

        ScrollView scroll = new ScrollView(act);
        scroll.setBackgroundColor(0xFF080C14);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = HomeExtras.dp(act, 16);
        root.setPadding(pad, HomeExtras.dp(act, 24), pad, HomeExtras.dp(act, 100));
        scroll.addView(root);

        TextView title = HomeExtras.title(act, en ? "Mods" : "Моды", 28f);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = HomeExtras.subtitle(act, en
                ? "Third-party .hhopt extensions"
                : "Сторонние модификации .hhopt");
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        // ── Карточка установки ───────────────────────────────────────────────
        HyperCard install = new HyperCard(act);
        install.setOrientation(LinearLayout.HORIZONTAL);
        install.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ip.bottomMargin = HomeExtras.dp(act, 14);
        install.setLayoutParams(ip);
        install.setClickable(true);
        install.setFocusable(true);

        TextView icon = new TextView(act);
        icon.setText("🧩");
        icon.setTextSize(20f);
        icon.setPadding(0, 0, HomeExtras.dp(act, 12), 0);
        install.addView(icon);

        LinearLayout info = new LinearLayout(act);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView t1 = HomeExtras.title(act, en ? "Install from file" : "Установить из файла", 14f);
        info.addView(t1);
        TextView t2 = HomeExtras.subtitle(act, en
                ? "Pick a .hhopt file on the device"
                : "Выберите .hhopt файл на устройстве");
        info.addView(t2);
        install.addView(info);

        TextView arrow = new TextView(act);
        arrow.setText("›");
        arrow.setTextColor(0xFF556070);
        arrow.setTextSize(20f);
        install.addView(arrow);

        install.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                HoptLoader.launchPicker(act, PICK_REQUEST);
            }
        });
        root.addView(install);

        // ── Формат-подсказка ─────────────────────────────────────────────────
        HyperCard hintCard = new HyperCard(act);
        hintCard.setOrientation(LinearLayout.VERTICAL);
        hintCard.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = HomeExtras.dp(act, 16);
        hintCard.setLayoutParams(hp);
        TextView hintTitle = HomeExtras.title(act, "ФОРМАТ .HHOPT", 10f);
        hintTitle.setTextColor(0xFF8FB6FF);
        hintCard.addView(hintTitle);
        TextView hint = HomeExtras.subtitle(act,
                "{\n  \"hhopt\": 1,\n  \"id\": \"com.example.mod\",\n  \"name\": \"My Mod\",\n  \"version\": \"1.0\",\n  \"minAppVersion\": \"2.1.0\",\n  \"cards\": [\n    {\"id\": \"card1\", \"title\": \"Кнопка\",\n     \"feature\": \"fps\"}\n  ]\n}\n\nМод объявляет карточки, ссылающиеся на функции HyperHub. Исполняемый код внутри .hhopt не запускается — это безопасно.");
        hint.setTextColor(0xFF8FA0B8);
        hintCard.addView(hint);
        root.addView(hintCard);

        // ── Список установленных модов ───────────────────────────────────────
        TextView listHeader = HomeExtras.title(act, en ? "INSTALLED MODS" : "УСТАНОВЛЕННЫЕ МОДЫ", 10f);
        listHeader.setTextColor(0xFF506070);
        root.addView(listHeader);

        List<HubStore.Mod> mods = host.store().mods();
        if (mods.isEmpty()) {
            TextView empty = HomeExtras.subtitle(act, en
                    ? "No mods installed yet"
                    : "Моды пока не установлены");
            empty.setPadding(0, HomeExtras.dp(act, 8), 0, 0);
            root.addView(empty);
        } else {
            for (final HubStore.Mod mod : mods) {
                root.addView(modCard(act, host, mod, en, accent));
            }
        }

        return scroll;
    }

    private static View modCard(final Activity act, final HubHost host,
                                final HubStore.Mod mod, final boolean en, final int accent) {
        HyperCard card = new HyperCard(act);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = HomeExtras.dp(act, 14);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = HomeExtras.dp(act, 10);
        card.setLayoutParams(p);

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        LinearLayout names = new LinearLayout(act);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = HomeExtras.title(act, mod.name + "  " + mod.version, 14f);
        names.addView(name);

        StringBuilder meta = new StringBuilder();
        if (!mod.author.isEmpty()) meta.append(mod.author).append(" · ");
        meta.append(en ? "format .hhopt v1" : "формат .hhopt v1");
        TextView meta1 = HomeExtras.subtitle(act, meta.toString()
                + (en ? " · id: " : " · id мода: ") + mod.id);
        names.addView(meta1);
        top.addView(names);

        // Тумблер вкл/выкл
        final ToggleView toggle = new ToggleView(act);
        toggle.setChecked(mod.enabled, false);
        toggle.setOnCheckedChangeListener(new ToggleView.OnCheckedChangeListener() {
            @Override public void onChanged(boolean checked) {
                host.vibrate();
                host.store().setModEnabled(mod.id, checked);
            }
        });
        top.addView(toggle);

        if (!mod.description.isEmpty()) {
            TextView desc = HomeExtras.subtitle(act, mod.description);
            desc.setTextColor(0xFFC0CCD8);
            card.addView(desc);
        }

        // Карточки, которые добавляет мод
        List<HoptLoader.Card> cards = HoptLoader.cardsOf(mod);
        if (!cards.isEmpty()) {
            TextView cardsTitle = HomeExtras.subtitle(act, (en ? "Adds cards: " : "Добавляет карточек: ") + cards.size());
            cardsTitle.setTextColor(0xFF8FB6FF);
            card.addView(cardsTitle);
            for (final HoptLoader.Card c : cards) {
                TextView chip = HomeExtras.chip(act, c.title + " → " + c.feature,
                        mod.enabled ? accent : 0xFF556070);
                chip.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!mod.enabled) return;
                        host.vibrate();
                        host.runFeature(c.feature);
                    }
                });
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cp.bottomMargin = HomeExtras.dp(act, 6);
                chip.setLayoutParams(cp);
                card.addView(chip);
            }
        }

        TextView delete = HomeExtras.ghostButton(act, en ? "Delete mod" : "Удалить мод");
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dp.topMargin = HomeExtras.dp(act, 8);
        delete.setLayoutParams(dp);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                new AlertDialog.Builder(act)
                        .setTitle(en ? "Delete mod?" : "Удалить мод?")
                        .setMessage(mod.name)
                        .setPositiveButton(en ? "Delete" : "Удалить",
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override public void onClick(android.content.DialogInterface d, int w) {
                                        host.store().removeMod(mod.id);
                                        host.toast(en ? "Mod deleted" : "Мод удалён");
                                        host.refreshCurrentScreen();
                                    }
                                })
                        .setNegativeButton(en ? "Cancel" : "Отмена", null)
                        .show();
            }
        });
        card.addView(delete);
        return card;
    }

    // ── Обработка результата выбора файла ────────────────────────────────────

    public static void handleInstallResult(final Activity act, final HubHost host, Uri uri) {
        InputStream in = null;
        try {
            in = act.getContentResolver().openInputStream(uri);
            HubStore.Mod mod = HoptLoader.installFromStream(act, host.store(), in);
            Toast.makeText(act,
                    (LocaleHelper.isEnglish(act) ? "Mod installed: " : "Мод установлен: ") + mod.name,
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            String reason = (e instanceof HoptLoader.HoptException) ? e.getMessage()
                    : (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            Toast.makeText(act,
                    (LocaleHelper.isEnglish(act) ? "Install failed: " : "Не удалось установить: ") + reason,
                    Toast.LENGTH_LONG).show();
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
        host.refreshCurrentScreen();
    }
}
