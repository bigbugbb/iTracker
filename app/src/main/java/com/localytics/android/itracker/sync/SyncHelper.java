package com.localytics.android.itracker.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.util.SparseLongArray;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.data.model.Activity;
import com.localytics.android.itracker.data.model.Location;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.data.model.Track;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.LogUtils;
import com.localytics.android.itracker.util.PrefUtils;
import com.localytics.android.itracker.util.UIUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGW;

/**
 * A helper class for dealing with conference data synchronization.
 * All operations occur on the thread they're called from, so it's best to wrap
 * calls in an {@link android.os.AsyncTask}, or better yet, a
 * {@link android.app.Service}.
 */
public class SyncHelper {
    private static final String TAG = LogUtils.makeLogTag("SyncHelper");

    private Context mContext;
    private TrackerDataHandler mTrackerDataHandler;
    private RemoteTrackerDataFetcher mRemoteDataFetcher;

    TransferUtility mTransferUtility;

    public SyncHelper(Context context) {
        mContext = context;
        mTrackerDataHandler = new TrackerDataHandler(mContext);
        mRemoteDataFetcher = new RemoteTrackerDataFetcher(mContext);
    }

    public static void requestManualSync(Account chosenAccount) {
        if (chosenAccount != null) {
            LOGD(TAG, "Requesting manual sync for account " + chosenAccount.name);
            Bundle b = new Bundle();
            b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            ContentResolver.setSyncAutomatically(chosenAccount, TrackerContract.CONTENT_AUTHORITY, true);
            ContentResolver.setIsSyncable(chosenAccount, TrackerContract.CONTENT_AUTHORITY, 1);

            boolean pending = ContentResolver.isSyncPending(chosenAccount, TrackerContract.CONTENT_AUTHORITY);
            if (pending) {
                LOGD(TAG, "Warning: sync is PENDING. Will cancel.");
            }
            boolean active = ContentResolver.isSyncActive(chosenAccount, TrackerContract.CONTENT_AUTHORITY);
            if (active) {
                LOGD(TAG, "Warning: sync is ACTIVE. Will cancel.");
            }

            if (pending || active) {
                LOGD(TAG, "Cancelling previously pending/active sync.");
                ContentResolver.cancelSync(chosenAccount, TrackerContract.CONTENT_AUTHORITY);
            }

            LOGD(TAG, "Requesting sync now.");
            ContentResolver.requestSync(chosenAccount, TrackerContract.CONTENT_AUTHORITY, b);
        } else {
            LOGD(TAG, "Can't request manual sync -- no chosen account.");
        }
    }

    /**
     * Attempts to perform conference data synchronization. The data comes from the remote URL
     * configured in {@link com.localytics.android.itracker.Config#MANIFEST_URL}. The remote URL
     * must point to a manifest file that, in turn, can reference other files. For more details
     * about conference data synchronization, refer to the documentation at
     * http://code.google.com/p/iosched.
     *
     * @param syncResult (optional) the sync result object to update with statistics.
     * @param account    the account associated with this sync
     * @return Whether or not the synchronization made any changes to the data.
     */
    public boolean performSync(SyncResult syncResult, Account account, Bundle extras) {
        boolean dataChanged = false;

        // Get auth token and use it for each data request
        final String authToken = AccountUtils.getAuthToken(mContext);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
            mContext,
            Config.COGNITO_IDENTITY_POOL_ID,
            Regions.US_EAST_1 // Region
        );

        // Initialize s3 client and the transfer utility
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        mTransferUtility = new TransferUtility(s3, mContext);

        long lastAttemptTime = PrefUtils.getLastSyncAttemptedTime(mContext);
        long now = UIUtils.getCurrentTime(mContext);
        long timeSinceAttempt = now - lastAttemptTime;
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

