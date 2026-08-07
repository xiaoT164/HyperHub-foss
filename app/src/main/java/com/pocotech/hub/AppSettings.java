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
    public String  getLanguage()    { return sp.getString("app_language", "en"); }

    public void setLiquidGlass(boolean v) { sp.edit().putBoolean("liquid_glass", v).apply(); }
    public void setBgAnim(boolean v)      { sp.edit().putBoolean("bg_anim", v).apply(); }
    public void setSplash(boolean v)      { sp.edit().putBoolean("splash", v).apply(); }
    public void setHaptic(boolean v)      { sp.edit().putBoolean("haptic", v).apply(); }
    public void setAccentColor(int c)     { sp.edit().putInt("accent_color", c).apply(); }
    public void setLanguage(String code)  { sp.edit().putString("app_language", code).apply(); }
}
