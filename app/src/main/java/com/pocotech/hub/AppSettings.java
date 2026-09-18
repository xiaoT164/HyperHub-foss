package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREFS = "ptb_prefs";
    private final SharedPreferences sp;

    public AppSettings(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isLiquidGlass()  { return sp.getBoolean("liquid_glass", true); }
    public boolean isBgAnim()       { return sp.getBoolean("bg_anim", true); }
    public boolean isSplash()       { return sp.getBoolean("splash", true); }
    public boolean isHaptic()       { return sp.getBoolean("haptic", true); }
    public int     getAccentColor() { return sp.getInt("accent_color", 0xFF4F8CFF); }
    public int     getThemeIndex()  { return sp.getInt("theme_index", 0); }
    public String  getLanguage()    { return sp.getString("app_language", "en"); }
    public boolean isOnboardingDone()   { return sp.getBoolean("onboarding_done", false); }
    public boolean isReminderEnabled()  { return sp.getBoolean("reminder_enabled", false); }
    public int     getReminderHour()    { return sp.getInt("reminder_hour", 20); }
    public int     getReminderMinute()  { return sp.getInt("reminder_minute", 0); }
    public boolean isAutoHealth()       { return sp.getBoolean("auto_health", true); }

    public void setLiquidGlass(boolean v) { sp.edit().putBoolean("liquid_glass", v).apply(); }
    public void setBgAnim(boolean v)      { sp.edit().putBoolean("bg_anim", v).apply(); }
    public void setSplash(boolean v)      { sp.edit().putBoolean("splash", v).apply(); }
    public void setHaptic(boolean v)      { sp.edit().putBoolean("haptic", v).apply(); }
    public void setAccentColor(int c)     { sp.edit().putInt("accent_color", c).apply(); }
    public void setThemeIndex(int i)      { sp.edit().putInt("theme_index", i).apply(); }
    public void setLanguage(String code)  { sp.edit().putString("app_language", code).apply(); }
    public void setOnboardingDone(boolean v)  { sp.edit().putBoolean("onboarding_done", v).apply(); }
    public void setReminderEnabled(boolean v) { sp.edit().putBoolean("reminder_enabled", v).apply(); }
    public void setReminderTime(int h, int m) {
        sp.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply();
    }
    public void setAutoHealth(boolean v)      { sp.edit().putBoolean("auto_health", v).apply(); }
}
