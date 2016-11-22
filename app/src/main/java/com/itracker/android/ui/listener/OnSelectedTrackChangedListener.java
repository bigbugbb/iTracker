package com.itracker.android.ui.listener;

import com.itracker.android.data.BaseUIListener;
import com.itracker.android.data.model.Track;


public interface OnSelectedTrackChangedListener extends BaseUIListener {

    void onSelectedTrackChanged(Track track);
}