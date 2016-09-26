package com.itracker.android.utils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.itracker.android.Application;

public class RequestUtils {

    private static RequestQueue sRequestQueue;

    public static synchronized RequestQueue getRequestQueue() {
        if (sRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            sRequestQueue = Volley.newRequestQueue(Application.getInstance());
        }
        return sRequestQueue;
    }

    public static <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
