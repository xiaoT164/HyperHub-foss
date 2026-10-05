package com.pocotech.hub;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public final class LocaleHelper {

    private LocaleHelper() {}

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN     = "en";
    public static final String LANG_RU     = "ru";

    public static Context wrap(Context base) {
        AppSettings prefs = new AppSettings(base);
        String code = prefs.getLanguage();
        if (code == null || LANG_SYSTEM.equals(code)) return base;
        Locale target = LANG_EN.equals(code) ? Locale.ENGLISH : new Locale("ru");
        Locale.setDefault(target);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) cfg.setLocale(target);
        else cfg.locale = target;
        return base.createConfigurationContext(cfg);
    }

    public static boolean isEnglish(Context ctx) {
        AppSettings prefs = new AppSettings(ctx);
        String code = prefs.getLanguage();
        if (LANG_EN.equals(code)) return true;
        if (LANG_RU.equals(code)) return false;
        Locale current = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? ctx.getResources().getConfiguration().getLocales().get(0)
                : ctx.getResources().getConfiguration().locale;
        return current != null && "en".equalsIgnoreCase(current.getLanguage());
    }
}