        if (!manualSync && timeSinceAttempt >= 0 && timeSinceAttempt < Config.MIN_INTERVAL_BETWEEN_SYNCS) {
            /*
            Code removed because it was causing a runaway sync; probably because we are setting
            syncResult.delayUntil incorrectly.

            Random r = new Random();
            long toWait = 10000 + r.nextInt(30000) // random jitter between 10 - 40 seconds
                    + Config.MIN_INTERVAL_BETWEEN_SYNCS - timeSinceAttempt;
            LOGW(TAG, "Sync throttled!! Another sync was attempted just " + timeSinceAttempt
                    + "ms ago. Requesting delay of " + toWait + "ms.");
            syncResult.fullSyncRequested = true;
            syncResult.delayUntil = (System.currentTimeMillis() + toWait) / 1000L;
            return false;*/
        }

        LogUtils.LOGI(TAG, "Performing sync for account: " + account);
        PrefUtils.markSyncAttemptedNow(mContext);
        long opStart;
        long remoteSyncDuration, choresDuration;

        opStart = System.currentTimeMillis();

        // remote sync consists of these operations, which we try one by one (and tolerate individual failures on each)
        final int OP_USER_DATA_SYNC  = 0;
        final int OP_TRACK_DATA_SYNC = 1;
        int[] opsToPerform = new int[] { OP_TRACK_DATA_SYNC };

        for (int op : opsToPerform) {
            try {
                switch (op) {
                    case OP_USER_DATA_SYNC:
                        break;
                    case OP_TRACK_DATA_SYNC:
                        dataChanged |= doTrackDataSync(authToken);
                        break;
//                    case OP_USER_FEEDBACK_SYNC:
//                        doUserFeedbackSync();
//                        break;
                }
            } catch (AuthException ex) {
                syncResult.stats.numAuthExceptions++;

                // if we have a token, try to refresh it
                if (AccountUtils.hasToken(mContext)) {
                    AccountUtils.refreshAuthToken(mContext);
                } else {
                    LOGW(TAG, "No auth token yet for this account. Skipping remote sync.");
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing remote sync.");
                increaseIoExceptions(syncResult);
            }
        }
        remoteSyncDuration = System.currentTimeMillis() - opStart;

        // If data has changed, there are a few chores we have to do
        opStart = System.currentTimeMillis();
        if (dataChanged) {
            try {
                performPostSyncChores(mContext);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing post sync chores.");
            }
        }
        choresDuration = System.currentTimeMillis() - opStart;

        int operations = mTrackerDataHandler.getContentProviderOperationsDone();
        if (syncResult != null && syncResult.stats != null) {
            syncResult.stats.numEntries += operations;
            syncResult.stats.numUpdates += operations;
        }

        if (dataChanged) {
            long totalDuration = choresDuration + remoteSyncDuration;
            LOGD(TAG, "SYNC STATS:\n" +
                    " *  Account synced: " + (account == null ? "null" : account.name) + "\n" +
                    " *  Content provider operations: " + operations + "\n" +
                    " *  Remote sync took: " + remoteSyncDuration + "ms\n" +
                    " *  Post-sync chores took: " + choresDuration + "ms\n" +
                    " *  Total time: " + totalDuration + "ms\n" +
                    " *  Total data read from cache: \n" +
                    (mRemoteDataFetcher.getTotalBytesReadFromCache() / 1024) + "kB\n" +
                    " *  Total data downloaded: \n" +
                    (mRemoteDataFetcher.getTotalBytesDownloaded() / 1024) + "kB");
        }

        LogUtils.LOGI(TAG, "End of sync (" + (dataChanged ? "data changed" : "no data change") + ")");

        updateSyncInterval(mContext, account);

        return dataChanged;
    }

    public static void performPostSyncChores(final Context context) {
        // Update search index
        LOGD(TAG, "Updating search index.");
//        context.getContentResolver().update(TrackerContract.SearchIndex.CONTENT_URI,
//                new ContentValues(), null, null);

        // Sync calendars
        LOGD(TAG, "Session data changed. Syncing starred sessions with Calendar.");
        syncCalendar(context);
    }

    private static void syncCalendar(Context context) {
//        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
//        intent.setClass(context, SessionCalendarService.class);
//        context.startService(intent);
    }

    private void doUserFeedbackSync() {
        LOGD(TAG, "Syncing feedback");
        new FeedbackSyncHelper(mContext).sync();
    }

