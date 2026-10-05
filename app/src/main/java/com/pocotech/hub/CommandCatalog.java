package com.pocotech.hub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Каталог команд HyperOS/MIUI, доступных через ADB.
 * Используется на экране «Команды» и в карточке глубокой оптимизации на главном экране.
 */
public final class CommandCatalog {

    public static final class Cmd {
        public final String key;
        public final String titleRu;
        public final String titleEn;
        public final String descRu;
        public final String descEn;
        public final String command;
        public final String group;
        public final boolean dangerous;

        Cmd(String key, String group, String tr, String te,
            String dr, String de, String command, boolean dangerous) {
            this.key = key;
            this.group = group;
            this.titleRu = tr;
            this.titleEn = te;
            this.descRu = dr;
            this.descEn = de;
            this.command = command;
            this.dangerous = dangerous;
        }
    }

    private static final Cmd[] COMMANDS = {
        new Cmd("lmk_full",
            "lmk", "Глубокая оптимизация LMK (полная)",
            "Deep LMK tuning (full reset)",
            "Сбрасывает все таймеры ActivityManager: немедленное освобождение фоновых процессов, GC раз в 0 мс, нет удержания провайдеров. Радикально снижает расход RAM, но может приводить к вылету из спящих приложений.",
            "Resets all ActivityManager constants: immediate trim of cached processes, 0 ms GC, no provider retention. Drastically cuts RAM usage but may force-stop sleeping apps.",
            "activity_manager_constants max_cached_processes=0,background_settle_time=0,fgservice_min_shown_time=0,fgservice_min_report_time=0,fgservice_screen_on_before_time=0,fgservice_screen_on_after_time=0,content_provider_retain_time=0,gc_timeout=0,gc_min_interval=0,full_pss_min_interval=0,full_pss_lowered_interval=0,power_check_interval=0,power_check_max_cpu_1=0,power_check_max_cpu_2=0,power_check_max_cpu_3=0,power_check_max_cpu_4=0,service_usage_interaction_time=0,usage_stats_interaction_interval=0,service_restart_duration=0,service_reset_run_duration=0,service_restart_duration_factor=0,service_min_restart_time_between=0,service_max_inactivity=0,service_bg_start_timeout=0,CUR_MAX_CACHED_PROCESSES=0,CUR_MAX_EMPTY_PROCESSES=0,CUR_TRIM_EMPTY_PROCESSES=0,CUR_TRIM_CACHED_PROCESSES=0",
            true),

        new Cmd("lmk_lite",
            "lmk", "Мягкая LMK-оптимизация",
            "Soft LMK optimization",
            "Ставит таймеры в 0, сохраняя 1 кэшированный процесс. Уменьшает фоновую активность без вылетов.",
            "Sets timers to 0 but keeps 1 cached process. Reduces background overhead without force-stops.",
            "activity_manager_constants max_cached_processes=1,background_settle_time=0,content_provider_retain_time=0,gc_timeout=0,gc_min_interval=0,service_max_inactivity=0",
            false),

        new Cmd("lmk_restore",
            "lmk", "Сброс LMK к стандарту",
            "Restore LMK defaults",
            "Возвращает заводские значения ActivityManager: очистить текущие твики LMK после теста.",
            "Restores default ActivityManager constants: clears current LMK tweaks after testing.",
            "activity_manager_constants reset",
            false),

        new Cmd("mqs_perf",
            "perf", "Режим производительности MQS",
            "MQS performance mode",
            "Включает MQS Enhanced Mode и интенсивный GC. Повышает FPS в играх, увеличивает нагрев.",
            "Enables MQS Enhanced Mode and aggressive GC. Improves FPS in games, increases heat.",
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.fboservice.ctrl true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.use_mi_new_strategy true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.smart_gc.enable true\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.power.default.powermode enhance\" s16 \"/storage/emulated/0/log.txt\" i32 600",
            true),

        new Cmd("trace_boost",
            "perf", "FPS boost: trace buffer",
            "FPS boost: trace buffer",
            "Увеличивает trace buffer для разработчика. Сначала включите режим разработчика.",
            "Increases the developer trace buffer. Enable developer mode first.",
            "settings put global trace_buffer_size_kb 32768\nsettings put global atrace_enabled false",
            false),

        new Cmd("flagship_anim",
            "perf", "Флагманские blur-анимации",
            "Flagship blur animations",
            "Включает системное blur на бюджетных POCO/Redmi. Может подтормаживать на 4 ГБ RAM.",
            "Enables system blur on budget POCO/Redmi. May lag on 4 GB RAM devices.",
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.cpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.gpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600\n" +
            "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.background_blur_supported true\" s16 \"/storage/emulated/0/log.txt\" i32 600",
            true),

        new Cmd("ping_opt",
            "net", "Оптимизация пинга",
            "Ping optimization",
            "Отключает регулирование Wi-Fi-сканирования и авто-оценку сети. Снижает задержку в онлайн-играх.",
            "Disables Wi-Fi scan throttling and network auto-scoring. Reduces latency in online games.",
            "settings put global wifi_scan_throttle_enabled 0\nsettings put global network_scoring_ui_enabled 0",
            false),

        new Cmd("power_tune",
            "batt", "Энергорежим + таймаут экрана",
            "Power tuning + screen timeout",
            "Отключает принудительный Low Power, ставит таймаут экрана 2 мин. Стабильнее, но чуть больше расхода.",
            "Disables forced Low Power, sets 2-min screen timeout. More stable, slightly higher drain.",
            "settings put global low_power_sticky 0\nsettings put system screen_off_timeout 120000",
            false),

        new Cmd("game_turbo",
            "game", "Game Turbo через ADB",
            "Game Turbo via ADB",
            "Включает Game Turbo, приоритизирует CPU/GPU и блокирует лишние уведомления во время игры.",
            "Enables Game Turbo, prioritizes CPU/GPU and blocks notifications while playing.",
            "settings put system miui_gaming_mode_enabled 1\nsettings put system miui_gaming_notification_enabled 0\nsettings put system miui_gaming_touch_mode 1",
            false),

        new Cmd("clean_desktop",
            "ui", "Чистый рабочий стол (без подписей)",
            "Clean desktop (no labels)",
            "Скрывает подписи под иконками. Перезагрузите лаунчер после применения.",
            "Hides icon labels. Restart launcher after applying.",
            "settings put system miui_home_no_word_model 1\nsettings put system show_text_under_icons 0\nsettings put system shelter_icon_name 1\nsettings put system miui_home_icon_title_max_lines 0\nsettings put system miui_home_icon_text_size 0\nsettings put system hide_icon_labels 1",
            false),

        new Cmd("ads_msa",
            "ui", "Отключение рекламы MIUI (msa)",
            "Disable MIUI ads (msa)",
            "Отзывает разрешение msa и отключает персонализированные рекомендации в системных приложениях.",
            "Revokes msa permission and turns off personalized recommendations.",
            "settings put global adb_enabled 1",
            false),
    };

    public static List<Cmd> all() { return Collections.unmodifiableList(Arrays.asList(COMMANDS)); }
    public static Cmd byKey(String key) {
        if (key == null) return null;
        for (Cmd c : COMMANDS) if (c.key.equals(key)) return c;
        return null;
    }
    public static List<Cmd> byGroup(String group) {
        List<Cmd> out = new ArrayList<>();
        for (Cmd c : COMMANDS) if (c.group.equals(group)) out.add(c);
        return out;
    }
}
