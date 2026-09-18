package com.pocotech.hub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Локальное хранилище v2.0.0: избранное, недавние, XP/уровень, серия дней,
 * история бенчмарков и пользовательские профили.
 * Формат хранения — JSON в SharedPreferences, полностью офлайн.
 */
public class HubStore {

    private static final String PREFS = "hub_store_v2";

    private static final String K_FAV        = "favorites";
    private static final String K_REC        = "recents";
    private static final String K_XP         = "xp";
    private static final String K_STREAK     = "streak";
    private static final String K_STREAK_BEST= "streak_best";
    private static final String K_LAST_DAY   = "last_day";
    private static final String K_TOTAL_DAYS = "total_days";
    private static final String K_DAYS       = "days";
    private static final String K_BENCH      = "bench_history";
    private static final String K_PROFILES   = "custom_profiles";
    private static final String K_USES       = "uses_total";
    private static final String K_PROF_USE   = "profile_uses";

    public static final int XP_VISIT     = 10;
    public static final int XP_ACTION    = 15;
    public static final int XP_FAVORITE  = 5;
    public static final int XP_BENCHMARK = 40;
    public static final int XP_PROFILE   = 25;

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

    // ── XP и уровень ─────────────────────────────────────────────────────────

    public int getXp() { return sp.getInt(K_XP, 0); }

    public void addXp(int amount) {
        if (amount == 0) return;
        sp.edit().putInt(K_XP, Math.max(0, getXp() + amount)).apply();
    }

    public int getLevel() { return getXp() / 100 + 1; }

    public int getLevelProgress() { return getXp() % 100; }

    public String getLevelTitle(boolean en) {
        int lvl = getLevel();
        if (en) {
            if (lvl >= 20) return "HyperOS Legend";
            if (lvl >= 15) return "Kernel Engineer";
            if (lvl >= 10) return "Tweaker Pro";
            if (lvl >= 6)  return "Advanced user";
            if (lvl >= 3)  return "Enthusiast";
            return "Beginner";
        }
        if (lvl >= 20) return "Легенда HyperOS";
        if (lvl >= 15) return "Инженер ядра";
        if (lvl >= 10) return "Твикер Pro";
        if (lvl >= 6)  return "Продвинутый";
        if (lvl >= 3)  return "Энтузиаст";
        return "Новичок";
    }

    // ── Серия дней ───────────────────────────────────────────────────────────

    /** Отмечает визит за сегодня, обновляет серию. Возвращает true, если это первый визит за день. */
    public boolean touchDaily() {
        String today = dayKey(0);
        String last = sp.getString(K_LAST_DAY, "");
        if (today.equals(last)) return false;

        int streak;
        if (dayKey(-1).equals(last)) {
            streak = sp.getInt(K_STREAK, 0) + 1;
        } else {
            streak = 1;
        }
        int best = Math.max(streak, sp.getInt(K_STREAK_BEST, 0));

        List<String> days = readList(K_DAYS);
        if (!days.contains(today)) days.add(today);
        while (days.size() > 60) days.remove(0);

        sp.edit()
                .putString(K_LAST_DAY, today)
                .putInt(K_STREAK, streak)
                .putInt(K_STREAK_BEST, best)
                .putInt(K_TOTAL_DAYS, sp.getInt(K_TOTAL_DAYS, 0) + 1)
                .apply();
        writeList(K_DAYS, days);
        addXp(XP_VISIT);
        return true;
    }

    public int streak() {
        String last = sp.getString(K_LAST_DAY, "");
        if (dayKey(0).equals(last) || dayKey(-1).equals(last)) return sp.getInt(K_STREAK, 0);
        return 0;
    }

    public int bestStreak() { return sp.getInt(K_STREAK_BEST, 0); }

    public int totalDays() { return sp.getInt(K_TOTAL_DAYS, 0); }

    public boolean isTodayActive() { return dayKey(0).equals(sp.getString(K_LAST_DAY, "")); }

