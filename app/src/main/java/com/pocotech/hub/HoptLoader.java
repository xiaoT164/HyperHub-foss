package com.pocotech.hub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Загрузчик сторонних модификаций HyperHub (формат .hhopt) — v2.1.0.
 *
 * Формат файла: ZIP-архив с манифестом manifest.json в корне
 * (совместимо: файл-архив, переименованный в .hhopt). Манифест:
 *
 * {
 *   "hhopt": 1,                     — версия формата (обязательно)
 *   "id": "com.example.mymod",      — уникальный id (обязательно)
 *   "name": "My Mod",               — название (обязательно)
 *   "version": "1.0",               — версия мода
 *   "minAppVersion": "2.1.0",       — минимальная версия HyperHub
 *   "author": "…", "description": "…",
 *   "cards": [ {"id": "card_x", "title": "…", "subtitle": "…", "feature": "fps"} ]
 * }
 *
 * Безопасность: мод не исполняет код. Он может только добавлять карточки,
 * ссылающиеся на функции из белого списка Features, — выполнение идёт через
 * стандартный HubHost.runFeature(). Никакого reflection, ни exec, ни сети.
 */
public final class HoptLoader {

    public static final int FORMAT_VERSION = 1;

    public static final class HoptException extends Exception {
        public HoptException(String message) { super(message); }
    }

    public static final class Card {
        public String id;
        public String title;
        public String subtitle;
        /** Ключ функции Features, которая запускается по тапу. */
        public String feature;
    }

    private HoptLoader() {}

    /** Признак «пик файлов .hhopt активирован» (Android 4.4+). */
    public static boolean hasPicker() {
        return android.os.Build.VERSION.SDK_INT >= 19;
    }

    /** Запускает системный выбор файла. Результат приходит в onActivityResult. */
    public static void launchPicker(Activity act, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/octet-stream", "application/x-zip-compressed"});
        act.startActivityForResult(intent, requestCode);
    }

    /** Читает InputStream выбранного файла и устанавливает мод. Возвращает установленный Mod. */
    public static HubStore.Mod installFromStream(Context ctx, HubStore store, InputStream in)
            throws HoptException {
        String json = readManifest(in);
        if (json == null || json.trim().isEmpty()) {
            throw new HoptException("manifest.json не найден внутри файла");
        }
        return installFromJson(ctx, store, json);
    }

    public static HubStore.Mod installFromJson(Context ctx, HubStore store, String json)
            throws HoptException {
        JSONObject o;
        try {
            o = new JSONObject(json);
        } catch (Exception e) {
            throw new HoptException("Некорректный JSON: " + e.getMessage());
        }

        int fmt = o.optInt("hhopt", -1);
        if (fmt != FORMAT_VERSION) {
            throw new HoptException("Неподдерживаемая версия формата: " + fmt
                    + " (ожидалась " + FORMAT_VERSION + ")");
        }

        String id = o.optString("id", "").trim();
        String name = o.optString("name", "").trim();
        if (id.isEmpty() || name.isEmpty()) {
            throw new HoptException("В манифесте обязательны поля \"id\" и \"name\"");
        }

        String minApp = o.optString("minAppVersion", "").trim();
        if (!minApp.isEmpty()) {
            // Устанавливаем мод, только если версия приложения не старше требуемой.
            if (compareVersions(BuildConfig.VERSION_NAME, minApp) < 0) {
                throw new HoptException("Мод требует HyperHub " + minApp
                        + "+, установлена " + BuildConfig.VERSION_NAME);
            }
        }

        HubStore.Mod mod = new HubStore.Mod();
        mod.id = id;
        mod.name = name;
        mod.version = o.optString("version", "1.0").trim();
        mod.author = o.optString("author", "").trim();
        mod.description = o.optString("description", "").trim();
        mod.installedAt = System.currentTimeMillis();
        mod.enabled = true;
        mod.manifestJson = o.toString();

        store.saveMod(mod);
        return mod;
    }

    /** Карточки мода, готовые к показу на главном экране. Пустой список — мод не добавляет карточек. */
    public static List<Card> cardsOf(HubStore.Mod mod) {
        List<Card> out = new ArrayList<>();
        if (mod == null) return out;
        try {
            JSONObject o = new JSONObject(mod.manifestJson);
            JSONArray cards = o.optJSONArray("cards");
            if (cards == null) return out;
            for (int i = 0; i < cards.length(); i++) {
                JSONObject c = cards.optJSONObject(i);
                if (c == null) continue;
                Card card = new Card();
                card.id = c.optString("id", "card_" + i);
                card.title = c.optString("title", "");
                card.subtitle = c.optString("subtitle", "");
                card.feature = c.optString("feature", "");
                // Белый список: если функция неизвестна, карточка не показывается.
                if (Features.byKey(card.feature) != null && !card.title.isEmpty()) {
                    out.add(card);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static String readManifest(InputStream in) {
        BufferedReader br = null;
        try {
            // Мод может быть как голым JSON, так и ZIP-архивом с manifest.json.
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            byte[] bytes = buf.toByteArray();

            if (bytes.length >= 4 && (bytes[0] == 'P') && bytes[1] == 'K') {
                java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                        new java.io.ByteArrayInputStream(bytes));
                java.util.zip.ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    String entryName = e.getName();
                    String base = entryName.substring(entryName.lastIndexOf('/') + 1);
                    if (!e.isDirectory() && ("manifest.json".equals(base) || "manifest.hhopt".equals(base))) {
                        br = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));
                        return readStream(br);
                    }
                }
                return null;
            }
            // Не ZIP — считаем содержимое манифестом напрямую.
            br = new BufferedReader(new InputStreamReader(
                    new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
            return readStream(br);
        } catch (Exception e) {
            return null;
        } finally {
            if (br != null) try { br.close(); } catch (Exception ignored) {}
            try { in.close(); } catch (Exception ignored) {}
        }
    }

    private static String readStream(BufferedReader br) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static int compareVersions(String a, String b) {
        int[] pa = parse(a), pb = parse(b);
        for (int i = 0; i < 3; i++) {
            if (pa[i] != pb[i]) return pa[i] < pb[i] ? -1 : 1;
        }
        return 0;
    }

    private static int[] parse(String v) {
        int[] out = new int[3];
        try {
            String[] parts = v.replaceAll("[^0-9.]", "").split("\\.");
            for (int i = 0; i < 3 && i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        } catch (Exception ignored) {}
        return out;
    }
}
