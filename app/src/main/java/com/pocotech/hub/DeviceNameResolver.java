package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DeviceNameResolver {

    private static final String CSV_URL =
        "https://raw.githubusercontent.com/KHwang9883/MobileModels-csv/main/models.csv";

    private static final String PREFS = "device_name_cache";
    private static final String KEY_LAST_UPDATE = "last_update_ms";
    private static final String CACHE_FILE = "xiaomi_models.csv";

    private static final long CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private static final Map<String, String> byModel = new LinkedHashMap<>();
    private static final Map<String, String> byCode = new LinkedHashMap<>();

    private static volatile boolean loaded = false;

    private DeviceNameResolver() {}

    
    public static void init(final Context context) {
        final Context appCtx = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                loadFromDiskCache(appCtx);
                loaded = true;

                SharedPreferences prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
                boolean stale = System.currentTimeMillis() - lastUpdate > CACHE_MAX_AGE_MS;

                if (stale) {
                    refreshFromNetwork(appCtx);
                }
            }
        }, "DeviceNameResolver-init").start();
    }

    
    public static String resolve(String model, String device) {
        String m = model == null ? "" : model.trim();
        String d = device == null ? "" : device.trim();

        String hit = byModel.get(m);
        if (hit != null) return hit;

        hit = byCode.get(d.toLowerCase(Locale.ROOT));
        if (hit != null) return hit;

        for (Map.Entry<String, String> e : byModel.entrySet()) {
            if (m.contains(e.getKey()) || e.getKey().contains(m)) return e.getValue();
        }

        String emergency = EMERGENCY_FALLBACK.get(d.toLowerCase(Locale.ROOT));
        if (emergency != null) return emergency;

        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        if (!manufacturer.isEmpty() && !m.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
            return capitalize(manufacturer) + " " + m;
        }
        return capitalize(m);
    }

    
    public static void forceRefresh(Context context) {
        refreshFromNetwork(context.getApplicationContext());
    }

    // ────────────────────────────────────────────────────────────────────

    private static void refreshFromNetwork(Context appCtx) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(CSV_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "HyperHub");

            if (conn.getResponseCode() != 200) return;

            File cacheFile = new File(appCtx.getCacheDir(), CACHE_FILE);
            File tmpFile = new File(appCtx.getCacheDir(), CACHE_FILE + ".tmp");

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(tmpFile);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            out.close();

            if (cacheFile.exists()) cacheFile.delete();
            tmpFile.renameTo(cacheFile);

            parseCsv(cacheFile);

            appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply();

        } catch (Exception e) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void loadFromDiskCache(Context appCtx) {
        File cacheFile = new File(appCtx.getCacheDir(), CACHE_FILE);
        if (cacheFile.exists()) {
            parseCsv(cacheFile);
        }
    }

    
    private static void parseCsv(File file) {
        Map<String, String> newByModel = new LinkedHashMap<>();
        Map<String, String> newByCode = new LinkedHashMap<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream(file), "UTF-8"));
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // пропускаем заголовок
                if (line.isEmpty()) continue;

                String[] cols = splitCsvLine(line);
                if (cols.length < 7) continue;

                String model = cols[0].trim();
                String deviceType = cols[1].trim();
                String brand = cols[2].trim();
                String code = cols[4].trim();
                String modelName = cols[6].trim();

                if (!"mob".equals(deviceType)) continue;
                if (!"xiaomi".equalsIgnoreCase(brand)) continue;
                if (modelName.isEmpty()) continue;

                if (!model.isEmpty()) newByModel.put(model, modelName);
                if (!code.isEmpty()) newByCode.put(code.toLowerCase(Locale.ROOT), modelName);
            }
            br.close();

            synchronized (DeviceNameResolver.class) {
                byModel.clear();
                byModel.putAll(newByModel);
                byCode.clear();
                byCode.putAll(newByCode);
            }
        } catch (Exception e) {
        }
    }

    
    private static String[] splitCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result.toArray(new String[0]);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    
    private static final Map<String, String> EMERGENCY_FALLBACK = new LinkedHashMap<>();
    static {
        EMERGENCY_FALLBACK.put("moon",    "POCO M6 4G");
        EMERGENCY_FALLBACK.put("gust",    "POCO C65");
        EMERGENCY_FALLBACK.put("emerald", "POCO M6 Pro");
    }
}
