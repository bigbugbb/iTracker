package com.localytics.android.itracker;

import com.localytics.android.Localytics;
import com.localytics.android.LocalyticsActivityLifecycleCallbacks;
import com.localytics.android.itracker.util.LogUtils;

public class Application extends android.app.Application {
    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(this));
        Localytics.registerPush(Config.GCM_SENDER_ID);
    }
}
