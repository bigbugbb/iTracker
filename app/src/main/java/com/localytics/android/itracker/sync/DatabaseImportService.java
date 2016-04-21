package com.localytics.android.itracker.sync;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.localytics.android.itracker.provider.TrackerContract;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class DatabaseImportService extends Service {

    private static final String TAG = makeLogTag(DatabaseImportService.class);

    /*
     * Constants of message sent to player command handler.
     */
    static final int MSG_EXEC = 100;

    /*
     * Constants of intent action sent to the service.
     */
    public static final String INTENT_ACTION_IMPORT_DATA = "import_data";
    public static final String EXTRA_IMPORT_FILE_PATH = "import_file_path";
    public static final String EXTRA_IMPORT_DATA_CATEGORY = "import_data_category";

    private Handler mServiceHandler;
    private HandlerThread mHandlerThread;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Can't bind to DatabaseImportService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LOGD(TAG, "Starting DataImportService");

        mHandlerThread = new HandlerThread(TAG + "DatabaseImportService Thread");
        mHandlerThread.start();
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceHandler.sendMessage(mServiceHandler.obtainMessage(MSG_EXEC, intent));
        /**
         * This service restarts if it's killed by system while there is still unhandled intent.
         * onStartCommand is called when the service is explicitly opened or re-created
         * by the system. The intent is always non null for START_REDELIVER_INTENT.
         */
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        mHandlerThread.quitSafely();
        super.onDestroy();
    }

    private class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_EXEC) {
                execCommand((Intent) msg.obj);
            } else {
                LOGE(TAG, "Unknown command: " + msg.what);
            }
        }
    }

    /**
     * Executes command received by the service.
     *
     * @param intent received intent
     */
    void execCommand(Intent intent) {
        try {
            final String action = intent.getAction();

            if (INTENT_ACTION_IMPORT_DATA.equals(action)) {
                Bundle extras = intent.getExtras();
                String category = extras.getString(EXTRA_IMPORT_DATA_CATEGORY);
                String filePath = extras.getString(EXTRA_IMPORT_FILE_PATH);

                switch (category) {
                    case TrackerContract.DATA_CATEGORY_MOTION: {
                        importMotions(filePath, extras);
                        break;
                    }
                    case TrackerContract.DATA_CATEGORY_LOCATION: {
                        importLocations(filePath, extras);
                        break;
                    }
                    case TrackerContract.DATA_CATEGORY_ACTIVITY: {
                        importActivities(filePath, extras);
                        break;
                    }
                    default: {
                        throw new UnsupportedOperationException("Can't handle action " + action);
                    }
                }
            }
        } catch (Exception e) {
            LOGD(TAG, "Import data error: " + e.getMessage());
        }
    }

    public void importActivities(String filePath, Bundle extras) {

    }

    public void importLocations(String filePath, Bundle extras) {

    }

    public void importMotions(String filePath, Bundle extras) {

    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        // only available when the application is debuggable
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return;
        }

        // TODO: write something
        writer.flush();
    }
}
