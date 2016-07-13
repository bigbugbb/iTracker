package com.localytics.android.itracker.monitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;

import com.localytics.android.itracker.Config;

import java.util.Date;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class TrackerBroadcastReceiver extends WakefulBroadcastReceiver {

    public static final String TAG = makeLogTag(TrackerBroadcastReceiver.class);

    public static final int REQUEST_CODE = 100;

    public static final String ACTION_START_SENSOR_MONITOR = "com.localytics.android.itracker.intent.action.START_SENSOR_MONITOR";
    public static final String ACTION_BOOTSTRAP_MONITOR_ALARM = "com.localytics.android.itracker.intent.action.BOOTSTRAP_MONITOR_ALARM";

    public void setAlarm(Context context, long delayTimeMills) {
        Intent contentIntent = new Intent(context, TrackerBroadcastReceiver.class);
        contentIntent.setAction(ACTION_START_SENSOR_MONITOR);

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
        } else if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            LOGD(TAG, "Got ACTION_PACKAGE_CHANGED");
            int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
            String[] packageNames = context.getPackageManager().getPackagesForUid(uid);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    LOGI(TAG, "PackageReplaced: " + packageName);
                }
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            LOGD(TAG, "Got ACTION_PACKAGE_ADDED");
            int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
            String[] packageNames = context.getPackageManager().getPackagesForUid(uid);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    LOGI(TAG, "PackageAdded: " + packageName);
                }
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            LOGD(TAG, "Got ACTION_PACKAGE_REMOVED");
            int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
            String[] packageNames = context.getPackageManager().getPackagesForUid(uid);
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            if (packageNames != null) {
                for (String packageName : packageNames) {
                    LOGI(TAG, "PackageRemoved, replacing: " + isReplacing + " name: " + packageName);
                }
            }
        } else if (action.equals(ACTION_BOOTSTRAP_MONITOR_ALARM)) {
            LOGD(TAG, "Got ACTION_BOOTSTRAP_MONITOR_ALARM");
            setAlarm(context, DateUtils.SECOND_IN_MILLIS);
        } else if (action.equals(ACTION_START_SENSOR_MONITOR)) {
            LOGD(TAG, "Got ACTION_START_SENSOR_MONITOR");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setAlarm(context, DateUtils.MINUTE_IN_MILLIS);
            }
            Intent serviceIntent = new Intent(context, SensorMonitorService.class);
            serviceIntent.putExtra(SensorMonitorService.MONITORED_SENSORS, new int[]{ Sensor.TYPE_ACCELEROMETER });
            serviceIntent.putExtra(SensorMonitorService.MONITORED_SENSOR_ARGS + Sensor.TYPE_ACCELEROMETER, new Bundle());
            startWakefulService(context, serviceIntent);
        } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (incomingNumber == null) {
                incomingNumber = "Num not accessible";
            }
            // TelephonyManager.EXTRA_STATE_IDLE, EXTRA_STATE_OFFHOOK and EXTRA_STATE_RINGING
            LOGD(TAG, "Got ACTION_PHONE_STATE: " + phoneState + " Number: " + incomingNumber);
        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            LOGD(TAG, "Got ACTION_USER_PRESENT");
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            LOGD(TAG, "Got ACTION_SCREEN_OFF");
        } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            LOGD(TAG, "Got ACTION_SCREEN_ON");
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            LOGD(TAG, "Got ACTION_AIRPLANE_MODE_CHANGED: " + intent.getBooleanExtra("state", false));
        } else if (action.equals(Intent.ACTION_ANSWER)) {
            LOGD(TAG, "Got ACTION_ANSWER");
        } else if (action.equals(Intent.ACTION_CALL_BUTTON)) {
            LOGD(TAG, "Got ACTION_CALL_BUTTON");
        } else if (action.equals(Intent.ACTION_CAMERA_BUTTON)) {
            LOGD(TAG, "Got ACTION_CAMERA_BUTTON");
        } else if (action.equals(Intent.ACTION_DOCK_EVENT)) {
            LOGD(TAG, "Got ACTION_DOCK_EVENT: " + intent.getStringExtra(Intent.EXTRA_DOCK_STATE));
        } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
            LOGD(TAG, "Got ACTION_HEADSET_PLUG: " + intent.getStringExtra("name"));
        } else if (action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
            LOGD(TAG, "Got ACTION_MEDIA_BAD_REMOVAL");
        } else if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
            LOGD(TAG, "Got ACTION_MEDIA_REMOVED");
        } else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            LOGD(TAG, "Got ACTION_NEW_OUTGOING_CALL");
        } else if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            LOGD(TAG, "Got ACTION_POWER_CONNECTED");
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            LOGD(TAG, "Got ACTION_POWER_DISCONNECTED");
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
            LOGD(TAG, "Got ACTION_SHUTDOWN");
        } else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            String timeZone = intent.getStringExtra("time-zone");
            LOGD(TAG, "Got ACTION_TIMEZONE_CHANGED: " + timeZone);
        } else if (action.equals(Intent.ACTION_DATE_CHANGED)) {
            LOGD(TAG, "Got ACTION_TIME_CHANGED: " + new Date());
        } else if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {
            LOGD(TAG, "Got SMS_RECEIVED");
        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            LOGD(TAG, "Got ACTION_LOCALE_CHANGED");
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            LOGD(TAG, "Got CONNECTIVITY_ACTION");                                                                                                                                                                         LOGD(TAG, "Got CONNECTIVITY_ACTION");
        }
    }
}
