package com.pocotech.hub;

import java.util.ArrayList;
import java.util.List;

/**
 * Единый реестр функций HyperHub (v2.0.0).
 * Используется для: главного экрана, избранного, недавних, профилей и поиска.
 */
public final class Features {

    public static final String SEC_OPT   = "opt";
    public static final String SEC_HID   = "hid";
    public static final String SEC_GUIDE = "guide";
    public static final String SEC_COMM  = "comm";

    public static final class F {
        public final String key;
        public final String ru;
        public final String en;
        public final String section;
        public final int cardId;
        public final String keywords;

        F(String key, String ru, String en, String section, int cardId, String keywords) {
            this.key = key;
            this.ru = ru;
            this.en = en;
            this.section = section;
            this.cardId = cardId;
            this.keywords = keywords;
        }
    }

    private Features() {}

    public static final F[] ALL = {
        new F("fps", "Разгон FPS", "FPS boost", SEC_OPT, R.id.btn_fps,
                "fps boost performance trace tracing gpu разгон производительность трассировка"),
        new F("perf", "Режим производительности", "Performance mode", SEC_OPT, R.id.btn_performance,
                "performance perf mqs enhance mode производительность"),
        new F("anim", "Флагманские анимации", "Flagship animations", SEC_OPT, R.id.btn_animations,
                "animations blur gpu flagship smooth анимации"),
        new F("ram", "RAM и фоновые процессы", "RAM and background apps", SEC_OPT, R.id.btn_ram_clean,
                "ram memory cleanup lmk background память очистка"),
        new F("net", "Оптимизация пинга", "Ping optimization", SEC_OPT, R.id.btn_network_opt,
                "network ping wifi latency signal сеть задержка"),
        new F("batt", "Настройка энергорежима", "Power tuning", SEC_OPT, R.id.btn_battery_opt,
                "battery power saving timeout charge батарея заряд"),
        new F("game", "Игровой турбо-режим", "Game Turbo mode", SEC_OPT, R.id.btn_game_mode,
                "game turbo gaming mode игра режим"),
        new F("bloat", "Отключение bloatware", "Disable bloatware", SEC_HID, R.id.btn_disable_apps,
                "bloatware disable mi apps приложения отключить"),
        new F("radio", "Radio Info", "Radio Info", SEC_HID, R.id.btn_radio_info,
                "radio info network engineer lte 5g сеть инженерный"),
        new F("cit", "CIT Диагностика", "CIT diagnostics", SEC_HID, R.id.btn_cit_test,
                "cit test diagnostics hardware тест диагностика"),
        new F("dev", "Параметры разработчика", "Developer options", SEC_HID, R.id.btn_usb_debug,
                "usb adb developer options разработчик"),
        new F("display", "Скрытые настройки дисплея", "Hidden display settings", SEC_HID, R.id.btn_display_hidden,
                "display screen dc dimming 120hz pwm дисплей экран"),
        new F("debug", "MIUI/HyperOS Debug Tools", "MIUI/HyperOS Debug Tools", SEC_HID, R.id.btn_miui_debug,
                "debug miui engineering codes инженерный код"),
        new F("labs", "Xiaomi / POCO Labs", "Xiaomi / POCO Labs", SEC_HID, R.id.btn_xiaomi_labs,
                "labs xiaomi poco beta features бета функции"),
        new F("ads", "Отключение рекламы", "Disable ads", SEC_GUIDE, R.id.btn_ads,
                "ads msa recommendations browser реклама"),
        new F("labels", "Чистый рабочий стол", "Clean home screen", SEC_GUIDE, R.id.btn_hide_labels,
                "labels icons home screen launcher ярлыки подписи"),
        new F("region", "Фишки смены региона", "Region switch perks", SEC_GUIDE, R.id.btn_region_hack,
                "region singapore india usa регион"),
        new F("space", "Second Space / Клонирование", "Second Space / App cloning", SEC_GUIDE, R.id.btn_second_space,
                "second space clone profile apps клонирование"),
        new F("font", "Сторонние шрифты", "Custom fonts", SEC_GUIDE, R.id.btn_font_hack,
                "font custom root milanpro shizuku шрифт"),
        new F("tg", "Telegram-канал", "Telegram channel", SEC_COMM, R.id.btn_telegram,
                "telegram channel community канал сообщество"),
        new F("donate", "Поддержать HyperHub", "Support HyperHub", SEC_COMM, R.id.btn_donate,
                "support donationalerts donate поддержать донат")
    };

    public static F byKey(String key) {
        if (key == null) return null;
        for (F f : ALL) if (f.key.equals(key)) return f;
        return null;
    }

    public static String title(String key, boolean en) {
        F f = byKey(key);
        if (f == null) return key;
        return en ? f.en : f.ru;
    }

    public static String sectionTitle(String section, boolean en) {
        if (SEC_OPT.equals(section))   return en ? "OPTIMIZATION" : "ОПТИМИЗАЦИЯ";
        if (SEC_HID.equals(section))   return en ? "HIDDEN SETTINGS" : "СКРЫТЫЕ НАСТРОЙКИ";
        if (SEC_GUIDE.equals(section)) return en ? "GUIDES" : "ГАЙДЫ";
        return en ? "COMMUNITY" : "СООБЩЕСТВО";
    }

    public static List<F> inSection(String section) {
        List<F> out = new ArrayList<>();
        for (F f : ALL) if (f.section.equals(section)) out.add(f);
        return out;
    }
}
