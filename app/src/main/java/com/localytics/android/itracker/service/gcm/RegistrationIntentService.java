package com.localytics.android.itracker.service.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.utils.PrefUtils;
import com.localytics.android.itracker.utils.ServerUtils;

import java.io.IOException;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = makeLogTag(RegistrationIntentService.class);
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        try {
            // Initially this call goes out to the network to retrieve the token, subsequent calls are local.
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(Config.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            LOGI(TAG, "GCM Registration Token: " + token);

            if (!PrefUtils.hasSentTokenToServer(context)) {
                sendTokenToServer(token);
            }

            // Subscribe to topic channels
            subscribeTopics(token);
        } catch (Exception e) {
            LOGD(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            PrefUtils.setSentTokenToServer(getApplicationContext(), false);
        }
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendTokenToServer(String token) {
        final Context context = getApplicationContext();
        // Send the token to the app server
        if (ServerUtils.register(context, token)) {
            PrefUtils.setSentTokenToServer(context, true);
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}