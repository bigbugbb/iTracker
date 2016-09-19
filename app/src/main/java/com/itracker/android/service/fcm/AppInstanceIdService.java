package com.itracker.android.service.fcm;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.itracker.android.utils.PrefUtils;
import com.itracker.android.utils.ServerUtils;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.LOGI;
import static com.itracker.android.utils.LogUtils.makeLogTag;

public class AppInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = makeLogTag(AppInstanceIdService.class);


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        try {
            // Get updated InstanceID token.
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            LOGD(TAG, "Refreshed token: " + refreshedToken);

            // If you want to send messages to this application instance or
            // manage this apps subscriptions on the server side, send the
            // Instance ID token to your app server.
            sendRegistrationToServer(refreshedToken);
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
    private void sendRegistrationToServer(String token) {
        final Context context = getApplicationContext();
        // Send the token to the app server
        if (ServerUtils.register(context, token)) {
            PrefUtils.setSentTokenToServer(context, true);
        }
    }
}