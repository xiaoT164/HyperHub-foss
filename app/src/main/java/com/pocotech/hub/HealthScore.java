package com.pocotech.hub;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Индекс здоровья устройства (0–100) на основе RAM, накопителя, температуры,
 * батареи и аптайма. Используется на главном экране и в разделе достижений.
 */
public final class HealthScore {

    public int score = 100;
    public int ramPercent = 0;
    public int storagePercent = 0;
    public int batteryPercent = -1;
    public float batteryTempC = 0f;
    public float cpuTempC = 0f;
    public int uptimeHours = 0;
    public boolean lowPower = false;

    public final List<String> adviceRu = new ArrayList<>();
    public final List<String> adviceEn = new ArrayList<>();

    public static HealthScore compute(Context ctx) {
        HealthScore hs = new HealthScore();

        // ── RAM ──────────────────────────────────────────────────────────────
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                if (mi.totalMem > 0) {
                    hs.ramPercent = (int) ((mi.totalMem - mi.availMem) * 100f / mi.totalMem);
                }
            }
        } catch (Exception ignored) {}

        // ── Хранилище ────────────────────────────────────────────────────────
        try {
            StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
            long total = sf.getBlockCountLong() * sf.getBlockSizeLong();
            long free = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
            if (total > 0) hs.storagePercent = (int) ((total - free) * 100f / total);
        } catch (Exception ignored) {}

        // ── Батарея ──────────────────────────────────────────────────────────
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bi = ctx.registerReceiver(null, filter);
            if (bi != null) {
                int level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) hs.batteryPercent = (int) (level * 100f / scale);
                int temp = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                hs.batteryTempC = temp / 10f;
            }
        } catch (Exception ignored) {}

        // ── CPU температура ──────────────────────────────────────────────────
        hs.cpuTempC = readCpuTemp();

        // ── Аптайм ───────────────────────────────────────────────────────────
        hs.uptimeHours = (int) (SystemClock.elapsedRealtime() / 3600000L);

        hs.evaluate(ctx);
        return hs;
    }

    private void evaluate(Context ctx) {
        boolean en = LocaleHelper.isEnglish(ctx);
        score = 100;

        if (ramPercent >= 90) {
            score -= 22;
            add(en, "Память загружена почти полностью — закройте тяжёлые приложения и ограничьте автозапуск",
                    "Memory is nearly full — close heavy apps and limit autostart");
        } else if (ramPercent >= 80) {
            score -= 12;
            add(en, "Высокая загрузка RAM: проверьте фоновые приложения",
                    "High RAM usage: check background apps");
        } else if (ramPercent >= 70) {
            score -= 5;
            add(en, "Загрузка RAM умеренная — изредка бывает полезна очистка фона",
                    "RAM usage is moderate — occasional background cleanup helps");
        }

        if (storagePercent >= 95) {
            score -= 20;
            add(en, "Хранилище заполнено почти полностью — это замедляет запись и обновления",
                    "Storage is nearly full — it slows down writes and updates");
        } else if (storagePercent >= 88) {
            score -= 10;
            add(en, "Свободного места меньше 12% — освободите несколько гигабайт",
                    "Less than 12% free space — free up a few gigabytes");
        } else if (storagePercent >= 80) {
            score -= 4;
        }

        if (batteryTempC >= 43f) {
            score -= 14;
            add(en, "Батарея горячая — снимите чехол, уберите из-под солнца, ограничьте нагрузку",
                    "Battery is hot — remove the case, avoid direct sun, reduce load");
        } else if (batteryTempC >= 40f) {
            score -= 7;
            add(en, "Батарея тёплая: активные игры и зарядка одновременно перегревают устройство",
                    "Battery is warm: heavy gaming while charging overheats the device");
        }

        if (cpuTempC >= 70f) {
            score -= 12;
            add(en, "CPU сильно нагрет — сделайте перерыв, троттлинг снижает производительность",
                    "CPU is very hot — take a break, throttling is reducing performance");
        } else if (cpuTempC >= 60f) {
            score -= 6;
            add(en, "Температура CPU повышенная: фоновая нагрузка или игры",
                    "CPU temperature is elevated: background load or gaming");
        }

        if (uptimeHours >= 480) {
            score -= 8;
            add(en, "Устройство не перезагружалось 20+ дней — перезагрузка очищает системные службы",
                    "The device has not been rebooted in 20+ days — a reboot clears system services");
        } else if (uptimeHours >= 336) {
            score -= 4;
            add(en, "Больше недели без перезагрузки — стоит перезапустить телефон",
                    "More than a week without a reboot — it is worth restarting the phone");
        }

        if (batteryPercent >= 0 && batteryPercent <= 15 && !isCharging(ctx)) {
            score -= 6;
            add(en, "Низкий заряд батареи — подключите зарядку, чтобы избежать троттлинга",
                    "Low battery — connect the charger to avoid throttling");
        }

        if (score > 100) score = 100;
        if (score < 5) score = 5;

        if (adviceRu.isEmpty()) {
            adviceRu.add("Всё в порядке: температура, память и накопитель в норме");
            adviceEn.add("All good: temperature, memory and storage are within normal range");
        }
    }

    private void add(boolean en, String ru, String enText) {
        adviceRu.add(ru);
        adviceEn.add(enText);
    }

    private boolean isCharging(Context ctx) {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bi = ctx.registerReceiver(null, filter);
            if (bi == null) return false;
            int status = bi.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Exception e) {
            return false;
        }
    }

    public String gradeRu() {
        if (score >= 90) return "Отличное состояние";
        if (score >= 75) return "Хорошее состояние";
        if (score >= 60) return "Есть что подтянуть";
        if (score >= 40) return "Требует внимания";
        return "Нужна чистка и охлаждение";
    }

    public String gradeEn() {
        if (score >= 90) return "Excellent";
        if (score >= 75) return "Good";
        if (score >= 60) return "Needs tuning";
        if (score >= 40) return "Needs attention";
        return "Needs cleanup";
    }

    public String grade(boolean en) { return en ? gradeEn() : gradeRu(); }

    public String summary(boolean en) {
        StringBuilder sb = new StringBuilder();
        sb.append(en ? "Health index: " : "Индекс здоровья: ").append(score).append("/100 (").append(grade(en)).append(")\n\n");
        sb.append(en ? "RAM: " : "RAM: ").append(ramPercent).append("%\n");
        sb.append(en ? "Storage: " : "Хранилище: ").append(storagePercent).append("%\n");
        if (batteryPercent >= 0) {
            sb.append(en ? "Battery: " : "Батарея: ").append(batteryPercent).append("% · ")
              .append(String.format(Locale.getDefault(), "%.1f °C", batteryTempC)).append("\n");
        }
        if (cpuTempC > 0) {
            sb.append("CPU: ").append(String.format(Locale.getDefault(), "%.1f °C", cpuTempC)).append("\n");
        }
        sb.append(en ? "Uptime: " : "Аптайм: ").append(uptimeHours).append(en ? " h" : " ч").append("\n\n");
        sb.append(en ? "Recommendations:\n" : "Рекомендации:\n");
        List<String> src = en ? adviceEn : adviceRu;
        for (String a : src) sb.append("• ").append(a).append("\n");
        return sb.toString();
    }

    public static float readCpuTemp() {
        String[] zones = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp"
        };
        for (String path : zones) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(path));
                String line = br.readLine();
                if (line != null) {
                    float t = Float.parseFloat(line.trim());
                    if (t > 1000f) t /= 1000f;
                    if (t > 0f && t < 130f) return t;
                }
            } catch (Exception ignored) {
            } finally {
                try { if (br != null) br.close(); } catch (Exception ignored) {}
            }
        }
        return 0f;
    }

    public static int readBatteryPercent(Context ctx) {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bi = ctx.registerReceiver(null, filter);
            if (bi == null) return -1;
            int level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) return (int) (level * 100f / scale);
        } catch (Exception ignored) {}
        return -1;
    }

    public static int readRamPercent(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                if (mi.totalMem > 0) return (int) ((mi.totalMem - mi.availMem) * 100f / mi.totalMem);
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
