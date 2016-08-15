package com.localytics.android.itracker.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.notification.NotificationManager;

/**
 * Basic service to work in background.
 *
 * @author alexander.ivanov
 */
public class XabberService extends Service {

    private static XabberService instance;

    public static XabberService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LogManager.i(this, "onCreate");
        changeForeground();
    }

    public void changeForeground() {
        if (SettingsManager.eventsPersistent()
                && Application.getInstance().isInitialized())
            startForeground(NotificationManager.PERSISTENT_NOTIFICATION_ID,
                    NotificationManager.getInstance()
                            .getPersistentNotification());
        else
            stopForeground(true);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Application.getInstance().onServiceStarted();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogManager.i(this, "onDestroy");
        stopForeground(true);
        Application.getInstance().onServiceDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberService.class);
    }

}