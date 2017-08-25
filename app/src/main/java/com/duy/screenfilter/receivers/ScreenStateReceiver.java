package com.duy.screenfilter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Duy on 24-Aug-17.
 */

public class ScreenStateReceiver extends BroadcastReceiver {
    private ScreenStateListener mScreenStateListener;

    public ScreenStateReceiver(ScreenStateListener mScreenStateListener) {
        this.mScreenStateListener = mScreenStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            if (mScreenStateListener != null) mScreenStateListener.onScreenTurnOn();
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            if (mScreenStateListener != null) mScreenStateListener.onScreenTurnOff();
        }
    }

    public interface ScreenStateListener {
        void onScreenTurnOn();

        void onScreenTurnOff();
    }
}
