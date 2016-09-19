package com.itracker.android.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.itracker.android.data.model.Backup;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class DataImportReceiver extends WakefulBroadcastReceiver {

    public static final String TAG = makeLogTag(DataImportReceiver.class);

    private static final String ACTION_IMPORT_DATA = "com.itracker.android.intent.action.IMPORT_DATA";

    public static Intent createImportDataIntent(Context context, String filePath, Backup pendingBackup) {
        Intent intent = new Intent(ACTION_IMPORT_DATA);
        Bundle extras = new Bundle();
        extras.putString(DataImportService.EXTRA_IMPORT_FILE_PATH, filePath);
        extras.putParcelable(DataImportService.EXTRA_IMPORT_BACKUP_INFO, pendingBackup);
        intent.putExtras(extras);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (ACTION_IMPORT_DATA.equals(action)) {
            LOGD(TAG, "Got ACTION_IMPORT_DATA");
            Intent serviceIntent = DataImportService.createDataImportIntent(context, intent.getExtras());
            startWakefulService(context, serviceIntent);
        }
    }
}
