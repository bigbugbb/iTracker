package com.localytics.android.itracker.ui.widget.player;

public interface VideoStateListener {

    void onFirstVideoFrameRendered();

    void onPlay();

    void onBuffer();

    boolean onStopWithExternalError(int position);

}