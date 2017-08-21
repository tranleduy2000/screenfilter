package com.duy.screenfilter.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.duy.screenfilter.R;

@SuppressLint("CommitPrefEdits")
public class NightScreenSettings {

    public static final String PREFERENCES_NAME = "settings";

    public static final String KEY_BRIGHTNESS = "brightness",
            KEY_MODE = "mode", KEY_FIRST_RUN = "first_run",
            KEY_DARK_THEME = "dark_theme", KEY_AUTO_MODE = "auto_mode",
            KEY_HOURS_SUNRISE = "hrs_sunrise", KEY_MINUTES_SUNRISE = "min_sunrise",
            KEY_HOURS_SUNSET = "hrs_sunset", KEY_MINUTES_SUNSET = "min_sunset";


    private SharedPreferences mPrefs;
    private Context context;

    private NightScreenSettings(Context context) {
        mPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);
        this.context = context;
    }

    public static NightScreenSettings newInstance(Context context) {
        return new NightScreenSettings(context);
    }

    public NightScreenSettings putBoolean(String key, boolean value) {
        mPrefs.edit().putBoolean(key, value).apply();
        return this;
    }

    public boolean getBoolean(String key, boolean def) {
        return mPrefs.getBoolean(key, def);
    }

    public NightScreenSettings putInt(String key, int value) {
        mPrefs.edit().putInt(key, value).apply();
        return this;
    }

    public int getInt(String key, int defValue) {
        try {
            return mPrefs.getInt(key, defValue);
        } catch (Exception e) {
            //class cast
        }
        return defValue;
    }

    public NightScreenSettings putString(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
        return this;
    }

    public String getString(String key, String defValue) {
        return mPrefs.getString(key, defValue);
    }


    public int getFilterColor() {
        return getInt(context.getString(R.string.key_pref_color),
                context.getResources().getColor(R.color.grey_800));
    }
}
