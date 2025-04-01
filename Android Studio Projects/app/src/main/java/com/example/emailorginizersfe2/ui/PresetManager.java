// PresetManager.java
package com.example.emailorginizersfe2.ui;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PresetManager {
    private static final String PREFS_NAME = "presets";
    private static final String KEY_PRESETS = "saved_presets";

    public static void savePreset(Context context, String title, List<String> positives, List<String> negatives) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray presetArray = getPresetArray(context);

        JSONObject preset = new JSONObject();
        try {
            preset.put("title", title);
            preset.put("positives", new JSONArray(positives));
            preset.put("negatives", new JSONArray(negatives));
            presetArray.put(preset);

            prefs.edit().putString(KEY_PRESETS, presetArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static JSONArray getPresetArray(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PRESETS, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static void clearAllPresets(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_PRESETS).apply();
    }
}