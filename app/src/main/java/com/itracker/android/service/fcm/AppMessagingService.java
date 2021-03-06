package com.itracker.android.service.fcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.ui.activity.TrackerActivity;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;

public class AppMessagingService extends FirebaseMessagingService {

    private final static String TAG = makeLogTag(AppMessagingService.class);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        LOGD(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            LOGD(TAG, "Message data payload: " + remoteMessage.getData());
            showNotification(remoteMessage.toString());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            LOGD(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onDeletedMessages() {
    }

    @Override
    public void onMessageSent(java.lang.String s) {
    }

    @Override
    public void onSendError(java.lang.String s, java.lang.Exception e) {
    }

    private void showNotification(String message) {
        Context context = Application.getInstance();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, new Intent(this, TrackerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.tracker_alert))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setContentIntent(contentIntent)
                .build();

        notificationManager.notify(99, notification);
    }
}