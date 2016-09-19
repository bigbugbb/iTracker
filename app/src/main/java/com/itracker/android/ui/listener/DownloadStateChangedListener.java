package com.itracker.android.ui.listener;

import android.net.Uri;
import android.os.Bundle;

import com.itracker.android.data.BaseUIListener;
import com.itracker.android.service.download.FileDownloadRequest;

public interface DownloadStateChangedListener extends BaseUIListener {
    void onPreparing(FileDownloadRequest request, Bundle extra);
    void onPaused(FileDownloadRequest request, Bundle extra);
    void onDownloading(FileDownloadRequest request, long currentFileSize, long totalFileSize, long downloadSpeed, Bundle extra);
    void onCanceled(FileDownloadRequest request, Bundle extra);
    void onCompleted(FileDownloadRequest request, Uri downloadedFileUri, Bundle extra);
    void onFailed(FileDownloadRequest request, String reason, Bundle extra);
}
