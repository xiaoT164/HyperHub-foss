package com.pocotech.hub;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public final class LocaleHelper {

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN = "en";
    public static final String LANG_RU = "ru";

    private LocaleHelper() {}

    public static Context wrap(Context base) {
        if (base == null) return null;
        AppSettings settings = new AppSettings(base);
        String lang = settings.getLanguage();
        if (LANG_SYSTEM.equals(lang)) return base;

        Locale locale = new Locale(normalize(lang));
        Locale.setDefault(locale);

        Resources res = base.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new android.os.LocaleList(locale));
            return base.createConfigurationContext(config);
        }
        res.updateConfiguration(config, res.getDisplayMetrics());
        return base;
    }

    public static void apply(Context context) {
        if (context == null) return;
        wrap(context);
    }

    public static String getLanguage(Context context) {
        AppSettings settings = new AppSettings(context);
        return normalize(settings.getLanguage());
    }

    public static String getEffectiveLanguage(Context context) {
        AppSettings settings = new AppSettings(context);
        String lang = settings.getLanguage();
        if (!LANG_SYSTEM.equals(lang)) return normalize(lang);

        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        return locale != null ? normalize(locale.getLanguage()) : LANG_EN;
    }

    public static boolean isEnglish(Context context) {
        return LANG_EN.equals(getEffectiveLanguage(context));
    }

    public static String label(Context context, String code) {
        String normalized = normalize(code);
        if (LANG_SYSTEM.equals(normalized)) {
            return isEnglish(context) ? "Follow system" : "По системе";
        }
        if (LANG_RU.equals(normalized)) {
            return isEnglish(context) ? "Russian" : "Русский";
        }
        return isEnglish(context) ? "English" : "English";
    }

    private static String normalize(String code) {
        if (code == null || code.trim().length() == 0) return LANG_EN;
        String value = code.trim().toLowerCase(Locale.US);
        if (value.startsWith("ru")) return LANG_RU;
        if (value.startsWith("en")) return LANG_EN;
        if (LANG_SYSTEM.equals(value)) return LANG_SYSTEM;
        return LANG_EN;
    }
}
