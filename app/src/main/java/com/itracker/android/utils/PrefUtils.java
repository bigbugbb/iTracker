package com.itracker.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.itracker.android.Config;
import com.itracker.android.R;

/**
 * Utilities and constants related to app app_preferences.
 */
public class PrefUtils {

    /**
     * Boolean preference that when checked, indicates that the user has completed account
     * authentication and the initial set up flow.
     */
    public static final String PREF_FIRST_USAGE = "_pref_first_usage";

    /**
     * Boolean preference that indicates whether we installed the boostrap data or not.
     */
    public static final String PREF_LAST_SELECTED_TAB = "_pref_last_selected_tab";

    public static final String PREF_UPDATE_VCARD_DONE = "_pref_update_vcard_done";

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

    /** String indicating the chosen google account name for youtube data */
    public static final String PREF_CHOSEN_GOOGLE_ACCOUNT_NAME = "_pref_chosen_google_account_name";

    public static final String PREF_CURRENT_DOWNLOAD_FILE_SIZE = "_pref_current_download_file_size_";

    public static final String PREF_CURRENT_DOWNLOAD_SPEED = "_pref_current_download_speed_";

    public static void init(final Context context) {}

    public static int getLastSelectedTab(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_LAST_SELECTED_TAB, 0);
    }

    public static void setLastSelectedTab(final Context context, int tab) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PREF_LAST_SELECTED_TAB, tab).apply();
    }

    public static boolean isVCardUpdated(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_UPDATE_VCARD_DONE, false);
    }

    public static void markVCardUpdated(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_UPDATE_VCARD_DONE, true).apply();
    }

    public static long getLastSyncAttemptedTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_ATTEMPTED, 0L);
    }

    public static void markSyncAttemptedNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_ATTEMPTED, System.currentTimeMillis()).apply();
    }

    public static long getCurSyncInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CUR_SYNC_INTERVAL, 0L);
    }

    public static void setCurSyncInterval(final Context context, long interval) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CUR_SYNC_INTERVAL, interval).apply();
    }

    public static boolean hasSentPushTokenToServer(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SENT_TOKEN_TO_SERVER, false);
    }

    public static void setSentPushTokenToServer(final Context context, boolean sent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SENT_TOKEN_TO_SERVER, sent).apply();
    }

    public static long getLastDateRangeUpdateTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_DATE_RANGE_UPDATED, 0L);
    }

    public static void setLastDateRangeUpdateTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_DATE_RANGE_UPDATED, System.currentTimeMillis()).apply();
    }

    public static String getChosenGoogleAccountName(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_CHOSEN_GOOGLE_ACCOUNT_NAME, "");
    }

    public static void setChosenGoogleAccountName(final Context context, String chosenAccountName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_CHOSEN_GOOGLE_ACCOUNT_NAME, chosenAccountName).apply();
    }

    public static int getMaxFileDownloadTasks(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.max_download_tasks_key), Config.DEFAULT_MAX_FILE_DOWNLOAD_TASKS);
    }

    public static long getMinIntervalBetweenDataSync(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int minute = sp.getInt(context.getString(R.string.data_sync_interval_time_key), Config.DEFAULT_MIN_INTERVAL_BETWEEN_SYNCS);
        return minute * DateUtils.MINUTE_IN_MILLIS;
    }

    public static int getMaxRestoredDataItemsPerSync(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.restored_data_items_per_sync_key), Config.DEFAULT_RESTORED_BACKUP_DATA_ITEMS_PER_SYNC);
    }

    public static boolean isNotificationMakeVibration(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.notification_vibrate_key), true);
    }

    public static boolean isNotificationMakeSound(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(context.getString(R.string.notification_sound_key), true);
    }

    public static long getCurrentDownloadFileSize(final Context context, final String downloadId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CURRENT_DOWNLOAD_FILE_SIZE + downloadId, 0);
    }

    public static void setCurrentDownloadFileSize(final Context context, final String downloadId, long currentFileSize) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CURRENT_DOWNLOAD_FILE_SIZE + downloadId, currentFileSize).apply();
    }

    public static long getCurrentDownloadSpeed(final Context context, final String downloadId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CURRENT_DOWNLOAD_SPEED + downloadId, 0);
    }

    public static void setCurrentDownloadSpeed(final Context context, final String downloadId, long currentDownloadSpeed) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CURRENT_DOWNLOAD_SPEED + downloadId, currentDownloadSpeed).apply();
    }
}