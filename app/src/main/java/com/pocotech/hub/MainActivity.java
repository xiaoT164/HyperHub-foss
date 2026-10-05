package com.pocotech.hub;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Locale;

/**
 * HyperHub 2.1.0 — утилиты HyperOS без геймификации.
 * Экраны: 0 Главная, 1 Команды (вместо обновлений), 2 Инфо, 3 Тест,
 * 4 Predict, 5 Настройки, 6 Профили, 7 Журнал оптимизаций.
 */
public class MainActivity extends Activity implements HubHost {

    private HubStore store;
    private AppSettings prefs;
    private FrameLayout fragmentContainer;
    private LinearLayout bottomNavContainer;

    private static final String KEY_CURRENT_SCREEN = "current_screen";
    private int currentScreen = -1;
    private boolean benchmarkRunning = false;
    private boolean screenTransitionRunning = false;

    private static final String[] NAV_KEYS = {
        "home", "commands", "info", "bench", "predict", "settings", "profiles", "log"
    };

    private static final String URL_TELEGRAM_CHANNEL = "https://t.me/HyperHubRu";
    private static final String URL_DEVELOPER        = "https://t.me/XiaoC65";
    private static final String URL_DONATE           = "https://www.donationalerts.com/r/xiaot";
    private static final String URL_PRIVACY_POLICY   = "https://telegra.ph/Politika-konfidencialnosti-HyperHub-07-10";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new AppSettings(this);
        setContentView(R.layout.main);

        DeviceNameResolver.init(this);
        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        bottomNavContainer = (LinearLayout) findViewById(R.id.bottom_navigation);
        buildBottomNav();

        store = new HubStore(this);

