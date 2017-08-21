package com.duy.screenfilter.services;

import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.activities.MainActivity;
import com.duy.screenfilter.utils.Utility;

import static android.view.WindowManager.LayoutParams;

public class MaskService extends Service {

    private static final int ANIMATE_DURATION_MILES = 250;
    private static final int NOTIFICATION_NO = 1024;
    private static final String TAG = "MaskService";

    private int brightness = 50;
    private WindowManager mWindowManager;
    private NotificationManager mNotificationManager;
    private AccessibilityManager mAccessibilityManager;
    private Notification mNoti;
    private View mLayout;
    private LayoutParams mLayoutParams;
    private int mode = Constants.MODE_NORMAL;
    private boolean isShowing = false;
    private MaskBinder mBinder = new MaskBinder();
    @ColorInt
    private int color = Color.BLACK;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAccessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMaskView();
        sendBroadcastStopService();
    }

    private void sendBroadcastStopService() {
        Intent intent = new Intent();
        intent.setAction(MainActivity.class.getCanonicalName());
        intent.putExtra(Constants.EXTRA_EVENT_ID, Constants.EVENT_DESTORY_SERVICE);
        sendBroadcast(intent);
    }

    private void createMaskView() {
        mAccessibilityManager.isEnabled();

        updateLayoutParams(mode, -1, color);
        mLayoutParams.gravity = Gravity.CENTER;

        if (mLayout == null) {
            mLayout = new View(this);
            mLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mLayout.setBackgroundColor(Color.BLACK);
            mLayout.setAlpha(0f);
        }

        try {
            mWindowManager.addView(mLayout, mLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();

            Intent intent = new Intent();
            intent.setAction(MainActivity.class.getCanonicalName());
            intent.putExtra(Constants.EXTRA_EVENT_ID, Constants.EVENT_CANNOT_START);
            sendBroadcast(intent);
        }
    }

    private void updateLayoutParams(int mode, int brightness, int color) {
        Log.d(TAG, "updateLayoutParams() called with: mode = [" + mode + "], brightness = [" + brightness + "]");

        if (mLayoutParams == null) mLayoutParams = new LayoutParams();

        this.mAccessibilityManager.isEnabled();

        switch (mode) {
            case Constants.MODE_NORMAL:
                mLayoutParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY;
                break;
            case Constants.MODE_OVERLAY_ALL:
                mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
                break;
        }
        if (mode == Constants.MODE_OVERLAY_ALL) {
            mLayoutParams.width = 0;
            mLayoutParams.height = 0;
            mLayoutParams.flags |= LayoutParams.FLAG_DIM_BEHIND;
            mLayoutParams.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
            mLayoutParams.flags &= 0xFFDFFFFF;
            mLayoutParams.flags &= 0xFFFFFF7F;
            mLayoutParams.format = PixelFormat.OPAQUE;
            mLayoutParams.dimAmount = constrain((100 - brightness) / 100.0F, 0.0F, 0.9F);
        } else {
            int max = Math.max(Utility.getTrueScreenWidth(this), Utility.getTrueScreenHeight(this));
            mLayoutParams.height = mLayoutParams.width = max + 200;
            mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCHABLE
                    | LayoutParams.FLAG_NOT_FOCUSABLE
                    | LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            mLayoutParams.format = PixelFormat.TRANSPARENT;
            float targetAlpha = (100 - this.brightness) * 0.01f;
            if (brightness != -1) {
                if (isShowing) {
                    if (Math.abs(targetAlpha - mLayout.getAlpha()) < 0.1f) {
                        mLayout.setAlpha(targetAlpha);
                    } else {
                        mLayout.animate().alpha(targetAlpha).setDuration(100).start();
                    }
                } else {
                    mLayout.animate().alpha(targetAlpha).setDuration(ANIMATE_DURATION_MILES).start();
                }
            }
        }

        if (mLayout != null) {
            int targetAlpha = (int) ((100 - this.brightness) * 0.01f * 255);
            mLayout.setBackgroundColor(Color.argb(targetAlpha,
                    Color.red(color), Color.green(color), Color.blue(color)));
        }
    }

    private float constrain(float paramFloat1, float paramFloat2, float paramFloat3) {
        if (paramFloat1 < paramFloat2) {
            return paramFloat2;
        }
        if (paramFloat1 > paramFloat3) {
            return paramFloat3;
        }
        return paramFloat1;
    }

    private void destroyMaskView() {
        isShowing = false;
        cancelNotification();
        if (mLayout != null) {
            mLayout.animate()
                    .alpha(0f)
                    .setDuration(ANIMATE_DURATION_MILES)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            try {
                                mWindowManager.removeViewImmediate(mLayout);
                                mLayout = null;
                            } catch (Exception ignored) {
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
        }
    }

    private void createNotification() {
        Log.i(TAG, "Create running notification");
        Intent openIntent = new Intent(this, MainActivity.class);
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(TileReceiver.ACTION_UPDATE_STATUS);
        Log.i(TAG, "Create " + Constants.ACTION_PAUSE + " action");
        pauseIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_PAUSE);
        pauseIntent.putExtra(Constants.EXTRA_BRIGHTNESS, brightness);

        Notification.Action pauseAction = new Notification.Action(
                R.drawable.ic_wb_incandescent_black_24dp,
                getString(R.string.notification_action_turn_off),
                PendingIntent.getBroadcast(getBaseContext(), 0, pauseIntent, Intent.FILL_IN_DATA));

        mNoti = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.notification_running_title))
                .setContentText(getString(R.string.notification_running_msg))
                .setSmallIcon(R.drawable.ic_brightness_2_white_36dp)
                .addAction(pauseAction)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build();

    }

    // implement pause notification
    private void createPauseNotification() {
        Log.i(TAG, "Create paused notification");
        Intent openIntent = new Intent(this, MainActivity.class);
        Intent resumeIntent = new Intent();
        resumeIntent.setAction(TileReceiver.ACTION_UPDATE_STATUS);
        resumeIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
        resumeIntent.putExtra(Constants.EXTRA_BRIGHTNESS, brightness);

        Intent closeIntent = new Intent(this, MaskService.class);
        closeIntent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);

        Notification.Action resumeAction = new Notification.Action(R.drawable.ic_wb_incandescent_black_24dp,
                getString(R.string.notification_action_turn_on),
                PendingIntent.getBroadcast(getBaseContext(), 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        mNoti = new Notification.Builder(getApplicationContext())
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
        if (mNoti == null) createPauseNotification();
        mNotificationManager.notify(NOTIFICATION_NO, mNoti);
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
            mode = intent.getIntExtra(Constants.EXTRA_MODE, mode);

            switch (action) {
                case Constants.ACTION_START:
                    start(intent);
                    break;
                case Constants.ACTION_PAUSE:
                    pause(intent);
                    break;
                case Constants.ACTION_STOP:
                    this.stop(intent);
                    break;
                case Constants.ACTION_UPDATE_BRIGHTNESS:
                    Log.d(TAG, "onStartCommand: update brightness");
                    update(intent);
                    break;
                case Constants.ACTION_UPDATE_COLOR:
                    Log.d(TAG, "onStartCommand: update color");
                    update(intent);
                    break;
            }

        }

        if (intent != null && !intent.getBooleanExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, false)) {
            Log.i(TAG, "Check Mask. Check from toggle:" + intent.getBooleanExtra(Constants.EXTRA_CHECK_FROM_TOGGLE, false));
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(
                    intent.getBooleanExtra(Constants.EXTRA_CHECK_FROM_TOGGLE, false)
                            ? "info.papdt.blackbulb.ACTION_TOGGLE"
                            : "info.papdt.blackbulb.ACTION_UPDATE_ACTIVITY_TOGGLE"
            );
            broadcastIntent.putExtra(Constants.EXTRA_EVENT_ID, Constants.EVENT_CHECK);
            broadcastIntent.putExtra("isShowing", isShowing);
            sendBroadcast(broadcastIntent);
        }

        return START_STICKY;
    }

    private void stop(Intent intent) {
        Log.i(TAG, "Stop Mask");
        isShowing = false;
        stopSelf();
    }

    private void start(Intent intent) {
        Log.i(TAG, "Start Mask");
        if (mLayout == null) createMaskView();

        createNotification();
        startForeground(NOTIFICATION_NO, mNoti);

        try {
            updateLayoutParams(mode, brightness, color);
            mWindowManager.updateViewLayout(mLayout, mLayoutParams);
            isShowing = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Set alpha:" + String.valueOf(100 - intent.getIntExtra(Constants.EXTRA_BRIGHTNESS, 0)));
    }

    private void pause(Intent intent) {
        Log.i(TAG, "Pause Mask");
        stopForeground(true);
        destroyMaskView();
        createPauseNotification();
        showPausedNotification();
        isShowing = false;
    }

    private void update(Intent intent) {
        Log.i(TAG, "Update Mask");
        brightness = intent.getIntExtra(Constants.EXTRA_BRIGHTNESS, brightness);
        color = intent.getIntExtra(Constants.EXTRA_COLOR, color);
        mAccessibilityManager.isEnabled();
        try {
            updateLayoutParams(mode, brightness, color);
            mWindowManager.updateViewLayout(mLayout, mLayoutParams);
            isShowing = true;
        } catch (Exception e) {
            e.printStackTrace();
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

    public class MaskBinder extends Binder {

        public boolean isMaskShowing() {
            return isShowing;
        }

    }

}
