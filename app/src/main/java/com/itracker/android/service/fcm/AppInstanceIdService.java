package com.itracker.android.service.fcm;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.itracker.android.Application;
import com.itracker.android.utils.PrefUtils;
import com.itracker.android.utils.ServerUtils;

import static com.itracker.android.utils.LogUtils.LOGD;
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
        updateFcmToken();
    }

    public static void updateFcmToken() {
        Application.getInstance().runInBackground(() -> {
            Context context = Application.getInstance();
            try {
                String token = FirebaseInstanceId.getInstance().getToken();
                LOGD(TAG, "Fcm push token: " + token);

                if (ServerUtils.register(token)) {
                    PrefUtils.setSentPushTokenToServer(context, true);
                }
            } catch (Exception e) {
                LOGD(TAG, "Failed to get the Fcm push token", e);
                // If an exception happens while fetching the new token or updating our registration data
                // on a third-party server, this ensures that we'll attempt the update at a later time.
                PrefUtils.setSentPushTokenToServer(context, false);
            }
        });
    }
}