package com.localytics.android.itracker.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class DataImportReceiver extends WakefulBroadcastReceiver {

    public static final String TAG = makeLogTag(DataImportReceiver.class);

    public static final String ACTION_IMPORT_DATA = "com.localytics.android.itracker.intent.action.IMPORT_DATA";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (ACTION_IMPORT_DATA.equals(action)) {
            LOGD(TAG, "Got ACTION_IMPORT_DATA");
            Intent serviceIntent = new Intent(context, DataImportService.class);
            Bundle extras = intent.getExtras();
            serviceIntent.putExtras(extras);
            serviceIntent.setAction(DataImportService.INTENT_ACTION_IMPORT_DATA);
            startWakefulService(context, serviceIntent);
        }
    }
}
