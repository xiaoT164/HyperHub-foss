package com.pocotech.hub;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Живой монитор (2.0.0): RAM, хранилище, температура, батарея в реальном времени. */
public final class MonitorSheet {

    private MonitorSheet() {}

    public static void show(final Activity act, final HubHost host) {
        final boolean en = LocaleHelper.isEnglish(act);
        final int accent = host.prefs().getAccentColor() != 0 ? host.prefs().getAccentColor() : 0xFF4F8CFF;

        final Dialog dialog = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            win.setGravity(Gravity.BOTTOM);
            win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        LinearLayout rootBox = new LinearLayout(act);
        rootBox.setOrientation(LinearLayout.VERTICAL);
        rootBox.setBackgroundColor(0x00101010);

        LinearLayout sheet = new LinearLayout(act);
        sheet.setOrientation(LinearLayout.VERTICAL);
        int pad = HomeExtras.dp(act, 20);
        sheet.setPadding(pad, pad, pad, HomeExtras.dp(act, 28));
        android.graphics.drawable.GradientDrawable sheetBg = new android.graphics.drawable.GradientDrawable();
        sheetBg.setCornerRadii(new float[]{
                HomeExtras.dp(act, 26), HomeExtras.dp(act, 26),
                HomeExtras.dp(act, 26), HomeExtras.dp(act, 26), 0, 0, 0, 0});
        sheetBg.setColor(0xF0151A24);
        sheetBg.setStroke(1, 0x2EFFFFFF);
        sheet.setBackground(sheetBg);

        LinearLayout.LayoutParams sheetP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sheetP.gravity = Gravity.BOTTOM;
        sheet.setLayoutParams(sheetP);

        TextView title = HomeExtras.title(act, en ? "Live monitor" : "Живой монитор", 18f);
        sheet.addView(title);
        sheet.addView(HomeExtras.subtitle(act, en
                ? "Real-time memory, storage, temperature and battery"
                : "Память, накопитель, температура и батарея в реальном времени"));

        final TextView ramLabel = rowLabel(act);
        final LiveMonitorView ramChart = chart(act, accent);
        final TextView stoLabel = rowLabel(act);
        final LiveMonitorView stoChart = chart(act, accent);
        final TextView tempLabel = rowLabel(act);

        sheet.addView(ramLabel);
        sheet.addView(ramChart);
        sheet.addView(stoLabel);
        sheet.addView(stoChart);
        sheet.addView(tempLabel);

        ScrollView scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, HomeExtras.dp(act, 130));
        scroll.setLayoutParams(sp);
        final TextView detail = new TextView(act);
        detail.setTextColor(0xFFC0CCD8);
        detail.setTextSize(12f);
        detail.setLineSpacing(HomeExtras.dp(act, 3), 1f);
        scroll.addView(detail);
        sheet.addView(scroll);

        TextView close = HomeExtras.actionButton(act, en ? "Close" : "Закрыть", accent);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.topMargin = HomeExtras.dp(act, 14);
        close.setLayoutParams(cp);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        sheet.addView(close);

        rootBox.addView(sheet);
        dialog.setContentView(rootBox);

        final Handler handler = new Handler();
        final Runnable ticker = new Runnable() {
            @Override public void run() {
                HealthScore hs = HealthScore.compute(act);
                ramChart.push(hs.ramPercent / 100f);
                stoChart.push(hs.storagePercent / 100f);

                ramLabel.setText((en ? "RAM — " : "RAM — ") + hs.ramPercent + "%");
                stoLabel.setText((en ? "Storage — " : "Накопитель — ") + hs.storagePercent + "%");

                StringBuilder temp = new StringBuilder();
                temp.append(en ? "Temperature — " : "Температура — ");
                temp.append(hs.cpuTempC > 0 ? String.format(Locale.getDefault(), "CPU %.1f °C", hs.cpuTempC)
                        : (en ? "CPU n/a" : "CPU н/д"));
                if (hs.batteryTempC > 0f) {
                    temp.append(String.format(Locale.getDefault(), " · %.1f °C", hs.batteryTempC));
                }
                tempLabel.setText(temp.toString());

                StringBuilder d = new StringBuilder();
                d.append(hs.summary(en));
                if (hs.uptimeHours > 0) {
                    d.append(en ? "\nUptime: " : "\nАптайм: ").append(hs.uptimeHours).append(en ? " h" : " ч");
                }
                detail.setText(d.toString());

                handler.postDelayed(this, 1200);
            }
        };

        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface d) {
                handler.removeCallbacks(ticker);
            }
        });

        dialog.show();
        handler.post(ticker);

        sheet.setTranslationY(HomeExtras.dp(act, 340));
        sheet.animate().translationY(0f).setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2.2f)).start();
    }

    private static TextView rowLabel(Activity act) {
        TextView tv = new TextView(act);
        tv.setTextColor(0xFFE6EEFF);
        tv.setTextSize(12f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(0, HomeExtras.dp(act, 10), 0, HomeExtras.dp(act, 4));
        return tv;
    }

    private static LiveMonitorView chart(Activity act, int accent) {
        LiveMonitorView v = new LiveMonitorView(act);
        v.setAccent(accent);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, HomeExtras.dp(act, 54)));
        return v;
    }
}
