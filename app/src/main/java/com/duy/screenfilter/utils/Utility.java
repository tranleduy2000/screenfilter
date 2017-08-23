package com.duy.screenfilter.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.services.MaskService;

import java.util.Calendar;

import static android.app.ActivityManager.RunningServiceInfo;

@SuppressWarnings("unchecked")
public class Utility {

    public static final int REQUEST_ALARM_SUNRISE = 1002;
    public static final int REQUEST_ALARM_SUNSET = 1003;

    public static final String TAG = Utility.class.getSimpleName();

    public static int getTrueScreenHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        int dpi = dm.heightPixels;

        return dpi;
    }

    public static int getTrueScreenWidth(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        int dpi = dm.widthPixels;

        return dpi;
    }


    public static void updateAlarmSettings(Context context) {
        if (true) return;

        AppSetting settings = AppSetting.getInstance(context);
        if (settings.getBoolean(AppSetting.KEY_AUTO_MODE, false)) {
            int hrsSunrise = settings.getInt(AppSetting.KEY_HOURS_SUNRISE, 6);
            int minSunrise = settings.getInt(AppSetting.KEY_MINUTES_SUNRISE, 0);
            int hrsSunset = settings.getInt(AppSetting.KEY_HOURS_SUNSET, 22);
            int minSunset = settings.getInt(AppSetting.KEY_MINUTES_SUNSET, 0);

            Calendar now = Calendar.getInstance();
            Calendar sunriseCalendar = (Calendar) now.clone();
            Calendar sunsetCalendar = (Calendar) now.clone();

            sunriseCalendar.set(Calendar.HOUR_OF_DAY, hrsSunrise);
            sunriseCalendar.set(Calendar.MINUTE, minSunrise);
            sunriseCalendar.set(Calendar.SECOND, 0);
            sunriseCalendar.set(Calendar.MILLISECOND, 0);
            if (sunriseCalendar.before(now)) sunriseCalendar.add(Calendar.DATE, 1);

            sunsetCalendar.set(Calendar.HOUR_OF_DAY, hrsSunset);
            sunsetCalendar.set(Calendar.MINUTE, minSunset);
            sunsetCalendar.set(Calendar.SECOND, 0);
            sunsetCalendar.set(Calendar.MILLISECOND, 0);
            if (sunsetCalendar.before(now)) sunsetCalendar.add(Calendar.DATE, 1);

            Log.i(TAG, "Reset alarm");

            cancelAlarm(context, REQUEST_ALARM_SUNRISE, Constants.ALARM_ACTION_STOP);
            cancelAlarm(context, REQUEST_ALARM_SUNSET, Constants.ALARM_ACTION_START);
            setAlarm(context,
                    AlarmManager.RTC_WAKEUP,
                    sunriseCalendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    REQUEST_ALARM_SUNRISE,
                    Constants.ALARM_ACTION_STOP);
            setAlarm(context,
                    AlarmManager.RTC_WAKEUP,
                    sunsetCalendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    REQUEST_ALARM_SUNSET,
                    Constants.ALARM_ACTION_START);
        } else {
            Log.i(TAG, "Cancel alarm");
            cancelAlarm(context, REQUEST_ALARM_SUNRISE, Constants.ALARM_ACTION_STOP);
            cancelAlarm(context, REQUEST_ALARM_SUNSET, Constants.ALARM_ACTION_START);
        }
    }

    private static void setAlarm(Context context, int type, long triggerAtMillis,
                                 long intervalMillis, int requestCode, String action) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(type, triggerAtMillis, intervalMillis, PendingIntent.getBroadcast(context,
                requestCode, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private static void cancelAlarm(Context context, int requestCode, String action) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(context,
                requestCode, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static float dpToPx(Context context, float dp) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static boolean isScreenFilterServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d(TAG, "isScreenFilterServiceRunning: " + service.service.getClassName());
            if (MaskService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
