package com.itracker.android.ui.listener;

import android.view.View;

import com.itracker.android.data.BaseUIListener;

public interface OnTrackItemSelectedListener extends BaseUIListener {
    void onTrackItemSelected(View itemView, int position);
}
