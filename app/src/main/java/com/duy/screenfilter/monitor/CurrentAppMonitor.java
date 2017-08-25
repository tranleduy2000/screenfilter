package com.duy.screenfilter.monitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.duy.screenfilter.receivers.ScreenStateReceiver;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.utils.AppSetting;

/**
 * Created by Duy on 24-Aug-17.
 */

public class CurrentAppMonitor implements ScreenStateReceiver.ScreenStateListener {
    private static final String TAG = "CurrentAppMonitor";
    private MaskService mMaskService;
    private PowerManager mPowerManager;
    private boolean isMonitoring = false;
    private ScreenStateReceiver mScreenStateReceiver = new ScreenStateReceiver(this);
    @Nullable
    private CurrentAppMonitoringThread mCamThread;

    public CurrentAppMonitor(MaskService context) {
        this.mMaskService = context;
        this.mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    @SuppressWarnings("deprecation")
    public boolean isScreenOn() {
        Log.d(TAG, "isScreenOn() called");
        if (Build.VERSION.SDK_INT >= 20) {
            return mPowerManager.isInteractive();
        } else {
            return mPowerManager.isScreenOn();
        }
    }

    @Override
    public void onScreenTurnOn() {
        Log.d(TAG, "onScreenTurnOn() called");

        startCamThread();
    }

    private void startCamThread() {
        if (mCamThread == null && isScreenOn()) {
            mCamThread = new CurrentAppMonitoringThread(mMaskService);
            mCamThread.setRunning(true);
            mCamThread.start();
        }
    }

    @Override
    public void onScreenTurnOff() {
        Log.d(TAG, "onScreenTurnOff() called");
        stopCamThread();
    }

    public void start() {
        AppSetting appSetting = AppSetting.newInstance(mMaskService);
        if (appSetting.isSecureSuspend()) {
            if (isMonitoring) {
                Log.d(TAG, "start isMonitoring = " + true);
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                mMaskService.registerReceiver(mScreenStateReceiver, intentFilter);
                isMonitoring = true;
                startCamThread();
            }
        }
    }

    private void stopCamThread() {
        if (mCamThread != null && !mCamThread.isInterrupted()) {
            mCamThread.setRunning(false);
            mCamThread = null;
        }
    }

    public void stop() {
        if (!isMonitoring) {
            Log.d(TAG, "stop isMonitoring = " + false);
        } else {
            Log.d(TAG, "stop: stop");
            stopCamThread();
            try {
                mMaskService.unregisterReceiver(mScreenStateReceiver);
            } catch (Exception ignored) {
            }
            isMonitoring = false;
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public void pause() {

    }
}

