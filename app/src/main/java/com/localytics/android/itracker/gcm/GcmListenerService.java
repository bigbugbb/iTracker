package com.localytics.android.itracker.gcm;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.TrackerActivity;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private final static String TAG = makeLogTag(GcmListenerService.class);

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data)
    {
        final Context context = getApplicationContext();

        try
        {
            LOGD(TAG, data.toString());
            showNotification(context, data.toString());
        }
        catch (Exception e)
        {
            LOGE(TAG, "Something went wrong with GCM", e);
        }
    }

    private void showNotification(Context context, String message) {
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