package com.localytics.android.itracker.download;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;

import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;


/**
 * Provides the public api to trigger the file download actions
 */
public class FileDownloadManager extends ContentObserver {

    private Context mContext;

    private static FileDownloadManager sInstance;

    public final static String ACTION_DOWNLOAD_FILE = "com.localytics.android.itracker.intent.action.DOWNLOAD_FILE";
    public final static String ACTION_UPDATE_STATUS = "com.localytics.android.itracker.intent.action.UPDATE_STATUS";

    public static FileDownloadManager getInstance(final Context context) {
        synchronized (FileDownloadManager.class) {
            if (sInstance == null) {
                sInstance = new FileDownloadManager(context);

                // Update the status of the download records in case there is a force closing during the previous downloads.
                Intent intent = new Intent(ACTION_UPDATE_STATUS);
                intent.setPackage(context.getPackageName());
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

    public void startDownload(String id) {
        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.START)
                .build();
        postRequest(request);
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
        Intent intent = new Intent(ACTION_DOWNLOAD_FILE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(FileDownloadService.FILE_DOWNLOAD_REQUEST, request);
        mContext.startService(intent);
    }
}
