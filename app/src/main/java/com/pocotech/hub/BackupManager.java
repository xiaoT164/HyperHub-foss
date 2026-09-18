package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Экспорт/импорт настроек и статистики HyperHub в JSON (2.0.0).
 * Файл сохраняется локально, данные дублируются в буфер обмена.
 */
public final class BackupManager {

    private static final String SETTINGS_PREFS = "ptb_prefs";

    private BackupManager() {}

    public static String export(Context ctx) {
        JSONObject root = new JSONObject();
        try {
            root.put("app", "HyperHub");
            root.put("version", BuildConfig.VERSION_NAME);
            root.put("versionCode", BuildConfig.VERSION_CODE);
            root.put("exported_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            root.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

            JSONObject settings = new JSONObject();
            SharedPreferences sp = ctx.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
                Object v = e.getValue();
                if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Float) {
                    settings.put(e.getKey(), v);
                } else if (v instanceof String) {
                    settings.put(e.getKey(), v);
                }
            }
            root.put("settings", settings);
            root.put("store", new JSONObject(new HubStore(ctx).exportJson()));
        } catch (Exception ignored) {}
        return root.toString();
    }

    public static boolean importJson(Context ctx, String json) {
        if (json == null || json.trim().isEmpty()) return false;
        try {
            JSONObject root = new JSONObject(json.trim());
            JSONObject settings = root.optJSONObject("settings");
            if (settings != null) {
                SharedPreferences.Editor ed = ctx.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).edit();
                java.util.Iterator<String> it = settings.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    Object v = settings.opt(key);
                    if (v instanceof Boolean) ed.putBoolean(key, (Boolean) v);
                    else if (v instanceof Integer) ed.putInt(key, (Integer) v);
                    else if (v instanceof Long) ed.putLong(key, (Long) v);
                    else if (v instanceof Double) ed.putInt(key, (int) ((Double) v).doubleValue());
                    else if (v != null) ed.putString(key, String.valueOf(v));
                }
                ed.apply();
            }
            JSONObject store = root.optJSONObject("store");
            if (store != null) {
                new HubStore(ctx).importJson(store.toString());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Сохраняет бэкап в файл и возвращает полный путь (или null). */
    public static String writeToFile(Context ctx, String json) {
        try {
            File dir = ctx.getExternalFilesDir(null);
            if (dir == null) dir = ctx.getFilesDir();
            if (!dir.exists() && !dir.mkdirs()) return null;
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            File out = new File(dir, "hyperhub_backup_" + stamp + ".json");
            FileOutputStream fos = new FileOutputStream(out);
            OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");
            w.write(json);
            w.flush();
            w.close();
            fos.close();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
