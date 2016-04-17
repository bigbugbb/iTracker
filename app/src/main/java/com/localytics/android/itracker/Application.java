package com.localytics.android.itracker;

import android.content.Context;
import android.support.multidex.MultiDex;

import com.localytics.android.itracker.util.LogUtils;

public class Application extends android.app.Application {

    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Config.enableStrictMode();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
