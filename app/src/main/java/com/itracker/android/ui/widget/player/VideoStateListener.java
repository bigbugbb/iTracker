package com.itracker.android.ui.widget.player;

public interface VideoStateListener {

    void onFirstVideoFrameRendered();

    void onPlay();

    void onBuffer();

    boolean onStopWithExternalError(int position);

}