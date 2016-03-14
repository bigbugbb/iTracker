package com.localytics.android.itracker;

import com.localytics.android.itracker.util.LogUtils;

public class Application extends android.app.Application {
    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Config.enableStrictMode();
        }
//        registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(this));
    }
}