    private boolean doUserDataSync() throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }
        return true;
    }

    /**
     * Checks if the remote server has new data that we need to import. If so, download
     * the new data and import it into the database.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem downloading or importing the data.
     */
    private boolean doTrackDataSync(@NonNull final String authToken) throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }

        if (Config.WIFI_ONLY_SYNC_ENABLED && !isUsingWifi()) {
            LOGD(TAG, "Not attempting remote sync because wifi-only sync is enabled but the device is not connected to WIFI");
            return false;
        }

        LOGD(TAG, "Starting track data sync.");

        // Sync activities
        syncData(TrackerContract.Activities.CONTENT_URI, new OnDataSyncingListener() {
            @Override
            public void onDataSyncing(Cursor cursor) {
                // NOTE: 1. at least one hour delay, but it's simple to implement
                //       2. the transaction should be based by hour

//                ContentProviderOperation

                List<Activity> data = new ArrayList<>(30);
                if (cursor.moveToFirst()) {
                    long nextTime, prevTime = 0;
                    do {
                        final Activity activity = new Activity(cursor);
                        nextTime = activity.time - activity.time % DateUtils.HOUR_IN_MILLIS;
                        if (nextTime - prevTime == DateUtils.HOUR_IN_MILLIS) {
                            // TODO: Write the previous hour's data into a temp file
                            
                        }
                        data.add(activity);
                        prevTime = nextTime;
                    } while (cursor.moveToNext());
                }


                // TODO: clear dirty column of the data for this hour

                // TODO: upload the temp file for this hour

                // TODO: get s3 url of the file just uploaded

                // TODO: update the url list with the s3 url
            }
        });

        // Sync locations
        syncData(TrackerContract.Locations.CONTENT_URI, new OnDataSyncingListener() {
            @Override
            public void onDataSyncing(Cursor cursor) {

            }
        });

        // Sync motions
        syncData(TrackerContract.Motions.CONTENT_URI, new OnDataSyncingListener() {
            @Override
            public void onDataSyncing(Cursor cursor) {

            }
        });

        // TODO: upload the payload with the track data and the track url list (dirty)

        return true;
    }

    private void syncData(@NonNull Uri uri, OnDataSyncingListener listener) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    uri,
                    null,
                    TrackerContract.SELECTION_BY_DIRTY,
                    new String[]{ Integer.toString(1) },
                    TrackerContract.BaseDataColumns.TIME + " ASC");
            if (cursor != null) {
                if (listener != null) {
                    listener.onDataSyncing(cursor);
                }
            }
        } catch (Exception e) {
            LOGE(TAG, "Error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Returns whether we are connected to the internet.
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private boolean isUsingWifi() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    private void increaseIoExceptions(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numIoExceptions;
        }
    }

    private void increaseSuccesses(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numEntries;
            ++syncResult.stats.numUpdates;
        }
    }

    public static class AuthException extends RuntimeException {
    }

    public static long calculateRecommendedSyncInterval(final Context context) {
        // TODO: dynamically sync interval change
        return DateUtils.HOUR_IN_MILLIS * 6;
    }

    public static void updateSyncInterval(final Context context, final Account account) {
        LOGD(TAG, "Checking sync interval for " + account);
        long recommended = calculateRecommendedSyncInterval(context);
        long current = PrefUtils.getCurSyncInterval(context);
        LOGD(TAG, "Recommended sync interval " + recommended + ", current " + current);
        if (recommended != current) {
            LOGD(TAG, "Setting up sync for account " + account + ", interval " + recommended + "ms");
            ContentResolver.setIsSyncable(account, TrackerContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, TrackerContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, TrackerContract.CONTENT_AUTHORITY, new Bundle(), recommended / DateUtils.SECOND_IN_MILLIS);
            PrefUtils.setCurSyncInterval(context, recommended);
        } else {
            LOGD(TAG, "No need to update sync interval.");
        }
    }

    private interface OnDataSyncingListener {
        void onDataSyncing(Cursor cursor);
    }
}