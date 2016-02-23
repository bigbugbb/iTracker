package com.localytics.android.itracker;

import android.text.format.DateUtils;

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.Format;

/**
 * Created by bigbug on 10/3/15.
 */
public class Config {

    // Manifest URL override for Debug (staging) builds:
    public static final String URL_BASE = "https://itracker-api-bigbugbb.c9users.io";
    public static final String MANIFEST_URL = URL_BASE + "/api";
    public static final String USERS_URL = URL_BASE + "/api/users";
    public static final String SESSIONS_URL = URL_BASE + "/api/sessions";

    public static final String MANIFEST_FORMAT = "itracker-api-json-v1";

    // GCM config
    public static final String GCM_SERVER_URL = MANIFEST_URL + "/push_regs";
    public static final String GCM_SENDER_ID = "476535703027";
    public static final String GCM_API_KEY = "AIzaSyCybHo5-n3EpJb2RIVTVzCC2TxhTMRuVcY";

    // Minimum interval between two consecutive syncs. This is a safety mechanism to throttle
    // syncs in case conference data gets updated too often or something else goes wrong that
    // causes repeated syncs.
    public static final long MIN_INTERVAL_BETWEEN_SYNCS = 10 * DateUtils.MINUTE_IN_MILLIS;

    public static final long MONITORING_INTERVAL_IN_MILLS = 1000 * 60;
    public static final int MONITORING_DURATION_IN_SECONDS = 10;
    public static final long MONITORING_DURATION_IN_MICROS = 1000 * 1000 * MONITORING_DURATION_IN_SECONDS;

    // Data saver config
    public static final boolean USE_EXTERNAL_DIRECTORY = true;
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
}