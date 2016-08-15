package com.localytics.android.itracker.utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestUtils {

    private static RequestQueue sRequestQueue;

    public static synchronized RequestQueue getRequestQueue(Context context) {
        if (sRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            sRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return sRequestQueue;
    }

    public static <T> void addToRequestQueue(Context context, Request<T> req) {
        getRequestQueue(context).add(req);
    }
}
