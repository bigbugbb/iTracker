package com.localytics.android.itracker.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.ui.MediaDownloadActivity;
import com.localytics.android.itracker.ui.PlayerActivity;

import java.util.HashMap;
import java.util.Map;


class FileDownloadNotificationBuilder {

    private static FileDownloadNotificationBuilder sInstance;

    private Context mContext;
    private Map<String, String> mSortKeys;

    private int mLastSortKey;

    final static String NOTIFICATION_GROUP_FILE_DOWNLOADS = "com.localytics.android.itracker.notification_group_file_downloads";

    public static FileDownloadNotificationBuilder getInstance(final Context context) {
        synchronized (FileDownloadNotificationBuilder.class) {
            if (sInstance == null) {
                sInstance = new FileDownloadNotificationBuilder(context);
            }
        }
        return sInstance;
    }

    private FileDownloadNotificationBuilder(final Context context) {
        mContext = context;
        mSortKeys = new HashMap<>();
        mLastSortKey = 0;
    }

    private String retrieveSortKey(String id) {
        String sortKey = mSortKeys.get(id);
        if (TextUtils.isEmpty(sortKey)) {
            sortKey = String.valueOf(mLastSortKey++);
            mSortKeys.put(id, sortKey);
        }
        return sortKey;
    }

    public Notification newDownloadingNotification(String id, int progress, Map<String, String> downloadInfo) {
        final String sortKey = retrieveSortKey(id);

        Intent intent = new Intent(Config.ACTION_DOWNLOAD_MEDIA);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Build task stack so it can navigate correctly to the parent activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(MediaDownloadActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_media_downloading);

        // Set the content for the custom views in the RemoteViews programmatically.
        contentView.setTextViewText(R.id.media_title, "File downloading...");
        contentView.setTextViewText(R.id.media_name, downloadInfo.get(TrackerContract.MediaDownloads.TITLE));
        contentView.setTextViewText(R.id.media_download_time, downloadInfo.get(TrackerContract.MediaDownloads.START_TIME));
        contentView.setProgressBar(R.id.media_download_progress, 100, progress, false);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setSortKey(sortKey)
                .setGroup(NOTIFICATION_GROUP_FILE_DOWNLOADS)
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                .build();

        // Add a big content view to the notification if supported.
        // Support for expanded notifications was added in API level 16.
        // (The normal contentView is shown when the notification is collapsed, when expanded the
        // big content view set here is displayed.)
        // Inflate and set the layout for the expanded notification view
//            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_media_download_extend);
//            notification.bigContentView = expandedView;

        return notification;
    }

    public Notification newCompletedNotification(String id, Uri fileUri, Map<String, String> downloadInfo) {
        final String sortKey = retrieveSortKey(id);

        Intent intent = new Intent(mContext, PlayerActivity.class);
        intent.setDataAndType(fileUri, getMimeType(fileUri));
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_media_download_completed);

        // Set the content for the custom views in the RemoteViews programmatically.
        contentView.setTextViewText(R.id.media_title, "Download completed");
        contentView.setTextViewText(R.id.media_name, downloadInfo.get(TrackerContract.MediaDownloads.TITLE));
        contentView.setTextViewText(R.id.media_download_time, downloadInfo.get(TrackerContract.MediaDownloads.START_TIME));

        // Build the notification
        Notification notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setSortKey(sortKey)
                .setGroup(NOTIFICATION_GROUP_FILE_DOWNLOADS)
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                .build();

        return notification;
    }

    public Notification newFailedNotification(String id, Map<String, String> downloadInfo, String reason) {
        final String sortKey = retrieveSortKey(id);

        Intent intent = new Intent(Config.ACTION_DOWNLOAD_MEDIA);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Build task stack so it can navigate correctly to the parent activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(MediaDownloadActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_media_download_failed);

        // Set the content for the custom views in the RemoteViews programmatically.
        contentView.setTextViewText(R.id.media_title, "File download failed");
        contentView.setTextViewText(R.id.media_name, downloadInfo.get(TrackerContract.MediaDownloads.TITLE));
        contentView.setTextViewText(R.id.media_download_time, downloadInfo.get(TrackerContract.MediaDownloads.START_TIME));
        contentView.setTextViewText(R.id.media_download_error, TextUtils.isEmpty(reason) ? "Unknown failure" : reason);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setSortKey(sortKey)
                .setGroup(NOTIFICATION_GROUP_FILE_DOWNLOADS)
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                .build();

        return notification;
    }

    public String getMimeType(Uri uri) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = mContext.getContentResolver();
            mimeType = resolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
