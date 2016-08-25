package com.localytics.android.itracker.ui;

import android.widget.BaseAdapter;

/**
 * Interface to update adapter on underling data change.
 */
public interface UpdatableAdapter {

    /**
     * Source data was changed.
     * <p/>
     * This function MUST be called from UI thread.
     * <p/>
     * This function must call {@link BaseAdapter#notifyDataSetChanged()} if
     * data was actually changed.
     */
    void onChange();
}