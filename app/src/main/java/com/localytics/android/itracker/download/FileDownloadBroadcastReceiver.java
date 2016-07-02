package com.localytics.android.itracker.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class FileDownloadBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = makeLogTag(FileDownloadBroadcastReceiver.class);

    public static final String ACTION_FILE_DOWNLOAD_PROGRESS = "com.localytics.android.itracker.intent.action.FILE_DOWNLOAD_PROGRESS";

    public static final String CURRENT_DOWNLOAD_STAGE = "current_download_stage";

    public static final int DOWNLOAD_STAGE_UNKNOWN      = -1;
    public static final int DOWNLOAD_STAGE_PREPARING    = 1;
    public static final int DOWNLOAD_STAGE_PAUSED       = 2;
    public static final int DOWNLOAD_STAGE_DOWNLOADING  = 3;
    public static final int DOWNLOAD_STAGE_CANCELED     = 4;
    public static final int DOWNLOAD_STAGE_COMPLETED    = 5;
    public static final int DOWNLOAD_STAGE_FAILED       = 6;

    public static final String CURRENT_REQUEST = "current_request";
    public static final String CURRENT_FILE_SIZE_BYTES = "current_file_size_bytes";
    public static final String TOTAL_FILE_SIZE_BYTES = "total_file_size_bytes";
    public static final String DOWNLOADED_FILE_URI = "download_file_uri";
    public static final String DOWNLOAD_SPEED = "download_speed";
    public static final String DOWNLOAD_FAILED_REASON = "download_failed_reason";
    public static final String EXTRA_DATA_INFO = "extra_data_info";

    @Override
    public void onReceive(Context context, Intent intent) {
        final FileDownloadRequest request = intent.getParcelableExtra(CURRENT_REQUEST);
        final Bundle bundle = intent.getBundleExtra(EXTRA_DATA_INFO);
        final int stage = intent.getIntExtra(CURRENT_DOWNLOAD_STAGE, DOWNLOAD_STAGE_UNKNOWN);
        switch (stage) {
            case DOWNLOAD_STAGE_PREPARING:
                onPreparing(request, bundle);
                break;
            case DOWNLOAD_STAGE_PAUSED:
                onPaused(request, bundle);
                break;
            case DOWNLOAD_STAGE_DOWNLOADING:
                long currentFileSize = intent.getLongExtra(CURRENT_FILE_SIZE_BYTES, -1);
                long totalFileSize = intent.getLongExtra(TOTAL_FILE_SIZE_BYTES, -1);
                long downloadSpeed = intent.getLongExtra(DOWNLOAD_SPEED, 0);
                onDownloading(request, currentFileSize, totalFileSize, downloadSpeed, bundle);
                break;
            case DOWNLOAD_STAGE_CANCELED:
                onCanceled(request, bundle);
                break;
            case DOWNLOAD_STAGE_COMPLETED:
                Uri downloadedFileUri = intent.getParcelableExtra(DOWNLOADED_FILE_URI);
                onCompleted(request, downloadedFileUri, bundle);
                break;
            case DOWNLOAD_STAGE_FAILED:
                onFailed(request, bundle);
                break;
        }
    }

    protected void onPreparing(FileDownloadRequest request, Bundle extra) {
    }

    protected void onPaused(FileDownloadRequest request, Bundle extra) {
    }

    protected void onDownloading(FileDownloadRequest request, long currentFileSize, long totalFileSize, long downloadSpeed, Bundle extra) {
    }

    protected void onCanceled(FileDownloadRequest request, Bundle extra) {
    }

    protected void onCompleted(FileDownloadRequest request, Uri downloadedFileUri, Bundle extra) {
    }

    protected void onFailed(FileDownloadRequest request, Bundle extra) {
    }
}
