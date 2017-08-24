package com.duy.screenfilter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.utils.AppSetting;
import com.duy.screenfilter.utils.Utility;

public class ActionReceiver extends BroadcastReceiver {

    private static final String TAG = "TileReceiver";

    public static void pauseService(Context context) {
        Log.d(TAG, "pauseService() called with: context = [" + context + "]");

        AppSetting settings = AppSetting.getInstance(context);
        Intent intent2 = new Intent(context, MaskService.class);
        intent2.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_PAUSE);
        intent2.putExtra(Constants.EXTRA_COLOR_PROFILE, settings.getColorProfile());
        context.startService(intent2);
    }

    public static void stopService(Context context) {
        Log.d(TAG, "stopService() called with: context = [" + context + "]");

        Intent intent = new Intent(context, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
        context.startService(intent);
    }

    public static void startService(Context context) {
        Log.d(TAG, "startService() called with: context = [" + context + "]");

        Utility.updateAlarmSettings(context);
        AppSetting settings = AppSetting.getInstance(context);
        Intent intent = new Intent(context, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
        intent.putExtra(Constants.EXTRA_COLOR_PROFILE, settings.getColorProfile());
        context.startService(intent);
    }


    public static void updateService(Context context) {
        Log.d(TAG, "updateService() called with: context = [" + context + "]");

        AppSetting settings = AppSetting.getInstance(context);
        Intent intent = new Intent(context, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE);
        intent.putExtra(Constants.EXTRA_COLOR_PROFILE, settings.getColorProfile());
        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received \"" + intent.getAction() + "\" action");
        if (Constants.ACTION_UPDATE_FROM_NOTIFICATION.equals(intent.getAction())) {
            String action = intent.getStringExtra(Constants.EXTRA_ACTION);
            switch (action) {
                case Constants.ACTION_START:
                    startService(context);
                    break;
                case Constants.ACTION_PAUSE:
                    pauseService(context);
                    break;
                case Constants.ACTION_STOP:
                    stopService(context);
                    break;
                case Constants.ACTION_UPDATE:
                    updateService(context);
                    break;
            }
        } else if (Constants.ALARM_ACTION_START.equals(intent.getAction())) {
            startService(context);
        } else if (Constants.ALARM_ACTION_STOP.equals(intent.getAction())) {
            stopService(context);
        }
    }
}
