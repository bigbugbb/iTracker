package com.itracker.android.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;

import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.data.FileDownloadManager;
import com.itracker.android.service.sensor.SensorMonitorService;
import com.itracker.android.utils.ConnectivityUtils;

import java.util.Date;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.LOGI;
import static com.itracker.android.utils.LogUtils.LOGW;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class SensorMonitorReceiver extends WakefulBroadcastReceiver {

    public static final String TAG = makeLogTag(SensorMonitorReceiver.class);

    public static final int REQUEST_CODE = 100;

    private static final String ACTION_START_SENSOR_MONITOR = "com.itracker.android.intent.action.START_SENSOR_MONITOR";
    private static final String ACTION_BOOTSTRAP_MONITOR_ALARM = "com.itracker.android.intent.action.BOOTSTRAP_MONITOR_ALARM";

    public static Intent createBootstrapIntent(Context context) {
        return new Intent(ACTION_BOOTSTRAP_MONITOR_ALARM);
    }

    public static Intent createStartMonitorIntent(Context context) {
        Intent intent = new Intent(context, SensorMonitorReceiver.class);
        intent.setAction(ACTION_START_SENSOR_MONITOR);
        return intent;
    }

    public void setAlarm(Context context, long delayTimeMills) {
        Intent contentIntent = createStartMonitorIntent(Application.getInstance());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, contentIntent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pendingIntent);

        // set() and setRepeating() are now inexact by default if your android:targetSdkVersion is 19 or higher.
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarm.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayTimeMills,
                    Config.MONITORING_INTERVAL_IN_MILLS,
                    pendingIntent
            );
        } else {
            alarm.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayTimeMills,
                    pendingIntent
            );
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            LOGD(TAG, "Got ACTION_BOOT_COMPLETED");
            setAlarm(context, DateUtils.SECOND_IN_MILLIS);
        } else if (action.equals(ACTION_BOOTSTRAP_MONITOR_ALARM)) {
            LOGD(TAG, "Got ACTION_BOOTSTRAP_MONITOR_ALARM");
            setAlarm(context, DateUtils.SECOND_IN_MILLIS);
        } else if (action.equals(ACTION_START_SENSOR_MONITOR)) {
            LOGD(TAG, "Got ACTION_START_SENSOR_MONITOR");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setAlarm(context, DateUtils.MINUTE_IN_MILLIS);
            }
            startWakefulService(context, SensorMonitorService.createIntent(context));
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            LOGD(TAG, "Got CONNECTIVITY_ACTION");

            final FileDownloadManager fdm = FileDownloadManager.getInstance();
            switch (ConnectivityUtils.getNetworkType(context)) {
                case ConnectivityManager.TYPE_WIFI: {
                    fdm.startAvailableDownloadsAsync(true);
                    break;
                }
                case ConnectivityManager.TYPE_MOBILE: {
                    fdm.pauseAvailableDownloadsAsync(true);
                    break;
                }
                default: {
                    LOGW(TAG, "Unknown network type");
                }
            }
        }
    }
}
