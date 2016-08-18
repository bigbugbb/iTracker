/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.localytics.android.itracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.localytics.android.itracker.BuildConfig;
import com.localytics.android.itracker.Config;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.LOGV;
import static com.localytics.android.itracker.utils.LogUtils.LOGW;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


/**
 * Helper class used to communicate with the itracker server.
 */
public final class ServerUtils {
    private static final String TAG = makeLogTag(ServerUtils.class);

    private static final String PREFERENCES = "com.localytics.android.itracker.service.gcm";
    private static final String PROPERTY_REGISTERED_TS = "registered_ts";
    private static final String PROPERTY_SENDER_ID = "sender_id";
    private static final String PROPERTY_PUSH_TOKEN = "push_token";
    private static final String PROPERTY_MESSAGE = "message";
    private static final String PROPERTY_TARGET_ACCOUNT = "target_email";

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_SECONDS = 2;

    private static boolean checkGcmEnabled() {
        if (TextUtils.isEmpty(Config.GCM_SERVER_URL)) {
            LOGD(TAG, "GCM feature disabled (no URL configured)");
            return false;
        } else if (TextUtils.isEmpty(Config.GCM_API_KEY)) {
            LOGD(TAG, "GCM feature disabled (no API key configured)");
            return false;
        } else if (TextUtils.isEmpty(Config.GCM_SENDER_ID)) {
            LOGD(TAG, "GCM feature disabled (no sender ID configured)");
            return false;
        }
        return true;
    }

