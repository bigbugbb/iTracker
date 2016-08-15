package com.localytics.android.itracker.utils;

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
    Callbacks mCallback = null;
    int mThrottleDelay = 1000;

    public interface Callbacks {
        void onThrottledContentObserverFired();
    }

    public ThrottledContentObserver(Callbacks callback) {
        super(null);
        mHandler  = new Handler();
        mCallback = callback;
    }

    public void setThrottleDelay(int throttleDelay) {
        mThrottleDelay = throttleDelay;
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
        mHandler.postDelayed(mScheduledRun, mThrottleDelay);
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