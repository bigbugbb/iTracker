package com.itracker.android.ui.widget.player.controller;

import com.itracker.android.ui.widget.player.TrackerPlayer;

public interface PlayerController {

    void setMediaPlayer(TrackerPlayer trackerPlayer);

    void setEnabled(boolean value);

    void show(int timeInMilliSeconds);

    void show();

    void hide();

    void setVisibilityListener(PlayerControllerVisibilityListener visibilityListener);

}