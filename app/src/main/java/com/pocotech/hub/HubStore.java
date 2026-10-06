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
 * Хранилище 2.1.0 — без геймификации.
 * Содержит только полезные данные: избранное, недавние,
 * историю бенчмарка, профили и журнал применённых оптимизаций.
 */
public class HubStore {

    private static final String PREFS = "hub_store_v2";

    private static final String K_FAV      = "favorites";
    private static final String K_REC      = "recents";
    private static final String K_BENCH    = "bench_history";
    private static final String K_PROFILES = "custom_profiles";
    private static final String K_USES     = "uses_total";
    private static final String K_PROF_USE = "profile_uses";
    private static final String K_ACTIONS  = "actions_log";

    /** Ключ глубокой оптимизации LMK из исходного ТЗ — отдельный инструмент на главном экране. */
    public static final String LMK_FULL_KEY = "lmk_full";

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
        if (fav.contains(key)) { fav.remove(key); added = false; }
        else { fav.add(0, key); added = true; }
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

    // ── Уровень пользователя (каждые 10 действий) ────────────────────────────
    public int getLevel() { return totalUses() / 10 + 1; }

    // ── Профили ──────────────────────────────────────────────────────────────
    public void countProfileUse() { sp.edit().putInt(K_PROF_USE, sp.getInt(K_PROF_USE, 0) + 1).apply(); }
    public int profileUses() { return sp.getInt(K_PROF_USE, 0); }

    // ── История бенчмарков ───────────────────────────────────────────────────
    public static class BenchRecord {
        public long time = 0L;
        public int total, cpuSingle, cpuMulti, ram, storage, gpu;
        public String rating = "";
        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("t", time); o.put("total", total); o.put("cpuS", cpuSingle);
                o.put("cpuM", cpuMulti); o.put("ram", ram); o.put("sto", storage);
                o.put("gpu", gpu); o.put("rating", rating);
            } catch (Exception ignored) {}
            return o;
        }
        public static BenchRecord fromJson(JSONObject o) {
            BenchRecord r = new BenchRecord();
            r.time = o.optLong("t", 0L); r.total = o.optInt("total", 0);
            r.cpuSingle = o.optInt("cpuS", 0); r.cpuMulti = o.optInt("cpuM", 0);
            r.ram = o.optInt("ram", 0); r.storage = o.optInt("sto", 0);
            r.gpu = o.optInt("gpu", 0); r.rating = o.optString("rating", "");
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
        for (BenchRecord r : benchHistory()) if (best == null || r.total > best.total) best = r;
        return best;
    }
    public void clearBenchHistory() { sp.edit().putString(K_BENCH, "[]").apply(); }

    // ── Пользовательские профили ─────────────────────────────────────────────
    public static class Profile {
        public String name = "";
        public List<String> keys = new ArrayList<>();
        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try { o.put("name", name); JSONArray a = new JSONArray(); for (String k : keys) a.put(k); o.put("keys", a); }
            catch (Exception ignored) {}
            return o;
        }
        public static Profile fromJson(JSONObject o) {
            Profile p = new Profile();
            p.name = o.optString("name", "");
            JSONArray a = o.optJSONArray("keys");
            if (a != null) for (int i = 0; i < a.length(); i++) p.keys.add(a.optString(i));
            return p;
        }
    }
    public List<Profile> profiles() {
        List<Profile> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(K_PROFILES, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Profile.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }
    public boolean saveProfile(String name, List<String> keys) {
        if (name == null || name.trim().isEmpty() || keys == null || keys.isEmpty()) return false;
        List<Profile> list = profiles();
        for (Profile p : list) if (p.name.equalsIgnoreCase(name.trim())) {
            p.keys = new ArrayList<>(keys); persistProfiles(list); return true;
        }
        Profile p = new Profile();
        p.name = name.trim();
        p.keys = new ArrayList<>(keys);
        list.add(0, p);
        while (list.size() > 10) list.remove(list.size() - 1);
        persistProfiles(list);
        return true;
    }
    public void removeProfile(String name) {
        List<Profile> list = profiles();
        for (int i = 0; i < list.size(); i++) if (list.get(i).name.equals(name)) { list.remove(i); break; }
        persistProfiles(list);
    }
    private void persistProfiles(List<Profile> list) {
        JSONArray arr = new JSONArray();
        for (Profile p : list) arr.put(p.toJson());
        sp.edit().putString(K_PROFILES, arr.toString()).apply();
    }

    // ── Журнал оптимизаций (вместо XP / серий / достижений) ────────────────
    public static class ActionEntry {
        public long time;
        public String key = "";
        public String title = "";
        public static ActionEntry create(String key, String title) {
            ActionEntry e = new ActionEntry();
            e.time = System.currentTimeMillis();
            e.key = key;
            e.title = title;
            return e;
        }
        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try { o.put("t", time); o.put("k", key); o.put("name", title); } catch (Exception ignored) {}
            return o;
        }
        public static ActionEntry fromJson(JSONObject o) {
            ActionEntry e = new ActionEntry();
            e.time = o.optLong("t", 0L);
            e.key = o.optString("k", "");
            e.title = o.optString("name", "");
            return e;
        }
    }
    public void logAction(String key, String title) {
        ActionEntry e = ActionEntry.create(key, title);
        List<ActionEntry> log = readActions();
        log.add(0, e);
        while (log.size() > 200) log.remove(log.size() - 1);
        JSONArray arr = new JSONArray();
        for (ActionEntry x : log) arr.put(x.toJson());
        sp.edit().putString(K_ACTIONS, arr.toString()).apply();
    }
    public List<ActionEntry> actions() { return readActions(); }
    public void clearActions() { sp.edit().putString(K_ACTIONS, "[]").apply(); }
    private List<ActionEntry> readActions() {
        List<ActionEntry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(K_ACTIONS, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(ActionEntry.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public void resetAll() { sp.edit().clear().apply(); }

    // ── Экспорт / импорт ─────────────────────────────────────────────────────
    public String exportJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("favorites", new JSONArray(favorites()));
            o.put("recents", new JSONArray(recents()));
            o.put("uses_total", sp.getInt(K_USES, 0));
            o.put("profile_uses", sp.getInt(K_PROF_USE, 0));
            o.put("bench_history", new JSONArray(sp.getString(K_BENCH, "[]")));
            JSONArray profs = new JSONArray();
            for (Profile p : profiles()) profs.put(p.toJson());
            o.put("custom_profiles", profs);
            JSONArray acts = new JSONArray();
            for (ActionEntry a : actions()) acts.put(a.toJson());
            o.put("actions_log", acts);
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
                    .putInt(K_PROF_USE, o.optInt("profile_uses", 0))
                    .putString(K_BENCH, o.optJSONArray("bench_history") != null
                            ? o.optJSONArray("bench_history").toString() : "[]")
                    .apply();
            JSONArray profs = o.optJSONArray("custom_profiles");
            JSONArray arr = new JSONArray();
            if (profs != null) for (int i = 0; i < profs.length(); i++) {
                JSONObject po = profs.optJSONObject(i); if (po != null) arr.put(po);
            }
            sp.edit().putString(K_PROFILES, arr.toString()).apply();

            JSONArray actsArr = o.optJSONArray("actions_log");
            JSONArray actsJson = new JSONArray();
            if (actsArr != null) for (int i = 0; i < actsArr.length(); i++) actsJson.put(actsArr.optJSONObject(i));
            sp.edit().putString(K_ACTIONS, actsJson.toString()).apply();
            return true;
        } catch (Exception e) { return false; }
    }

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
