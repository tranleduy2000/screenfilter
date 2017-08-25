package com.duy.screenfilter.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.activities.MainActivity;
import com.duy.screenfilter.model.ColorProfile;
import com.duy.screenfilter.monitor.CurrentAppMonitor;
import com.duy.screenfilter.utils.Utility;
import com.duy.screenfilter.view.MaskView;

import static android.view.WindowManager.LayoutParams;

public class MaskService extends Service implements ServiceController {

    private static final int ANIMATE_DURATION_MILES = 700;
    private static final int NOTIFICATION_NO = 1024;
    private static final String TAG = "MaskService";

    private WindowManager mWindowManager;
    private NotificationManager mNotificationManager;
    private AccessibilityManager mAccessibilityManager;
    private CurrentAppMonitor mCurrentAppMonitor;

    private MaskView mMaskView;
    private LayoutParams mLayoutParams;

    private boolean isShowing = false;
    private MaskBinder mBinder = new MaskBinder();
    private ColorProfile mColorProfile = null;
    private Status status;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAccessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        mCurrentAppMonitor = new CurrentAppMonitor(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMaskView();
    }

    private void createMaskView(Intent startIntent) {
        try {
            mColorProfile = (ColorProfile) startIntent.getSerializableExtra(Constants.EXTRA_COLOR_PROFILE);
            updateLayoutParams();
            mWindowManager.addView(mMaskView, mLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean updateLayoutParams() {
        if (mLayoutParams == null) {
            int max = Math.max(Utility.getTrueScreenWidth(this), Utility.getTrueScreenHeight(this));

            mLayoutParams = new LayoutParams();
            mLayoutParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY;
            mLayoutParams.height = mLayoutParams.width = max + 200;
            mLayoutParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
            mLayoutParams.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.flags |= LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            mLayoutParams.format = PixelFormat.TRANSPARENT;
            mLayoutParams.gravity = Gravity.CENTER;
        }
        if (mMaskView == null) {
            mMaskView = new MaskView(this);
            mMaskView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        return mMaskView.setProfile(mColorProfile);
    }

    private void destroyMaskView() {
        isShowing = false;
        cancelNotification();
        if (mMaskView != null) {
            mMaskView.animate()
                    .alpha(0f)
                    .setDuration(ANIMATE_DURATION_MILES).start();
            mMaskView = null;
        }
    }

    private Notification createRunningNotification() {
        Log.i(TAG, "Create running notification");

        Intent openIntent = new Intent(this, MainActivity.class);
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_UPDATE_FROM_NOTIFICATION);
        pauseIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_PAUSE);

        Notification.Action pauseAction = new Notification.Action(
                R.drawable.ic_wb_incandescent_black_24dp,
                getString(R.string.notification_action_turn_off),
                PendingIntent.getBroadcast(getBaseContext(), 0, pauseIntent, Intent.FILL_IN_DATA));

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.notification_running_title))
                .setContentText(getString(R.string.notification_running_msg))
                .setSmallIcon(R.drawable.ic_brightness_2_white_36dp)
                .addAction(pauseAction)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build();
    }

    private Notification createPauseNotification() {
        Log.i(TAG, "Create paused notification");
        Intent openIntent = new Intent(this, MainActivity.class);
        Intent resumeIntent = new Intent();
        resumeIntent.setAction(Constants.ACTION_UPDATE_FROM_NOTIFICATION);
        resumeIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
        resumeIntent.putExtra(Constants.EXTRA_COLOR_PROFILE, mColorProfile);

        Intent closeIntent = new Intent(this, MaskService.class);
        closeIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);

        Notification.Action resumeAction = new Notification.Action(R.drawable.ic_wb_incandescent_black_24dp,
                getString(R.string.notification_action_turn_on),
                PendingIntent.getBroadcast(getBaseContext(), 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        return new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.notification_paused_title))
                .setContentText(getString(R.string.notification_paused_msg))
                .setSmallIcon(R.drawable.ic_brightness_2_white_36dp)
                .addAction(resumeAction)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setDeleteIntent(PendingIntent.getService(getBaseContext(), 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
    }

    private void showPausedNotification() {
        mNotificationManager.notify(NOTIFICATION_NO, createPauseNotification());
    }

    private void cancelNotification() {
        try {
            mNotificationManager.cancel(NOTIFICATION_NO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int arg) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], arg = [" + arg + "]");
        if (intent != null && intent.hasExtra(Constants.EXTRA_ACTION)) {
            String action = intent.getStringExtra(Constants.EXTRA_ACTION);
            switch (action) {
                case Constants.ACTION_START:
                    start(intent);
                    break;
                case Constants.ACTION_PAUSE:
                    pause(intent);
                    break;
                case Constants.ACTION_STOP:
                    stop(intent);
                    break;
                case Constants.ACTION_UPDATE:
                    update(intent);
                    break;
            }
        }
        return START_STICKY;
    }

    public void stop(@Nullable Intent intent) {
        Log.i(TAG, "Stop Mask");
        if (status != Status.STOPPED) {
            status = Status.STOPPED;
            isShowing = false;
            mCurrentAppMonitor.stop();
            stopForeground(true);
            stopSelf();
        }
    }

    public Status getStatus() {
        return status;
    }

    public void start(@Nullable Intent intent) {
        if (!isShowing) {
            try {
                status = Status.STARTING;
                createMaskView(intent);

                startForeground(NOTIFICATION_NO, createRunningNotification());
                mCurrentAppMonitor.start();

                updateLayoutParams();
                mWindowManager.updateViewLayout(mMaskView, mLayoutParams);

                isShowing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "start: service already started");
        }
    }

    public void pause(@Nullable Intent intent) {
        Log.i(TAG, "Pause Mask");
        if (status != Status.PAUSED) {
            status = Status.PAUSED;
            stopForeground(true);
            destroyMaskView();
            showPausedNotification();
            isShowing = false;
        }
    }

    public void update(Intent intent) {
        try {
            status = Status.UPDATING;
            mColorProfile = (ColorProfile) intent.getSerializableExtra(Constants.EXTRA_COLOR_PROFILE);
            if (updateLayoutParams()) {
                try {
                    mWindowManager.updateViewLayout(mMaskView, mLayoutParams);
                } catch (Exception e) {//not attached to window manager
                    mWindowManager.addView(mMaskView, mLayoutParams);
                }
            }
            isShowing = true;
        } catch (Exception e) {
            e.printStackTrace();
            isShowing = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    enum Status {STARTING, PAUSED, UPDATING, STOPPED}

    public class MaskBinder extends Binder {

        public boolean isMaskShowing() {
            return isShowing;
        }

    }
}
