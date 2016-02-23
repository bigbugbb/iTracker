package com.localytics.android.itracker.util;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

/**
 * A ContentObserver that bundles multiple consecutive changes in a short time period into one.
 * This can be used in place of a regular ContentObserver to protect against getting
 * too many consecutive change events as a result of data changes. This observer will wait
 * a while before firing, so if multiple requests come in in quick succession, they will
 * cause it to fire only once.
 */
public class ThrottledContentObserver extends ContentObserver {
    Handler mHandler;
    Runnable mScheduledRun = null;
    private static final int THROTTLE_DELAY = 1000;
    Callbacks mCallback = null;

    public interface Callbacks {
        void onThrottledContentObserverFired();
    }

    public ThrottledContentObserver(Callbacks callback) {
        super(null);
        mHandler  = new Handler();
        mCallback = callback;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (mScheduledRun != null) {
            mHandler.removeCallbacks(mScheduledRun);
        } else {
            mScheduledRun = new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onThrottledContentObserverFired();
                    }
                }
            };
        }
        mHandler.postDelayed(mScheduledRun, THROTTLE_DELAY);
    }

    public void cancelPendingCallback() {
        if (mScheduledRun != null) {
            mHandler.removeCallbacks(mScheduledRun);
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }
}