        int startScreen = (savedInstanceState != null) ? savedInstanceState.getInt(KEY_CURRENT_SCREEN, 0) : 0;
        showScreenInternal(startScreen, startScreen, false);
        setNavHighlight(startScreen);
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

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void buildBottomNav() {
        if (bottomNavContainer == null) return;
        bottomNavContainer.removeAllViews();
        int accent = prefs.getAccentColor();
        for (int i = 0; i < NAV_KEYS.length; i++) {
            final int idx = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setClickable(true);

            TextView icon = new TextView(this);
            icon.setText(navIconGlyph(NAV_KEYS[i]));
            icon.setTextSize(20f);
            icon.setTag("icon");
            icon.setGravity(Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(navLabel(NAV_KEYS[i]));
            label.setTextSize(10f);
            label.setTag("label");
            label.setGravity(Gravity.CENTER);

            View dot = new View(this);
            LinearLayout.LayoutParams dotp = new LinearLayout.LayoutParams(dp(6), dp(6));
            dotp.topMargin = 4;
            dot.setLayoutParams(dotp);
            dot.setBackground(new android.graphics.drawable.GradientDrawable() {
                { setCornerRadius(dp(getResources().getDisplayMetrics().density, 3)); setColor(accent); }
            });
            dot.setAlpha(0f);
            dot.setTag("dot");

            cell.addView(icon);
            cell.addView(label);
            cell.addView(dot);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (idx == currentScreen || screenTransitionRunning) return;
                    vibrate();
                    showScreen(idx);
                    setNavHighlight(idx);
                }
            });
            bottomNavContainer.addView(cell);
        }
    }

    private String navIconGlyph(String key) {
        switch (key) {
            case "home":    return "\u2302";
            case "commands":return "$";
            case "info":    return "i";
            case "bench":   return "\u2261";
            case "predict": return "?";
            case "settings":return "\u2699";
            case "profiles":return "\u2605";
            case "log":     return "\u29C9";
            default: return "\u2022";
        }
    }

    private String navLabel(String key) {
        boolean en = LocaleHelper.isEnglish(this);
        if ("home".equals(key))     return en ? "Home" : "Главная";
        if ("commands".equals(key)) return en ? "Commands" : "Команды";
        if ("info".equals(key))     return en ? "Info" : "Инфо";
        if ("bench".equals(key))    return en ? "Bench" : "Тест";
        if ("predict".equals(key))  return "Predict";
        if ("settings".equals(key))return en ? "Settings" : "Настройки";
        if ("profiles".equals(key)) return en ? "Profiles" : "Профили";
        if ("log".equals(key))      return en ? "Log" : "Журнал";
        return key;
    }

    private void setNavHighlight(int idx) {
        if (bottomNavContainer == null) return;
        int accent = prefs.getAccentColor();
        int inactive = 0xFF8A8A8F;
        for (int i = 0; i < bottomNavContainer.getChildCount(); i++) {
            LinearLayout cell = (LinearLayout) bottomNavContainer.getChildAt(i);
            boolean active = (i == idx);
            TextView icon = (TextView) cell.findViewWithTag("icon");
            TextView label = (TextView) cell.findViewWithTag("label");
            View dot = cell.findViewWithTag("dot");
            int color = active ? accent : inactive;
            if (icon != null) {
                icon.setTextColor(color);
                icon.animate().scaleX(active ? 1.15f : 1f).scaleY(active ? 1.15f : 1f).setDuration(200).start();
            }
            if (label != null) label.setTextColor(color);
            if (dot != null) dot.animate().alpha(active ? 1f : 0f).setDuration(200).start();
        }
    }

    private void showScreen(int idx) { showScreenInternal(idx, currentScreen, true); }

    private void showScreenInternal(final int idx, final int prevScreen, boolean animate) {
        if (currentScreen == 3 && idx != 3 && hubBench != null && benchmarkRunning) {
            hubBench.cancel();
            benchmarkRunning = false;
        }
        currentScreen = idx;

        final View old = fragmentContainer.getChildCount() > 0 ? fragmentContainer.getChildAt(0) : null;
        if (!animate) fragmentContainer.removeAllViews();
        else if (old != null) old.animate().cancel();

        View view;
        switch (idx) {
            case 1: view = CommandsScreen.build(this, this); break;
            case 2: view = buildInfoScreen(); break;
            case 3: view = buildBenchmarkScreen(); break;
            case 4: view = buildPredictScreen(); break;
            case 5: view = buildSettingsScreen(); break;
            case 6: view = ProfilesScreen.build(this, this); break;
            case 7: view = ActionLogScreen.build(this, this); break;
            default: view = buildHomeScreen();
        }

        UiLocalizer.localizeViewTree(view, this);
        if (!animate) { fragmentContainer.addView(view); return; }

        screenTransitionRunning = true;
        float direction = (idx > prevScreen) ? 1f : -1f;
        view.setTranslationX(64f * direction);
        view.setAlpha(0f);
        view.setScaleX(0.985f); view.setScaleY(0.985f);
        fragmentContainer.addView(view);
        view.bringToFront();

        android.animation.AnimatorSet enter = new android.animation.AnimatorSet();
        enter.playTogether(
                android.animation.ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 64f * direction, 0f),
                android.animation.ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                android.animation.ObjectAnimator.ofFloat(view, View.SCALE_X, 0.985f, 1f),
                android.animation.ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.985f, 1f));
        enter.setDuration(280);
        enter.setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f));

        if (old != null) {
            android.animation.AnimatorSet exit = new android.animation.AnimatorSet();
            exit.playTogether(
                    android.animation.ObjectAnimator.ofFloat(old, View.TRANSLATION_X, 0f, -36f * direction),
                    android.animation.ObjectAnimator.ofFloat(old, View.ALPHA, 1f, 0f),
                    android.animation.ObjectAnimator.ofFloat(old, View.SCALE_X, 1f, 0.992f),
                    android.animation.ObjectAnimator.ofFloat(old, View.SCALE_Y, 1f, 0.992f));
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
            @Override public void onAnimationEnd(android.animation.Animator animation) { screenTransitionRunning = false; }
            @Override public void onAnimationCancel(android.animation.Animator animation) { screenTransitionRunning = false; }
        });
        enter.start();
    }

    // ════════════ HubHost ════════════
    @Override public void runFeature(String key) {
        if (key == null) return;
        store.pushRecent(key);
        CommandCatalog.Cmd cmd = CommandCatalog.byKey(key);
        if (cmd != null) {
            store.logAction(key, LocaleHelper.isEnglish(this) ? cmd.titleEn : cmd.titleRu);
            UiKit.copyToClipboard(this, cmd.command,
                    LocaleHelper.isEnglish(this) ? "Command copied" : "Команда скопирована");
            UiKit.makeSheetCommand(this, cmd, this);
            return;
        }
        if (HubStore.LMK_FULL_KEY.equals(key) || "lmk".equals(key)) {
            runLmkOptimization();
        }
    }

    @Override public void openScreen(int index) { showScreen(index); setNavHighlight(index); }

    @Override public void showSheet(String title, String body) {
        new GlassSheet(this).title(title).body(body).show();
    }

    @Override public void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    @Override public void refreshCurrentScreen() {
        int idx = currentScreen;
        showScreenInternal(idx, idx, false);
        setNavHighlight(idx);
    }
    @Override public AppSettings prefs() { return prefs; }
    @Override public HubStore store() { return store; }
    @Override public void vibrate() { doVibrate(); }

    public void doVibrate() {
        if (prefs != null && !prefs.isHaptic()) return;
        Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vib.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE));
            else vib.vibrate(28);
        }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) {
            Toast.makeText(this, LocaleHelper.isEnglish(this) ? "Could not open link" : "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
        }
    }

    // ════════════ Главная ════════════
    private View buildHomeScreen() {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout root = UiKit.column(this, 16);

        TextView headerTitle = UiKit.title(this, "HyperHub", 28f);
        root.addView(headerTitle);
        TextView sub = UiKit.subtitle(this,
                "v" + BuildConfig.VERSION_NAME + (en ? " · HyperOS toolkit" : " · Утилиты HyperOS"));
        root.addView(sub);

        EditText search = new EditText(this);
        search.setHint(en ? "Search tools" : "Поиск инструментов");
        search.setBackground(UiKit.cardBg(this, 14, true));
        search.setPadding(16, 14, 16, 14);
        search.setTextColor(0xFFFFFFFF);
        search.setHintTextColor(0xFF64748B);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        search.setLayoutParams(slp);
        root.addView(search);

        // Главная карточка по ТЗ — глубокая оптимизация LMK
        root.addView(UiKit.sectionHeader(this, en ? "DEEP OPTIMIZATION" : "ГЛУБОКАЯ ОПТИМИЗАЦИЯ"));
        addLmkCard(root);

        root.addView(UiKit.sectionHeader(this, en ? "ADB COMMANDS" : "ADB-КОМАНДЫ"));
        for (CommandCatalog.Cmd c : CommandCatalog.all()) {
            if (HubStore.LMK_FULL_KEY.equals(c.key)) continue; // уже выведена главной карточкой
            root.addView(buildCommandCard(c));
        }

        root.addView(UiKit.sectionHeader(this, en ? "TOOLS" : "СИСТЕМНЫЕ ИНСТРУМЕНТЫ"));
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.VERTICAL);
        root.addView(tools);
        addToolLink(tools, "FPS boost", "Разгон FPS");
        addToolLink(tools, "Performance mode", "Режим производительности");
        addToolLink(tools, "Flagship animations", "Флагманские анимации");
        addToolLink(tools, "RAM and background", "RAM и фоновые приложения");
        addToolLink(tools, "Ping optimization", "Оптимизация пинга");
        addToolLink(tools, "Power tuning", "Настройка энергорежима");
        addToolLink(tools, "Game Turbo mode", "Игровой турбо-режим");

        TextView empty = new TextView(this);
        empty.setText(en ? "No tools match your query" : "Ничего не найдено");
        empty.setTextColor(0xFF64748B);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        root.addView(empty);

        UiKit.bindSearch(search, empty, new Runnable() {
            @Override public void run() {
                String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
                for (int i = 0; i < root.getChildCount(); i++) {
                    View v = root.getChildAt(i);
                    Object tag = v.getTag();
                    if (tag instanceof String && ((String) tag).startsWith("card:")) {
                        String tokens = ((String) tag).substring(5);
                        v.setVisibility(q.isEmpty() || tokens.toLowerCase(Locale.ROOT).contains(q)
                                ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });
        return UiKit.makeScroll(this, root);
    }

    private void addLmkCard(LinearLayout root) {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout card = UiKit.rowCard(this, 18);
        card.setTag("card:lmk deep lmk полная глубокая activity manager constants");

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        body.setLayoutParams(bodyLp);
        body.addView(UiKit.title(this, en ? "Deep LMK optimization" : "Глубокая оптимизация LMK", 16f));
        TextView desc = UiKit.subtitle(this,
                en ? "Resets ActivityManager constants: cached=0, GC=0 ms, no provider retain. Maximum RAM release."
                   : "Сбрасывает константы ActivityManager: кэш=0, GC=0 мс, нет удержания провайдеров. Максимум свободной RAM.");
        desc.setPadding(0, 6, 0, 6);
        body.addView(desc);

        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.addView(UiKit.chip(this, "ADB", 0xFF4F46E5));
        badges.addView(UiKit.chip(this, en ? "effective" : "эффективно", 0xFF22C55E));
        badges.addView(UiKit.chip(this, en ? "advanced" : "продвинутое", 0xFFF59E0B));
        body.addView(badges);
        card.addView(body);

        TextView apply = UiKit.actionBadge(this, en ? "Apply" : "Применить", prefs.getAccentColor());
        apply.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { runLmkOptimization(); } });
        card.addView(apply);
        card.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { runLmkOptimization(); } });
        root.addView(card);
    }

    private LinearLayout buildCommandCard(CommandCatalog.Cmd c) {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout card = UiKit.rowCard(this, 16);
        String tag = (en ? c.titleEn : c.titleRu) + " " + c.group + " " + c.key
                + " " + (en ? c.descEn : c.descRu);
        card.setTag("card:" + tag);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        body.setLayoutParams(bodyLp);
        body.addView(UiKit.title(this, en ? c.titleEn : c.titleRu, 15f));
        TextView d = UiKit.subtitle(this, en ? c.descEn : c.descRu);
        d.setMaxLines(2);
        body.addView(d);
        card.addView(body);

        TextView copy = UiKit.actionBadge(this, en ? "Copy" : "Копировать", prefs.getAccentColor());
        copy.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { runFeature(c.key); } });
        card.addView(copy);
        card.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { runFeature(c.key); } });
        return card;
    }

    private void addToolLink(LinearLayout root, String en, String ru) {
        boolean eng = LocaleHelper.isEnglish(this);
        LinearLayout card = UiKit.rowCard(this, 14);
        card.setTag("card:" + (eng ? en : ru).toLowerCase(Locale.ROOT));
        TextView t = UiKit.title(this, eng ? en : ru, 14f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        t.setLayoutParams(tlp);
        card.addView(t);
        TextView right = UiKit.subtitle(this, eng ? "open on Commands" : "открыть в Командах");
        card.addView(right);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showScreen(1); }
        });
        root.addView(card);
    }

    private void runLmkOptimization() {
        boolean en = LocaleHelper.isEnglish(this);
        CommandCatalog.Cmd cmd = CommandCatalog.byKey(HubStore.LMK_FULL_KEY);
        if (cmd == null) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle(en ? "Deep LMK optimization" : "Глубокая оптимизация LMK")
                .setMessage((en ? cmd.descEn : cmd.descRu)
                        + "\n\n" + (en ? "Execute via Termux or ADB Shell, then reboot. Command will be copied to clipboard." : "Выполняйте через Termux или ADB Shell, затем перезагрузите устройство. Команда скопируется в буфер обмена."))
                .setPositiveButton(en ? "Copy & log" : "Скопировать и записать", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        store.logAction(HubStore.LMK_FULL_KEY, en ? "LMK deep optimization" : "Глубокая оптимизация LMK");
                        UiKit.copyToClipboard(MainActivity.this, cmd.command,
                                en ? "Command copied. Apply via Termux/ADB and reboot." : "Команда скопирована. Выполните через Termux/ADB и перезагрузите устройство.");
                    }
                })
                .setNegativeButton(en ? "Cancel" : "Отмена", null)
                .show();
    }

    // ════════════ Инфо ════════════
    private View buildInfoScreen() {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout root = UiKit.column(this, 16);
        root.setPadding(16, 28, 16, 110);

        root.addView(UiKit.title(this, en ? "Device info" : "Инфо устройства", 28f));
        root.addView(UiKit.subtitle(this, en ? "All data read locally — no network" : "Только локальные данные, без сети"));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView refresh = new TextView(this);
        refresh.setText(en ? "Refresh" : "Обновить");
        refresh.setBackground(UiKit.cardBg(this, 12, true));
        refresh.setPadding(20, 12, 20, 12);
        refresh.setTextColor(0xFFFFFFFF);
        refresh.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { refreshCurrentScreen(); } });
        TextView copy = new TextView(this);
        copy.setText(en ? "Copy summary" : "Скопировать");
        copy.setBackground(UiKit.cardBg(this, 12, true));
        copy.setPadding(20, 12, 20, 12);
        copy.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.leftMargin = 16;
        copy.setLayoutParams(ll);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("device", buildInfoText()));
                toast(en ? "Summary copied" : "Сводка скопирована");
            }
        });
        buttons.addView(refresh);
        buttons.addView(copy);
        root.addView(buttons);

        addInfoRow(root, en ? "Model" : "Модель", Build.MODEL);
        addInfoRow(root, "Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addInfoRow(root, en ? "Build" : "Сборка", Build.DISPLAY);
        addInfoRow(root, en ? "Security patch" : "Патч безопасности", getSecurityPatch());
        addInfoRow(root, en ? "Uptime" : "Аптайм", getDeviceUptime());
        addInfoRow(root, en ? "Screen" : "Экран", getScreenRes());
        addInfoRow(root, en ? "Cores" : "Ядра", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addInfoRow(root, en ? "CPU" : "Процессор", getCpuName());
        addInfoRow(root, en ? "GPU" : "Графика", getGpuRenderer());
        addInfoRow(root, en ? "CPU temp" : "Температура CPU", getCpuTemp());

        IntentFilter bf = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent bi = registerReceiver(null, bf);
        if (bi != null) {
            int level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct = (scale > 0) ? (int) (level * 100f / scale) : 0;
            int temp = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int volt = bi.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            addInfoRow(root, en ? "Battery" : "Батарея", pct + "% · " + String.format(Locale.US, "%.1f °C", temp / 10f) + " · " + String.format(Locale.US, "%.3f V", volt / 1000f));
        }

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mi);
            long usedRam = mi.totalMem - mi.availMem;
            float ramPct = (float) usedRam / mi.totalMem;
            long usedMb = usedRam / (1024 * 1024);
            long totalMb = mi.totalMem / (1024 * 1024);
            addInfoRow(root, "RAM", String.format(Locale.US, "%d%% · %d MB из %d MB", (int) (ramPct * 100), usedMb, totalMb));
        }

        try {
            StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
            long freeBytes = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
            long totalBytes = sf.getBlockCountLong() * sf.getBlockSizeLong();
            long freeGB = freeBytes / (1024 * 1024 * 1024);
            long totalGB = totalBytes / (1024 * 1024 * 1024);
            addInfoRow(root, en ? "Storage" : "Хранилище", freeGB + " GB free of " + totalGB + " GB");
        } catch (Exception ignored) {}

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null) {
                    String ssid = wi.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length() - 1);
                    int speed = wi.getLinkSpeed();
                    int rssi = wi.getRssi();
                    addInfoRow(root, "Wi-Fi", (ssid == null || ssid.isEmpty() ? "—" : ssid) + " · " + speed + " Mbps · " + rssi + " dBm");
                }
            } else addInfoRow(root, "Wi-Fi", en ? "Wi-Fi off" : "Wi-Fi выключен");
        } catch (Exception e) { addInfoRow(root, "Wi-Fi", "—"); }
        addInfoRow(root, "IP", getLocalIp());
        return UiKit.makeScroll(this, root);
    }

    private void addInfoRow(LinearLayout root, String key, String value) {
        LinearLayout row = UiKit.rowCard(this, 14);
        TextView k = UiKit.title(this, key, 14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k.setLayoutParams(lp);
        row.addView(k);
        row.addView(UiKit.subtitle(this, value));
        root.addView(row);
    }

    private String buildInfoText() {
        return "HyperHub · Сводка устройства\n" +
                "Model: " + Build.MODEL + "\n" +
                "Android: " + Build.VERSION.RELEASE + "\n" +
                "Build: " + Build.DISPLAY + "\n" +
                "Screen: " + getScreenRes() + "\n" +
                "CPU: " + getCpuName() + "\n" +
                "GPU: " + getGpuRenderer() + "\n" +
                "CPU temp: " + getCpuTemp() + "\n";
    }

    public String getSecurityPatch() {
        try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return Build.VERSION.SECURITY_PATCH; }
        catch (Exception ignored) {}
        return "—";
    }
    public String getDeviceUptime() {
        long ms = SystemClock.elapsedRealtime();
        return (ms / 3600000) + " ч " + ((ms % 3600000) / 60000) + " мин";
    }
    public String getScreenRes() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        return dm.widthPixels + " × " + dm.heightPixels + " @ " + dm.densityDpi + " dpi";
    }
    public String getCpuName() {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware") || line.startsWith("Processor")) { br.close(); return line.split(":")[1].trim(); }
            }
            br.close();
        } catch (Exception ignored) {}
        return Build.HARDWARE;
    }
    public String getGpuRenderer() {
        try {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            javax.microedition.khronos.egl.EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl.eglInitialize(display, null);
            int[] attribs = { EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE };
            javax.microedition.khronos.egl.EGLConfig[] configs = new javax.microedition.khronos.egl.EGLConfig[1];
            int[] n = new int[1];
            egl.eglChooseConfig(display, attribs, configs, 1, n);
            javax.microedition.khronos.egl.EGLContext context = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, new int[]{0x3098, 2, EGL10.EGL_NONE});
            javax.microedition.khronos.egl.EGLSurface surface = egl.eglCreatePbufferSurface(display, configs[0], new int[]{EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE});
            egl.eglMakeCurrent(display, surface, surface, context);
            String r = GLES20.glGetString(GLES20.GL_RENDERER);
            egl.eglDestroyContext(display, context);
            egl.eglDestroySurface(display, surface);
            return r != null ? r : "—";
        } catch (Exception e) { return "—"; }
    }
    public String getCpuTemp() {
        for (String p : new String[]{"/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp"}) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(p));
                String l = br.readLine(); br.close();
                if (l != null) {
                    float t = Float.parseFloat(l.trim());
                    if (t > 1000) t /= 1000f;
                    return String.format(Locale.US, "%.1f °C", t);
                }
            } catch (Exception ignored) {}
        }
        return "—";
    }
    public String getLocalIp() {
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (!addr.isLoopbackAddress()) {
                        String s = addr.getHostAddress();
                        if (s != null && !s.contains(":")) return s;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "—";
    }

    // ════════════ Predict ════════════
    private View buildPredictScreen() {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout root = UiKit.column(this, 16);
        root.setPadding(16, 28, 16, 110);
        String name = DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE);

        root.addView(UiKit.title(this, "Predict", 28f));
        TextView head = UiKit.subtitle(this, name + " · " + Build.MODEL + " / " + Build.DEVICE);
        head.setPadding(0, 4, 0, 12);
        root.addView(head);

        int verdict = predictVerdict(Build.MODEL, Build.DEVICE, name);
        LinearLayout badge = UiKit.rowCard(this, 16);
        String verdictText; int verdictColor;
        if (verdict == 1) { verdictText = en ? "Will get HyperOS 4" : "Получит HyperOS 4"; verdictColor = 0xFF22C55E; }
        else if (verdict == -1) { verdictText = en ? "Won't get HyperOS 4" : "Не получит HyperOS 4"; verdictColor = 0xFFEF4444; }
        else { verdictText = en ? "Maybe won't get it" : "Возможно, не получит"; verdictColor = 0xFFF59E0B; }
        TextView bt = UiKit.title(this, verdictText, 18f);
        bt.setTextColor(verdictColor);
        LinearLayout.LayoutParams btp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bt.setLayoutParams(btp);
        badge.addView(bt);
        root.addView(badge);

        root.addView(UiKit.subtitle(this, en ? "Indicative list based on the official Xiaomi roadmap." : "Список ориентировочный — по официальной дорожной карте Xiaomi."));

        root.addView(UiKit.sectionHeader(this, "── XIAOMI ──"));
        addPredictLine(root, "Xiaomi 13 Lite / Civi 3 / MIX Fold 2", en ? "Borderline" : "Пограничная");
        addPredictLine(root, "Xiaomi 17/15/14/14T/13 + Civi 4-5 + Mix Flip", en ? "In update window" : "В окне обновлений");
        addPredictLine(root, "Xiaomi 12/12T/12S + MIX 4", en ? "Too old" : "Слишком старая");

        root.addView(UiKit.sectionHeader(this, "── REDMI ──"));
        addPredictLine(root, "K90 / K80 / K70 / K60 Ultra", en ? "Top line" : "Верхняя линейка");
        addPredictLine(root, "Note 15 / Note 14 / Turbo 4-5", en ? "In window" : "В окне");
        addPredictLine(root, "Redmi A3 / A4 / A5 / Pad Pro / SE", en ? "Too short support" : "Короткая поддержка");

        root.addView(UiKit.sectionHeader(this, "── POCO ──"));
        addPredictLine(root, "F8 / F7 / F6, X8 / X7, M8 / M7", en ? "Main lines" : "Основные серии");
        addPredictLine(root, "C85 / C81", en ? "May get it" : "С шансом");
        addPredictLine(root, "C75 / C71 / M4 / X4 / F4 / F3", en ? "Out of window" : "Вне окна");
        return UiKit.makeScroll(this, root);
    }
    private void addPredictLine(LinearLayout root, String line, String tag) {
        LinearLayout row = UiKit.rowCard(this, 14);
        TextView l = UiKit.title(this, line, 14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l.setLayoutParams(lp);
        row.addView(l);
        TextView t = UiKit.chip(this, tag, 0xFF4F46E5);
        row.addView(t);
        root.addView(row);
    }
    private int predictVerdict(String model, String device, String display) {
        String[] willGet = {"Xiaomi 15", "Xiaomi 14", "Xiaomi 13", "Redmi K90", "Redmi K80", "Redmi K70",
                "Redmi Note 15", "Redmi Note 14", "POCO F8", "POCO F7", "POCO F6", "POCO X8", "POCO X7"};
        String[] wont = {"Redmi A3", "Redmi A4", "Redmi A5", "POCO C75", "POCO M3", "POCO X3", "Xiaomi 12", "Redmi Note 11"};
        String lc = (model + " " + device + " " + display).toLowerCase(Locale.ROOT);
        for (String s : willGet) if (lc.contains(s.toLowerCase(Locale.ROOT))) return 1;
        for (String s : wont) if (lc.contains(s.toLowerCase(Locale.ROOT))) return -1;
        return 0;
    }

    // ════════════ Бенчмарк ════════════
    private HubBench hubBench;
    private View buildBenchmarkScreen() {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout root = UiKit.column(this, 16);
        root.setPadding(16, 28, 16, 110);

        root.addView(UiKit.title(this, "HubBench v2", 28f));
        root.addView(UiKit.subtitle(this, en ? "Local benchmark, results stay on device" : "Локальный тест, результаты остаются на устройстве"));

        TextView device = UiKit.title(this, DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE) + " · " + getCpuName(), 14f);
        device.setPadding(0, 8, 0, 12);
        root.addView(device);

        final TextView status = new TextView(this);
        status.setText(en ? "Ready" : "Готов");
        status.setTextColor(0xFF9AA6B8);
        root.addView(status);

        final ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams pbl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        pbl.topMargin = 8;
        progress.setLayoutParams(pbl);
        root.addView(progress);

        TextView start = new TextView(this);
        start.setText(en ? "Start test" : "Запустить тест");
        start.setTextColor(0xFFFFFFFF);
        start.setBackground(new android.graphics.drawable.GradientDrawable() {
            { setCornerRadius(dp(14)); setColor(prefs.getAccentColor()); }
        });
        start.setPadding(24, 16, 24, 16);
        start.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sl.topMargin = 16;
        start.setLayoutParams(sl);
        root.addView(start);

        final LinearLayout resultHost = new LinearLayout(this);
        resultHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultHost);

        start.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                vibrate();
                if (benchmarkRunning) {
                    if (hubBench != null) hubBench.cancel();
                    benchmarkRunning = false;
                    status.setText(LocaleHelper.isEnglish(MainActivity.this) ? "Cancelled" : "Отменено");
                    start.setText(LocaleHelper.isEnglish(MainActivity.this) ? "Start test" : "Запустить тест");
                    return;
                }
                benchmarkRunning = true;
                start.setText(LocaleHelper.isEnglish(MainActivity.this) ? "Cancel" : "Отменить");
                progress.setProgress(0);
                resultHost.removeAllViews();
                hubBench = new HubBench(MainActivity.this);
                hubBench.run(new HubBench.Callback() {
                    @Override public void onProgress(final String stage, final int percent) {
                        runOnUiThread(new Runnable() { @Override public void run() { status.setText(stage); progress.setProgress(percent); } });
                    }
                    @Override public void onComplete(final BenchmarkResult result) {
                        runOnUiThread(new Runnable() { @Override public void run() {
                            benchmarkRunning = false;
                            start.setText(LocaleHelper.isEnglish(MainActivity.this) ? "Start test" : "Запустить тест");
                            if (!result.benchmarkValid) {
                                status.setText(result.failureMessage != null ? result.failureMessage
                                        : (LocaleHelper.isEnglish(MainActivity.this) ? "Test failed" : "Тест не завершён"));
                                return;
                            }
                            status.setText(LocaleHelper.isEnglish(MainActivity.this) ? "Done" : "Готово");
                            renderBenchResult(resultHost, result);
                            saveBench(result);
                        } });
                    }
                });
            }
        });
        return UiKit.makeScroll(this, root);
    }
    private void saveBench(BenchmarkResult r) {
        HubStore.BenchRecord rec = new HubStore.BenchRecord();
        rec.time = System.currentTimeMillis();
        rec.total = r.totalScore;
        rec.cpuSingle = r.cpuSingleScore;
        rec.cpuMulti = r.cpuMultiScore;
        rec.ram = r.ramScore;
        rec.storage = r.storageScore;
        rec.gpu = r.gpuScore;
        rec.rating = r.getRating();
        store.addBenchmark(rec);
        store.logAction("bench", LocaleHelper.isEnglish(this) ? "HubBench run" : "Прогон HubBench");
    }
    private void renderBenchResult(LinearLayout host, BenchmarkResult r) {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout card = UiKit.rowCard(this, 18);
        card.setOrientation(LinearLayout.VERTICAL);
        TextView t = UiKit.title(this, en ? "Result" : "Результат", 16f);
        card.addView(t);
        TextView total = UiKit.title(this, String.format(Locale.US, "%,d", r.totalScore).replace(',', ' '), 28f);
        total.setTextColor(prefs.getAccentColor());
        card.addView(total);
        TextView rating = UiKit.subtitle(this, (en ? "Rating: " : "Оценка: ") + r.getRating());
        card.addView(rating);
        addBenchLine(card, en ? "CPU single" : "CPU 1 ядро", r.cpuSingleRaw + " Mops/с");
        addBenchLine(card, en ? "CPU multi"  : "CPU multi",  r.cpuMultiRaw + " Mops/с");
        addBenchLine(card, en ? "RAM" : "RAM", r.ramBandwidth + " MB/s · " + r.ramLatencyNs + " ns");
        addBenchLine(card, en ? "Storage" : "Хранилище", r.storageRead + " SQLite TPS · " + r.storageWrite + " MB/s");
        if (r.gpuTested) addBenchLine(card, en ? "GPU" : "Графика", String.format(Locale.US, "%.0f fps", r.gpuAvgFps));
        host.addView(card);
    }
    private void addBenchLine(LinearLayout host, String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);
        TextView k = UiKit.title(this, key, 13f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k.setLayoutParams(lp);
        row.addView(k);
        row.addView(UiKit.subtitle(this, value));
        host.addView(row);
    }

    // ════════════ Настройки ════════════
    private View buildSettingsScreen() {
        boolean en = LocaleHelper.isEnglish(this);
        LinearLayout root = UiKit.column(this, 16);
        root.setPadding(16, 28, 16, 110);

        root.addView(UiKit.title(this, en ? "Settings" : "Настройки", 28f));
        root.addView(UiKit.subtitle(this, "v" + BuildConfig.VERSION_NAME));
        addInfoRow(root, en ? "Device" : "Устройство", DeviceNameResolver.resolve(Build.MODEL, Build.DEVICE));
        addInfoRow(root, "Android", Build.VERSION.RELEASE);
        addInfoRow(root, en ? "HyperOS / MIUI" : "HyperOS / MIUI", Build.DISPLAY);

        root.addView(UiKit.sectionHeader(this, en ? "APPEARANCE" : "ВНЕШНИЙ ВИД"));
        addSwitchRow(root, en ? "Accent theme" : "Акцентная тема", false, new ToggleListener() {
            @Override public void onChanged(boolean v) { showThemeChooser(); }
        });
        addSwitchRow(root, en ? "Background animation" : "Анимация фона", prefs.isBgAnim(), new ToggleListener() {
            @Override public void onChanged(boolean v) { prefs.setBgAnim(v); }
        });
        addSwitchRow(root, en ? "Haptic feedback" : "Виброотклик", prefs.isHaptic(), new ToggleListener() {
            @Override public void onChanged(boolean v) { prefs.setHaptic(v); }
        });
        addSwitchRow(root, en ? "Splash screen" : "Сплеш-экран", prefs.isSplash(), new ToggleListener() {
            @Override public void onChanged(boolean v) { prefs.setSplash(v); }
        });

        root.addView(UiKit.sectionHeader(this, en ? "LANGUAGE" : "ЯЗЫК"));
        LinearLayout langRow = UiKit.rowCard(this, 14);
        TextView lang = UiKit.title(this, en ? "App language" : "Язык приложения", 14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lang.setLayoutParams(lp);
        langRow.addView(lang);
        TextView langVal = UiKit.subtitle(this, prefs.getLanguage().equals("ru") ? "Русский" :
                prefs.getLanguage().equals("en") ? "English" : (en ? "System" : "По системе"));
        langRow.addView(langVal);
        langRow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showLanguageChooser(); }
        });
        root.addView(langRow);

        root.addView(UiKit.sectionHeader(this, en ? "DATA" : "ДАННЫЕ"));
        LinearLayout backup = UiKit.rowCard(this, 14);
        TextView bup = UiKit.title(this, en ? "Backup and restore" : "Резервная копия", 14f);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bup.setLayoutParams(blp);
        backup.addView(bup);
        backup.addView(UiKit.subtitle(this, "JSON"));
        backup.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { showBackupDialog(); } });
        root.addView(backup);

        LinearLayout reset = UiKit.rowCard(this, 14);
        TextView rst = UiKit.title(this, en ? "Clear local data" : "Очистить локальные данные", 14f);
        rst.setTextColor(0xFFEF4444);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rst.setLayoutParams(rlp);
        reset.addView(rst);
        reset.addView(UiKit.subtitle(this, en ? "favorites, profiles, log" : "избранное, профили, журнал"));
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(en ? "Clear all local data?" : "Очистить локальные данные?")
                        .setMessage(en ? "Settings, favorites, profiles, log and benchmark history will be cleared."
                                       : "Настройки, избранное, профили, журнал и история бенчмарков будут удалены.")
                        .setPositiveButton(en ? "Clear" : "Очистить", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                store.resetAll();
                                toast(en ? "All local data cleared" : "Локальные данные очищены");
                                recreate();
                            }
                        }).setNegativeButton(en ? "Cancel" : "Отмена", null).show();
            }
        });
        root.addView(reset);

        root.addView(UiKit.sectionHeader(this, en ? "ABOUT" : "О ПРИЛОЖЕНИИ"));
        addLinkRow(root, en ? "Telegram channel" : "Telegram-канал", "@HyperHubRu", URL_TELEGRAM_CHANNEL);
        addLinkRow(root, en ? "Developer" : "Разработчик", "@XiaoC65", URL_DEVELOPER);
        addLinkRow(root, en ? "Support project" : "Поддержать проект", "DonationAlerts", URL_DONATE);
        addLinkRow(root, en ? "Privacy policy" : "Политика конфиденциальности", "telegraph", URL_PRIVACY_POLICY);

        return UiKit.makeScroll(this, root);
    }

    private void addLinkRow(LinearLayout root, String title, String sub, final String url) {
        LinearLayout row = UiKit.rowCard(this, 14);
        TextView t = UiKit.title(this, title, 14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        t.setLayoutParams(lp);
        row.addView(t);
        row.addView(UiKit.subtitle(this, sub));
        row.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { openUrl(url); } });
        root.addView(row);
    }

    private void showThemeChooser() {
        boolean en = LocaleHelper.isEnglish(this);
        final String[] names = {"Ocean", "Mint", "Violet", "Amber", "Sunset", "Graphite"};
        final int[] colors = {0xFF4F46E5, 0xFF14B8A6, 0xFF8B5CF6, 0xFFF59E0B, 0xFFEF4444, 0xFF64748B};
        int selected = 0;
        for (int i = 0; i < colors.length; i++) if (colors[i] == prefs.getAccentColor()) selected = i;
        new android.app.AlertDialog.Builder(this)
                .setTitle(en ? "Accent theme" : "Акцентная тема")
                .setSingleChoiceItems(names, selected, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        prefs.setAccentColor(colors[which]);
                        dialog.dismiss();
                        refreshCurrentScreen();
                    }
                }).setNegativeButton(en ? "Cancel" : "Отмена", null).show();
    }

    private void showLanguageChooser() {
        boolean en = LocaleHelper.isEnglish(this);
        new android.app.AlertDialog.Builder(this)
                .setTitle(en ? "App language" : "Язык приложения")
                .setSingleChoiceItems(new String[]{en ? "System" : "По системе", "English", "Русский"}, 0,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                                String code = which == 0 ? AppSettings.LANG_SYSTEM : which == 1 ? AppSettings.LANG_EN : AppSettings.LANG_RU;
                                prefs.setLanguage(code);
                                dialog.dismiss();
                                recreate();
                            }
                        }).setNegativeButton(en ? "Cancel" : "Отмена", null).show();
    }

    private void showBackupDialog() {
        boolean en = LocaleHelper.isEnglish(this);
        new android.app.AlertDialog.Builder(this)
                .setTitle(en ? "Backup and restore" : "Резервная копия")
                .setMessage(en ? "Save or restore settings, favorites, profiles, log and benchmark history."
                               : "Сохранить или восстановить настройки, избранное, профили, журнал и историю бенчмарков.")
                .setPositiveButton(en ? "Export" : "Экспорт", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String json = BackupManager.export(this);
                        String path = BackupManager.writeToFile(this, json);
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("hyperhub", json));
                        toast((en ? "Exported to " : "Экспортировано в ") + (path != null ? path : "clipboard"));
                    }
                })
                .setNeutralButton(en ? "Import" : "Импорт", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        String json = "";
                        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                            CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);
                            if (cs != null) json = cs.toString();
                        }
                        boolean ok = BackupManager.importJson(this, json);
                        toast(ok ? (en ? "Backup restored" : "Бэкап восстановлен")
                                : (en ? "Invalid JSON in clipboard" : "В буфере нет бэкапа"));
                        if (ok) recreate();
                    }
                })
                .setNegativeButton(en ? "Close" : "Закрыть", null).show();
    }

    private interface ToggleListener { void onChanged(boolean v); }

    private void addSwitchRow(LinearLayout root, String label, boolean initial, final ToggleListener cb) {
        LinearLayout row = UiKit.rowCard(this, 14);
        TextView k = UiKit.title(this, label, 14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k.setLayoutParams(lp);
        row.addView(k);
        final View sw = new View(this);
        LinearLayout.LayoutParams swp = new LinearLayout.LayoutParams(dp(44), dp(22));
        sw.setLayoutParams(swp);
        sw.setBackground(makeSwitchBg(initial));
        sw.setTag(initial);
        row.addView(sw);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (prefs != null && prefs.isHaptic()) doVibrate();
                boolean cur = (Boolean) sw.getTag();
                boolean nv = !cur;
                sw.setTag(nv);
                cb.onChanged(nv);
                sw.setBackground(makeSwitchBg(nv));
            }
        });
        root.addView(row);
    }

    private android.graphics.drawable.GradientDrawable makeSwitchBg(boolean on) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(on ? 0xFF22C55E : 0xFF475569);
        return g;
    }
}
