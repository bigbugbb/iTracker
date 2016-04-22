package com.localytics.android.itracker.sync;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import com.localytics.android.itracker.data.model.Backup;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.DataFileUtils;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class DataImportService extends Service {

    private static final String TAG = makeLogTag(DataImportService.class);

    /*
     * Constants of message sent to player command handler.
     */
    static final int MSG_EXEC = 100;

    /*
     * Constants of intent action sent to the service.
     */
    public static final String INTENT_ACTION_IMPORT_DATA = "import_data";
    public static final String EXTRA_IMPORT_FILE_PATH = "import_file_path";
    public static final String EXTRA_IMPORT_BACKUP_INFO = "import_backup_info";

    private static final DateTimeFormatter sFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private Handler mServiceHandler;
    private HandlerThread mHandlerThread;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Can't bind to DataImportService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LOGD(TAG, "Starting DataImportService");

        mHandlerThread = new HandlerThread(TAG + "DataImportService Thread");
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

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Intent intent = (Intent) msg.obj;

            if (msg.what == MSG_EXEC) {
                execCommand(intent);
            } else {
                LOGE(TAG, "Unknown command: " + msg.what);
            }

            DataImportReceiver.completeWakefulIntent(intent);
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
                String filePath = extras.getString(EXTRA_IMPORT_FILE_PATH);
                Backup backup = extras.getParcelable(EXTRA_IMPORT_BACKUP_INFO);

                switch (backup.category) {
                    case TrackerContract.DATA_CATEGORY_MOTION: {
                        importMotions(filePath, backup, extras);
                        break;
                    }
                    case TrackerContract.DATA_CATEGORY_LOCATION: {
                        importLocations(filePath, backup, extras);
                        break;
                    }
                    case TrackerContract.DATA_CATEGORY_ACTIVITY: {
                        importActivities(filePath, backup, extras);
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

    public void importActivities(String filePath, Backup backup, Bundle extras) {
        DateTime datetime = sFormatter.parseDateTime(backup.date);
        long trackId = TrackerContract.Tracks.getTrackIdOfDateTime(getApplicationContext(), datetime);

        if (trackId == -1) {
            LOGE(TAG, String.format("Failed to get track id to import activity data on %s:%s", backup.date, backup.hour));
            return;
        }

        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(TrackerContract.Activities.CONTENT_URI);

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(unzipFile(filePath)), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER);

            List<String[]> lines = reader.readAll();
            ContentValues[] values = new ContentValues[lines.size()];
            long now = DateTime.now().getMillis();
            for (int i = 0; i < lines.size(); ++i) {
                final String[] line = lines.get(i);
                ContentValues cv = new ContentValues();
                cv.put(TrackerContract.Activities.TIME, Long.parseLong(line[0]));
                cv.put(TrackerContract.Activities.TYPE, line[1]);
                cv.put(TrackerContract.Activities.TYPE_ID, Integer.parseInt(line[2]));
                cv.put(TrackerContract.Activities.CONFIDENCE, Integer.parseInt(line[3]));
                cv.put(TrackerContract.Activities.TRACK_ID, trackId);
                cv.put(TrackerContract.SyncColumns.UPDATED, now);
                values[i] = cv;
            }

            if (getContentResolver().bulkInsert(uri, values) == lines.size()) {
                LOGI(TAG, String.format("Succeed to import %s data of %s:%s", backup.category, backup.date, backup.hour));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void importLocations(String filePath, Backup backup, Bundle extras) {
        DateTime datetime = sFormatter.parseDateTime(backup.date);
        long trackId = TrackerContract.Tracks.getTrackIdOfDateTime(getApplicationContext(), datetime);

        if (trackId == -1) {
            LOGE(TAG, String.format("Failed to get track id to import location data on %s:%s", backup.date, backup.hour));
            return;
        }

        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(TrackerContract.Locations.CONTENT_URI);

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(unzipFile(filePath)), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER);

            List<String[]> lines = reader.readAll();
            ContentValues[] values = new ContentValues[lines.size()];
            long now = DateTime.now().getMillis();
            for (int i = 0; i < lines.size(); ++i) {
                final String[] line = lines.get(i);
                ContentValues cv = new ContentValues();
                cv.put(TrackerContract.Locations.TIME, Long.parseLong(line[0]));
                cv.put(TrackerContract.Locations.LATITUDE, Float.parseFloat(line[1]));
                cv.put(TrackerContract.Locations.LONGITUDE, Float.parseFloat(line[2]));
                cv.put(TrackerContract.Locations.ALTITUDE, Float.parseFloat(line[3]));
                cv.put(TrackerContract.Locations.ACCURACY, Float.parseFloat(line[4]));
                cv.put(TrackerContract.Locations.SPEED, Float.parseFloat(line[5]));
                cv.put(TrackerContract.Locations.TRACK_ID, trackId);
                cv.put(TrackerContract.SyncColumns.UPDATED, now);
                values[i] = cv;
            }

            if (getContentResolver().bulkInsert(uri, values) == lines.size()) {
                LOGI(TAG, String.format("Succeed to import %s data of %s:%s", backup.category, backup.date, backup.hour));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void importMotions(@NonNull String filePath, Backup backup, Bundle extras) {
        DateTime datetime = sFormatter.parseDateTime(backup.date);
        long trackId = TrackerContract.Tracks.getTrackIdOfDateTime(getApplicationContext(), datetime);

        if (trackId == -1) {
            LOGE(TAG, String.format("Failed to get track id to import motion data on %s:%s", backup.date, backup.hour));
            return;
        }

        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(TrackerContract.Motions.CONTENT_URI);

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(unzipFile(filePath)), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER);

            List<String[]> lines = reader.readAll();
            ContentValues[] values = new ContentValues[lines.size()];
            long now = DateTime.now().getMillis();
            for (int i = 0; i < lines.size(); ++i) {
                final String[] line = lines.get(i);
                ContentValues cv = new ContentValues();
                cv.put(TrackerContract.Motions.TIME, Long.parseLong(line[0]));
                cv.put(TrackerContract.Motions.DATA, Integer.parseInt(line[1]));
                cv.put(TrackerContract.Motions.SAMPLING, Integer.parseInt(line[2]));
                cv.put(TrackerContract.Motions.TRACK_ID, trackId);
                cv.put(TrackerContract.SyncColumns.UPDATED, now);
                values[i] = cv;
            }

            if (getContentResolver().bulkInsert(uri, values) == lines.size()) {
                LOGI(TAG, String.format("Succeed to import %s data of %s:%s", backup.category, backup.date, backup.hour));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File unzipFile(@NonNull String filePath) throws IOException {
        String zipFilePath = filePath;
        String dstFilePath = filePath.substring(0, filePath.lastIndexOf('.'));
        DataFileUtils.unzip(zipFilePath, dstFilePath);
        File dstFile = new File(dstFilePath);
        if (dstFile.exists()) {
            LOGD(TAG, "Start import file: " + dstFile);
        } else {
            throw new IOException("Failed to get the file to import.");
        }
        return dstFile;
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
