package com.localytics.android.itracker.sync;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.data.model.Activity;
import com.localytics.android.itracker.data.model.BaseData;
import com.localytics.android.itracker.data.model.Location;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Activities;
import com.localytics.android.itracker.provider.TrackerContract.Links;
import com.localytics.android.itracker.provider.TrackerContract.Locations;
import com.localytics.android.itracker.provider.TrackerContract.Motions;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.DeviceUtils;
import com.localytics.android.itracker.util.LogUtils;
import com.localytics.android.itracker.util.PrefUtils;
import com.opencsv.CSVWriter;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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

    private AmazonS3Client mS3Client;
    private TransferUtility mTransferUtility;

    private String mAuthToken;
    private String mAccountName;

    private final ArrayList<ContentProviderOperation> mOps = new ArrayList<>();

    private final static String ACTIVITY_TYPE_STR = "activity_";

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
        mAuthToken = AccountUtils.getAuthToken(mContext);
        mAccountName = AccountUtils.getActiveAccountName(mContext);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
            mContext,
            Config.COGNITO_IDENTITY_POOL_ID,
            Regions.US_EAST_1 // Region
        );

        // Initialize s3 client and the transfer utility
        mS3Client = new AmazonS3Client(credentialsProvider);
        mTransferUtility = new TransferUtility(mS3Client, mContext);

        long lastAttemptTime = PrefUtils.getLastSyncAttemptedTime(mContext);
        long now = System.currentTimeMillis();
        long timeSinceAttempt = now - lastAttemptTime;
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

        if (!manualSync && timeSinceAttempt >= 0 && timeSinceAttempt < Config.MIN_INTERVAL_BETWEEN_SYNCS) {
//            Random r = new Random();
//            long toWait = 10000 + r.nextInt(30000) // random jitter between 10 - 40 seconds
//                    + Config.MIN_INTERVAL_BETWEEN_SYNCS - timeSinceAttempt;
//            LOGW(TAG, "Sync throttled!! Another sync was attempted just " + timeSinceAttempt
//                    + "ms ago. Requesting delay of " + toWait + "ms.");
//            syncResult.delayUntil = (System.currentTimeMillis() + toWait) / 1000L;
            return false;
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
                        dataChanged |= doTrackerDataSync();
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
    private boolean doTrackerDataSync() throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }

        if (Config.WIFI_ONLY_SYNC_ENABLED && !isUsingWifi()) {
            LOGD(TAG, "Not attempting remote sync because wifi-only sync is enabled but the device is not connected to WIFI");
            return false;
        }

        LOGD(TAG, "Starting track data sync.");

        syncData(Activities.CONTENT_URI, mDataSyncingListener);
        syncData(Locations.CONTENT_URI, mDataSyncingListener);
        syncData(Motions.CONTENT_URI, mDataSyncingListener);

        // TODO: upload the payload with the track data and the track url list (dirty) to the app server

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
                    TrackerContract.ORDER_BY_TIME_ASC);
            if (cursor != null && listener != null) {
                listener.onDataSyncing(uri, cursor);
            }
        } catch (Exception e) {
            LOGE(TAG, "Error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private OnDataSyncingListener mDataSyncingListener = new OnDataSyncingListener() {
        @Override
        public void onDataSyncing(final Uri uri, Cursor cursor) throws IOException {
            if (cursor.moveToFirst()) {
                long nextTime, prevTime = 0, trackId = -1;
                final List<BaseData> data = new LinkedList<>();

                do {
                    final BaseData item = dataItemFromCursor(uri, cursor);
                    nextTime = item.time - item.time % DateUtils.HOUR_IN_MILLIS;
                    if (trackId != -1 && prevTime != 0 && nextTime - prevTime >= DateUtils.HOUR_IN_MILLIS) {
                        final File file = makeDataFileForUpload(uri, getLocalFilename(uri, prevTime), data);
                        uploadDataFileForTimeRange(uri, file, prevTime, nextTime, trackId);
                        data.clear();
                    }
                    data.add(item);
                    prevTime = nextTime;
                    trackId = item.track_id;
                } while (cursor.moveToNext());
            }
        }
    };

    private BaseData dataItemFromCursor(@NonNull Uri uri, @NonNull Cursor cursor) {
        if (uri == Activities.CONTENT_URI) {
            return new Activity(cursor);
        } else if (uri == Locations.CONTENT_URI) {
            return new Location(cursor);
        } else if (uri == Motions.CONTENT_URI) {
            return new Motion(cursor);
        } else {
            throw new UnsupportedOperationException(String.format("Data type from uri(%s) not supported.", uri));
        }
    }

    private TransferObserver uploadDataFileForTimeRange(@NonNull final Uri uri,
                                                        @NonNull final File file,
                                                        final long beginTime,
                                                        final long endTime,
                                                        final long trackId) {

        if (!file.exists() || file.length() == 0) {
            return null;
        }

        final String deviceId = DeviceUtils.getDeviceUUID(mContext);
        final String bucket = Config.S3_BUCKET_NAME;
        final String key = getDataFileKey(uri, beginTime, deviceId);
        final String url = mS3Client.getResourceUrl(bucket, key);
        final String[] selectionArgs = new String[]{Long.toString(beginTime), Long.toString(endTime)};

        TransferObserver observer = uploadFileToS3(bucket, key, file);
        observer.setTransferListener(new TransferListener() { // Triggered in main thread
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    FileUtils.deleteQuietly(file);

                    // Clear dirty column for data in this hour if the file has been uploaded successfully
                    mOps.add(ContentProviderOperation.newUpdate(TrackerContract.addCallerIsSyncAdapterParameter(uri))
                            .withValue(TrackerContract.SyncColumns.DIRTY, null)
                            .withSelection(TrackerContract.SELECTION_BY_INTERVAL, selectionArgs)
                            .build());

                    // Update links table with the s3 url for the uploaded file
                    ContentValues values = new ContentValues();
                    values.put(Links.LINK, url);
                    values.put(Links.TYPE, getDataType(uri));
                    values.put(Links.START_TIME, beginTime);
                    values.put(Links.END_TIME, endTime);
                    values.put(Links.DEVICE_ID, DeviceUtils.getDeviceUUID(mContext));
                    values.put(Links.TRACK_ID, trackId);
                    values.put(TrackerContract.SyncColumns.UPDATED, DateTime.now().getMillis());
                    mOps.add(ContentProviderOperation.newInsert(TrackerContract.addCallerIsSyncAdapterParameter(Links.CONTENT_URI))
                            .withValues(values)
                            .build());

                    // Trigger the import operations
                    Intent intent = new Intent(mContext, DatabaseImportService.class);
                    intent.setAction(DatabaseImportService.INTENT_ACTION_IMPORT_LINKS);
                    intent.putParcelableArrayListExtra(DatabaseImportService.EXTRA_CONTENT_PROVIDER_OPERATIONS, mOps);
                    mContext.startService(intent);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                FileUtils.deleteQuietly(file);
            }
        });
        return observer;
    }

    private File makeDataFileForUpload(@NonNull Uri uri, String filename, List<? extends BaseData> data) throws IOException {
        File file = new File(mContext.getCacheDir(), filename);
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(file, false));
            if (uri == Activities.CONTENT_URI) {
                for (Object item : data) {
                    Activity activity = (Activity) item;
                    writer.writeNext(activity.convertToCsvLine());
                }
            } else if (uri == Locations.CONTENT_URI) {
                for (Object item : data) {
                    Location location = (Location) item;
                    writer.writeNext(location.convertToCsvLine());
                }
            } else if (uri == Motions.CONTENT_URI) {
                for (Object item : data) {
                    Motion motion = (Motion) item;
                    writer.writeNext(motion.convertToCsvLine());
                }
            } else {
                throw new UnsupportedOperationException(String.format("Data type from uri(%s) not supported.", uri));
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    private String getDataType(@NonNull Uri uri) {
        if (uri == Activities.CONTENT_URI) {
            return "activity";
        } else if (uri == Locations.CONTENT_URI) {
            return "location";
        } else if (uri == Motions.CONTENT_URI) {
            return "motion";
        }
        return "unknown";
    }

    private String getLocalFilename(@NonNull Uri uri, long time) {
        return String.format("%s_%d.csv", getDataType(uri), time);
    }

    private TransferObserver uploadFileToS3(String bucket, String key, File fileToUpload) {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/csv");
        metadata.setContentLength(fileToUpload.length());
        metadata.addUserMetadata("x-user-account", AccountUtils.getActiveAccountName(mContext));
        return mTransferUtility.upload(bucket, key, fileToUpload, metadata);
    }

    private String getDataFileKey(@NonNull Uri uri, long time, @NonNull final String deviceId) {
        String type = "unknown_";
        if (uri == Activities.CONTENT_URI) {
            type = "activity_";
        } else if (uri == Locations.CONTENT_URI) {
            type = "location_";
        } else if (uri == Motions.CONTENT_URI) {
            type = "motion_";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mAccountName)
            .append('/')
            .append(new DateTime(time).toString(Config.S3_KEY_PREFIX_PATTERN))
            .append('/')
            .append(type)
            .append(deviceId);
        return sb.toString();
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
        void onDataSyncing(Uri uri, Cursor cursor) throws IOException;
    }
}