    /**
     * Register this account/device pair within the server.
     *
     * @param context   Current context
     * @param pushToken The GCM registration ID for this device
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String pushToken) {
        if (!checkGcmEnabled()) {
            return false;
        }

        LOGI(TAG, "registering device (push_token = " + pushToken + ")");
        String registerUrl = Config.GCM_SERVER_URL + "/register";

        for (int i = 1; i <= MAX_ATTEMPTS; ++i) {
            try {
                RequestFuture<String> future = RequestFuture.newFuture();
                StringRequest request = new StringRequest(Request.Method.POST, registerUrl, future, future) {

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/x-www-form-urlencoded");
                        headers.put("Accept", Config.API_HEADER_ACCEPT);
                        headers.put("Authorization", AccountUtils.getAuthToken(context));
                        return headers;
                    }

                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put(PROPERTY_PUSH_TOKEN, pushToken);
                        return params;
                    }
                };
                RequestUtils.getRequestQueue(context).add(request);

                String response = future.get(10, TimeUnit.SECONDS);
                if (!TextUtils.isEmpty(response)) {
                    LOGV(TAG, "Push token updated: " + response);
                    if (BuildConfig.DEBUG) {
                        ping(context);
                    }
                    return true;
                } else {
                    LOGW(TAG, "Empty response, server must be broken");
                    break;
                }
            } catch (ExecutionException | TimeoutException e) {
                LOGE(TAG, "Fail to update push token: " + e.getMessage());
                try {
                    long timeToWait = (long) Math.pow(BACKOFF_SECONDS, i);
                    LOGD(TAG, "Wait for " + timeToWait + " seconds to try again...");
                    Thread.sleep(timeToWait * DateUtils.SECOND_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGE(TAG, "Push token update request has been cancelled: " + ex.getMessage());
                    break;
                }
            } catch (InterruptedException e) {
                LOGE(TAG, "Push token update request has been cancelled: " + e.getMessage());
                break;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     *
     * @param context Current context
     * @param gcmId   The GCM registration ID for this device
     */
    static void unregister(final Context context, final String gcmId) {
        if (!checkGcmEnabled()) {
            return;
        }

        LOGI(TAG, "unregistering device (gcmId = " + gcmId + ")");
        String serverUrl = Config.GCM_SERVER_URL + "/unregister";
        String authToken = AccountUtils.getAuthToken(context);
        Map<String, String> params = new HashMap<String, String>();
        params.put("reg_id", gcmId);
        try {
            post(serverUrl, authToken, params);
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            LOGD(TAG, "Unable to unregister from application server", e);
        } finally {
            // Regardless of server success, clear local preferences
            setRegisteredOnServer(context, false, null, null);
        }
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Request user data sync.
     *
     * @param context Current context
     */
//    public static void notifyUserDataChanged(final Context context) {
//        if (!checkGcmEnabled()) {
//            return;
//        }
//
//        LOGI(TAG, "Notifying GCM that user data changed");
//        String serverUrl = Config.GCM_SERVER_URL + "/send/self/sync_user";
//        try {
//            String gcmKey = AccountUtils.getGcmKey(context, AccountUtils.getActiveAccountName(context));
//            if (gcmKey != null) {
//                post(serverUrl, new HashMap<String, String>(), gcmKey);
//            }
//        } catch (IOException e) {
//            LOGE(TAG, "Unable to notify GCM about user data change", e);
//        }
//    }

    /**
     * Sets whether the device was successfully registered in the server side.
     *
     * @param context   Current context
     * @param flag      True if registration was successful, false otherwise
     * @param senderId  The push sender id, which equals the project id from google dev console
     * @param pushToken The push registration token from google gcm api
     */
    private static void setRegisteredOnServer(Context context, boolean flag, String senderId, String pushToken) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        LOGD(TAG, "Setting registered on server status as: " + flag + ", gcmKey=" + AccountUtils.sanitizeGcmKey(senderId));
        Editor editor = prefs.edit();
        if (flag) {
            editor.putLong(PROPERTY_REGISTERED_TS, new Date().getTime());
            editor.putString(PROPERTY_SENDER_ID, senderId == null ? "" : senderId);
            editor.putString(PROPERTY_PUSH_TOKEN, pushToken);
        } else {
            editor.remove(PROPERTY_PUSH_TOKEN);
        }
        editor.apply();
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     *
     * @param context Current context
     * @return True if registration was successful, false otherwise
     */
    public static boolean isRegisteredOnServer(Context context, String pushToken) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        // Find registration threshold
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        long yesterdayTS = cal.getTimeInMillis();
        long regTS = prefs.getLong(PROPERTY_REGISTERED_TS, 0);

        pushToken = pushToken == null ? "" : pushToken;

        if (regTS > yesterdayTS) {
            LOGV(TAG, "GCM registration current. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);

            final String registeredPushToken = prefs.getString(PROPERTY_PUSH_TOKEN, "");
            if (registeredPushToken.equals(pushToken)) {
                LOGD(TAG, "GCM registration is valid and for the correct gcm key: "
                        + AccountUtils.sanitizeGcmKey(registeredPushToken));
                return true;
            }
            LOGD(TAG, "GCM registration is for DIFFERENT gcm key "
                    + AccountUtils.sanitizeGcmKey(registeredPushToken) + ". We were expecting "
                    + AccountUtils.sanitizeGcmKey(pushToken));
            return false;
        } else {
            LOGV(TAG, "GCM registration expired. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);
            return false;
        }
    }

    public static String getGcmId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(PROPERTY_PUSH_TOKEN, null);
    }

    /**
     *  Unregister the current GCM ID when we sign-out
     *
     * @param context Current context
     */
    public static void onSignOut(Context context) {
        String gcmId = getGcmId(context);
        if (gcmId != null) {
            unregister(context, gcmId);
        }
    }

    public static void ping(Context context) {
        String pushSendUrl = Config.GCM_SERVER_URL;
        try {
            Map<String, String> params = new HashMap<>();
            params.put(PROPERTY_MESSAGE, "pong");
            params.put(PROPERTY_TARGET_ACCOUNT, AccountUtils.getActiveAccountName(context));
            post(pushSendUrl, AccountUtils.getAuthToken(context), params);
        } catch (IOException e) {
            LOGE(TAG, "Fail to self send a test message");
        }
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint  POST address.
     * @param authToken Auth token for api call.
     * @param params    request parameters.
     * @throws java.io.IOException propagated from POST.
     */
    private static void post(String endpoint, String authToken, Map<String, String> params) throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        LOGV(TAG, "Posting '" + body + "' to " + url);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Content-Length", Integer.toString(body.length()));
            conn.setRequestProperty("Accept", "application/vnd.itracker.v1");
            conn.setRequestProperty("Authorization", authToken);
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status / 100 != 2) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
