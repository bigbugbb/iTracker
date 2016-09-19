package com.itracker.android.ui.widget.player.controller;

/**
 * Called to notify that the control have been made visible or hidden.
 * Implementation might want to show/hide actionbar or do other ui adjustments.
 * <p/>
 * Implementation must be provided via the corresponding setter.
 */
public interface PlayerControllerVisibilityListener {

    void onControlsVisibilityChange(boolean value);

}