    public List<String> activeDays() { return readList(K_DAYS); }

    public static String dayKey(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // ── Использование профилей ───────────────────────────────────────────────

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

    // ── Пользовательские профили ─────────────────────────────────────────────

    public static class Profile {
        public String name = "";
        public List<String> keys = new ArrayList<>();

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("name", name);
                JSONArray a = new JSONArray();
                for (String k : keys) a.put(k);
                o.put("keys", a);
            } catch (Exception ignored) {}
            return o;
        }

        public static Profile fromJson(JSONObject o) {
            Profile p = new Profile();
            p.name = o.optString("name", "");
            JSONArray a = o.optJSONArray("keys");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) p.keys.add(a.optString(i));
            }
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
        for (Profile p : list) {
            if (p.name.equalsIgnoreCase(name.trim())) {
                p.keys = new ArrayList<>(keys);
                persistProfiles(list);
                return true;
            }
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
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(name)) { list.remove(i); break; }
        }
        persistProfiles(list);
    }

    private void persistProfiles(List<Profile> list) {
        JSONArray arr = new JSONArray();
        for (Profile p : list) arr.put(p.toJson());
        sp.edit().putString(K_PROFILES, arr.toString()).apply();
    }

    // ── Достижения ───────────────────────────────────────────────────────────

    public static class Achievement {
        public String id, title, desc;
        public boolean done;
        public int progress;
    }

    public List<Achievement> achievements(boolean en) {
        List<Achievement> out = new ArrayList<>();
        int bench = benchHistory().size();
        int bestStreak = bestStreak();
        int fav = favoritesCount();
        int uses = totalUses();
        int profUses = profileUses();

        out.add(a("first", en ? "First step" : "Первый шаг",
                en ? "Open any tool from the Home screen" : "Откройте любой инструмент на главном экране",
                uses >= 1, Math.min(100, uses * 20)));
        out.add(a("fav3", en ? "Favorites" : "Избранное",
                en ? "Pin 3 tools to favorites" : "Закрепите 3 инструмента в избранном",
                fav >= 3, (int) (fav * 100f / 3f)));
        out.add(a("fav8", en ? "Curator" : "Куратор",
                en ? "Pin 8 tools to favorites" : "Закрепите 8 инструментов в избранном",
                fav >= 8, (int) (fav * 100f / 8f)));
        out.add(a("bench1", en ? "Measured" : "Измерено",
                en ? "Run the HubBench benchmark once" : "Запустите бенчмарк HubBench один раз",
                bench >= 1, bench >= 1 ? 100 : 0));
        out.add(a("bench3", en ? "Dynamics" : "Динамика",
                en ? "Save 3 benchmark runs to history" : "Сохраните 3 прогона бенчмарка в историю",
                bench >= 3, (int) (bench * 100f / 3f)));
        out.add(a("streak3", en ? "Three in a row" : "Три дня подряд",
                en ? "Open HyperHub 3 days in a row" : "Заходите в HyperHub 3 дня подряд",
                bestStreak >= 3, (int) (bestStreak * 100f / 3f)));
        out.add(a("streak7", en ? "Week of tweaks" : "Неделя твиков",
                en ? "Open HyperHub 7 days in a row" : "Заходите в HyperHub 7 дней подряд",
                bestStreak >= 7, (int) (bestStreak * 100f / 7f)));
        out.add(a("streak14", en ? "Month marathon" : "Марафон",
                en ? "Keep a 14-day streak" : "Удержите серию 14 дней",
                bestStreak >= 14, (int) (bestStreak * 100f / 14f)));
        out.add(a("prof1", en ? "Profiler" : "Профилировщик",
                en ? "Apply a preset optimization profile" : "Примените готовый профиль оптимизации",
                profUses >= 1, Math.min(100, profUses * 100)));
        out.add(a("prof5", en ? "Scenario master" : "Мастер сценариев",
                en ? "Apply profiles 5 times" : "Примените профили 5 раз",
                profUses >= 5, (int) (profUses * 100f / 5f)));
        out.add(a("lvl5", en ? "Level 5" : "Уровень 5",
                en ? "Reach level 5" : "Достигните 5 уровня",
                getLevel() >= 5, (int) (getLevel() * 100f / 5f)));
        out.add(a("lvl10", en ? "Level 10" : "Уровень 10",
                en ? "Reach level 10" : "Достигните 10 уровня",
                getLevel() >= 10, (int) (getLevel() * 100f / 10f)));
        return out;
    }

    private static Achievement a(String id, String title, String desc, boolean done, int progress) {
        Achievement ac = new Achievement();
        ac.id = id;
        ac.title = title;
        ac.desc = desc;
        ac.done = done;
        ac.progress = Math.max(0, Math.min(100, progress));
        return ac;
    }

    public int achievementsDone(boolean en) {
        int n = 0;
        for (Achievement a : achievements(en)) if (a.done) n++;
        return n;
    }

    // ── Сброс ────────────────────────────────────────────────────────────────

    public void resetStats() {
        sp.edit()
                .putInt(K_XP, 0)
                .putInt(K_STREAK, 0)
                .putInt(K_STREAK_BEST, 0)
                .putInt(K_TOTAL_DAYS, 0)
                .putInt(K_USES, 0)
                .putInt(K_PROF_USE, 0)
                .putString(K_LAST_DAY, "")
                .apply();
        writeList(K_DAYS, new ArrayList<String>());
    }

    // ── Экспорт / импорт ─────────────────────────────────────────────────────

    public String exportJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("favorites", new JSONArray(favorites()));
            o.put("recents", new JSONArray(recents()));
            o.put("xp", getXp());
            o.put("streak", sp.getInt(K_STREAK, 0));
            o.put("streak_best", sp.getInt(K_STREAK_BEST, 0));
            o.put("total_days", sp.getInt(K_TOTAL_DAYS, 0));
            o.put("last_day", sp.getString(K_LAST_DAY, ""));
            o.put("uses_total", sp.getInt(K_USES, 0));
            o.put("profile_uses", sp.getInt(K_PROF_USE, 0));
            o.put("days", new JSONArray(readList(K_DAYS)));
            o.put("bench_history", new JSONArray(sp.getString(K_BENCH, "[]")));
            JSONArray profs = new JSONArray();
            for (Profile p : profiles()) profs.put(p.toJson());
            o.put("custom_profiles", profs);
        } catch (Exception ignored) {}
        return o.toString();
    }

    public boolean importJson(String json) {
        if (json == null || json.trim().isEmpty()) return false;
        try {
            JSONObject o = new JSONObject(json);
            writeList(K_FAV, jsonList(o.optJSONArray("favorites")));
            writeList(K_REC, jsonList(o.optJSONArray("recents")));
            writeList(K_DAYS, jsonList(o.optJSONArray("days")));
            sp.edit()
                    .putInt(K_XP, o.optInt("xp", 0))
                    .putInt(K_STREAK, o.optInt("streak", 0))
                    .putInt(K_STREAK_BEST, o.optInt("streak_best", 0))
                    .putInt(K_TOTAL_DAYS, o.optInt("total_days", 0))
                    .putInt(K_USES, o.optInt("uses_total", 0))
                    .putInt(K_PROF_USE, o.optInt("profile_uses", 0))
                    .putString(K_LAST_DAY, o.optString("last_day", ""))
                    .putString(K_BENCH, o.optJSONArray("bench_history") != null
                            ? o.optJSONArray("bench_history").toString() : "[]")
                    .apply();
            JSONArray profs = o.optJSONArray("custom_profiles");
            JSONArray arr = new JSONArray();
            if (profs != null) {
                for (int i = 0; i < profs.length(); i++) {
                    JSONObject po = profs.optJSONObject(i);
                    if (po != null) arr.put(po);
                }
            }
            sp.edit().putString(K_PROFILES, arr.toString()).apply();
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
