package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Локальное хранилище v2.1.0: избранное, недавние, история бенчмарков
 * и сторонние модификации (.hhopt). Формат хранения — JSON в SharedPreferences,
 * полностью офлайн. Геймификация (XP, уровни, серии, достижения) и профили
 * удалены начиная с 2.1.0.
 */
public class HubStore {

    private static final String PREFS = "hub_store_v2";

    private static final String K_FAV      = "favorites";
    private static final String K_REC      = "recents";
    private static final String K_BENCH    = "bench_history";
    private static final String K_USES     = "uses_total";
    private static final String K_MODS     = "hhopt_mods";

    private final SharedPreferences sp;

    public HubStore(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── Избранное ────────────────────────────────────────────────────────────

    public List<String> favorites() { return readList(K_FAV); }

    public boolean isFavorite(String key) { return key != null && favorites().contains(key); }

    public boolean toggleFavorite(String key) {
        if (key == null) return false;
        List<String> fav = favorites();
        boolean added;
        if (fav.contains(key)) {
            fav.remove(key);
            added = false;
        } else {
            fav.add(0, key);
            added = true;
        }
        while (fav.size() > 12) fav.remove(fav.size() - 1);
        writeList(K_FAV, fav);
        return added;
    }

    public int favoritesCount() { return favorites().size(); }

    // ── Недавние ─────────────────────────────────────────────────────────────

    public List<String> recents() { return readList(K_REC); }

    public void pushRecent(String key) {
        if (key == null) return;
        List<String> rec = recents();
        rec.remove(key);
        rec.add(0, key);
        while (rec.size() > 8) rec.remove(rec.size() - 1);
        writeList(K_REC, rec);
        sp.edit().putInt(K_USES, sp.getInt(K_USES, 0) + 1).apply();
    }

    public int totalUses() { return sp.getInt(K_USES, 0); }

    // ── История бенчмарков ───────────────────────────────────────────────────

    public static class BenchRecord {
        public long time = 0L;
        public int total, cpuSingle, cpuMulti, ram, storage, gpu;
        public String rating = "";

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("t", time);
                o.put("total", total);
                o.put("cpuS", cpuSingle);
                o.put("cpuM", cpuMulti);
                o.put("ram", ram);
                o.put("sto", storage);
                o.put("gpu", gpu);
                o.put("rating", rating);
            } catch (Exception ignored) {}
            return o;
        }

