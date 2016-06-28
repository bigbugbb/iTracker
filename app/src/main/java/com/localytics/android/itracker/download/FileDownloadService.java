package com.localytics.android.itracker.download;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 6/15/16.
 */
public class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private Map<String, FileDownloadTask> mTasks;

    private LocalBroadcastManager mBroadcastManager;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;

    private String INTERRUPT_BY_PAUSE = "interrupt_by_pause";
    private String INTERRUPT_BY_CANCEL = "interrupt_by_cancel";

    public void onCreate() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mTasks = Collections.synchronizedMap(new HashMap<String, FileDownloadTask>());
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        mExecutor.shutdown();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            FileDownloadRequest request = intent.getParcelableExtra(FILE_DOWNLOAD_REQUEST);
            if (request != null) {
                handleRequest(request);
            }
        }
        return START_STICKY;
    }

    private void handleRequest(FileDownloadRequest request) {
        synchronized (this) {
            FileDownloadTask currentTask = mTasks.get(request.mId);
            if (request.mAction == FileDownloadRequest.RequestAction.START) {
                if (currentTask == null) {
                    Context context = getApplicationContext();
                    FileDownloadTask task = new FileDownloadTask(context, request);
                    mTasks.put(request.mId, task);
                    mExecutor.execute(task);
                } else {
                    LOGD(TAG, "Download task " + request.mId + " is already running");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.PAUSE) {
                if (currentTask != null) {
                    currentTask.pause();
                } else {
                    LOGD(TAG, "Download task " + request.mId + " doesn't exist");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.CANCEL) {
                // TODO: delete all local resource and reset the db status of this file item
            }
        }
    }

    class FileDownloadTask implements Runnable {

        private Context mContext;
        private ContentResolver mResolver;
        private FileDownloadRequest mRequest;

        private AtomicBoolean mPaused;
        private AtomicBoolean mCanceled;

        private String mContentType;
        private DownloadStatus mStatus;
        private File mFinalFile;

        FileDownloadTask(Context context, FileDownloadRequest request) {
            mContext = context;
            mRequest = request;
            mPaused = new AtomicBoolean();
            mCanceled = new AtomicBoolean();
            mResolver = context.getContentResolver();
            mStatus = DownloadStatus.PENDING;
        }

        void pause() {
            mPaused.set(true);
        }

        @Override
        public void run() {
            InputStream input = null;
            OutputStream output = null;
            try {
                /***** Preparing *****/
                updateStatus(DownloadStatus.PREPARING, true);

                onPreparing();

                long currentFileSize = 0, totalFileSize = 0, readBetweenInterval = 0;

                // Query for any existing download record for this media
                Cursor cursor = mResolver.query(FileDownloads.CONTENT_URI, null, FileDownloads.FILE_ID + " = ?", new String[]{ mRequest.mId }, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            totalFileSize = cursor.getLong(cursor.getColumnIndex(FileDownloads.TOTAL_SIZE));
                        }
                    } finally {
                        cursor.close();
                    }
                }

                // Retrieve existing local file size if exists, otherwise create a new file
                File destFile = new File(mRequest.mDestUri.toString());
                if (!destFile.exists() || destFile.isDirectory()) {
                    destFile.getParentFile().mkdirs();
                    destFile.createNewFile();
                } else {
                    currentFileSize = destFile.length();
                }

                // Try to connect the download url (with location redirect)
                URL url = new URL(mRequest.mSrcUri.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(30000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0...");
                connection.setRequestProperty("Range", "bytes=" + currentFileSize + "-");
                LOGI(TAG, "Original URL: " + connection.getURL());
                connection.connect();
                LOGI(TAG, "Connected URL: " + connection.getURL());
                input = connection.getInputStream();
                LOGI(TAG, "Redirected URL: " + connection.getURL());

                /***** Downloading *****/
                updateStatus(DownloadStatus.DOWNLOADING, false);

                ContentValues values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                if (totalFileSize == 0) {
                    totalFileSize = Long.parseLong(connection.getHeaderField("Content-Length"));
                    values.put(FileDownloads.TOTAL_SIZE, totalFileSize);
                }
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{ mRequest.mId });

                mContentType = connection.getHeaderField("Content-Type");

                // Keep downloading the file
                output = new BufferedOutputStream(new FileOutputStream(destFile));
                long bytesToWrite = totalFileSize - currentFileSize;
                int BUFFER_SIZE = 1024 * 32;
                byte[] buffer = new byte[BUFFER_SIZE];
                long prevTime = SystemClock.uptimeMillis();
                while (bytesToWrite > 0) {
                    int read;
                    if (bytesToWrite > BUFFER_SIZE) {
                        read = input.read(buffer);
                    } else {
                        read = input.read(buffer, 0, (int) bytesToWrite);
                    }
                    output.write(buffer, 0, read);

                    readBetweenInterval += read;

                    // Pause means cancel the download task but retain the partial downloaded file
                    if (mPaused.get()) {
                        throw new DownloadInterruptedException(
                                "Download has been paused but you can call startDownload again to resume it",
                                INTERRUPT_BY_PAUSE);
                    }

                    if (mCanceled.get()) {
                        throw new DownloadInterruptedException(
                                "Download has been canceled and the data downloaded will be removed",
                                INTERRUPT_BY_CANCEL);
                    }

                    currentFileSize += read;
                    bytesToWrite -= read;

                    long timeInterval = SystemClock.uptimeMillis() - prevTime;
                    if (timeInterval > DateUtils.SECOND_IN_MILLIS) {
                        long downloadSpeed = (long) (readBetweenInterval / (timeInterval / (float) DateUtils.SECOND_IN_MILLIS));
                        onDownloading(currentFileSize, totalFileSize, downloadSpeed);
                        prevTime = SystemClock.uptimeMillis();
                        readBetweenInterval = 0;
                    }
                }
                output.flush();
                closeOutput(output);

                /***** Completed *****/
                if ((mFinalFile = updateFileName()) != null) {
                    LOGD(TAG, String.format("Download %s has been completed (file: %s)", mRequest.mId, mFinalFile));
                    updateStatus(DownloadStatus.COMPLETED, true);
                    onCompleted();
                } else {
                    LOGE(TAG, String.format("Failed to generate the final file"));
                    updateStatus(DownloadStatus.FAILED, true);
                    onFailed();
                }
            } catch (DownloadInterruptedException e) {
                if (e.getReason().equals(INTERRUPT_BY_PAUSE)) {
                    LOGD(TAG, String.format("Download %s has been paused", mRequest.mId), e);
                    updateStatus(DownloadStatus.PAUSED, true);
                    onPaused();
                } else if (e.getReason().equals(INTERRUPT_BY_CANCEL)) {
                    LOGD(TAG, String.format("Download %s has been canceled", mRequest.mId), e);
                    updateStatus(DownloadStatus.CANCELED, true);
                    onCanceled();
                }
            } catch (IOException e) {
                LOGE(TAG, String.format("Failed to download %s from %s", mRequest.mId, mRequest.mSrcUri), e);
                updateStatus(DownloadStatus.FAILED, true);
                onFailed();
            } finally {
                closeInput(input);
                closeOutput(output);

                if (mCanceled.get()) {
                    File destFile = new File(mRequest.mDestUri.toString());
                    destFile.delete();
                }

                mTasks.put(mRequest.mId, null);
            }
        }

        private File updateFileName() {
            String type = mContentType.split("[/]")[1]; // Assume the content type is always correct from youtube
            String path = mRequest.mDestUri.getPath();
            path = path.substring(0, path.lastIndexOf("/") + 1) + getTitle().replace(" ", "_");
            File oldFile = new File(mRequest.mDestUri.toString());
            File newFile = new File(path + "." + type);
            for (int i = 1; newFile.exists(); ++i) {
                newFile = new File(String.format("%s(%d).%s", path, i, type));
            }
            if (oldFile.renameTo(newFile)) {
                return newFile;
            } else {
                oldFile.delete(); // Just make sure it's been deleted.
            }
            return null;
        }

        private String getTitle() {
            String title = null;
            Cursor cursor = mContext.getContentResolver().query(
                    FileDownloads.MEDIA_DOWNLOADS_URI,
                    null,
                    FileDownloads.FILE_ID + " = ?",
                    new String[]{mRequest.mId},
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        title = cursor.getString(cursor.getColumnIndex(TrackerContract.MediaDownloads.TITLE));
                    }
                } finally {
                    cursor.close();
                }
            }

            if (TextUtils.isEmpty(title)) {
                title = mRequest.mId;
            }

            return title;
        }

        private void closeInput(InputStream input) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeOutput(OutputStream output) {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onPreparing() {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_PREPARING);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);
        }

        protected void onPaused() {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_PAUSED);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);
        }

        protected void onDownloading(long currentFileSize, long totalFileSize, long downloadSpeed) {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_DOWNLOADING);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_FILE_SIZE_BYTES, currentFileSize);
            intent.putExtra(FileDownloadBroadcastReceiver.TOTAL_FILE_SIZE_BYTES, totalFileSize);
            intent.putExtra(FileDownloadBroadcastReceiver.DOWNLOAD_SPEED, downloadSpeed);
            mBroadcastManager.sendBroadcast(intent);
        }

        protected void onCanceled() {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_CANCELED);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);
        }

        protected void onCompleted() {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_COMPLETED);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            intent.putExtra(FileDownloadBroadcastReceiver.DOWNLOADED_FILE_URI, Uri.fromFile(mFinalFile));
            mBroadcastManager.sendBroadcast(intent);
        }

        protected void onFailed() {
            Intent intent = new Intent(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadBroadcastReceiver.DOWNLOAD_STAGE_FAILED);
            intent.putExtra(FileDownloadBroadcastReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);
        }

        private void updateStatus(DownloadStatus status, boolean syncDb) {
            synchronized (this) {
                mStatus = status;
            }

            if (syncDb) {
                ContentValues values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{ mRequest.mId });
            }
        }
    }

    private class DownloadTaskComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            return 0;
        }
    }

    /**
     * Thrown when a download is interrupted.
     */
    public class DownloadInterruptedException extends IOException {

        private String mReason;

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace
         * filled in.
         */
        public DownloadInterruptedException() {
        }

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
         * detail message filled in.
         *
         * @param detailMessage
         *            the detail message for this exception.
         */
        public DownloadInterruptedException(String detailMessage) {
            super(detailMessage);
        }

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
         * detail message filled in.
         *
         * @param detailMessage
         *            the detail message for this exception.
         * @param reason
         *            the reason for this interruption
         */
        public DownloadInterruptedException(String detailMessage, String reason) {
            super(detailMessage);
            mReason = reason;
        }

        public String getReason() {
            return mReason;
        }
    }
}
