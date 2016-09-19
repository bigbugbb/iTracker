package com.itracker.android.data.extension.blocking;

import com.itracker.android.data.BaseUIListener;

public interface OnBlockedListChangedListener extends BaseUIListener{
    void onBlockedListChanged(String account);
}
