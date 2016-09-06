package com.localytics.android.itracker.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.provider.TrackerContract.MediaDownloads;
import com.localytics.android.itracker.ui.listener.DownloadStateChangedListener;
import com.localytics.android.itracker.utils.PrefUtils;
import com.localytics.android.itracker.utils.YouTubeExtractor;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

class FileDownloadTask implements Runnable {
    private static final String TAG = makeLogTag(FileDownloadTask.class);

    private Context mContext;
    private ContentResolver mResolver;
    private FileDownloadRequest mRequest;

    private AtomicBoolean mPaused;
    private AtomicBoolean mCanceled;

    private String mContentType;
    private DownloadStatus mStatus;
    private Map<String, String> mDownloadInfo;

    private FileDownloadNotificationBuilder mNotificationBuilder;

    private OnTaskEndedListener mOnTaskEndedListener;

    private final static int BUFFER_SIZE = 1024 * 32;
    private final static int RECONNECT_COUNT = 5;

    private String INTERRUPT_BY_PAUSE = "interrupt_by_pause";
    private String INTERRUPT_BY_CANCEL = "interrupt_by_cancel";

    FileDownloadTask(FileDownloadRequest request, OnTaskEndedListener onTaskEndedListener) {
        mContext = Application.getInstance();
        mRequest = request;
        mOnTaskEndedListener = onTaskEndedListener;
        mPaused = new AtomicBoolean();
        mCanceled = new AtomicBoolean();
        mResolver = mContext.getContentResolver();
        mStatus = DownloadStatus.PENDING;
        mNotificationBuilder = FileDownloadNotificationBuilder.getInstance();
    }

    FileDownloadRequest getRequest() {
        return mRequest;
    }

    void pause() {
        mPaused.set(true);
    }

    void cancel() {
        mCanceled.set(true);
    }

    private void download(int retries) throws Exception {
        boolean reconnect = false;
        InputStream input = null;
        RandomAccessFile output = null;

        try {
            long currentFileSize, totalFileSize = 0, readBetweenInterval = 0;

            // Query for any existing download record for this media
            Cursor cursor = mResolver.query(FileDownloads.CONTENT_URI, null, FileDownloads.FILE_ID + " = ?", new String[]{mRequest.mId}, null);
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
            destFile.getParentFile().mkdirs();
            output = new RandomAccessFile(destFile, "rw");
            currentFileSize = output.length();
            output.seek(currentFileSize);

            // Try to connect the download url (with location redirect)
            URL url = new URL(mRequest.mSrcUri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
            mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{mRequest.mId});

            mContentType = connection.getHeaderField("Content-Type");

            // Keep downloading the file
            long bytesToWrite = totalFileSize - currentFileSize;
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
        } catch (IOException e) {
            if (e instanceof DownloadInterruptedException) {
                throw e;
            } else {
                LOGE(TAG, "Something wrong with the connection", e);
                if (retries >= RECONNECT_COUNT) {
                    throw e;
                }
                reconnect = true;
            }
        } finally {
            closeInput(input);
            closeOutput(output);
        }

        if (reconnect && retries < RECONNECT_COUNT) {
            updateStatus(DownloadStatus.CONNECTING);
            waitBeforeReconnect(retries);
            download(retries + 1);
        }
    }

