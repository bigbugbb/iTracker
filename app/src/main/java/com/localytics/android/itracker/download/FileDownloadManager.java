package com.localytics.android.itracker.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;

import com.localytics.android.itracker.data.model.FileDownload;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;


/**
 * Provides the public api to trigger the file download actions
 */
public class FileDownloadManager extends ContentObserver {

    private Context mContext;

    private FileDownloadListener mListener = new SimpleFileDownloadListener();

    private static FileDownloadManager sInstance;

    private final static String FILE_DOWNLOAD_INTENT_ACTION = "com.localytics.android.itracker.intent.action.FILE_DOWNLOAD";

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

    public void setFileDownloadListener(FileDownloadListener listener) {
        mListener = listener;
    }

    public FileDownloadListener getFileDownloadListener() {
        return mListener;
    }

    public void startDownload(String id, Uri srcUri, Uri destUri) {
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
        Intent intent = new Intent(FILE_DOWNLOAD_INTENT_ACTION);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(FileDownloadService.FILE_DOWNLOAD_REQUEST, request);
        mContext.startService(intent);
    }

    public static class SimpleFileDownloadListener implements FileDownloadListener {

        @Override
        public void onPrepare(String downloadId) {

        }

        @Override
        public void onProgress(String downloadId, long currentFileSize, long totalFileSize) {

        }

        @Override
        public void onPaused(String downloadId) {

        }

        @Override
        public void onCanceled(String downloadId) {

        }

        @Override
        public void onCompleted(String downloadId) {

        }

        @Override
        public void onFailed(String downloadId, Exception e) {

        }
    }
}
