package com.localytics.android.itracker.data;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.TaskStackBuilder;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.service.download.FileDownloadRequest;
import com.localytics.android.itracker.service.download.FileDownloadService;
import com.localytics.android.itracker.ui.MediaDownloadActivity;
import com.localytics.android.itracker.utils.ConnectivityUtils;
import com.localytics.android.itracker.utils.YouTubeExtractor;

import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


/**
 * Provides the public api to trigger the file download actions
 */
public class FileDownloadManager extends ContentObserver {
    private final static String TAG = makeLogTag(FileDownloadManager.class);

    private Context mContext;

    private static FileDownloadManager sInstance;
    private static final int NOTIFICATION_ID = 8888;

    public static FileDownloadManager getInstance() {
        synchronized (FileDownloadManager.class) {
            if (sInstance == null) {
                sInstance = new FileDownloadManager();
            }
        }
        return sInstance;
    }

    private FileDownloadManager() {
        super(null);

        mContext = Application.getInstance();
        mContext.getContentResolver().registerContentObserver(FileDownloads.CONTENT_URI, true, this);
    }

    public void recoverStatus() {
        Intent intent = FileDownloadService.createIntent(Application.getInstance());
        intent.setAction(FileDownloadService.ACTION_RECOVER_STATUS);
        mContext.startService(intent);
    }

    public boolean startDownload(String id) {
        if (!ConnectivityUtils.isWifiConnected(mContext)) {
            return false;
        }

        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.START)
                .build();
        postRequest(request);

        return true;
    }

    public boolean startDownload(String id, Uri srcUri, Uri destUri) {
        if (!ConnectivityUtils.isWifiConnected(mContext)) {
            return false;
        }

        FileDownloadRequest request = new FileDownloadRequest.Builder()
                .setRequestId(id)
                .setRequestAction(FileDownloadRequest.RequestAction.START)
                .setSourceUri(srcUri)
                .setDestinationUri(destUri)
                .build();
        postRequest(request);

        return true;
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
        Intent intent = FileDownloadService.createFileDownloadIntent(Application.getInstance(), request);
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

    public boolean startAvailableDownloads() {
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
                    boolean hasDownloadRequest = false;
                    for (MediaDownload download : downloads) {
                        Uri srcUri = Uri.parse(getVideoTargetUrl(download.identifier));
                        Uri destUri = Uri.parse(Config.FILE_DOWNLOAD_DIR_PATH + download.identifier);
                        if (startDownload(download.identifier, srcUri, destUri)) {
                            hasDownloadRequest = true;
                        }
                    }
                    return hasDownloadRequest;
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }

        return false;
    }

    public boolean pauseAvailableDownloads() {
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
                    boolean hasPausedDownload = false;
                    for (MediaDownload download : downloads) {
                        pauseDownload(download.identifier);
                        hasPausedDownload = true;
                    }
                    return hasPausedDownload;
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }

        return false;
    }

    public void startAvailableDownloadsAsync(final boolean withNotification) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (startAvailableDownloads() && withNotification) {
                    sendStartDownloadingNotification(mContext);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    public void pauseAvailableDownloadsAsync(final boolean withNotification) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (pauseAvailableDownloads() && withNotification) {
                    sendPauseDownloadingNotification(mContext);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void sendStartDownloadingNotification(final Context context) {
        String message = context.getResources().getString(R.string.file_download_wifi_connected);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, createDownloadPromptNotification(context, message));

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(NOTIFICATION_ID);
            }
        }, 5000);
    }

    private void sendPauseDownloadingNotification(final Context context) {
        String message = context.getResources().getString(R.string.file_download_wifi_disconnected);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, createDownloadPromptNotification(context, message));

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(NOTIFICATION_ID);
            }
        }, 5000);
    }

    private Notification createDownloadPromptNotification(Context context, String message) {
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle("File downloads")
                .setContentText(message);

        Intent intent = new Intent(Config.ACTION_DOWNLOAD_MEDIA);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Build task stack so it can navigate correctly to the parent activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MediaDownloadActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setFullScreenIntent(pendingIntent, true);
        } else {
            notificationBuilder.setContentIntent(pendingIntent);
        }

        return notificationBuilder.build();
    }
}
