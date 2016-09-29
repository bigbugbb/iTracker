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
package com.itracker.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.itracker.android.BuildConfig;
import com.itracker.android.Config;

import org.json.JSONException;
import org.json.JSONObject;

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

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.LOGE;
import static com.itracker.android.utils.LogUtils.LOGI;
import static com.itracker.android.utils.LogUtils.LOGV;
import static com.itracker.android.utils.LogUtils.LOGW;
import static com.itracker.android.utils.LogUtils.makeLogTag;


/**
 * Helper class used to communicate with the itracker server.
 */
public final class ServerUtils {
    private static final String TAG = makeLogTag(ServerUtils.class);

    private static final String PROPERTY_PUSH_TOKEN = "push_token";
    private static final String PROPERTY_MESSAGE = "message";
    private static final String PROPERTY_TARGET_ACCOUNT = "target_email";

    /**
     * Register this account/device pair within the server.
     *
     * @param context   Current context
     * @param pushToken The FCM registration ID for this device
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String pushToken) {
        LOGI(TAG, "registering device (push_token = " + pushToken + ")");
        String registerUrl = Config.FCM_SERVER_URL + "/register";

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put(PROPERTY_PUSH_TOKEN, pushToken);

            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest jsObjRequest = new JsonObjectRequest(registerUrl, jsonRequest, future, future) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", Config.API_HEADER_ACCEPT);
                    headers.put("Authorization", AccountUtils.getAuthToken(context));
                    return headers;
                }
            };
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 1));
            RequestUtils.addToRequestQueue(jsObjRequest);

            JSONObject response = future.get(10, TimeUnit.SECONDS);
            LOGV(TAG, "Push token updated: " + response);
            if (BuildConfig.DEBUG) {
                ping(context);
            }

            return true;
        } catch (Exception e) {
            LOGE(TAG, "Fail to update push token: " + e.getMessage());
        }

        return false;
    }

    public static void ping(Context context) {
        String pushSendUrl = Config.FCM_SERVER_URL;
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
            conn.setRequestProperty("Accept", Config.API_HEADER_ACCEPT);
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
