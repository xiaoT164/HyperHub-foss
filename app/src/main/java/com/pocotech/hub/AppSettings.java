package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;

/** Простая обёртка над SharedPreferences. */
public class AppSettings {

    private static final String PREFS = "ptb_prefs";

    public static final String K_ACCENT          = "accent";
    public static final String K_BG_ANIM         = "bg_anim";
    public static final String K_HAPTIC          = "haptic";
    public static final String K_SPLASH          = "splash";
    public static final String K_LIQUID_GLASS    = "liquid_glass";
    public static final String K_ONBOARDING_DONE = "onboarding_done";
    public static final String K_LANGUAGE        = "language";
    public static final String K_REMINDER        = "reminder_enabled";
    public static final String K_REMINDER_HOUR   = "reminder_hour";
    public static final String K_REMINDER_MIN    = "reminder_minute";

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN     = "en";
    public static final String LANG_RU     = "ru";

    private final SharedPreferences sp;

    public AppSettings(Context ctx) {
        // В attachBaseContext() у Application ещё нет application-контекста:
        // getApplicationContext() возвращает null до конца attach(), поэтому
        // обращаться к нему напрямую нельзя — падало с NullPointerException
        // ещё до создания первого экрана. Берём сам ctx, если application-контекста нет.
        Context appCtx = ctx.getApplicationContext();
        if (appCtx == null) appCtx = ctx;
        sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getAccentColor() { return sp.getInt(K_ACCENT, 0xFF4F46E5); }
    public void setAccentColor(int color) { sp.edit().putInt(K_ACCENT, color).apply(); }

    public boolean isBgAnim() { return sp.getBoolean(K_BG_ANIM, true); }
    public void setBgAnim(boolean v) { sp.edit().putBoolean(K_BG_ANIM, v).apply(); }

    public boolean isHaptic() { return sp.getBoolean(K_HAPTIC, true); }
    public void setHaptic(boolean v) { sp.edit().putBoolean(K_HAPTIC, v).apply(); }

    public boolean isSplash() { return sp.getBoolean(K_SPLASH, true); }
    public void setSplash(boolean v) { sp.edit().putBoolean(K_SPLASH, v).apply(); }

    public boolean isLiquidGlass() { return sp.getBoolean(K_LIQUID_GLASS, true); }
    public void setLiquidGlass(boolean v) { sp.edit().putBoolean(K_LIQUID_GLASS, v).apply(); }

    public boolean isOnboardingDone() { return sp.getBoolean(K_ONBOARDING_DONE, false); }
    public void setOnboardingDone(boolean v) { sp.edit().putBoolean(K_ONBOARDING_DONE, v).apply(); }

    public String getLanguage() { return sp.getString(K_LANGUAGE, LANG_SYSTEM); }
    public void setLanguage(String lang) { sp.edit().putString(K_LANGUAGE, lang).apply(); }

    // ── Ежедневное напоминание ───────────────────────────────────────────────
    public boolean isReminderEnabled() { return sp.getBoolean(K_REMINDER, false); }
    public void setReminderEnabled(boolean v) { sp.edit().putBoolean(K_REMINDER, v).apply(); }

    public int getReminderHour() { return sp.getInt(K_REMINDER_HOUR, 20); }
    public int getReminderMinute() { return sp.getInt(K_REMINDER_MIN, 0); }

    /** Задаёт время напоминания; часы/минуты автоматически приводятся к допустимому диапазону. */
    public void setReminderTime(int hour, int minute) {
        int h = Math.max(0, Math.min(23, hour));
        int m = Math.max(0, Math.min(59, minute));
        sp.edit().putInt(K_REMINDER_HOUR, h).putInt(K_REMINDER_MIN, m).apply();
    }
}
