package com.localytics.android.itracker.gcm;


import android.content.Context;
import android.os.Bundle;

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
            // do something
            LOGD(TAG, data.toString());
        }
        catch (Exception e)
        {
            LOGE(TAG, "Something went wrong with GCM", e);
        }
    }
}