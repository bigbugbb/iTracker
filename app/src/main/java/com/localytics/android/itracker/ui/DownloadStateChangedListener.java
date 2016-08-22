package com.localytics.android.itracker.ui;

import android.net.Uri;
import android.os.Bundle;

import com.localytics.android.itracker.data.BaseUIListener;
import com.localytics.android.itracker.service.download.FileDownloadRequest;

public interface DownloadStateChangedListener extends BaseUIListener {
    void onPreparing(FileDownloadRequest request, Bundle extra);
    void onPaused(FileDownloadRequest request, Bundle extra);
    void onDownloading(FileDownloadRequest request, long currentFileSize, long totalFileSize, long downloadSpeed, Bundle extra);
    void onCanceled(FileDownloadRequest request, Bundle extra);
    void onCompleted(FileDownloadRequest request, Uri downloadedFileUri, Bundle extra);
    void onFailed(FileDownloadRequest request, String reason, Bundle extra);
}
