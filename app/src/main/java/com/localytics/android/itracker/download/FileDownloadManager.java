package com.localytics.android.itracker.download;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.util.ConnectivityUtils;
import com.localytics.android.itracker.util.YouTubeExtractor;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


/**
 * Provides the public api to trigger the file download actions
 */
public class FileDownloadManager extends ContentObserver {
    private final static String TAG = makeLogTag(FileDownloadManager.class);

    private Context mContext;

    private static FileDownloadManager sInstance;

    public static FileDownloadManager getInstance(final Context context) {
        synchronized (FileDownloadManager.class) {
            if (sInstance == null) {
                sInstance = new FileDownloadManager(context);
            }
        }
        return sInstance;
    }

    private FileDownloadManager(final Context context) {
        super(null);

        if (context == null) {
            throw new IllegalArgumentException("Context can never be null");
        }

        mContext = context;
        mContext.getContentResolver().registerContentObserver(FileDownloads.CONTENT_URI, true, this);
    }

    public void recoverStatus() {
        Intent intent = new Intent(mContext, FileDownloadService.class);
        intent.setAction(FileDownloadService.ACTION_RECOVER_STATUS);
        mContext.startService(intent);
    }

    public void startDownload(String id) {
        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.START)
                .build();
        postRequest(request);
    }

    public void startDownload(String id, Uri srcUri, Uri destUri) {
        if (!ConnectivityUtils.isWifiConnected(mContext)) {
            return;
        }

        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.START)
                .setSourceUri(srcUri)
                .setDestinationUri(destUri)
                .build();
        postRequest(request);
    }

    public void pauseDownload(String id) {
        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.PAUSE)
                .build();
        postRequest(request);
    }

    public void cancelDownload(String id) {
        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.CANCEL)
                .build();
        postRequest(request);
    }

    private void postRequest(FileDownloadRequest request) {
        Intent intent = new Intent(FileDownloadService.ACTION_DOWNLOAD_FILE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(FileDownloadService.FILE_DOWNLOAD_REQUEST, request);
        mContext.startService(intent);
    }

    private String getVideoTargetUrl(final String videoId) {
        YouTubeExtractor.Result result = new YouTubeExtractor(videoId).extract(null);
        if (result != null) {
            Uri videoUri = result.getBestAvaiableQualityVideoUri();
            if (videoUri != null) {
                return videoUri.toString();
            }
        }
        return "";
    }

    public void startAvailableDownloads() {
        String availableStatus = String.format("%s,%s,%s,%s,%s",
                DownloadStatus.PENDING.value(), DownloadStatus.PREPARING.value(), DownloadStatus.DOWNLOADING.value(), DownloadStatus.CONNECTING.value(), DownloadStatus.PAUSED.value());
        Cursor cursor = mContext.getContentResolver().query(
                FileDownloads.buildMediaDownloadUriByStatus(availableStatus),
                null,
                null,
                null,
                TrackerContract.FileDownloads.START_TIME + " DESC");
        if (cursor != null) {
            try {
                MediaDownload[] downloads = MediaDownload.downloadsFromCursor(cursor);
                if (downloads != null) {
                    for (MediaDownload download : downloads) {
                        Uri srcUri = Uri.parse(getVideoTargetUrl(download.identifier));
                        Uri destUri = Uri.parse(Config.FILE_DOWNLOAD_DIR_PATH + download.identifier);
                        startDownload(download.identifier, srcUri, destUri);
                    }
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }
    }

    public void pauseAvailableDownloads() {
        String availableStatus = String.format("%s,%s,%s,%s",
                DownloadStatus.PENDING.value(), DownloadStatus.PREPARING.value(), DownloadStatus.DOWNLOADING.value(), DownloadStatus.CONNECTING.value());
        Cursor cursor = mContext.getContentResolver().query(
                FileDownloads.buildMediaDownloadUriByStatus(availableStatus),
                null,
                null,
                null,
                TrackerContract.FileDownloads.START_TIME + " DESC");
        if (cursor != null) {
            try {
                MediaDownload[] downloads = MediaDownload.downloadsFromCursor(cursor);
                if (downloads != null) {
                    for (MediaDownload download : downloads) {
                        pauseDownload(download.identifier);
                    }
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }
    }

    public void startAvailableDownloadsAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                startAvailableDownloads();
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    public void pauseAvailableDownloadsAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                pauseAvailableDownloads();
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }
}
