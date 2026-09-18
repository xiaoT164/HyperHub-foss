package com.pocotech.hub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Дополнительные карточки настроек v2.0.0: тема/акцент, напоминания,
 * экспорт-импорт бэкапа, сброс статистики, живые подсказки.
 */
public final class SettingsExtras {

    private SettingsExtras() {}

    public static void bind(final Activity act, final View root, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final AppSettings prefs = host.prefs();

        // ── Тема / акцент ────────────────────────────────────────────────────
        click(act, root, R.id.card_theme, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                showThemeChooser(act, host);
            }
        });

        // ── Ежедневное напоминание ───────────────────────────────────────────
        ToggleView reminderToggle = (ToggleView) root.findViewById(R.id.toggle_reminder);
        if (reminderToggle != null) {
            reminderToggle.setChecked(prefs.isReminderEnabled(), false);
            reminderToggle.setOnCheckedChangeListener(new ToggleView.OnCheckedChangeListener() {
                @Override public void onChanged(boolean checked) {
                    prefs.setReminderEnabled(checked);
                    ReminderReceiver.applyFromPrefs(act);
                    if (checked) {
                        String label = en ? "Daily reminder is on" : "Ежедневное напоминание включено";
                        locateHint(root, label);
                    }
                }
            });
            click(act, root, R.id.card_reminder, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.vibrate();
                    reminderToggle.performClick();
                }
            });
        }
        click(act, root, R.id.card_reminder_time, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                showReminderTime(act, host, root);
            }
        });

        // ── Бэкап ────────────────────────────────────────────────────────────
        click(act, root, R.id.card_backup, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                showBackupDialog(act, host);
            }
        });

        // ── Сброс статистики ─────────────────────────────────────────────────
        click(act, root, R.id.card_reset_stats, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                new AlertDialog.Builder(act)
                        .setTitle(en ? "Reset progress?" : "Сбросить прогресс?")
                        .setMessage(en
                                ? "XP, streak, achievements and benchmark history will be cleared. Tools and favorites stay."
                                : "XP, серия дней, достижения и история бенчмарков будут очищены. Инструменты и избранное останутся.")
                        .setPositiveButton(en ? "Reset" : "Сбросить", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                host.store().resetStats();
                                host.toast(en ? "Stats reset" : "Статистика сброшена");
                                host.refreshCurrentScreen();
                            }
                        })
                        .setNegativeButton(en ? "Cancel" : "Отмена", null)
                        .show();
            }
        });

        // ── Пересмотреть интро ───────────────────────────────────────────────
        click(act, root, R.id.card_onboarding, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                host.prefs().setOnboardingDone(false);
                host.openScreen(8);
            }
        });

        updateThemeSummary(act, root, host);
        updateReminderSummary(act, root, host);
    }

    // ── тема ─────────────────────────────────────────────────────────────────

    private static final int[] THEME_COLORS = {
            0xFF4F8CFF, 0xFF29D3A0, 0xFF9B6BFF, 0xFFFFCC00, 0xFFFF7043, 0xFFB0BEC5
    };

    private static void showThemeChooser(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final String[] names = en
                ? new String[]{"Ocean", "Mint", "Violet", "Amber", "Sunset", "Graphite"}
                : new String[]{"Океан", "Мята", "Фиолет", "Янтарь", "Закат", "Графит"};
        int selected = 0;
        int current = host.prefs().getAccentColor();
        for (int i = 0; i < THEME_COLORS.length; i++) {
            if (THEME_COLORS[i] == current) selected = i;
        }
        new AlertDialog.Builder(act)
                .setTitle(en ? "Accent theme" : "Акцентная тема")
                .setSingleChoiceItems(names, selected, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        host.prefs().setAccentColor(THEME_COLORS[which]);
                        dialog.dismiss();
                        host.toast(LocaleHelper.isEnglish(act) ? "Theme applied" : "Тема применена");
                        host.refreshCurrentScreen();
                    }
                })
                .setNegativeButton(en ? "Cancel" : "Отмена", null)
                .show();
    }

    private static void updateThemeSummary(Activity act, View root, HubHost host) {
        TextView tv = (TextView) root.findViewById(R.id.theme_value);
        if (tv == null) return;
        int c = host.prefs().getAccentColor();
        String name = LocaleHelper.isEnglish(act) ? "Ocean" : "Океан";
        if (c == 0xFF29D3A0) name = LocaleHelper.isEnglish(act) ? "Mint" : "Мята";
        else if (c == 0xFF9B6BFF) name = LocaleHelper.isEnglish(act) ? "Violet" : "Фиолет";
        else if (c == 0xFFFFCC00) name = LocaleHelper.isEnglish(act) ? "Amber" : "Янтарь";
        else if (c == 0xFFFF7043) name = LocaleHelper.isEnglish(act) ? "Sunset" : "Закат";
        else if (c == 0xFFB0BEC5) name = LocaleHelper.isEnglish(act) ? "Graphite" : "Графит";
        tv.setText(name);
    }

    // ── напоминание ──────────────────────────────────────────────────────────

    private static void showReminderTime(final Activity act, final HubHost host, final View root) {
        final boolean en = LocaleHelper.isEnglish(act);
        final String[] labels = new String[24];
        for (int i = 0; i < 24; i++) labels[i] = String.format(java.util.Locale.US, "%02d:00", i);
        new AlertDialog.Builder(act)
                .setTitle(en ? "Reminder time" : "Время напоминания")
                .setSingleChoiceItems(labels, host.prefs().getReminderHour(),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                                host.prefs().setReminderTime(which, 0);
                                host.prefs().setReminderEnabled(true);
                                ReminderReceiver.applyFromPrefs(act);
                                dialog.dismiss();
                                ToggleView t = (ToggleView) root.findViewById(R.id.toggle_reminder);
                                if (t != null) t.setChecked(true, false);
                                updateReminderSummary(act, root, host);
                                host.toast(en ? "Reminder scheduled" : "Напоминание запланировано");
                            }
                        })
                .setNegativeButton(en ? "Cancel" : "Отмена", null)
                .show();
    }

    private static void updateReminderSummary(Activity act, View root, HubHost host) {
        TextView tv = (TextView) root.findViewById(R.id.reminder_value);
        if (tv == null) return;
        boolean en = LocaleHelper.isEnglish(act);
        if (!host.prefs().isReminderEnabled()) {
            tv.setText(en ? "Off" : "Выключено");
            return;
        }
        tv.setText(String.format(java.util.Locale.US, "%02d:%02d",
                host.prefs().getReminderHour(), host.prefs().getReminderMinute()));
    }

    private static void locateHint(View root, String text) {
        TextView tv = (TextView) root.findViewById(R.id.reminder_value);
        if (tv != null && tv.getText() != null) {
            // подсказка обновляется в updateReminderSummary
        }
    }

    // ── бэкап ────────────────────────────────────────────────────────────────

    private static void showBackupDialog(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        new AlertDialog.Builder(act)
                .setTitle(en ? "Backup and restore" : "Резервная копия и восстановление")
                .setMessage(en
                        ? "Export saves settings, favorites, profiles, benchmark history and progress to a JSON file on the device (and to the clipboard). Import pastes JSON back."
                        : "Экспорт сохраняет настройки, избранное, профили, историю бенчмарков и прогресс в JSON-файл на устройстве (и в буфер обмена). Импорт вставляет JSON обратно.")
                .setPositiveButton(en ? "Export" : "Экспорт", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String json = BackupManager.export(act);
                        String path = BackupManager.writeToFile(act, json);
                        ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("hyperhub_backup", json));
                        String msg = (en ? "Exported. Clipboard updated." : "Экспортировано. Буфер обмена обновлён.")
                                + (path != null ? "\n" + path : "");
                        Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton(en ? "Import" : "Импорт", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                        String json = "";
                        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                                && cm.getPrimaryClip().getItemCount() > 0) {
                            CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(act);
                            if (cs != null) json = cs.toString();
                        }
                        if (json.trim().isEmpty() || !json.contains("HyperHub")) {
                            Toast.makeText(act, en
                                    ? "Copy a HyperHub backup to the clipboard first"
                                    : "Сначала скопируйте бэкап HyperHub в буфер обмена", Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean ok = BackupManager.importJson(act, json);
                        Toast.makeText(act, ok
                                ? (en ? "Backup restored" : "Бэкап восстановлен")
                                : (en ? "Import failed: invalid JSON" : "Импорт не удался: некорректный JSON"),
                                Toast.LENGTH_LONG).show();
                        host.refreshCurrentScreen();
                    }
                })
                .setNegativeButton(en ? "Close" : "Закрыть", null)
                .show();
    }

    // ── общее ────────────────────────────────────────────────────────────────

    private static void click(Activity act, View root, int id, View.OnClickListener l) {
        View v = root.findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }

    /** Читает текстовый файл (используется для подсказок о восстановлении). */
    static String readStream(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static void openLink(Activity act, String url) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    static LinearLayout column(Activity act) {
        LinearLayout l = new LinearLayout(act);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
}