    private void waitBeforeReconnect(int retries) {
        try {
            long time = DateUtils.SECOND_IN_MILLIS * (long) Math.pow(2, retries);
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }

    @Override
    public void run() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            /***** Preparing *****/
            updateStatus(DownloadStatus.PREPARING);

            onPreparing();

            updateUri();

            download(0);

            /***** Completed *****/
            File finalFile;
            if ((finalFile = updateFileName(mDownloadInfo.get(MediaDownloads.TITLE))) == null) {
                throw new IOException("Can't generate the final file");
            } else {
                LOGD(TAG, String.format("Download %s has been completed (file: %s)", mRequest.mId, finalFile));
                updateLocalLocation(finalFile);
                updateStatus(DownloadStatus.COMPLETED);
                onCompleted(Uri.fromFile(finalFile));
            }
        } catch (DownloadInterruptedException e) {
            if (e.getReason().equals(INTERRUPT_BY_PAUSE)) {
                LOGD(TAG, String.format("Download %s has been paused", mRequest.mId), e);
                updateStatus(DownloadStatus.PAUSED);
                onPaused();
            } else if (e.getReason().equals(INTERRUPT_BY_CANCEL)) {
                LOGD(TAG, String.format("Download %s has been canceled", mRequest.mId), e);
                updateStatus(DownloadStatus.CANCELED);
                onCanceled();
            }
        } catch (Exception e) {
            LOGE(TAG, String.format("Failed to download %s from %s", mRequest.mId, mRequest.mSrcUri), e);
            updateStatus(DownloadStatus.FAILED);
            onFailed(e);
        } finally {
            if (mCanceled.get()) {
                File destFile = new File(mRequest.mDestUri.toString());
                destFile.delete();
            }

            if (mOnTaskEndedListener != null) {
                mOnTaskEndedListener.onTaskEnded(this);
            }
        }
    }

    private void updateUri() throws Exception {
        if (mRequest.mSrcUri == null) {
            YouTubeExtractor.Result result = new YouTubeExtractor(mRequest.getId()).extract(null);
            if (result != null) {
                mRequest.mSrcUri = result.getBestAvaiableQualityVideoUri();
                if (mRequest.mSrcUri == null) {
                    throw new Exception("Can't get the uri for video " + mRequest.getId());
                }
            }
        }
        if (mRequest.mDestUri == null) {
            mRequest.mDestUri = Uri.parse(Config.FILE_DOWNLOAD_DIR_PATH + mRequest.getId());
        }
    }

    private File updateFileName(String fileTitle) {
        String type = mContentType.split("[/]")[1]; // Assume the content type is always correct from youtube
        String path = mRequest.mDestUri.getPath();
        path = path.substring(0, path.lastIndexOf("/") + 1) + fileTitle.replace(" ", "_");
        File oldFile = new File(mRequest.mDestUri.toString());
        File newFile = new File(path + "." + type);
        for (int i = 1; newFile.exists(); ++i) {
            newFile = new File(String.format("%s(%d).%s", path, i, type));
        }
        if (oldFile.renameTo(newFile)) {
            return newFile;
        }
        return null;
    }

    private Map<String, String> getDownloadInfo() {
        Map<String, String> downloadInfo = new HashMap<>();

        String title = null, startTime = null;
        Cursor cursor = mContext.getContentResolver().query(
                FileDownloads.MEDIA_DOWNLOADS_URI,
                null,
                FileDownloads.FILE_ID + " = ?",
                new String[] { mRequest.mId },
                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    title = cursor.getString(cursor.getColumnIndex(MediaDownloads.TITLE));
                    startTime = cursor.getString(cursor.getColumnIndex(MediaDownloads.START_TIME));
                }
            } finally {
                cursor.close();
            }
        }

        downloadInfo.put(MediaDownloads.TITLE, TextUtils.isEmpty(title) ? mRequest.getId() : title);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm a");
        if (TextUtils.isEmpty(startTime)) {
            startTime = DateTime.now().toString(formatter);
        } else {
            startTime = ISODateTimeFormat.dateTime().parseDateTime(startTime).toString(formatter);
        }
        startTime = startTime.startsWith("0") ? startTime.substring(1) : startTime;
        downloadInfo.put(MediaDownloads.START_TIME, startTime);

        return downloadInfo;
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

    private void closeOutput(RandomAccessFile output) {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onPreparing() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onPreparing(mRequest, null);
                }
            }
        });

        mDownloadInfo = getDownloadInfo();
    }

    protected void onPaused() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onPaused(mRequest, null);
                }
            }
        });

        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(mRequest.getId().hashCode());
    }

    protected void onDownloading(final long currentFileSize, final long totalFileSize, final long downloadSpeed) {
        // Store the data so we can use them to update ui even if the download is stopped.
        PrefUtils.setCurrentDownloadSpeed(mContext, mRequest.getId(), downloadSpeed);
        PrefUtils.setCurrentDownloadFileSize(mContext, mRequest.getId(), currentFileSize);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onDownloading(mRequest, currentFileSize, totalFileSize, downloadSpeed, null);
                }
            }
        });

        int progress = (int) (100 * (float) currentFileSize / totalFileSize);
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mNotificationBuilder.newDownloadingNotification(mRequest.getId(), progress, mDownloadInfo);
        nm.notify(mRequest.getId().hashCode(), notification);
    }

    protected void onCanceled() {
        File tempFile = new File(mRequest.mDestUri.toString());
        tempFile.delete();

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onCanceled(mRequest, null);
                }
            }
        });

        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(mRequest.getId().hashCode());
    }

    protected void onCompleted(final Uri finalFileUri) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onCompleted(mRequest, finalFileUri, null);
                }
            }
        });

        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mNotificationBuilder.newCompletedNotification(mRequest.getId(), finalFileUri, mDownloadInfo);
        nm.notify(mRequest.getId().hashCode(), notification);
    }

    protected void onFailed(Exception exception) {
        final String reason = TextUtils.isEmpty(exception.getMessage()) ? exception.toString() : exception.getMessage();
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadStateChangedListener downloadStateChangedListener
                        : Application.getInstance().getUIListeners(DownloadStateChangedListener.class)) {
                    downloadStateChangedListener.onFailed(mRequest, reason, null);
                }
            }
        });

        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mNotificationBuilder.newFailedNotification(mRequest.getId(), mDownloadInfo, reason);
        nm.notify(mRequest.getId().hashCode(), notification);
    }

    void updateLocalLocation(File downloadedFile) {
        ContentValues values = new ContentValues();
        values.put(FileDownloads.LOCAL_LOCATION, downloadedFile.toString());
        mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{mRequest.mId});
    }

    void updateStatus(DownloadStatus status) {
        updateStatus(status, true);
    }

    void updateStatus(DownloadStatus status, boolean syncDb) {
        synchronized (this) {
            mStatus = status;
        }

        if (syncDb) {
            ContentValues values = new ContentValues();
            values.put(FileDownloads.STATUS, mStatus.value());
            if (status == DownloadStatus.COMPLETED) {
                values.put(FileDownloads.FINISH_TIME, DateTime.now().toString());
            }
            mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{ mRequest.mId });
        }
    }

    public interface OnTaskEndedListener {
        void onTaskEnded(FileDownloadTask task);
    }
}

