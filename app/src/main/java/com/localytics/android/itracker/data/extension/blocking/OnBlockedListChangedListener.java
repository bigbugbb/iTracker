package com.localytics.android.itracker.data.extension.blocking;

import com.localytics.android.itracker.data.BaseUIListener;

public interface OnBlockedListChangedListener extends BaseUIListener{
    void onBlockedListChanged(String account);
}
