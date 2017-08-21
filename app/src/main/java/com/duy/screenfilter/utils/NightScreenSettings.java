package com.duy.screenfilter.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class NightScreenSettings {

    public static final String PREFERENCES_NAME = "settings";

    public static final String KEY_BRIGHTNESS = "brightness";
    public static final String KEY_MODE = "mode";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_DARK_THEME = "dark_theme";
    public static final String KEY_AUTO_MODE = "auto_mode";
    public static final String KEY_HOURS_SUNRISE = "hrs_sunrise";
    public static final String KEY_MINUTES_SUNRISE = "min_sunrise";
    public static final String KEY_HOURS_SUNSET = "hrs_sunset";
    public static final String KEY_MINUTES_SUNSET = "min_sunset";

    private volatile static NightScreenSettings sInstance;

    private SharedPreferences mPrefs;

    private NightScreenSettings(Context context) {
        mPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);
    }

    public static NightScreenSettings getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NightScreenSettings.class) {
                if (sInstance == null) {
                    sInstance = new NightScreenSettings(context);
                }
            }
        }
        return sInstance;
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
        return mPrefs.getInt(key, defValue);
    }

    public NightScreenSettings putString(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
        return this;
    }

    public String getString(String key, String defValue) {
        return mPrefs.getString(key, defValue);
    }

}
