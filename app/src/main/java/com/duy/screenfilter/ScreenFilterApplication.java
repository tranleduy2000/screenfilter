package com.duy.screenfilter;

import android.app.Application;

import com.duy.screenfilter.utils.AppSetting;
import com.duy.screenfilter.utils.Utility;

public class ScreenFilterApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utility.updateAlarmSettings(this);
        AppSetting.newInstance(this);
//        if (BuildConfig.DEBUG) FirebaseCrash.setCrashCollectionEnabled(false);
    }

}
