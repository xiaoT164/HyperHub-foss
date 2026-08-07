package com.pocotech.hub;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES20;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.pocotech.hub.BuildConfig;

public class MainActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    private FrameLayout fragmentContainer;
    private AppSettings prefs;
    private static final String KEY_CURRENT_SCREEN = "current_screen";
    private int currentScreen = -1; // -1 = ничего не показано
    private View benchScreenView; // текущий view экрана бенчмарка, для обновления после GPU-Activity
    private boolean benchmarkRunning = false;
    private String benchmarkStatusText = null;
    private boolean predictSheetOpen = false;
    private boolean screenTransitionRunning = false;
    private int androidVersionTapCount = 0;
    private long androidVersionTapDeadlineMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new AppSettings(this);
        setContentView(R.layout.main);
        UiLocalizer.localizeViewTree(getWindow().getDecorView(), this);

        DeviceNameResolver.init(this);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);

        HyperNavBar bottomNav = (HyperNavBar) findViewById(R.id.bottom_navigation);
        if (bottomNav != null && !prefs.isLiquidGlass()) {
            bottomNav.setGlassEnabled(false);
        }

        int startScreen = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_CURRENT_SCREEN, 0) : 0;
        showScreenInternal(startScreen, startScreen, false);
        setNavHighlight(startScreen);

        final int[] navIds = {
            R.id.nav_home, R.id.nav_updates, R.id.nav_info,
            R.id.nav_benchmark, R.id.nav_predict, R.id.nav_settings
        };
        for (int i = 0; i < navIds.length; i++) {
            final int idx = i;
            View nav = findViewById(navIds[i]);
            if (nav != null) {
                nav.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (idx == currentScreen || screenTransitionRunning) return;
                        vibrate();
                        showScreen(idx);
                        setNavHighlight(idx);
                    }
                });
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_SCREEN, currentScreen);
    }

    @Override protected void onDestroy() {
        if (hubBench != null) hubBench.cancel();
        super.onDestroy();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, tr("Не удалось открыть ссылку"), Toast.LENGTH_SHORT).show();
        }
    }

    private void setNavHighlight(int idx) {
        int activeColor = prefs.getAccentColor() != 0 ? prefs.getAccentColor() : 0xFF4F8CFF;
        int inactiveColor = 0xFF8A8A8F;

        int[] iconIds      = { R.id.nav_home_icon, R.id.nav_updates_icon, R.id.nav_info_icon, R.id.nav_benchmark_icon, R.id.nav_predict_icon, R.id.nav_settings_icon };
        int[] indicatorIds = { R.id.nav_home_indicator, R.id.nav_updates_indicator, R.id.nav_info_indicator, R.id.nav_benchmark_indicator, R.id.nav_predict_indicator, R.id.nav_settings_indicator };
        int[] labelIds     = { R.id.nav_home_label, R.id.nav_updates_label, R.id.nav_info_label, R.id.nav_benchmark_label, R.id.nav_predict_label, R.id.nav_settings_label };

        for (int i = 0; i < iconIds.length; i++) {
            boolean active = (i == idx);
            ImageView icon = (ImageView) findViewById(iconIds[i]);
            View indicator = findViewById(indicatorIds[i]);
            TextView label = (TextView) findViewById(labelIds[i]);

            if (icon != null) {
                icon.setColorFilter(active ? activeColor : inactiveColor);
                icon.animate().scaleX(active ? 1.15f : 1f).scaleY(active ? 1.15f : 1f).setDuration(200).start();
            }
            if (indicator != null) {
                indicator.animate().alpha(active ? 1f : 0f).setDuration(200).start();
            }
            if (label != null) {
                label.setTextColor(active ? activeColor : inactiveColor);
            }
        }
    }

    private void showScreen(int idx) {
        showScreenInternal(idx, currentScreen, true);
    }

    private void showScreenInternal(final int idx, final int prevScreen, boolean animate) {
        if (currentScreen == 3 && idx != 3 && hubBench != null && benchmarkRunning) {
            hubBench.cancel();
            benchmarkRunning = false;
        }
        currentScreen = idx;
        if (idx != 4) {
            predictSheetOpen = false;
            predictSheetCloseCallback = null;
        }

        final View old = fragmentContainer.getChildCount() > 0 ? fragmentContainer.getChildAt(0) : null;
        if (!animate) {
            fragmentContainer.removeAllViews();
        } else if (old != null) {
            old.animate().cancel();
        }

        View view;
        switch (idx) {
            case 1:  view = getLayoutInflater().inflate(R.layout.screen_updates, fragmentContainer, false); setupUpdatesScreen(view); break;
            case 2:  view = getLayoutInflater().inflate(R.layout.screen_info, fragmentContainer, false); setupInfoScreen(view); break;
            case 3:  view = getLayoutInflater().inflate(R.layout.screen_benchmark, fragmentContainer, false); setupBenchmarkScreen(view); break;
            case 4:  view = getLayoutInflater().inflate(R.layout.screen_predict, fragmentContainer, false); setupPredictScreen(view); break;
            case 5:  view = getLayoutInflater().inflate(R.layout.screen_settings, fragmentContainer, false); setupSettingsScreen(view); break;
            default: view = getLayoutInflater().inflate(R.layout.screen_home, fragmentContainer, false); setupHomeButtons(view); break;
        }

        UiLocalizer.localizeViewTree(view, this);
        if (!animate) {
            fragmentContainer.addView(view);
            return;
        }

        screenTransitionRunning = true;
        float direction = (idx > prevScreen) ? 1f : -1f;
        view.setTranslationX(64f * direction);
        view.setAlpha(0f);
        view.setScaleX(0.985f);
        view.setScaleY(0.985f);
        fragmentContainer.addView(view);
        view.bringToFront();

        android.animation.AnimatorSet enter = new android.animation.AnimatorSet();
        enter.playTogether(
            android.animation.ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 64f * direction, 0f),
            android.animation.ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
            android.animation.ObjectAnimator.ofFloat(view, View.SCALE_X, 0.985f, 1f),
            android.animation.ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.985f, 1f)
        );
        enter.setDuration(280);
        enter.setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f));

        if (old != null) {
            android.animation.AnimatorSet exit = new android.animation.AnimatorSet();
            exit.playTogether(
                android.animation.ObjectAnimator.ofFloat(old, View.TRANSLATION_X, 0f, -36f * direction),
                android.animation.ObjectAnimator.ofFloat(old, View.ALPHA, 1f, 0f),
                android.animation.ObjectAnimator.ofFloat(old, View.SCALE_X, 1f, 0.992f),
                android.animation.ObjectAnimator.ofFloat(old, View.SCALE_Y, 1f, 0.992f)
            );
            exit.setDuration(220);
            exit.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            exit.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    fragmentContainer.removeView(old);
                }
            });
            exit.start();
        }

        enter.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                screenTransitionRunning = false;
            }

            @Override public void onAnimationCancel(android.animation.Animator animation) {
                screenTransitionRunning = false;
            }
        });
        enter.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HOME SCREEN
    // ════════════════════════════════════════════════════════════════════════

    private void setupHomeButtons(View v) {
        View bgAnim = v.findViewById(R.id.bg_anim);
        if (bgAnim != null) {
            bgAnim.setVisibility(prefs.isBgAnim() ? View.VISIBLE : View.GONE);
        }
        if (!prefs.isLiquidGlass()) {
            int[] cardIds = {
                R.id.btn_donate,
                R.id.btn_fps, R.id.btn_performance, R.id.btn_animations,
                R.id.btn_ram_clean, R.id.btn_network_opt, R.id.btn_battery_opt,
                R.id.btn_disable_apps, R.id.btn_radio_info, R.id.btn_cit_test,
                R.id.btn_usb_debug, R.id.btn_display_hidden, R.id.btn_ads,
                R.id.btn_hide_labels, R.id.btn_region_hack, R.id.btn_second_space,
                R.id.btn_font_hack, R.id.btn_telegram,
                R.id.btn_game_mode, R.id.btn_miui_debug, R.id.btn_xiaomi_labs
            };
            for (int id : cardIds) {
                View found = v.findViewById(id);
                if (found instanceof HyperCard) {
                    ((HyperCard) found).setGlassEnabled(false);
                } else if (found instanceof LiquidGlassCardView) {
                    ((LiquidGlassCardView) found).setGlassEnabled(false);
                }
            }
        }

        click(v, R.id.btn_fps, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Разгон FPS")
                .body("Важно: незначительно увеличивает нагрузку на CPU при трассировке. Рекомендуется на устройствах с 6+ ГБ RAM.\n\n" +
                    "1. Настройки → О телефоне → нажмите 7 раз на «Версию ОС», чтобы стать разработчиком.\n\n" +
                    "2. Дополнительно → Для разработчиков → найдите «Размер буфера трассировки» и выставьте максимум (32 МБ на ядро).\n\n" +
                    "3. Отключите «Включить трассировку по умолчанию» — снимает фоновую запись системных событий.")
                .show();
        }});

        final String cmdPerf = "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.fboservice.ctrl true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.use_mi_new_strategy true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.smart_gc.enable true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.power.default.powermode enhance\" s16 \"/storage/emulated/0/log.txt\" i32 600";

        click(v, R.id.btn_performance, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Режим производительности")
                .body("Важно: повышает энергопотребление и нагрев устройства. Не рекомендуется при заряде ниже 30%.\n\nЕсли команда возвращает ошибку — служба MQS недоступна на вашей прошивке (часть версий HyperOS 2.x).\n\nТребуется Termux или ADB Shell. Скопируйте команды и выполните по одной. После — перезагрузите телефон.")
                .copyButton("Скопировать команды", cmdPerf)
                .show();
        }});

        final String cmdAnim = "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.cpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.gpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.background_blur_supported true\" s16 \"/storage/emulated/0/log.txt\" i32 600";

        click(v, R.id.btn_animations, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Флагманские анимации")
                .body("Важно: фоновое размытие увеличивает нагрузку на GPU. На слабых устройствах (4 ГБ RAM) возможны подвисания.\n\nВключает плавные анимации и blur-эффекты на бюджетных POCO/Redmi.\n\n1. Скачайте Brevent или Shizuku + aShell.\n2. Активируйте беспроводную отладку.\n3. Выполните команды через ADB.\n4. Перезагрузите устройство.")
                .copyButton("Скопировать команды", cmdAnim)
                .show();
        }});

        click(v, R.id.btn_ram_clean, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("RAM и фоновые процессы")
                .body("Android самостоятельно управляет памятью через механизм LMK (Low Memory Killer) — это эффективнее любой ручной чистки.\n\nЕсли телефон реально тормозит из-за нехватки RAM:\n\n1. Настройки → Приложения → найдите «тяжёлые» приложения и ограничьте их фоновую активность.\n\n2. Безопасность → Оптимизация — штатный инструмент HyperOS.\n\n3. Отключите ненужные автозапуски: Настройки → Приложения → Автозапуск.")
                .show();
        }});

        final String cmdNet = "settings put global wifi_scan_throttle_enabled 0\nsettings put global network_scoring_ui_enabled 0";
        click(v, R.id.btn_network_opt, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Оптимизация пинга")
                .body("Важно: отключает автоматическую оценку качества сети. Телефон перестанет автоматически переключаться на более быстрый Wi-Fi.\n\nОтключает ограничение частоты Wi-Fi-сканирования и лишние сетевые службы — снижает задержку в играх.\n\nТребуется ADB Shell. Выполните команды через Termux или компьютер.")
                .copyButton("Скопировать команды", cmdNet)
                .show();
        }});

        final String cmdBatt = "settings put global low_power_sticky 0\nsettings put system screen_off_timeout 120000";
        click(v, R.id.btn_battery_opt, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Настройка энергорежима")
                .body("Важно: отключает принудительный режим экономии — телефон будет расходовать чуть больше энергии в обычном режиме, но работать стабильнее.\n\n• Снимает принудительный Low Power режим\n• Выставляет таймаут экрана 2 минуты\n\nДля реальной экономии батареи дополнительно отключите «Всегда активный экран» и уберите лишние фоновые приложения.")
                .copyButton("Скопировать команды", cmdBatt)
                .show();
        }});

        final String cmdGame = "settings put system miui_gaming_mode_enabled 1\nsettings put system miui_gaming_notification_enabled 0\nsettings put system miui_gaming_touch_mode 1";
        click(v, R.id.btn_game_mode, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Игровой турбо-режим")
                .body("Важно: блокирует уведомления во время игры. После выхода из игры режим отключается автоматически.\n\nВключает встроенный «Game Turbo» режим HyperOS:\n• Максимальная приоритизация CPU/GPU для игр\n• Отключение лишних фоновых уведомлений\n• Улучшенный тач-отклик\n\nТакже: Настройки → Особые возможности → Game Turbo → добавьте ваши игры.")
                .copyButton("Скопировать ADB-команды", cmdGame)
                .show();
        }});

        final String cmdMiuiDebug = "adb shell am start -n com.android.settings/.Settings\nsettings put global development_settings_enabled 1";
        click(v, R.id.btn_miui_debug, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("MIUI/HyperOS Debug Tools")
                .body("Набор скрытых инженерных инструментов для диагностики:\n\n• *#*#6484#*#* — Диагностика hardware\n• *#*#4636#*#* — Phone Info (сотовая сеть, батарея)\n• *#*#2846579#*#* — ProjectMenu HUAWEI (только EMUI)\n• *#*#0*#*#* — LCD тест\n• *#*#7378423#*#* — Service menu Sony\n\nДля MIUI/HyperOS нажмите кнопку ниже для открытия инженерного меню через код набора.")
                .copyButton("Скопировать инфо", "*#*#4636#*#* → Phone Info\n*#*#6484#*#* → Hardware Test\n*#*#0*#*#* → LCD Test")
                .show();
        }});

        click(v, R.id.btn_xiaomi_labs, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("Xiaomi / POCO Labs")
                .body("Бета-функции, доступные через скрытые разделы прошивки:\n\n• Always-on Display — Настройки → Экран → Всегда активный экран\n\n• Note Asst — скрытый AI-помощник, активируется через Глобальный/CN регион\n\n• AI Call Recording (CN прошивка) — автоматическая транскрипция\n\n• Hyper Charge профили — Настройки → Батарея → Режим зарядки\n\n• Dynamic Island style — сторонние решения: Dynamic Notch, ZaBar\n\n️ Для части функций нужен CN-регион или специальная прошивка.")
                .show();
        }});

        click(v, R.id.btn_disable_apps, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(tr(" Отключение bloatware"))
                .setMessage(tr("Важно: Отключение системных MIUI-служб может нарушить работу уведомлений, синхронизации и платёжных сервисов.\n\nНе отключайте то, в чём не уверены.\n\nБезопасно отключать: Mi Video, Mi Music, Mi Browser, GetApps, Mi AI."))
                .setPositiveButton(tr("Открыть приложения"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        launchSettings(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    }
                })
                .setNegativeButton(tr("Отмена"), null)
                .show();
        }});

        click(v, R.id.btn_radio_info, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(tr(" Radio Info"))
                .setMessage(tr("Инженерный экран для просмотра параметров сотовой сети: уровень сигнала, тип сети (LTE/5G), информация о базовых станциях.\n\n️ Не меняйте настройки в этом меню без понимания — можно потерять сигнал сети. Используйте только для просмотра.\n\nЕсли экран не откроется — попробуйте набрать *#*#4636#*#* в приложении «Телефон»."))
                .setPositiveButton(tr("Открыть"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String[] candidates = {
                            "com.android.settings.RadioInfo",
                            "com.android.phone.settings.RadioInfo",
                            "com.android.phone.RadioInfo"
                        };
                        boolean opened = false;
                        for (String cls : candidates) {
                            try {
                                Intent i = new Intent(Intent.ACTION_MAIN);
                                i.setClassName("com.android.settings", cls);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                opened = true;
                                break;
                            } catch (Exception ignored) {}
                            try {
                                Intent i = new Intent(Intent.ACTION_MAIN);
                                i.setClassName("com.android.phone", cls);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                opened = true;
                                break;
                            } catch (Exception ignored) {}
                        }
                        if (!opened) {
                            Toast.makeText(MainActivity.this,
                                LocaleHelper.isEnglish(MainActivity.this)
                                    ? "Radio Info is unavailable. Try *#*#4636#*#*"
                                    : "Radio Info недоступен. Попробуйте *#*#4636#*#*",
                                Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(tr("Отмена"), null)
                .show();
        }});

        click(v, R.id.btn_cit_test, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(tr(" CIT Диагностика"))
                .setMessage(tr("Заводской тест аппаратной части: экран, виброотдача, камера, микрофон, датчики, Wi-Fi, Bluetooth.\n\n️ Некоторые тесты запускают датчики в нестандартном режиме. Не прерывайте тест принудительно — завершайте через кнопку «Выход» внутри приложения."))
                .setPositiveButton(tr("Открыть"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String[][] candidates = {
                            {"com.miui.cit",          "com.miui.cit.CITActivity"},
                            {"com.miui.cit",          "com.miui.cit.MainActivity"},
                            {"com.qualcomm.qti.qdma", "com.qualcomm.qti.qdma.MainActivity"}
                        };
                        boolean opened = false;
                        for (String[] pkgCls : candidates) {
                            try {
                                Intent i = new Intent(Intent.ACTION_MAIN);
                                i.setClassName(pkgCls[0], pkgCls[1]);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                opened = true;
                                break;
                            } catch (Exception ignored) {}
                        }
                        if (!opened) {
                            Toast.makeText(MainActivity.this,
                                LocaleHelper.isEnglish(MainActivity.this)
                                    ? "CIT test is unavailable on this firmware."
                                    : "CIT-тест недоступен на этой прошивке.",
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(tr("Отмена"), null)
                .show();
        }});

        click(v, R.id.btn_usb_debug, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(tr("️ Параметры разработчика"))
                .setMessage(tr("Важно: Раздел для опытных пользователей. Некорректные настройки могут снизить производительность или нарушить работу системы.\n\nДля включения ADB: откройте этот раздел → «Отладка по USB» → подтвердите на компьютере при первом подключении."))
                .setPositiveButton(tr("Открыть"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        launchSettings(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    }
                })
                .setNegativeButton(tr("Отмена"), null)
                .show();
        }});

        click(v, R.id.btn_display_hidden, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("️ Скрытые настройки дисплея")
                .body("Все параметры меняются штатно через настройки — без ADB и root.\n\n• DC Dimming — снижает мерцание ШИМ на низкой яркости. Настройки → Экран → Дополнительные настройки. Полезно при усталости глаз.\n\n• Частота обновления — там же, выберите «Auto» или фиксированный 120 Гц. Auto экономит батарею.\n\n• Цветовой профиль — Настройки → Экран → Цветовая схема. «Насыщенный» + ручной баланс для AMOLED.\n\n• Режим чтения — Настройки → Спецвозможности. Снижает синий свет без желтизны от PWM.\n\n️ DC Dimming может незначительно снизить равномерность яркости экрана.")
                .show();
        }});

        click(v, R.id.btn_ads, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title(" Отключение рекламы")
                .body("Не влияет на производительность. Только убирает рекламные рекомендации.\n\n1. Настройки → Пароли и безопасность → Доступ к личным данным.\n\n2. Найдите «msa» (MIUI System Ads), отзовите разрешение. Подождите 10 сек и подтвердите.\n\n3. Безопасность → Настройки () → Отключите «Получать рекомендации».\n\n4. Повторите в Проводнике и Очистке.\n\n5. Браузер Mi → Настройки → Конфиденциальность → отключите персонализированную рекламу.")
                .show();
        }});

        final String cmdLabels = "settings put system miui_home_no_word_model 1; settings put system show_text_under_icons 0; settings put system shelter_icon_name 1; settings put system miui_home_icon_title_max_lines 0; settings put system miui_home_icon_text_size 0; settings put system hide_icon_labels 1";
        click(v, R.id.btn_hide_labels, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title("️ Чистый рабочий стол")
                .body("Важно: После выполнения подписи под иконками пропадут. Чтобы вернуть — перезагрузите телефон или выполните сброс лаунчера.\n\nСкрывает все подписи под иконками — рабочий стол выглядит минималистично.\n\nВыполните в Termux или ADB Shell. После — перезапустите лаунчер или перезагрузите телефон.")
                .copyButton("Скопировать команду", cmdLabels)
                .show();
        }});

        click(v, R.id.btn_region_hack, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title(" Фишки смены региона")
                .body("Смена региона обратима — можно вернуть в любой момент. Язык системы остаётся русским.\n\n️ После смены региона некоторые приложения могут попросить обновить данные. Проверьте работу банковских приложений после смены.\n\nНастройки → Расширенные настройки → Регион.\n\nСингапур — снимает ограничение громкости наушников по EU-нормам. Обновления приходят раньше.\n\nИндия — открывает каталог шрифтов в «Темах» + системные звуки.\n\nСША — оптимизирует плавность жестов лаунчера.")
                .show();
        }});

        click(v, R.id.btn_second_space, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title(" Second Space / Клонирование")
                .body("Важно: Second Space занимает дополнительное место в памяти (~1–2 ГБ). На устройствах с 4 ГБ RAM может замедлить переключение между профилями.\n\nSecond Space — второй полноценный профиль с отдельными аккаунтами и приложениями.\n\nКак включить:\nНастройки → Спецвозможности → Second Space → Включить.\n\nКлонирование приложений:\nНастройки → Приложения → Клонирование приложений. Поддерживаются Telegram, WhatsApp, Instagram и другие.\n\nОба профиля полностью изолированы: разные аккаунты Google, разные данные.")
                .show();
        }});

        click(v, R.id.btn_font_hack, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            new GlassSheet(MainActivity.this)
                .title(" Сторонние шрифты")
                .body("Важно: Root-метод: замена системного шрифта напрямую. При неправильном шрифте интерфейс может отображаться некорректно. Сделайте резервную копию оригинального файла перед заменой.\n\nС Root:\n1. Скопируйте TTF/OTF в /sdcard/Download/.\n2. Переименуйте в MiLanProVF.ttf.\n3. Через Root Explorer скопируйте в /system/fonts/ с заменой.\n\nБез Root (рекомендуется):\nShizuku + Font Manager из Play Market — позволяет ставить шрифты через ADB без root.")
                .show();
        }});

        // ── TELEGRAM ─────────────────────────────────────────────────────────
        click(v, R.id.btn_telegram, new View.OnClickListener() { @Override public void onClick(View x) {
            vibrate();
            openUrl("https://t.me/HyperHubRu");
        }});

        click(v, R.id.btn_donate, new View.OnClickListener() {
            @Override public void onClick(View x) {
                vibrate();
                openUrl("https://www.donationalerts.com/r/xiaot");
            }
        });

        bindHomeSearch(v);
    }

    private void bindHomeSearch(final View v) {
        final EditText search = (EditText) v.findViewById(R.id.home_search);
        final TextView empty = (TextView) v.findViewById(R.id.home_empty_state);
        if (search == null) return;

        final int[] cardIds = {
            R.id.btn_donate,
            R.id.btn_fps, R.id.btn_performance, R.id.btn_animations, R.id.btn_ram_clean,
            R.id.btn_network_opt, R.id.btn_battery_opt, R.id.btn_disable_apps, R.id.btn_radio_info,
            R.id.btn_cit_test, R.id.btn_usb_debug, R.id.btn_display_hidden, R.id.btn_ads,
            R.id.btn_hide_labels, R.id.btn_region_hack, R.id.btn_second_space, R.id.btn_font_hack,
            R.id.btn_telegram, R.id.btn_game_mode, R.id.btn_miui_debug, R.id.btn_xiaomi_labs
        };
        final String[] keywords = {
            "support donationalerts donate поддержать донат",
            "fps boost performance trace tracing gpu разгон производительность трассировка",
            "performance perf mqs enhance mode производительность mqс enhance",
            "animations blur gpu flagship smooth анимации blur флагман",
            "ram memory cleanup lmk background память очистка ram",
            "network ping wifi latency signal сеть ping wifi задержка",
            "battery power saving timeout charge батарея заряд энергосбережение",
            "bloatware disable mi apps приложения отключить mi",
            "radio info network engineer lte 5g сеть инженерный",
            "cit test diagnostics hardware тест диагностика cit",
            "usb adb developer options разработчик",
            "display screen dc dimming 120hz pwm дисплей экран",
            "ads msa recommendations browser реклама",
            "labels icons home screen launcher ярлыки подписи рабочий стол",
            "region singapore india usa регион",
            "second space clone profile apps клонирование профиль",
            "font custom root milanpro shizuku шрифт",
            "telegram channel community канал сообщество",
            "game turbo gaming mode игра режим",
            "debug miui engineering codes инженерный код набор",
            "labs xiaomi poco beta features бета функции"
        };

        Runnable filter = new Runnable() {
            @Override public void run() {
                String query = search.getText() == null ? "" : UiLocalizer.normalizeSearch(search.getText().toString().trim().toLowerCase(Locale.ROOT));
                int visibleCount = 0;
                for (int i = 0; i < cardIds.length; i++) {
                    View card = v.findViewById(cardIds[i]);
                    if (card == null) continue;
                    boolean visible = query.isEmpty() || keywords[i].contains(query);
                    card.setVisibility(visible ? View.VISIBLE : View.GONE);
                    if (visible) visibleCount++;
                }
                if (empty != null) {
                    empty.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
                }
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        filter.run();
    }

    private void setupUpdatesScreen(View v) {
        applyVisualPrefs(v);
        click(v, R.id.btn_check_update, new View.OnClickListener() {
            @Override public void onClick(View vv) {
                vibrate();
                openUrl(URL_TELEGRAM_CHANNEL);
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // INFO SCREEN
    // ════════════════════════════════════════════════════════════════════════

    private void applyVisualPrefs(View v) {
        View bgAnim = v.findViewById(R.id.bg_anim);
        if (bgAnim != null) bgAnim.setVisibility(prefs.isBgAnim() ? View.VISIBLE : View.GONE);
        if (!prefs.isLiquidGlass()) {
            int[] allCardIds = {
                R.id.btn_donate,
                R.id.card_about_device, R.id.card_language, R.id.card_toggle_bg_anim,
                R.id.card_toggle_haptic, R.id.card_toggle_splash, R.id.card_developer,
                R.id.card_telegram_channel, R.id.card_donate, R.id.card_privacy,
                R.id.card_inf_1, R.id.card_inf_2, R.id.card_inf_3,
                R.id.card_inf_4, R.id.card_inf_5,
                R.id.btn_fps, R.id.btn_performance, R.id.btn_animations,
                R.id.btn_ram_clean, R.id.btn_network_opt, R.id.btn_battery_opt,
                R.id.btn_disable_apps, R.id.btn_radio_info, R.id.btn_cit_test,
                R.id.btn_usb_debug, R.id.btn_display_hidden, R.id.btn_ads,
                R.id.btn_hide_labels, R.id.btn_region_hack, R.id.btn_second_space,
                R.id.btn_font_hack, R.id.btn_telegram,
                R.id.btn_game_mode, R.id.btn_miui_debug, R.id.btn_xiaomi_labs
            };
            for (int id : allCardIds) {
                View found = v.findViewById(id);
                if (found instanceof HyperCard) {
                    ((HyperCard) found).setGlassEnabled(false);
                } else if (found instanceof LiquidGlassCardView) {
                    ((LiquidGlassCardView) found).setGlassEnabled(false);
                }
            }
        }
    }

    private void setupInfoScreen(final View v) {
        applyVisualPrefs(v);

        View refresh = v.findViewById(R.id.info_btn_refresh);
        if (refresh != null) {
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    vibrate();
                    populateInfoScreen(v);
                    UiLocalizer.localizeViewTree(v, MainActivity.this);
                    Toast.makeText(MainActivity.this, tr("Данные обновлены"), Toast.LENGTH_SHORT).show();
                }
            });
        }

        View copy = v.findViewById(R.id.info_btn_copy);
        if (copy != null) {
            copy.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    vibrate();
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("device_info", buildInfoSummary(v)));
                        Toast.makeText(MainActivity.this, tr("Сводка скопирована"), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        populateInfoScreen(v);
        UiLocalizer.localizeViewTree(v, this);
    }

    private void populateInfoScreen(View v) {
        setText(v, R.id.info_model, Build.MODEL);
        setText(v, R.id.info_android, "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        setText(v, R.id.info_firmware, Build.DISPLAY);
        setText(v, R.id.info_security_patch, getSecurityPatch());
        setText(v, R.id.info_uptime, getDeviceUptime());
        setText(v, R.id.info_screen, getScreenRes());
        setText(v, R.id.info_cores, Runtime.getRuntime().availableProcessors() + " ядер");
        setText(v, R.id.info_cpu, getCpuName());
        setText(v, R.id.info_cpu_abi, Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : "Н/Д");
        setText(v, R.id.info_cpu_freq, getMaxCpuFreq());
        setText(v, R.id.info_gpu, getGpuRenderer());
        setText(v, R.id.info_cpu_temp, getCpuTemp());

        IntentFilter bf = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent bi = registerReceiver(null, bf);
        if (bi != null) {
            int level  = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale  = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct    = (scale > 0) ? (int) (level * 100f / scale) : 0;
            int temp   = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int volt   = bi.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            int status = bi.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = bi.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

            setText(v, R.id.info_battery, pct + "%");

            String statusStr;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:    statusStr = "Заряжается "; break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING: statusStr = "Разряжается"; break;
                case BatteryManager.BATTERY_STATUS_FULL:        statusStr = "Заряжен "; break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:statusStr = "Не заряд."; break;
                default: statusStr = "Н/Д";
            }
            String plugStr = "";
            if (plugged == BatteryManager.BATTERY_PLUGGED_USB)       plugStr = " (USB)";
            else if (plugged == BatteryManager.BATTERY_PLUGGED_AC)   plugStr = " (AC)";
            else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) plugStr = LocaleHelper.isEnglish(this) ? " (Wireless)" : " (БП)";

            setText(v, R.id.info_charge_status, statusStr + plugStr);
            setText(v, R.id.info_temp, String.format(Locale.getDefault(), "%.1f °C", temp / 10f));
            setText(v, R.id.info_voltage, String.format(Locale.getDefault(), LocaleHelper.isEnglish(this) ? "%.3f V" : "%.3f В", volt / 1000f));

            BatteryBarView bBar = (BatteryBarView) v.findViewById(R.id.battery_bar);
            if (bBar != null) bBar.setPercent(pct / 100f);
        }

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mi);
            long usedRam = mi.totalMem - mi.availMem;
            float ramPct = (float) usedRam / mi.totalMem;
            long usedMb  = usedRam / (1024 * 1024);
            long totalMb = mi.totalMem / (1024 * 1024);
            setText(v, R.id.info_ram_pct, (int) (ramPct * 100) + "%");
            setText(v, R.id.info_ram, LocaleHelper.isEnglish(this)
                    ? ("Using " + usedMb + " MB of " + totalMb + " MB")
                    : ("Используется " + usedMb + " МБ из " + totalMb + " МБ"));
            UsageBarView ramBar = (UsageBarView) v.findViewById(R.id.ram_bar);
            if (ramBar != null) ramBar.setPercent(ramPct);
        }

        StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
        long freeBytes  = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
        long totalBytes = sf.getBlockCountLong() * sf.getBlockSizeLong();
        long usedBytes  = totalBytes - freeBytes;
        float stPct     = (float) usedBytes / totalBytes;
        long freeGb     = freeBytes  / (1024 * 1024 * 1024);
        long totalGb    = totalBytes / (1024 * 1024 * 1024);
        setText(v, R.id.info_storage_pct, (int) (stPct * 100) + "%");
        setText(v, R.id.info_storage, LocaleHelper.isEnglish(this)
                ? ("Free " + freeGb + " GB of " + totalGb + " GB")
                : ("Свободно " + freeGb + " ГБ из " + totalGb + " ГБ"));
        UsageBarView stBar = (UsageBarView) v.findViewById(R.id.storage_bar);
        if (stBar != null) stBar.setPercent(stPct);

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String op = tm != null ? tm.getNetworkOperatorName() : "";
            setText(v, R.id.info_operator, op.isEmpty() ? "Н/Д" : op);
        } catch (Exception e) {
            setText(v, R.id.info_operator, "Н/Д");
        }
        setText(v, R.id.info_ip, getLocalIp());

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null) {
                    String ssid = wi.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    setText(v, R.id.info_wifi_ssid, (ssid == null || ssid.isEmpty()) ? "Не подключено" : ssid);
                    int linkSpeedMbps = wi.getLinkSpeed();
                    setText(v, R.id.info_wifi_speed, linkSpeedMbps > 0 ? linkSpeedMbps + (LocaleHelper.isEnglish(this) ? " Mbps" : " Мбит/с") : "Н/Д");
                    int rssi = wi.getRssi();
                    setText(v, R.id.info_wifi_rssi, rssi != 0 ? rssi + " dBm" : "Н/Д");
                } else {
                    setText(v, R.id.info_wifi_ssid, "Не подключено");
                    setText(v, R.id.info_wifi_speed, "—");
                    setText(v, R.id.info_wifi_rssi, "—");
                }
            } else {
                setText(v, R.id.info_wifi_ssid, "Wi‑Fi выключен");
                setText(v, R.id.info_wifi_speed, "—");
                setText(v, R.id.info_wifi_rssi, "—");
            }
        } catch (Exception e) {
            setText(v, R.id.info_wifi_ssid, "Н/Д");
            setText(v, R.id.info_wifi_speed, "Н/Д");
            setText(v, R.id.info_wifi_rssi, "Н/Д");
        }
        UiLocalizer.localizeViewTree(v, this);
    }

    private String buildInfoSummary(View v) {
        return " HyperHub — Сводка устройства\n"
                + "════════════════════════\n"
                + "Модель: "            + getTextValue(v, R.id.info_model) + "\n"
                + "Android: "           + getTextValue(v, R.id.info_android) + "\n"
                + "Сборка: "            + getTextValue(v, R.id.info_firmware) + "\n"
                + "Патч безопасности: " + getTextValue(v, R.id.info_security_patch) + "\n"
                + "Аптайм: "            + getTextValue(v, R.id.info_uptime) + "\n"
                + "CPU: "               + getTextValue(v, R.id.info_cpu) + "\n"
                + "Ядра: "              + getTextValue(v, R.id.info_cores) + "\n"
                + "GPU: "               + getTextValue(v, R.id.info_gpu) + "\n"
                + "Температура CPU: "   + getTextValue(v, R.id.info_cpu_temp) + "\n"
                + "Батарея: "           + getTextValue(v, R.id.info_battery) + " — " + getTextValue(v, R.id.info_charge_status) + "\n"
                + "Темп. батареи: "     + getTextValue(v, R.id.info_temp) + "\n"
                + "Напряжение: "        + getTextValue(v, R.id.info_voltage) + "\n"
                + "RAM: "               + getTextValue(v, R.id.info_ram) + "\n"
                + "Хранилище: "         + getTextValue(v, R.id.info_storage) + "\n"
                + "Wi‑Fi: "             + getTextValue(v, R.id.info_wifi_ssid) + "\n"
                + "Скорость Wi‑Fi: "    + getTextValue(v, R.id.info_wifi_speed) + "\n"
                + "IP: "                + getTextValue(v, R.id.info_ip) + "\n"
                + "Оператор: "          + getTextValue(v, R.id.info_operator);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PREDICT SCREEN
    // ════════════════════════════════════════════════════════════════════════

    private Runnable predictSheetCloseCallback = null;

    private void setupPredictScreen(final View v) {
        applyVisualPrefs(v);

        String model = Build.MODEL.trim();
        String device = Build.DEVICE.trim();

        String displayName = DeviceNameResolver.resolve(model, device);

        setText(v, R.id.predict_device_name, displayName);
        setText(v, R.id.predict_device_model, model + " / " + device);

        final String[] willGet = {
            // Xiaomi
            "Xiaomi 17","Xiaomi 17 Pro","Xiaomi 17 Pro Max","Xiaomi 17 Max","Xiaomi 17 Ultra","Xiaomi 17T","Xiaomi 17T Pro",
            "Xiaomi 15","Xiaomi 15 Pro","Xiaomi 15 Ultra","Xiaomi 15S Pro","Xiaomi 15T","Xiaomi 15T Pro",
            "Xiaomi 14","Xiaomi 14 Pro","Xiaomi 14 Ultra","Xiaomi 14T","Xiaomi 14T Pro","Xiaomi 14 Civi",
            "Xiaomi 13","Xiaomi 13 Pro","Xiaomi 13 Ultra","Xiaomi 13T","Xiaomi 13T Pro",
            "Xiaomi Civi 4 Pro","Xiaomi Civi 5 Pro",
            "Xiaomi Mix Flip","Xiaomi Mix Flip 2","Xiaomi Mix Fold 3","Xiaomi Mix Fold 4",
            "Xiaomi Pad 8","Xiaomi Pad 8 Pro","Xiaomi Pad 7","Xiaomi Pad 7 Pro","Xiaomi Pad 7 Ultra","Xiaomi Pad 7S Pro","Xiaomi Pad 6S Pro","Xiaomi Pad Mini",

            // Redmi
            "Redmi K90","Redmi K90 Max","Redmi K90 Pro","Redmi K90 Pro Max","Redmi K90 Ultra",
            "Redmi K80","Redmi K80 Pro","Redmi K80 Ultra",
            "Redmi K70","Redmi K70 Pro","Redmi K70 Ultra",
            "Redmi K60 Ultra",
            "Redmi Note 15","Redmi Note 15 Pro","Redmi Note 15 Pro+","Redmi Note 15 SE","Redmi Note 15R",
            "Redmi Note 14","Redmi Note 14 Pro","Redmi Note 14 Pro+","Redmi Note 14S",
            "Redmi Turbo 4","Redmi Turbo 4 Pro","Redmi Turbo 5",
            "Redmi 15","Redmi 15A","Redmi 15C","Redmi 15R",
            "Redmi A7 Pro",
            "Redmi Pad 2","Redmi Pad 2 Pro","Redmi K Pad",

            // POCO
            "POCO F8","POCO F8 Pro","POCO F8 Ultra",
            "POCO F7","POCO F7 Pro","POCO F7 Ultra",
            "POCO F6","POCO F6 Pro",
            "POCO X8","POCO X8 Pro","POCO X8 Pro Max",
            "POCO X7","POCO X7 Pro","POCO X6 Pro",
            "POCO M8","POCO M8s","POCO M8 Pro",
            "POCO M7","POCO M7 4G","POCO M7 Pro","POCO M7 Plus",
            "POCO C85","POCO C85x","POCO C81","POCO C81 Pro",
            "POCO Pad X1","POCO Pad M1","POCO Pad C1"
        };

        final String[] wontGet = {
            // Xiaomi
            "Xiaomi 13 Lite","Xiaomi Civi 3","Xiaomi 12","Xiaomi 12 Pro","Xiaomi 12T","Xiaomi 12T Pro","Xiaomi 12S","Xiaomi 12S Pro","Xiaomi 12S Ultra","Xiaomi MIX 4","Xiaomi Mix Fold 2",

            // Redmi
            "Redmi 14","Redmi 14C","Redmi Note 13","Redmi Note 13 Pro","Redmi Note 13 Pro+","Redmi Note 12","Redmi Note 11",
            "Redmi K60 ","Redmi K60E","Redmi K60 Pro","Redmi K50","Redmi K50 Ultra","Redmi Turbo 3",
            "Redmi A3","Redmi A3x","Redmi A4","Redmi A5","Redmi Pad Pro","Redmi Pad SE",

            // POCO
            "POCO C75","POCO C71","POCO C81x","POCO C65","POCO C61","POCO C55","POCO C51",
            "POCO M4","POCO M3","POCO X4","POCO X3","POCO F4","POCO F3"
        };

        String lowerModel   = model.toLowerCase(Locale.ROOT);
        String lowerDevice  = device.toLowerCase(Locale.ROOT);
        String lowerDisplay = displayName.toLowerCase(Locale.ROOT);

        boolean gets = false;
        for (String code : willGet) {
            String lc = code.toLowerCase(Locale.ROOT);
            if (lowerModel.contains(lc) || lowerDevice.contains(lc) || lowerDisplay.contains(lc)) {
                gets = true;
                break;
            }
        }

        boolean wont = false;
        if (!gets) {
            for (String code : wontGet) {
                String lc = code.toLowerCase(Locale.ROOT).trim();
                if (lowerModel.contains(lc) || lowerDevice.contains(lc) || lowerDisplay.contains(lc) ||
                    lowerModel.startsWith(lc) || lowerDisplay.startsWith(lc) ||
                    lowerDisplay.equals(lc) ||
                    lowerDisplay.contains(lc + " ") || lowerDisplay.contains(lc + "/") ||
                    lowerModel.contains(lc + " ") || lowerModel.contains(lc + "/")) {
                    wont = true;
                    break;
                }
            }
        }

        final android.view.View badge = v.findViewById(R.id.predict_badge);
        final TextView badgeIcon = (TextView) v.findViewById(R.id.predict_badge_icon);
        final TextView badgeText = (TextView) v.findViewById(R.id.predict_badge_text);

        if (badge != null && badgeIcon != null && badgeText != null) {
            String icon, text, desc;
            int bgRes;
            if (gets) {
                icon  = "";
                text  = "Получит HyperOS 4";
                desc  = "Получит обновление HyperOS 4";
                bgRes = R.drawable.predict_badge_green;
            } else if (wont) {
                icon  = "";
                text  = "Не получит HyperOS 4";
                desc  = "Не получит обновление HyperOS 4";
                bgRes = R.drawable.predict_badge_bg;
            } else {
                icon  = "";
                text  = "Возможно, не получит";
                desc  = "Данные по устройству отсутствуют";
                bgRes = R.drawable.predict_badge_bg;
            }
            badgeIcon.setText(icon);
            badgeText.setText(text);
            badge.setBackgroundResource(bgRes);
            badge.setContentDescription(desc);
        }

        final android.view.View overlay = v.findViewById(R.id.predict_sheet_overlay);
        final android.view.View sheet   = v.findViewById(R.id.predict_sheet);
        final boolean[] sheetOpen = {false};

        predictSheetCloseCallback = new Runnable() {
            @Override public void run() {
                if (sheetOpen[0]) closeSheet(sheet, overlay, sheetOpen);
            }
        };

        android.view.View swipeHint = v.findViewById(R.id.predict_swipe_hint);
        if (swipeHint != null) {
            final float[] touchStartY = {0f};
            swipeHint.setOnTouchListener(new android.view.View.OnTouchListener() {
                @Override
                public boolean onTouch(View vv, android.view.MotionEvent e) {
                    switch (e.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            touchStartY[0] = e.getRawY();
                            return true;
                        case android.view.MotionEvent.ACTION_UP:
                            float dy = touchStartY[0] - e.getRawY();
                            if (dy > 20 * getResources().getDisplayMetrics().density || Math.abs(dy) < 10) {
                                vibrate();
                                openSheet(sheet, overlay, sheetOpen);
                                populatePredictList(v);
                            }
                            return true;
                    }
                    return false;
                }
            });
            swipeHint.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View vv) {
                    vibrate();
                    openSheet(sheet, overlay, sheetOpen);
                    populatePredictList(v);
                }
            });
        }

        if (overlay != null) {
            overlay.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View vv) {
                    closeSheet(sheet, overlay, sheetOpen);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (predictSheetOpen && predictSheetCloseCallback != null) {
            predictSheetCloseCallback.run();
            return;
        }
        super.onBackPressed();
    }

    private void populatePredictList(View v) {
        LinearLayout list = (LinearLayout) v.findViewById(R.id.predict_device_list);
        if (list == null) return;
        list.removeAllViews();

        String[][] devices = {
            {"", "── XIAOMI ──────────────────", ""},
            {"", "17 / 17T / 15 / 15T / 14 / 14T / 13 / 13T", "Флагманы и T-серия ещё в окне обновлений"},
            {"", "14 Civi / Civi 4 Pro / Civi 5 Pro", "Новые Civi-модели"},
            {"", "Mix Flip / Flip 2 / Fold 3 / Fold 4", "Складные Xiaomi с активной поддержкой"},
            {"", "Pad 8 / Pad 7 / 7S Pro / 6S Pro / Mini", "Планшеты актуальных поколений"},
            {"", "13 Lite / Civi 3 / MIX Fold 2", "Пограничные модели вне безопасного окна"},
            {"", "12 / 12T / 12S / MIX 4", "Серия слишком старая для HyperOS 4"},

            {"", "── REDMI ───────────────────", ""},
            {"", "K90 / K80 / K70 / K60 Ultra", "Верхняя линейка Redmi"},
            {"", "Note 15 / Note 15 Pro / Note 15 Pro+ / Note 15R", "Новая Note-серия"},
            {"", "Note 14 / Note 14 Pro / Note 14 Pro+ / 14S", "Серия 14 ещё проходит по политике"},
            {"", "Turbo 4 / Turbo 4 Pro / Turbo 5", "Turbo-линейка"},
            {"", "Redmi 15 / 15A / 15C / 15R", "Бюджетные 15-е модели"},
            {"", "Redmi A7 Pro / Pad 2 / Pad 2 Pro / K Pad", "Из новых бюджетных и планшетов"},
            {"", "Redmi 14 / 14C", "Низкая вероятность по срокам поддержки"},
            {"", "Note 13 / Note 12 / Note 11", "Старые поколения Note"},
            {"", "K60 / K60E / K60 Pro / K50 / Turbo 3", "Вне окна Android 17 / HyperOS 4"},
            {"", "A3 / A3x / A4 / A5 / Pad Pro / Pad SE", "Политика обновлений слишком короткая"},
            {"", "14R и часть редких региональных Redmi", "Ждём официальный список для глобалки"},

            {"", "── POCO ────────────────────", ""},
            {"", "F8 / F7 / F6", "Основная F-линейка"},
            {"", "X8 / X7 / X6 Pro", "X-серия актуальных поколений"},
            {"", "M8 / M8s / M8 Pro / M7 / M7 Pro / M7 Plus", "Средний бюджетный сегмент"},
            {"", "C85 / C85x / C81 / C81 Pro", "Новые C-модели с шансом на апдейт"},
            {"", "POCO Pad X1 / M1 / C1", "Новые планшеты POCO"},
            {"", "C75 / C71 / C81x / C65 / C61 / C55 / C51", "Слишком короткое окно поддержки"},
            {"", "M4 / M3 / X4 / X3 / F4 / F3", "Старые поколения POCO"},
            {"", "Отдельные локальные ребренды", "Статус зависит от региона и базовой модели"}
        };

        float density = getResources().getDisplayMetrics().density;
        for (String[] d : devices) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            rowP.bottomMargin = (int)(8 * density);
            row.setLayoutParams(rowP);
            row.setPadding((int)(12*density), (int)(10*density), (int)(12*density), (int)(10*density));

            if (d[0].equals("")) {
                TextView sep = new TextView(this);
                sep.setText(d[1]);
                sep.setTextColor(Color.argb(120, 160, 180, 220));
                sep.setTextSize(9f);
                sep.setTypeface(android.graphics.Typeface.MONOSPACE);
                LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                sepP.topMargin = (int)(10 * density);
                sepP.bottomMargin = (int)(4 * density);
                sep.setLayoutParams(sepP);
                list.addView(sep);
                continue;
            }

            android.graphics.drawable.GradientDrawable rowBg = new android.graphics.drawable.GradientDrawable();
            rowBg.setCornerRadius(10 * density);
            int bgColor = d[0].equals("") ? Color.argb(30, 0, 220, 100)
                        : d[0].equals("") ? Color.argb(30, 220, 50, 50)
                        : Color.argb(20, 255, 255, 255);
            rowBg.setColor(bgColor);
            row.setBackground(rowBg);

            TextView icon = new TextView(this);
            icon.setText(d[0]);
            icon.setTextSize(16f);
            LinearLayout.LayoutParams iconP = new LinearLayout.LayoutParams(
                (int)(28*density), LinearLayout.LayoutParams.WRAP_CONTENT);
            icon.setLayoutParams(iconP);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(this);
            name.setText(d[1]);
            name.setTextColor(Color.WHITE);
            name.setTextSize(13f);

            TextView tag = new TextView(this);
            tag.setText(d[2]);
            tag.setTextColor(Color.argb(150, 200, 200, 220));
            tag.setTextSize(10f);

            info.addView(name);
            info.addView(tag);
            row.addView(icon);
            row.addView(info);
            list.addView(row);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // BENCHMARK SCREEN
    // ════════════════════════════════════════════════════════════════════════

    private HubBench hubBench;

    private void setupBenchmarkScreen(final View v) {
        benchScreenView = v;

        final TextView deviceLine = (TextView) v.findViewById(R.id.bench_device_line);
        final TextView status     = (TextView) v.findViewById(R.id.bench_status);
        final ProgressBar progress = (ProgressBar) v.findViewById(R.id.bench_progress);
        final View startBtn       = v.findViewById(R.id.bench_start_btn);
        final TextView startLabel = (TextView) v.findViewById(R.id.bench_start_label);

        if (deviceLine != null) {
            deviceLine.setText(getCurrentDeviceBenchmarkName() + "  •  " + getCurrentCpuBrief());
        }

        if (startBtn != null) {
            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    vibrate();
                    if (benchmarkRunning) {
                        if (hubBench != null) hubBench.cancel();
                        benchmarkRunning = false;
                        if (startLabel != null) startLabel.setText(tr("Запустить тест"));
                        if (status != null) status.setText(tr("Отменено"));
                        return;
                    }
                    startHubBench(v, status, progress, startLabel);
                }
            });
        }
    }

    private void startHubBench(final View v, final TextView status,
                               final ProgressBar progress, final TextView startLabel) {
        View resultCard = v.findViewById(R.id.bench_result_card);
        if (resultCard != null) resultCard.setVisibility(View.GONE);
        if (progress != null) progress.setProgress(0);

        benchmarkRunning = true;
        if (startLabel != null) startLabel.setText(tr("Отменить"));

        hubBench = new HubBench(this);
        hubBench.run(new HubBench.Callback() {
            @Override public void onProgress(final String stage, final int percent) {
                runOnUiThread(new Runnable() { @Override public void run() {
                    if (benchScreenView != v) return; // экран уже сменили — не трогаем
                    if (status != null) status.setText(stage);
                    if (progress != null) progress.setProgress(percent);
                }});
            }
            @Override public void onComplete(final BenchmarkResult result) {
                runOnUiThread(new Runnable() { @Override public void run() {
                    benchmarkRunning = false;
                    if (benchScreenView != v) return;
                    if (startLabel != null) startLabel.setText(tr("Запустить ещё раз"));
                    if (progress != null) progress.setProgress(100);
                    if (!result.benchmarkValid) {
                        if (status != null) status.setText(result.failureMessage != null
                                ? result.failureMessage : tr("Тест не завершён"));
                        return;
                    }
                    if (status != null) status.setText(tr("Готово"));
                    renderHubBenchmarkResult(v, result);
                }});
            }
        });
    }

    private void renderHubBenchmarkResult(View v, final BenchmarkResult r) {
        View card = v.findViewById(R.id.bench_result_card);
        if (card == null) return;
        card.setVisibility(View.VISIBLE);

        setRow(card, R.id.bench_cpu1_score, R.id.bench_cpu1_bar, R.id.bench_cpu1_raw,
                r.cpuSingleScore, r.cpuSingleRaw + " Mops/с");
        setRow(card, R.id.bench_cpuM_score, R.id.bench_cpuM_bar, R.id.bench_cpuM_raw,
                r.cpuMultiScore, r.cpuMultiRaw + " Mops/с · " + r.cpuCores + tr(" ядер"));
        setRow(card, R.id.bench_ram_score, R.id.bench_ram_bar, R.id.bench_ram_raw,
                r.ramScore, r.ramBandwidth + " " + tr("МиБ/с"));
        setRow(card, R.id.bench_sto_score, R.id.bench_sto_bar, R.id.bench_sto_raw,
                r.storageScore, r.storageRead + " SQLite TPS · " + r.storageWrite + " " + tr("МиБ/с запись"));

        TextView gpuScore = (TextView) card.findViewById(R.id.bench_gpu_score);
        ProgressBar gpuBar = (ProgressBar) card.findViewById(R.id.bench_gpu_bar);
        TextView gpuRaw = (TextView) card.findViewById(R.id.bench_gpu_raw);
        if (r.gpuTested) {
            setRow(card, R.id.bench_gpu_score, R.id.bench_gpu_bar, R.id.bench_gpu_raw,
                    r.gpuScore, String.format(Locale.US, "%.0f fps · ", r.gpuAvgFps)
                            + tr("просадка") + " " + Math.round(r.gpuDropPercent) + "% · "
                            + r.getGpuStability());
        } else {
            if (gpuScore != null) gpuScore.setText(tr("н/д"));
            if (gpuBar != null) gpuBar.setProgress(0);
            if (gpuRaw != null) gpuRaw.setText(tr("GPU-тест недоступен на этом устройстве"));
        }

        final TextView total = (TextView) card.findViewById(R.id.bench_total);
        if (total != null) {
            android.animation.ValueAnimator anim =
                    android.animation.ValueAnimator.ofInt(0, r.totalScore);
            anim.setDuration(900);
            anim.setInterpolator(new android.view.animation.DecelerateInterpolator(2f));
            anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    total.setText(fmtScore((Integer) a.getAnimatedValue()));
                }
            });
            anim.start();
        }

        TextView rating = (TextView) card.findViewById(R.id.bench_rating);
        if (rating != null) rating.setText(r.getRating());

        TextView conf = (TextView) card.findViewById(R.id.bench_confidence);
        if (conf != null) conf.setText(tr("Точность") + ": " + r.getConfidenceLabel());

        TextView caps = (TextView) card.findViewById(R.id.bench_caps);
        if (caps != null) caps.setText(r.getCapabilities());

        TextView method = (TextView) card.findViewById(R.id.bench_method);
        if (method != null) method.setText(r.getMethodologySummary() + " · HubBench v2");
    }

    private void setRow(View card, int scoreId, int barId, int rawId, int score, String raw) {
        TextView s = (TextView) card.findViewById(scoreId);
        ProgressBar b = (ProgressBar) card.findViewById(barId);
        TextView rw = (TextView) card.findViewById(rawId);
        if (s != null) s.setText(fmtScore(score));
        if (b != null) b.setProgress(benchBarPct(score));
        if (rw != null) rw.setText(raw);
    }

    /** Linear benchmark bar normalization: 10k baseline ≈ 17%, 60k = 100%. */
    private static int benchBarPct(int score) {
        return Math.max(2, Math.min(100, Math.round(score / 600f)));
    }

    private static String fmtScore(int v) {
        return String.format(Locale.US, "%,d", v).replace(',', ' ');
    }

    private String getCurrentDeviceBenchmarkName() {
        String name = DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE);
        if (name == null || name.trim().isEmpty()) name = Build.MANUFACTURER + " " + Build.MODEL;
        return name;
    }

    private String getCurrentCpuBrief() {
        String cpu = getBestCurrentCpuName();
        return cpu == null || cpu.trim().isEmpty() ? tr("CPU не определён") : cpu;
    }

    private String getBestCurrentCpuName() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                String socModel = Build.SOC_MODEL;
                if (socModel != null && socModel.trim().length() > 1 && !"unknown".equalsIgnoreCase(socModel.trim())) {
                    return socModel.trim();
                }
            }
        } catch (Throwable ignored) {}
        String cpu = getCpuName();
        if (cpu != null && cpu.trim().length() > 1) return cpu.trim();
        return Build.HARDWARE;
    }

    // ════════════════════════════════════════════════════════════════════════
    // SETTINGS SCREEN
    // ════════════════════════════════════════════════════════════════════════

    // About screen links
    private static final String URL_TELEGRAM_CHANNEL = "https://t.me/HyperHubRu";
    private static final String URL_DEVELOPER        = "https://t.me/XiaoC65";
    private static final String URL_DONATE           = "https://www.donationalerts.com/r/xiaot";
    private static final String URL_PRIVACY_POLICY   = "https://telegra.ph/Politika-konfidencialnosti-HyperHub-07-10";

    private void setupSettingsScreen(final View v) {
        applyVisualPrefs(v);
        setText(v, R.id.about_version, LocaleHelper.isEnglish(this) ? ("Version " + BuildConfig.VERSION_NAME) : ("Версия " + BuildConfig.VERSION_NAME));

        setText(v, R.id.about_device_name, DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE));
        setText(v, R.id.about_device_codename, Build.DEVICE);
        setText(v, R.id.about_android_version, Build.VERSION.RELEASE);
        setText(v, R.id.about_os_version, getHyperOsVersion());
        setText(v, R.id.about_easter_hint, "");
        View easterHint = v.findViewById(R.id.about_easter_hint);
        if (easterHint != null) easterHint.setVisibility(View.GONE);

        View androidVersionTap = v.findViewById(R.id.about_android_row);
        if (androidVersionTap == null) androidVersionTap = v.findViewById(R.id.about_android_version);
        if (androidVersionTap != null) {
            androidVersionTap.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View vv) {
                    handleAndroidVersionEasterEggTap();
                }
            });
        }

        final TextView languageValue = (TextView) v.findViewById(R.id.language_value);
        updateLanguageSummary(languageValue);
        click(v, R.id.card_language, new View.OnClickListener() {
            @Override public void onClick(View vv) {
                vibrate();
                showLanguageChooser(languageValue);
            }
        });

        ToggleView bgAnimToggle = (ToggleView) v.findViewById(R.id.toggle_bg_anim);
        if (bgAnimToggle != null) {
            bgAnimToggle.setChecked(prefs.isBgAnim());
            bgAnimToggle.setOnCheckedChangeListener(new ToggleView.OnCheckedChangeListener() {
                @Override public void onChanged(boolean checked) {
                    prefs.setBgAnim(checked);
                    View current = fragmentContainer != null && fragmentContainer.getChildCount() > 0
                            ? fragmentContainer.getChildAt(0) : null;
                    if (current != null) {
                        View bgAnim = current.findViewById(R.id.bg_anim);
                        if (bgAnim != null) bgAnim.setVisibility(checked ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }

        ToggleView hapticToggle = (ToggleView) v.findViewById(R.id.toggle_haptic);
        if (hapticToggle != null) {
            hapticToggle.setChecked(prefs.isHaptic());
            hapticToggle.setOnCheckedChangeListener(new ToggleView.OnCheckedChangeListener() {
                @Override public void onChanged(boolean checked) {
                    prefs.setHaptic(checked);
                    if (checked) vibrate();
                }
            });
        }

        ToggleView splashToggle = (ToggleView) v.findViewById(R.id.toggle_splash);
        if (splashToggle != null) {
            splashToggle.setChecked(prefs.isSplash());
            splashToggle.setOnCheckedChangeListener(new ToggleView.OnCheckedChangeListener() {
                @Override public void onChanged(boolean checked) {
                    prefs.setSplash(checked);
                    if (prefs.isHaptic()) vibrate();
                }
            });
        }

        click(v, R.id.card_developer, new View.OnClickListener() {
            @Override public void onClick(View v2) {
                vibrate();
                openUrl(URL_DEVELOPER);
            }
        });

        click(v, R.id.card_telegram_channel, new View.OnClickListener() {
            @Override public void onClick(View v2) {
                vibrate();
                openUrl(URL_TELEGRAM_CHANNEL);
            }
        });

        click(v, R.id.card_donate, new View.OnClickListener() {
            @Override public void onClick(View v2) {
                vibrate();
                openUrl(URL_DONATE);
            }
        });

        click(v, R.id.card_privacy, new View.OnClickListener() {
            @Override public void onClick(View v2) {
                vibrate();
                openUrl(URL_PRIVACY_POLICY);
            }
        });

        UiLocalizer.localizeViewTree(v, this);
    }

    private void updateLanguageSummary(TextView target) {
        if (target == null) return;
        String code = prefs.getLanguage();
        if (LocaleHelper.LANG_SYSTEM.equals(code)) {
            target.setText(LocaleHelper.isEnglish(this) ? "Follow system" : "По системе");
        } else if (LocaleHelper.LANG_RU.equals(code)) {
            target.setText(LocaleHelper.isEnglish(this) ? "Russian" : "Русский");
        } else {
            target.setText("English");
        }
    }

    private void showLanguageChooser(final TextView languageValue) {
        final String[] codes = {LocaleHelper.LANG_SYSTEM, LocaleHelper.LANG_EN, LocaleHelper.LANG_RU};
        final String[] labels = {
            LocaleHelper.isEnglish(this) ? "Follow system" : "По системе",
            "English",
            LocaleHelper.isEnglish(this) ? "Russian" : "Русский"
        };
        int selected = 1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(prefs.getLanguage())) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle(LocaleHelper.isEnglish(this) ? "App language" : "Язык приложения")
            .setSingleChoiceItems(labels, selected, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    prefs.setLanguage(codes[which]);
                    updateLanguageSummary(languageValue);
                    dialog.dismiss();
                    recreate();
                }
            })
            .setNegativeButton(tr("Отмена"), null)
            .show();
    }

    private void handleAndroidVersionEasterEggTap() {
        long now = SystemClock.uptimeMillis();
        if (now > androidVersionTapDeadlineMs) {
            androidVersionTapCount = 0;
        }
        androidVersionTapDeadlineMs = now + 2800L;
        androidVersionTapCount++;
        vibrate();

        int left = 5 - androidVersionTapCount;
        if (left <= 0) {
            androidVersionTapCount = 0;
            androidVersionTapDeadlineMs = 0L;
            showHyperEggDialog();
        }
    }

    private void showHyperEggDialog() {
        final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(memInfo);
        long totalRamGb = Math.max(1L, Math.round(memInfo.totalMem / 1073741824d));

        int seed = Math.abs((Build.DEVICE + "|" + Build.MODEL + "|" + Build.ID).hashCode());
        int reactorCharge = 60 + (seed % 41);
        String[] modes = {"Photon Rabbit", "Nebula Fox", "Quantum Panda", "Aurora Lynx", "Turbo Koi"};
        String mode = modes[seed % modes.length];
        int pad = (int) (20f * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(24f * getResources().getDisplayMetrics().density);
        bg.setColor(0xFF101826);
        bg.setStroke((int) (1.2f * getResources().getDisplayMetrics().density), 0x33FFCC00);
        root.setBackground(bg);

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("CORE LAB");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        android.widget.TextView subtitle = new android.widget.TextView(this);
        subtitle.setText(DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE) + " • Android " + Build.VERSION.RELEASE);
        subtitle.setTextColor(0xFF8FB6FF);
        subtitle.setTextSize(12f);
        subtitle.setPadding(0, (int) (6f * getResources().getDisplayMetrics().density), 0, (int) (14f * getResources().getDisplayMetrics().density));
        root.addView(subtitle);

        android.widget.TextView charge = new android.widget.TextView(this);
        charge.setText(reactorCharge + "%");
        charge.setTextColor(0xFFFFCC00);
        charge.setTextSize(42f);
        charge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(charge);

        android.widget.TextView chargeLabel = new android.widget.TextView(this);
        chargeLabel.setText(tr("Заряд Hyper Reactor"));
        chargeLabel.setTextColor(0xFF7F8EA8);
        chargeLabel.setTextSize(11f);
        chargeLabel.setPadding(0, 0, 0, (int) (16f * getResources().getDisplayMetrics().density));
        root.addView(chargeLabel);

        android.widget.TextView body = new android.widget.TextView(this);
        body.setText(LocaleHelper.isEnglish(this)
            ? ("Core mode: " + mode + "\n" +
               "CPU threads: " + cores + "\n" +
               "RAM class: " + totalRamGb + " GB\n" +
               "Reactor charge: " + reactorCharge + "%\n\n" +
               "Tip: for the most consistent benchmark result, run the test after the phone cools down and with no background downloads.")
            : ("Режим ядра: " + mode + "\n" +
               "CPU потоков: " + cores + "\n" +
               "RAM-класс: " + totalRamGb + " GB\n" +
               "Заряд реактора: " + reactorCharge + "%\n\n" +
               "Совет: для максимально честного результата запускай бенчмарк после остывания телефона и без фоновых загрузок."));
        body.setTextColor(0xFFD6DEEF);
        body.setTextSize(13f);
        body.setLineSpacing(0f, 1.18f);
        root.addView(body);

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(root)
            .setPositiveButton(LocaleHelper.isEnglish(this) ? "Close" : "Закрыть", null)
            .create();
        dialog.show();
        UiLocalizer.localizeViewTree(root, this);
    }

    
    private String getHyperOsVersion() {
        String[] props = { "ro.mi.os.version.incremental", "ro.build.version.incremental", "ro.miui.ui.version.name" };
        for (String prop : props) {
            String val = getSystemProp(prop);
            if (val != null && !val.isEmpty()) return val;
        }
        return Build.DISPLAY;
    }

    private String getSystemProp(String key) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", key});
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            return line != null ? line.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SHEET HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private void openSheet(final android.view.View sheet, android.view.View overlay, boolean[] sheetOpen) {
        if (sheet == null || overlay == null) return;
        sheetOpen[0] = true;
        predictSheetOpen = true;
        overlay.setVisibility(android.view.View.VISIBLE);
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(300).start();

        Runnable doAnimate = new Runnable() {
            @Override public void run() {
                float startY = sheet.getHeight() > 0 ? sheet.getHeight() : sheet.getTranslationY();
                if (sheet.getTranslationY() <= 0) {
                    startY = sheet.getTranslationY() == 0 ? 0 : Math.max(startY, 400);
                }
                sheet.animate()
                        .translationY(0f)
                        .setDuration(350)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                        .start();
            }
        };
        if (sheet.getHeight() == 0) {
            sheet.post(doAnimate);
        } else {
            doAnimate.run();
        }
    }

    private void closeSheet(android.view.View sheet, android.view.View overlay, boolean[] sheetOpen) {
        if (sheet == null || overlay == null) return;
        sheetOpen[0] = false;
        predictSheetOpen = false;
        overlay.animate().alpha(0f).setDuration(250)
                .withEndAction(new Runnable() {
                    @Override public void run() { overlay.setVisibility(android.view.View.GONE); }
                }).start();
        sheet.animate()
                .translationY(sheet.getHeight() > 0 ? sheet.getHeight() : 2000)
                .setDuration(300)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(2f))
                .start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SYSTEM INFO HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private String getSecurityPatch() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Build.VERSION.SECURITY_PATCH;
            }
        } catch (Exception ignored) {}
        return "Н/Д";
    }

    private String getDeviceUptime() {
        long millis = SystemClock.elapsedRealtime();
        long hours   = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        return hours + " ч " + minutes + " мин";
    }

    private String getScreenRes() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        return dm.widthPixels + " × " + dm.heightPixels + " @ " + (int) dm.densityDpi + " dpi";
    }

    private String getCpuName() {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware") || line.startsWith("Processor")) {
                    br.close();
                    return line.split(":")[1].trim();
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return Build.HARDWARE;
    }

    private String getMaxCpuFreq() {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
            String line = br.readLine();
            br.close();
            if (line != null) {
                long khz = Long.parseLong(line.trim());
                return String.format(Locale.getDefault(), "%.2f ГГц", khz / 1_000_000.0);
            }
        } catch (Exception ignored) {}
        return "Н/Д";
    }

    private String getGpuRenderer() {
        try {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            javax.microedition.khronos.egl.EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl.eglInitialize(display, null);
            int[] attribs = { EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE };
            javax.microedition.khronos.egl.EGLConfig[] configs = new javax.microedition.khronos.egl.EGLConfig[1];
            int[] numConfigs = new int[1];
            egl.eglChooseConfig(display, attribs, configs, 1, numConfigs);
            javax.microedition.khronos.egl.EGLContext context = egl.eglCreateContext(display, configs[0],
                EGL10.EGL_NO_CONTEXT, new int[]{ 0x3098, 2, EGL10.EGL_NONE });
            javax.microedition.khronos.egl.EGLSurface surface = egl.eglCreatePbufferSurface(display, configs[0],
                new int[]{ EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE });
            egl.eglMakeCurrent(display, surface, surface, context);
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            egl.eglDestroyContext(display, context);
            egl.eglDestroySurface(display, surface);
            return renderer != null ? renderer : "Н/Д";
        } catch (Exception e) {
            return "Н/Д";
        }
    }

    private String getCpuTemp() {
        String[] paths = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp"
        };
        for (String path : paths) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(path));
                String line = br.readLine();
                br.close();
                if (line != null) {
                    float t = Float.parseFloat(line.trim());
                    if (t > 1000) t /= 1000f;
                    return String.format(Locale.getDefault(), "%.1f °C", t);
                }
            } catch (Exception ignored) {}
        }
        return "Н/Д";
    }

    private String getLocalIp() {
        try {
            List<NetworkInterface> ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface iface : ifaces) {
                List<InetAddress> addrs = Collections.list(iface.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && !sAddr.contains(":")) return sAddr;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Н/Д";
    }

    private String getTextValue(View root, int id) {
        String fallback = "Н/Д";
        try {
            TextView tv = (TextView) root.findViewById(id);
            if (tv != null && tv.getText() != null) return tv.getText().toString();
        } catch (Exception ignored) {}
        return fallback;
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS

    private void launchSettings(String action) {
        try {
            Intent i = new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            } catch (Exception ex) {
                Toast.makeText(this, LocaleHelper.isEnglish(this) ? "Could not open settings" : "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String tr(String value) {
        return UiLocalizer.translate(this, value);
    }

    private void click(View root, int id, View.OnClickListener l) {
        View btn = root.findViewById(id);
        if (btn != null) btn.setOnClickListener(l);
    }

    private void setText(View root, int id, String text) {
        TextView tv = (TextView) root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void vibrate() {
        if (!prefs.isHaptic()) return;
        Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(28);
            }
        }
    }
}
