package com.localytics.android.itracker;

import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;

import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.download.FileDownloadService;
import com.localytics.android.itracker.monitor.TrackerBroadcastReceiver;
import com.localytics.android.itracker.util.LogUtils;

public class Application extends android.app.Application {

    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();

        // Bootstrap the monitor here in case the app is opened again after crashed or killed.
        bootstrapBackgroundMonitor();

        bootstrapFileDownloadService();

        if (BuildConfig.DEBUG) {
//            Config.enableStrictMode();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void bootstrapBackgroundMonitor() {
        Intent intent = new Intent(TrackerBroadcastReceiver.ACTION_BOOTSTRAP_MONITOR_ALARM);
        sendBroadcast(intent);
    }

    private void bootstrapFileDownloadService() {
        FileDownloadManager fdm = FileDownloadManager.getInstance(this);
        fdm.recoverStatus();
    }
}
