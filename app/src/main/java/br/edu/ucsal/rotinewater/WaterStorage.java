package br.edu.ucsal.rotinewater;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WaterStorage {

    private static final String PREFS_NAME = "water_prefs";
    private static final String KEY_GOAL = "goal";
    private static final String KEY_DATE = "date";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_VOLUME = "volumeMl";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final int DEFAULT_GOAL = 2000;

    private final SharedPreferences prefs;

    public WaterStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkNewDay();
    }

    public int getGoal() {
        return prefs.getInt(KEY_GOAL, DEFAULT_GOAL);
    }

    public void setGoal(int goalMl) {
        prefs.edit().putInt(KEY_GOAL, goalMl).apply();
    }

    public int getTotal() {
        return prefs.getInt(KEY_TOTAL, 0);
    }

    public List<WaterEntry> getEntries() {
        List<WaterEntry> entries = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_ENTRIES, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                entries.add(new WaterEntry(item.getInt(KEY_VOLUME), item.getLong(KEY_TIMESTAMP)));
            }
        } catch (JSONException e) {
            entries.clear();
        }
        return entries;
    }

    public void addEntry(int volumeMl) {
        checkNewDay();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_ENTRIES, "[]"));
            JSONObject item = new JSONObject();
            item.put(KEY_VOLUME, volumeMl);
            item.put(KEY_TIMESTAMP, System.currentTimeMillis());
            array.put(item);
            prefs.edit()
                    .putString(KEY_ENTRIES, array.toString())
                    .putInt(KEY_TOTAL, getTotal() + volumeMl)
                    .apply();
        } catch (JSONException e) {
            resetDay();
        }
    }

    public void checkNewDay() {
        if (!today().equals(prefs.getString(KEY_DATE, ""))) {
            resetDay();
        }
    }

    private void resetDay() {
        prefs.edit()
                .putString(KEY_DATE, today())
                .putString(KEY_ENTRIES, "[]")
                .putInt(KEY_TOTAL, 0)
                .apply();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
