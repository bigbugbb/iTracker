package com.localytics.android.itracker.ui;

/**
 * Created by bigbug on 1/13/16.
 */
public interface OnTimeRangeChangedListener {
    void onBeginTimeChanged(long begin);
    void onEndTimeChanged(long end);
}
