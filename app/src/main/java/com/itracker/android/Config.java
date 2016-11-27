package com.itracker.android;

import android.os.StrictMode;
import android.text.format.DateUtils;

import com.itracker.android.ui.activity.TrackerActivity;
import com.itracker.android.utils.ExternalStorageUtils;
import com.itracker.android.utils.SdkVersionUtils;

import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.Format;

/**
 * Created by bigbug on 10/3/15.
 */
public class Config {

    public static final String MANIFEST_FORMAT = "itracker-api-json-v1";
    public static final String API_HEADER_ACCEPT = "application/vnd.itracker.v1";

    // Minimum interval between two consecutive syncs. This is a safety mechanism to throttle
    // syncs in case conference data gets updated too often or something else goes wrong that
    // causes repeated syncs.
    public static final int DEFAULT_MIN_INTERVAL_BETWEEN_SYNCS = 5; // in minute
    public static final int DEFAULT_RESTORED_BACKUP_DATA_ITEMS_PER_SYNC = 500;
    public static final long TRACK_DATA_UPLOAD_TIMEOUT = 10 * DateUtils.MINUTE_IN_MILLIS;

    public static final long MONITORING_INTERVAL_IN_MILLS = 1000 * 60;
    public static final int MONITORING_DURATION_IN_SECONDS = 10;
    public static final long MONITORING_DURATION_IN_MICROS = 1000 * 1000 * MONITORING_DURATION_IN_SECONDS;

    // Data saver config
    public static final Format TRACKER_FILENAME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HHZZ");

    // Values for the EventPoint feedback API. Sync happens at the same time as schedule sync,
    // and before that values are stored locally in the database.

    public static final String FEEDBACK_API_CODE = "";
    public static final String FEEDBACK_URL = "";
    public static final String FEEDBACK_API_KEY = "";
    public static final String FEEDBACK_DUMMY_REGISTRANT_ID = "";
    public static final String FEEDBACK_SURVEY_ID = "";

    // Sensor data related constants
    public static final int ACCELEROMETER_DATA_AMPLIFIER = 50;
    public static final int ACCELEROMETER_SENSOR_MAX_MAGNITUDE = 20;
    public static final int ACCELEROMETER_DATA_MAX_MAGNITUDE = ACCELEROMETER_SENSOR_MAX_MAGNITUDE * ACCELEROMETER_DATA_AMPLIFIER;

    // Data request intervals
    public static final long LOCATION_REQUEST_INTERVAL = DateUtils.MINUTE_IN_MILLIS * 2;
    public static final long ACTIVITY_REQUEST_INTERVAL = DateUtils.MINUTE_IN_MILLIS * 1;

    // Default days back from today
    public static final int DEFAULT_DAYS_BACK_FROM_TODAY = 90;

    // Data sync configurations
    public static final boolean WIFI_ONLY_SYNC_ENABLED = true;

    // S3 Bucket name
    public static final String S3_BUCKET_NAME = "itracker-track-data";

    // S3 Key prefix pattern
    public static final DateTimeFormatter S3_KEY_PREFIX_PATTERN = DateTimeFormat.forPattern("yyyy/MM/dd/HH");

    // YouTube upload playlist id
    public static final String YOUTUBE_UPLOAD_PLAYLIST = "WL";

    // Default max file download tasks
    public static final int DEFAULT_MAX_FILE_DOWNLOAD_TASKS = 3;

    public static final String FILE_DOWNLOAD_DIR_PATH = ExternalStorageUtils.getSdCardPath() + "/iTracker/downloads/";

    public static void enableStrictMode() {
        StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                new StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog();
        StrictMode.VmPolicy.Builder vmPolicyBuilder =
                new StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog();

        if (SdkVersionUtils.hasHoneycomb()) {
            threadPolicyBuilder.penaltyFlashScreen();
            vmPolicyBuilder.setClassInstanceLimit(TrackerActivity.class, 1);
        }
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }
}