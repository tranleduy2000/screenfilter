package com.duy.screenfilter.monitor;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.annotation.Nullable;

import com.duy.screenfilter.receivers.ActionReceiver;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.utils.AppSetting;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Duy on 24-Aug-17.
 */

public class CurrentAppMonitoringThread extends Thread {
    private static final String TAG = "CurrentAppMonitoringThr";
    private final AtomicBoolean running = new AtomicBoolean();
    private MaskService mContext;
    private AppSetting appSetting;
    private boolean isLastSecuredApp = false;

    public CurrentAppMonitoringThread(MaskService context) {
        this.mContext = context;
        this.appSetting = AppSetting.getInstance(context.getApplicationContext());
    }

    @Nullable
    private static String getCurrentApp(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return getCurrentAppUsingUsageStats(context);
        } else {
            return getCurrentAppUsingActivityManager(context);
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static String getCurrentAppUsingActivityManager(Context context) {
        if (belowAPI(21)) {
            ActivityManager am = (ActivityManager) new ContextWrapper(context).getBaseContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            return am.getRunningTasks(1).get(0).topActivity.getPackageName();
        }
        return null;
    }

    private static boolean belowAPI(int api) {
        return Build.VERSION.SDK_INT < api;
    }

    @Nullable
    public static String getCurrentAppUsingUsageStats(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                // Although the UsageStatsManager was added in API 21, the
                // constant to specify it wasn't added until API 22.
                // So we use the value of that constant on API 21.
                String usageStatsServiceString;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    usageStatsServiceString = (atLeastAPI(22)) ? Context.USAGE_STATS_SERVICE : "usagestats";
                } else {
                    usageStatsServiceString = "usagestats";
                }
                @SuppressWarnings("WrongConstant")
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(usageStatsServiceString);
                long time = System.currentTimeMillis();
                List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

                if (appList != null && appList.size() > 0) {
                    TreeMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                    for (UsageStats usageStats : appList) {
                        mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    }
                    if (!mySortedMap.isEmpty()) {
                        return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exceptions to allow the user to determine if it
            // works him/herself
        }

        return null;
    }

    private static boolean atLeastAPI(int api) {
        return Build.VERSION.SDK_INT >= api;
    }

    @Override
    public void run() {
        super.run();
        while (running.get()) {
            try {
                String currentApp = getCurrentApp(mContext);
                if (isAppSecured(currentApp)) {
                    if (running.get() && !isLastSecuredApp) {
                        ActionReceiver.pauseService(mContext);
                        isLastSecuredApp = true;
                    }
                } else {
                    if (running.get() && isLastSecuredApp) {
                        ActionReceiver.updateService(mContext);
                        isLastSecuredApp = false;
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isAppSecured(@Nullable String app) {
        if (app == null) return false;
        String[] securedApp = new String[]{
                "com.android.packageinstaller",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "me.phh.superuser",
                "com.owncloud.android",
                "com.google.android.packageinstaller"};
        for (String s : securedApp) {
            if (app.equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }
}
