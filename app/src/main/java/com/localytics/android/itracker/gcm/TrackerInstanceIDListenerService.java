package com.localytics.android.itracker.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.localytics.android.itracker.util.LogUtils;
import com.localytics.android.itracker.util.PrefUtils;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class TrackerInstanceIDListenerService extends InstanceIDListenerService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);

        PrefUtils.setSentTokenToServer(getApplicationContext(), false);
    }
}