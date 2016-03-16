package com.localytics.android.itracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utilities and constants related to app preferences.
 */
public class PrefUtils {

    /**
     * Boolean preference that when checked, indicates that the user has completed account
     * authentication and the initial set up flow.
     */
    public static final String PREF_SETUP_DONE = "_pref_setup_done";

    public static final String PREF_FIRST_USAGE = "_pref_first_usage";

    public static final String PREF_ALARM_SETUP_DONE = "_pref_alarm_setup_done";

    /**
     * Boolean preference that indicates whether we installed the boostrap data or not.
     */
    public static final String PREF_DATA_BOOTSTRAP_DONE = "_pref_data_bootstrap_done";

    /** Long indicating when a sync was last ATTEMPTED (not necessarily succeeded) */
    public static final String PREF_LAST_SYNC_ATTEMPTED = "_pref_last_sync_attempted";

    /** Long indicating when a sync last SUCCEEDED */
    public static final String PREF_LAST_SYNC_SUCCEEDED = "_pref_last_sync_succeeded";

    /** Sync interval that's currently configured */
    public static final String PREF_CUR_SYNC_INTERVAL = "_pref_cur_sync_interval";

    public static final String PREF_SENT_TOKEN_TO_SERVER = "_pref_sent_token_to_server";

    /** Boolean preference that indicates whether we enabled the sdk test mode or not. */
    public static final String PREF_SDK_TEST_MODE_ENABLED = "sdk_test_mode_enabled";

    public static final String PREF_HARDWARE_DECODING = "_pref_hardware_decoding";

    /** Long indicating when the date range filter is set */
    public static final String PREF_LAST_DATE_RANGE_UPDATED = "_pref_last_date_range_updated";

    public static void init(final Context context) {}

    public static void markNotFirstUsage(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_FIRST_USAGE, false).apply();
    }

    public static boolean isFirstUsage(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_FIRST_USAGE, true);
    }

    public static void markSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SETUP_DONE, true).apply();
    }

    public static boolean isSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

//        // Check what year we're configured for
//        int conferenceYear = sp.getInt(PREF_CONFERENCE_YEAR, 0);
//        if (conferenceYear != Config.CONFERENCE_YEAR) {
//            // Application is configured for a different conference year. Reset
//            // preferences and re-run setup.
//            sp.edit().clear().putInt(PREF_CONFERENCE_YEAR, Config.CONFERENCE_YEAR).commit();
//        }

        return sp.getBoolean(PREF_SETUP_DONE, false);
    }

    public static void markAlarmSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ALARM_SETUP_DONE, true).apply();
    }

    public static boolean isAlarmSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ALARM_SETUP_DONE, false);
    }

    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DATA_BOOTSTRAP_DONE, true).apply();
    }

    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DATA_BOOTSTRAP_DONE, false);
    }

    public static long getLastSyncAttemptedTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_ATTEMPTED, 0L);
    }

    public static void markSyncAttemptedNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_ATTEMPTED, System.currentTimeMillis()).apply();
    }

    public static long getLastSyncSucceededTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_SUCCEEDED, 0L);
    }

    public static void markSyncSucceededNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_SUCCEEDED, System.currentTimeMillis()).apply();
    }

    public static long getCurSyncInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CUR_SYNC_INTERVAL, 0L);
    }

    public static void setCurSyncInterval(final Context context, long interval) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CUR_SYNC_INTERVAL, interval).apply();
    }

    public static boolean hasSentTokenToServer(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SENT_TOKEN_TO_SERVER, false);
    }

    public static void setSentTokenToServer(final Context context, boolean sent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SENT_TOKEN_TO_SERVER, sent).apply();
    }

    public static boolean isSdkTestModeEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SDK_TEST_MODE_ENABLED, false);
    }

    public static void enableSdkTestMode(final Context context, boolean enabled) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SDK_TEST_MODE_ENABLED, enabled).apply();
    }

    public static boolean isHardwareDecoding(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_HARDWARE_DECODING, true);
    }

    public static void enableHardwareDecoding(final Context context, boolean enabled) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_HARDWARE_DECODING, enabled).apply();
    }

    public static long getLastDateRangeUpdateTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_DATE_RANGE_UPDATED, 0L);
    }

    public static void setLastDateRangeUpdateTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_DATE_RANGE_UPDATED, System.currentTimeMillis()).apply();
    }

    public static void registerOnSharedPreferenceChangeListener(final Context context,
                                                                SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(final Context context,
                                                                  SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }
}