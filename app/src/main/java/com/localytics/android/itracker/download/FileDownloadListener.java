package com.localytics.android.itracker.download;

/**
 * Created by bigbug on 6/16/16.
 */
public interface FileDownloadListener {
    void onPrepare(String downloadId);
    void onProgress(String downloadId, long currentFileSize, long totalFileSize);
    void onPaused(String downloadId);
    void onCanceled(String downloadId);
    void onCompleted(String downloadId);
    void onFailed(String downloadId, Exception e);
}
