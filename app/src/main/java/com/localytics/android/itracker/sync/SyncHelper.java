package com.localytics.android.itracker.sync;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.data.model.Activity;
import com.localytics.android.itracker.data.model.Backup;
import com.localytics.android.itracker.data.model.BaseData;
import com.localytics.android.itracker.data.model.Location;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Activities;
import com.localytics.android.itracker.provider.TrackerContract.Backups;
import com.localytics.android.itracker.provider.TrackerContract.Locations;
import com.localytics.android.itracker.provider.TrackerContract.Motions;
import com.localytics.android.itracker.provider.TrackerContract.SyncState;
import com.localytics.android.itracker.utils.AccountUtils;
import com.localytics.android.itracker.utils.ConnectivityUtils;
import com.localytics.android.itracker.utils.DataFileUtils;
import com.localytics.android.itracker.utils.LogUtils;
import com.localytics.android.itracker.utils.PrefUtils;
import com.localytics.android.itracker.utils.RequestUtils;
import com.opencsv.CSVWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.LOGW;

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

    private RequestQueue mRequestQueue;

    private String mAuthToken;
    private String mAccountName;

    private File mCacheDataDir;

    private AtomicBoolean mDataChanged;

    private CountDownLatch mTrackDataUploadLatch;

    private final ArrayList<ContentProviderOperation> mOps = new ArrayList<>();

    private DataImportReceiver mDataImportReceiver = new DataImportReceiver();

    public SyncHelper(Context context) {
        mContext = context;
        mTrackerDataHandler = new TrackerDataHandler(mContext);
        mRemoteDataFetcher = new RemoteTrackerDataFetcher(mContext);
        mDataChanged = new AtomicBoolean(false);
        mTrackDataUploadLatch = new CountDownLatch(3);
        mRequestQueue = RequestUtils.getRequestQueue(mContext);
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

        // Get auth token and use it for each data request
        mAuthToken = AccountUtils.getAuthToken(mContext);
        mAccountName = AccountUtils.getActiveAccountName(mContext);

        if (TextUtils.isEmpty(mAuthToken) || TextUtils.isEmpty(mAccountName)) {
            LOGE(TAG, "Auth token and account name can not be empty!");
            return false;
        } else {
            // Make sure data cache dir is created for the data files generated to upload or
            // downloaded from s3. Each account has its own directory so the data can be isolated.
            mCacheDataDir = new File(mContext.getCacheDir(), mAccountName);
            try {
                FileUtils.forceMkdir(mCacheDataDir);
            } catch (IOException e) {
                LOGE(TAG, "Failed to make cache dir: " + e.getMessage());
                return false;
            }
        }

        // Download available media files
        if (ConnectivityUtils.isOnline(mContext)) {
            FileDownloadManager.getInstance().startAvailableDownloads();
        }

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
        long remoteSyncDuration;

        opStart = System.currentTimeMillis();

        // remote sync consists of these operations, which we try one by one (and tolerate individual failures on each)
        final int OP_USER_DATA_SYNC     = 0;
        final int OP_TRACK_DATA_SYNC    = 1;
        final int OP_USER_FEEDBACK_SYNC = 2;
        int[] opsToPerform = new int[] { OP_USER_DATA_SYNC, OP_TRACK_DATA_SYNC, OP_USER_FEEDBACK_SYNC };

        for (int op : opsToPerform) {
            try {
                switch (op) {
                    case OP_USER_DATA_SYNC:
                        doUserDataSync(extras);
                        break;
                    case OP_TRACK_DATA_SYNC:
                        doTrackerDataSync(extras);
                        break;
                    case OP_USER_FEEDBACK_SYNC:
                        doUserFeedbackSync(extras);
                        break;
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

        int operations = mTrackerDataHandler.getContentProviderOperationsDone();
        if (syncResult != null && syncResult.stats != null) {
            syncResult.stats.numEntries += operations;
            syncResult.stats.numUpdates += operations;
        }

        if (mDataChanged.get()) {
            long totalDuration = remoteSyncDuration;
            LOGD(TAG, "SYNC STATS:\n" +
                    " *  Account synced: " + (account == null ? "null" : account.name) + "\n" +
                    " *  Content provider operations: " + operations + "\n" +
                    " *  Remote sync took: " + remoteSyncDuration + "ms\n" +
                    " *  Total time: " + totalDuration + "ms\n" +
                    " *  Total data read from cache: \n" +
                    (mRemoteDataFetcher.getTotalBytesReadFromCache() / 1024) + "kB\n" +
                    " *  Total data downloaded: \n" +
                    (mRemoteDataFetcher.getTotalBytesDownloaded() / 1024) + "kB");
        }

        LogUtils.LOGI(TAG, "End of sync (" + (mDataChanged.get() ? "data changed" : "no data change") + ")");

        updateSyncInterval(mContext, account);

        return mDataChanged.get();
    }

    private void doUserFeedbackSync(Bundle extras) {
        LOGD(TAG, "Syncing user feedback");
//        new FeedbackSyncHelper(mContext).sync();
    }

    private boolean doUserDataSync(Bundle extras) throws IOException {
        LOGD(TAG, "Syncing user data");
        return true;
    }

    /**
     * Checks if the remote server has new data that we need to import. If so, download
     * the new data and import it into the database.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem downloading or importing the data.
     */
    private void doTrackerDataSync(final Bundle extras) throws IOException, InterruptedException {
        if (!ConnectivityUtils.isOnline(mContext)) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return;
        }

        if (Config.WIFI_ONLY_SYNC_ENABLED && !ConnectivityUtils.isOnline(mContext)) {
            LOGD(TAG, "Not attempting remote sync because wifi-only sync is enabled but the device is not connected to WIFI");
            return;
        }

        /*********************************************************************/
        LOGD(TAG, "Start uploading track data.");
        Uri[] uris = new Uri[]{ Activities.CONTENT_URI, Locations.CONTENT_URI, Motions.CONTENT_URI };
        for (Uri uri : uris) {
            uploadData(uri);
        }
        if (mTrackDataUploadLatch.await(Config.TRACK_DATA_UPLOAD_TIMEOUT, TimeUnit.MILLISECONDS)) {
            try {
                mContext.getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, mOps);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            } finally {
                mOps.clear();
            }
        }
        LOGD(TAG, "Complete uploading track data.");
        /*********************************************************************/

        /*********************************************************************/
        LOGD(TAG, "Start requesting and handling response from backups api.");
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(Config.BACKUPS_URL, future, future) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", AccountUtils.getAuthToken(mContext));
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // TODO: setup the params based on the input extras
                DateTime today = DateTime.now();
                DateTime twoWeeksAgo = today.minus(13);
                Map<String, String> params = new HashMap<>();
                params.put("start_date", twoWeeksAgo.toString("yyyy-MM-dd"));
                params.put("end_date", today.toString("yyyy-MM-dd"));
                return params;
            }
        };
        mRequestQueue.add(request);

        try {
            String response = future.get(30, TimeUnit.SECONDS);
            LOGD(TAG, "Backup information:\n" + response);

            if (!TextUtils.isEmpty(response)) {
                mTrackerDataHandler.applyAppData(
                        new String[]{response},
                        DateFormatUtils.SMTP_DATETIME_FORMAT.format(DateTime.now().getMillis()),
                        true);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGE(TAG, "Can't get backup information: " + e.getMessage());
        } catch (TimeoutException e) {
            LOGE(TAG, "Timeout during retrieving backup information: " + e.getMessage());
        }
        LOGD(TAG, "Complete requesting and handling response from backups api.");
        /*********************************************************************/

        /*********************************************************************/
        LOGD(TAG, "Start restoring track data.");
        restoreData();
        LOGD(TAG, "Complete restoring track data.");
        /*********************************************************************/
    }

    private void restoreData() {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Backups.CONTENT_URI,
                    null,
                    TrackerContract.SELECTION_BY_SYNC,
                    new String[] {SyncState.PENDING.value()},
                    Backups.DATE + " DESC");
            if (cursor != null) {
                List<Backup> pendingBackups = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    pendingBackups.add(new Backup(cursor));
                }
                if (pendingBackups.size() > 0) {
                    restorePendingBackups(pendingBackups);
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

    private void restorePendingBackups(List<Backup> pendingBackups) {
        final Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(Backups.CONTENT_URI);

        // Update backups sync state from pending to syncing
        mOps.clear();
        for (Backup pendingBackup : pendingBackups) {
            mOps.add(ContentProviderOperation.newUpdate(uri)
                    .withValue(TrackerContract.SyncColumns.SYNC, SyncState.SYNCING.value())
                    .withSelection(Backups.SELECTION_BY_S3_KEY, new String[]{pendingBackup.s3_key})
                    .build());
        }

        try {
            mContext.getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, mOps);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        } finally {
            mOps.clear();
        }

        // Trigger the file downloads
        for (final Backup pendingBackup : pendingBackups) {
            final String bucket = Config.S3_BUCKET_NAME;
            final String key = pendingBackup.s3_key;
            final String category = pendingBackup.category;
            final String filename = getDestFilename(pendingBackup.category, pendingBackup.timestamp());

            // Make sure the file to download doesn't exist otherwise s3 transfer will try to download
            // from the last range but it's invalid for our use case and a 416 http error will occur.
            final File fileToDownload = new File(mCacheDataDir, filename);
            LOGD(TAG, String.format("Download " + fileToDownload + " for %s-%02d %s", pendingBackup.date, pendingBackup.hour, pendingBackup.category));

            final TransferObserver observer = mTransferUtility.download(bucket, key, fileToDownload);
            observer.setTransferListener(new TransferListener() { // Triggered in main thread
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state == TransferState.COMPLETED) {
                        // Update backups sync state from syncing to synced
                        ContentValues values = new ContentValues();
                        values.put(Backups.SYNC, SyncState.SYNCED.value());
                        if (mContext.getContentResolver().update(uri, values, Backups.SELECTION_BY_S3_KEY, new String[]{key}) == 0) {
                            LOGE(TAG, "Failed to update backups sync state from syncing to synced: " + key);
                        } else {
                            // Start the data import service to import data from the downloaded file.
                            Intent intent = new Intent(DataImportReceiver.ACTION_IMPORT_DATA);
                            Bundle extras = new Bundle();
                            extras.putString(DataImportService.EXTRA_IMPORT_FILE_PATH, fileToDownload.getAbsolutePath());
                            extras.putParcelable(DataImportService.EXTRA_IMPORT_BACKUP_INFO, pendingBackup);
                            intent.putExtras(extras);
                            mContext.sendBroadcast(intent);
                        }
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                }

                @Override
                public void onError(int id, Exception ex) {
                    // Update backups sync state from syncing to pending
                    ContentValues values = new ContentValues();
                    values.put(Backups.SYNC, SyncState.PENDING.value());
                    if (mContext.getContentResolver().update(uri, values, Backups.SELECTION_BY_S3_KEY, new String[]{key}) == 0) {
                        LOGE(TAG, "Failed to update backups sync state from syncing to pending: " + key);
                    }
                }
            });
        }
    }

    private void uploadData(@NonNull Uri uri) {
        boolean gotFileToUpload = false;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    uri,
                    null,
                    TrackerContract.SELECTION_BY_DIRTY,
                    new String[]{ Integer.toString(1) },
                    TrackerContract.ORDER_BY_TIME_ASC);
            if (cursor != null) {
                long nextTime, prevTime = 0, trackId = -1;
                final List<BaseData> data = new ArrayList<>(cursor.getCount());

                while (cursor.moveToNext()) {
                    final BaseData item = dataItemFromCursor(uri, cursor);
                    nextTime = item.time - item.time % DateUtils.HOUR_IN_MILLIS;
                    if (trackId != -1 && prevTime != 0 && nextTime - prevTime >= DateUtils.HOUR_IN_MILLIS) {
                        final File file = makeUploadDataFile(getSourceFilename(uri, prevTime), data);
                        if (file != null) {
                            gotFileToUpload = true;
                            uploadDataFileForTimeRange(uri, file, prevTime, nextTime);
                        }
                        data.clear();
                    }
                    data.add(item);
                    prevTime = nextTime;
                    trackId = item.track_id;
                }
            }
        } catch (Exception e) {
            LOGE(TAG, "Error: " + e.getMessage());
        } finally {
            if (!gotFileToUpload) {
                mTrackDataUploadLatch.countDown();
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

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
                                                        @NonNull final File srcFile,
                                                        final long beginTime,
                                                        final long endTime) {
        if (!srcFile.exists() || srcFile.length() == 0) {
            LOGE(TAG, "Couldn't handle the source file: " + srcFile);
            return null;
        }

        final String bucket = Config.S3_BUCKET_NAME;
        final String key = getDataFileKey(uri, beginTime);
        final String[] selectionArgs = new String[]{Long.toString(beginTime), Long.toString(endTime)};

        final DateTime time = new DateTime(beginTime);

        // zip the source file
        String srcFilePath = srcFile.getAbsolutePath();
        String zipFilePath = srcFilePath + ".zip";
        DataFileUtils.zip(srcFilePath, zipFilePath);
        final File zipFile = new File(zipFilePath);
        if (!zipFile.exists() || zipFile.length() == 0) {
            LOGE(TAG, "Couldn't compress source file: " + srcFile);
            return null;
        }

        final TransferObserver observer = uploadFileToS3(bucket, key, zipFile);
        observer.setTransferListener(new TransferListener() { // Triggered in main thread
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    FileUtils.deleteQuietly(srcFile);
                    FileUtils.deleteQuietly(zipFile);

                    // Clear dirty column for data in this hour if the file has been uploaded successfully
                    mOps.add(ContentProviderOperation.newUpdate(TrackerContract.addCallerIsSyncAdapterParameter(uri))
                            .withValue(TrackerContract.SyncColumns.DIRTY, 0)
                            .withSelection(TrackerContract.SELECTION_BY_INTERVAL, selectionArgs)
                            .build());

                    // Update links table with the s3 url for the uploaded file
                    ContentValues values = new ContentValues();
                    values.put(Backups.S3_KEY, key);
                    values.put(Backups.CATEGORY, TrackerContract.categoryFromUri(uri));
                    values.put(Backups.DATE, time.toString("yyyy-MM-dd"));
                    values.put(Backups.HOUR, time.getHourOfDay());
                    values.put(Backups.SYNC, SyncState.SYNCED.value());
                    values.put(TrackerContract.SyncColumns.UPDATED, DateTime.now().getMillis());
                    mOps.add(ContentProviderOperation.newInsert(TrackerContract.addCallerIsSyncAdapterParameter(Backups.CONTENT_URI))
                            .withValues(values)
                            .build());

                    // Decrement the latch so the syncing thread could be released
                    mTrackDataUploadLatch.countDown();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                FileUtils.deleteQuietly(srcFile);
                FileUtils.deleteQuietly(zipFile);
                mTrackDataUploadLatch.countDown();
            }
        });

        return observer;
    }

    private File makeUploadDataFile(@NonNull String filename,
                                    @NonNull List<? extends BaseData> data) throws IOException {
        File file = new File(mCacheDataDir, filename);
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(file, false));
            for (BaseData item : data) {
                writer.writeNext(item.convertToCsvLine());
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

    private String getSourceFilename(@NonNull Uri uri, long time) {
        return getSourceFilename(TrackerContract.categoryFromUri(uri), time);
    }

    private String getSourceFilename(@NonNull String category, long time) {
        return String.format("%s_%d.csv", category, time);
    }

    private String getDestFilename(@NonNull String category, long time) {
        return String.format("%s_%d.csv.zip", category, time);
    }

    private TransferObserver uploadFileToS3(String bucket, String key, File fileToUpload) {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/csv");
        metadata.setContentEncoding("zip");
        return mTransferUtility.upload(bucket, key, fileToUpload, metadata);
    }

    private String getDataFileKey(@NonNull Uri uri, long time) {
        String category = "unknown";
        if (uri == Activities.CONTENT_URI) {
            category = TrackerContract.DATA_CATEGORY_ACTIVITY;
        } else if (uri == Locations.CONTENT_URI) {
            category = TrackerContract.DATA_CATEGORY_LOCATION;
        } else if (uri == Motions.CONTENT_URI) {
            category = TrackerContract.DATA_CATEGORY_MOTION;
        }
        return new StringBuilder()
                .append(new DateTime(time).toString(Config.S3_KEY_PREFIX_PATTERN))
                .append('/')
                .append(mAccountName)
                .append('/')
                .append(category)
                .append(".csv.zip")
                .toString();
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
}