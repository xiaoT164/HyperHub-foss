package com.pocotech.hub;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Онбординг v2.0.0: 4 шага, которые объясняют, зачем возвращаться в приложение.
 * Показывается один раз (флаг в AppSettings).
 */
public final class OnboardingScreen {

    private OnboardingScreen() {}

    private static final int PAGES = 4;

    public static View build(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final int accent = host.prefs().getAccentColor() != 0 ? host.prefs().getAccentColor() : 0xFF4F8CFF;
        final int[] page = {0};

        final LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF080C14);
        int pad = HomeExtras.dp(act, 22);
        root.setPadding(pad, HomeExtras.dp(act, 60), pad, HomeExtras.dp(act, 40));

        final TextView dots = new TextView(act);
        dots.setTextSize(13f);
        dots.setTextColor(accent);
        dots.setGravity(android.view.Gravity.CENTER);
        dots.setPadding(0, 0, 0, HomeExtras.dp(act, 20));
        root.addView(dots);

        final TextView icon = new TextView(act);
        icon.setTextSize(52f);
        icon.setGravity(android.view.Gravity.CENTER);
        root.addView(icon);

        final TextView title = new TextView(act);
        title.setTextSize(26f);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, HomeExtras.dp(act, 16), 0, HomeExtras.dp(act, 12));
        root.addView(title);

        final ScrollView scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        final TextView body = new TextView(act);
        body.setTextSize(14f);
        body.setTextColor(0xFFC0CCD8);
        body.setLineSpacing(HomeExtras.dp(act, 5), 1f);
        body.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        scroll.addView(body);
        root.addView(scroll);

        final TextView primary = HomeExtras.actionButton(act, "", accent);
        primary.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(primary);

        final TextView secondary = HomeExtras.ghostButton(act, "");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = HomeExtras.dp(act, 10);
        secondary.setLayoutParams(sp);
        root.addView(secondary);

        final String[] icons = {"🚀", "🩺", "🎯", "⚡"};
        final String[] ru = {
                "HyperHub 2.0 — центр управления HyperOS.\n\nВсе твики, диагностика и бенчмарк собраны в одном месте: 21 инструмент, поиск, избранное и профили. Команды для ADB копируются в один тап.",
                "Каждый день — новая цифра.\n\nИндекс здоровья оценивает RAM, накопитель, температуру и батарею, показывает рекомендации и графики в реальном времени. Открывайте приложение, чтобы видеть, что изменилось.",
                "Возвращаться становится интересно.\n\nСерия дней, XP и уровень, 12 достижений и история бенчмарков с графиком и разницей к прошлому прогону. Прогресс хранится локально.",
                "Профили делают работу за вас.\n\n«Игровой», «Батарея», «Чистый интерфейс», «Диагностика», «Приватность» — готовые сценарии, а также свои наборы. Плюс ежедневное напоминание о проверке устройства."
        };
        final String[] enT = {
                "HyperHub 2.0 — your HyperOS control center.\n\nAll tweaks, diagnostics and the benchmark live in one place: 21 tools, search, favorites and profiles. ADB commands copy in a single tap.",
                "A new number every day.\n\nThe health index scores RAM, storage, temperature and battery, with recommendations and real-time charts. Open the app to see what changed.",
                "Coming back becomes rewarding.\n\nStreaks, XP and level, 12 achievements, plus benchmark history with a chart and the delta versus the previous run. Progress is stored locally.",
                "Profiles do the work for you.\n\nGaming, Battery, Clean UI, Diagnostics, Privacy — ready scenarios plus your own sets. A daily reminder keeps the device checked."
        };

        final Runnable render = new Runnable() {
            @Override public void run() {
                int i = page[0];
                icon.setText(icons[i]);
                title.setText(en
                        ? new String[]{"Welcome to HyperHub", "Device health", "Progress and goals", "Profiles and daily check"}[i]
                        : new String[]{"Добро пожаловать в HyperHub", "Здоровье устройства", "Прогресс и цели", "Профили и ежедневная проверка"}[i]);
                body.setText(en ? enT[i] : ru[i]);
                StringBuilder d = new StringBuilder();
                for (int k = 0; k < PAGES; k++) d.append(k == i ? "● " : "○ ");
                dots.setText(d.toString().trim());

                primary.setText(i == PAGES - 1
                        ? (en ? "Start using HyperHub" : "Начать пользоваться")
                        : (en ? "Next" : "Далее"));
                secondary.setText(i == 0
                        ? (en ? "Skip" : "Пропустить")
                        : (en ? "Back" : "Назад"));
            }
        };

        primary.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                if (page[0] == PAGES - 1) {
                    host.prefs().setOnboardingDone(true);
                    host.openScreen(0);
                } else {
                    page[0]++;
                    render.run();
                }
            }
        });

        secondary.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.vibrate();
                if (page[0] == 0) {
                    host.prefs().setOnboardingDone(true);
                    host.openScreen(0);
                } else {
                    page[0]--;
                    render.run();
                }
            }
        });

        render.run();
        return root;
    }
}