        public static BenchRecord fromJson(JSONObject o) {
            BenchRecord r = new BenchRecord();
            r.time      = o.optLong("t", 0L);
            r.total     = o.optInt("total", 0);
            r.cpuSingle = o.optInt("cpuS", 0);
            r.cpuMulti  = o.optInt("cpuM", 0);
            r.ram       = o.optInt("ram", 0);
            r.storage   = o.optInt("sto", 0);
            r.gpu       = o.optInt("gpu", 0);
            r.rating    = o.optString("rating", "");
            return r;
        }
    }

    public List<BenchRecord> benchHistory() {
        List<BenchRecord> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(K_BENCH, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(BenchRecord.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public void addBenchmark(BenchRecord record) {
        if (record == null) return;
        List<BenchRecord> hist = benchHistory();
        hist.add(0, record);
        while (hist.size() > 20) hist.remove(hist.size() - 1);
        JSONArray arr = new JSONArray();
        for (BenchRecord r : hist) arr.put(r.toJson());
        sp.edit().putString(K_BENCH, arr.toString()).apply();
    }

    public BenchRecord benchBest() {
        BenchRecord best = null;
        for (BenchRecord r : benchHistory()) {
            if (best == null || r.total > best.total) best = r;
        }
        return best;
    }

    public BenchRecord benchPrevious() {
        List<BenchRecord> hist = benchHistory();
        return hist.size() >= 2 ? hist.get(1) : null;
    }

    public void clearBenchHistory() { sp.edit().putString(K_BENCH, "[]").apply(); }

    // ── Модификации .hhopt ───────────────────────────────────────────────────

    public static class Mod {
        public String id = "";
        public String name = "";
        public String version = "";
        public String author = "";
        public String description = "";
        /** Момент установки (мс). */
        public long installedAt = 0L;
        /** Исходный JSON-манифест — нужен, чтобы применять мод без перечитывания файла. */
        public String manifestJson = "{}";
        /** Включён ли мод. */
        public boolean enabled = true;

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("version", version);
                o.put("author", author);
                o.put("description", description);
                o.put("installedAt", installedAt);
                o.put("manifest", manifestJson);
                o.put("enabled", enabled);
            } catch (Exception ignored) {}
            return o;
        }

        public static Mod fromJson(JSONObject o) {
            Mod m = new Mod();
            m.id           = o.optString("id", "");
            m.name         = o.optString("name", "");
            m.version      = o.optString("version", "1.0");
            m.author       = o.optString("author", "");
            m.description  = o.optString("description", "");
            m.installedAt  = o.optLong("installedAt", 0L);
            m.manifestJson = o.optString("manifest", "{}");
            m.enabled      = o.optBoolean("enabled", true);
            return m;
        }
    }

    public List<Mod> mods() {
        List<Mod> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(K_MODS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(Mod.fromJson(o));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public Mod modById(String id) {
        if (id == null) return null;
        for (Mod m : mods()) if (id.equals(m.id)) return m;
        return null;
    }

    public boolean saveMod(Mod mod) {
        if (mod == null || mod.id == null || mod.id.trim().isEmpty()) return false;
        List<Mod> list = mods();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(mod.id)) {
                list.set(i, mod);
                persistMods(list);
                return true;
            }
        }
        list.add(0, mod);
        while (list.size() > 20) list.remove(list.size() - 1);
        persistMods(list);
        return true;
    }

    public boolean removeMod(String id) {
        List<Mod> list = mods();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) { list.remove(i); persistMods(list); return true; }
        }
        return false;
    }

    public boolean setModEnabled(String id, boolean enabled) {
        Mod m = modById(id);
        if (m == null) return false;
        m.enabled = enabled;
        return saveMod(m);
    }

    private void persistMods(List<Mod> list) {
        JSONArray arr = new JSONArray();
        for (Mod m : list) arr.put(m.toJson());
        sp.edit().putString(K_MODS, arr.toString()).apply();
    }

    // ── Сброс ────────────────────────────────────────────────────────────────

    public void resetStats() {
        sp.edit()
                .putInt(K_USES, 0)
                .putString(K_BENCH, "[]")
                .apply();
    }

    // ── Экспорт / импорт ─────────────────────────────────────────────────────

    public String exportJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("favorites", new JSONArray(favorites()));
            o.put("recents", new JSONArray(recents()));
            o.put("uses_total", sp.getInt(K_USES, 0));
            o.put("bench_history", new JSONArray(sp.getString(K_BENCH, "[]")));
            JSONArray modsArr = new JSONArray();
            for (Mod m : mods()) modsArr.put(m.toJson());
            o.put("hhopt_mods", modsArr);
        } catch (Exception ignored) {}
        return o.toString();
    }

    public boolean importJson(String json) {
        if (json == null || json.trim().isEmpty()) return false;
        try {
            JSONObject o = new JSONObject(json);
            writeList(K_FAV, jsonList(o.optJSONArray("favorites")));
            writeList(K_REC, jsonList(o.optJSONArray("recents")));
            sp.edit()
                    .putInt(K_USES, o.optInt("uses_total", 0))
                    .putString(K_BENCH, o.optJSONArray("bench_history") != null
                            ? o.optJSONArray("bench_history").toString() : "[]")
                    .apply();
            JSONArray modsArr = o.optJSONArray("hhopt_mods");
            JSONArray arr = new JSONArray();
            if (modsArr != null) {
                for (int i = 0; i < modsArr.length(); i++) {
                    JSONObject po = modsArr.optJSONObject(i);
                    if (po != null) arr.put(po);
                }
            }
            sp.edit().putString(K_MODS, arr.toString()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Внутреннее ───────────────────────────────────────────────────────────

    private static List<String> jsonList(JSONArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            String v = arr.optString(i, "");
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    private List<String> readList(String key) {
        Set<String> set = sp.getStringSet(key, null);
        List<String> out = new ArrayList<>();
        if (set != null) out.addAll(set);
        return out;
    }

    private void writeList(String key, List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>(values);
        sp.edit().putStringSet(key, set).apply();
    }
}
