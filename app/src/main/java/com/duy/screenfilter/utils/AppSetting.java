package com.duy.screenfilter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class AppSetting {
    public static final String KEY_BRIGHTNESS = "brightness";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_DARK_THEME = "dark_theme";
    public static final String KEY_AUTO_MODE = "auto_mode";
    public static final String KEY_HOURS_SUNRISE = "hrs_sunrise";
    public static final String KEY_MINUTES_SUNRISE = "min_sunrise";
    public static final String KEY_HOURS_SUNSET = "hrs_sunset";
    public static final String KEY_MINUTES_SUNSET = "min_sunset";
    public static final String KEY_COLOR = "color";
    private static final String PREF_NAME = "setting";
    private static final String TAG = "AppSetting";
    private volatile static AppSetting sInstance;
    private SharedPreferences mPrefs;

    private AppSetting(Context context) {
        this.mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
    }

    public static AppSetting getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppSetting(context);
        }
        return sInstance;
    }

    public AppSetting putBoolean(String key, boolean value) {
        mPrefs.edit().putBoolean(key, value).apply();
        return this;
    }

    public boolean getBoolean(String key, boolean def) {
        return mPrefs.getBoolean(key, def);
    }

    public AppSetting putInt(String key, int value) {
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

    public AppSetting putString(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
        return this;
    }

    public String getString(String key, String defValue) {
        try {
            return mPrefs.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public int getFilterColor() {
        return getInt(KEY_COLOR, Color.BLACK);
    }

    public AppSetting setFilterColor(int color) {
        mPrefs.edit().putInt(KEY_COLOR, color).apply();
        return this;
    }
}
