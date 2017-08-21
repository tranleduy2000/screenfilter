package com.duy.screenfilter.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.utils.NightScreenSettings;
import com.duy.screenfilter.utils.Utility;

public class TileReceiver extends BroadcastReceiver {

    public static final String ACTION_UPDATE_STATUS = "info.papdt.blackbulb.ACTION_UPDATE_STATUS";
    private static final String TAG = "TileReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        NightScreenSettings settings = NightScreenSettings.getInstance(context);
        Log.i(TAG, "received \"" + intent.getAction() + "\" action");
        if (ACTION_UPDATE_STATUS.equals(intent.getAction())) {
            String action = intent.getStringExtra(Constants.EXTRA_ACTION);
            int brightness = intent.getIntExtra(Constants.EXTRA_BRIGHTNESS, 50);
            boolean dontSendCheck = intent.getBooleanExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, false);

            Log.i(TAG, "handle \"" + action + "\" action");
            switch (action) {
                case Constants.ACTION_START:
                    Intent intent1 = new Intent(context, MaskService.class);
                    intent1.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
                    intent1.putExtra(Constants.EXTRA_BRIGHTNESS, settings.getInt(NightScreenSettings.KEY_BRIGHTNESS, brightness));
                    intent1.putExtra(Constants.EXTRA_MODE, settings.getInt(NightScreenSettings.KEY_MODE, Constants.MODE_NO_PERMISSION));
                    intent1.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, dontSendCheck);
                    context.startService(intent1);
                    break;
                case Constants.ACTION_PAUSE:
                    Intent intent2 = new Intent(context, MaskService.class);
                    intent2.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_PAUSE);
                    intent2.putExtra(Constants.EXTRA_BRIGHTNESS, settings.getInt(NightScreenSettings.KEY_BRIGHTNESS, brightness));
                    intent2.putExtra(Constants.EXTRA_MODE, settings.getInt(NightScreenSettings.KEY_MODE, Constants.MODE_NO_PERMISSION));
                    intent2.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, dontSendCheck);
                    context.startService(intent2);
                    break;
                case Constants.ACTION_STOP:
                    Intent intent3 = new Intent(context, MaskService.class);
                    intent3.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
                    intent3.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, dontSendCheck);
                    context.startService(intent3);
                    break;
            }
        } else if (Constants.ALARM_ACTION_START.equals(intent.getAction())) {
            Utility.updateAlarmSettings(context);
            Intent intent1 = new Intent(context, MaskService.class);
            intent1.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
            intent1.putExtra(Constants.EXTRA_BRIGHTNESS, settings.getInt(NightScreenSettings.KEY_BRIGHTNESS, 50));
            intent1.putExtra(Constants.EXTRA_MODE, settings.getInt(NightScreenSettings.KEY_MODE, Constants.MODE_NO_PERMISSION));
            intent1.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, false);
            context.startService(intent1);
        } else if (Constants.ALARM_ACTION_STOP.equals(intent.getAction())) {
            Utility.updateAlarmSettings(context);
            Intent intent1 = new Intent(context, MaskService.class);
            intent1.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
            intent1.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, false);
            context.startService(intent1);
        }
    }

}
