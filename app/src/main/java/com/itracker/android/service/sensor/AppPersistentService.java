package com.itracker.android.service.sensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.itracker.android.Application;
import com.itracker.android.data.LogManager;
import com.itracker.android.data.SettingsManager;
import com.itracker.android.data.notification.NotificationManager;

public class AppPersistentService extends Service {

    private static AppPersistentService sInstance;

    public static AppPersistentService getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        LogManager.i(this, "onCreate");
        changeForeground();
    }

    public void changeForeground() {
        if (SettingsManager.eventsPersistent()
                && Application.getInstance().isInitialized())
            startForeground(NotificationManager.PERSISTENT_NOTIFICATION_ID,
                    NotificationManager.getInstance().getPersistentNotification());
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
        return new Intent(context, AppPersistentService.class);
    }

}