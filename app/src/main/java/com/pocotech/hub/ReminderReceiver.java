package com.pocotech.hub;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Random;

/**
 * Ежедневное напоминание (2.0.0): проверка здоровья устройства, серии дней и бенчмарка.
 * Полностью локально, без сети.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "hub_daily";
    public static final int NOTIFICATION_ID = 4201;
    private static final int REQUEST_CODE = 9911;

    private static final String[] TIPS_RU = {
        "Проверьте индекс здоровья устройства и серию дней",
        "Не забудьте про фон: закройте тяжёлые приложения",
        "Хороший момент, чтобы прогнать HubBench и сравнить результат",
        "Проверьте обновления HyperOS в разделе Predict",
        "Загляните в профили: «Игровой» или «Батарея» под текущую задачу"
    };

    private static final String[] TIPS_EN = {
        "Check your device health index and streak",
        "Mind the background: close heavy apps",
        "A good moment to run HubBench and compare results",
        "Check HyperOS updates in the Predict section",
        "Try a profile: Game or Battery, depending on the task"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        AppSettings prefs = new AppSettings(context);
        if (!prefs.isReminderEnabled()) return;

        boolean en = LocaleHelper.isEnglish(context);
        int streak = new HubStore(context).streak();

        String title = en ? "HyperHub daily check" : "Ежедневная проверка HyperHub";
        String[] tips = en ? TIPS_EN : TIPS_RU;
        String tip = tips[new Random().nextInt(tips.length)];
        String body = tip + (streak > 0
                ? (en ? "\nStreak: " + streak + " day(s) — keep it going!" : "\nСерия: " + streak + " дн. — не прерывайте!")
                : "");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    en ? "Daily check" : "Ежедневная проверка",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(en
                    ? "Reminders to check device health and updates"
                    : "Напоминания проверить здоровье устройства и обновления");
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(context, REQUEST_CODE, open, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.poco_icon)
                .setContentTitle(title)
                .setContentText(body.replace("\n", " "))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {}
    }

    /** Планирует ежедневное напоминание на указанное время. */
    public static void schedule(Context context, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) {}
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
        am.cancel(pi);
    }

    public static void applyFromPrefs(Context context) {
        AppSettings prefs = new AppSettings(context);
        if (prefs.isReminderEnabled()) {
            schedule(context, prefs.getReminderHour(), prefs.getReminderMinute());
        } else {
            cancel(context);
        }
    }
}
