package com.pocotech.hub;

/** Мост между UI-модулями v2.0.0 и MainActivity. */
public interface HubHost {

    /** Выполнить функцию по её ключу (см. {@link Features}). */
    void runFeature(String key);

    /** Открыть экран по индексу навигации (6 — профили, 7 — достижения, 8 — онбординг). */
    void openScreen(int index);

    /** Показать нижний лист-подсказку. */
    void showSheet(String title, String body);

    void toast(String message);

    void vibrate();

    /** Перезапустить экран целиком (после смены данных). */
    void refreshCurrentScreen();

    AppSettings prefs();

    HubStore store();
}
