package com.localytics.android.itracker.sync;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.localytics.android.itracker.provider.TrackerContract;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

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
    public static final String INTENT_ACTION_IMPORT_LINKS      = "import_links";
    public static final String INTENT_ACTION_IMPORT_ACTIVITIES = "import_activities";
    public static final String INTENT_ACTION_IMPORT_LOCATIONS  = "import_locations";
    public static final String INTENT_ACTION_IMPORT_MOTIONS    = "import_motions";

    public static final String EXTRA_CONTENT_PROVIDER_OPERATIONS = "extra_content_provider_ops";

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
            switch (intent.getAction()) {
                case INTENT_ACTION_IMPORT_LINKS:
                    ArrayList<ContentProviderOperation> ops = intent.getParcelableArrayListExtra(EXTRA_CONTENT_PROVIDER_OPERATIONS);
                    if (ops != null) {
                        getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                    }
                    break;
                case INTENT_ACTION_IMPORT_ACTIVITIES:
                    break;
                case INTENT_ACTION_IMPORT_LOCATIONS:
                    break;
                case INTENT_ACTION_IMPORT_MOTIONS:
                    break;
            }
        } catch (Exception e) {
            LOGD(TAG, "Import data error: " + e.getMessage());
        }
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