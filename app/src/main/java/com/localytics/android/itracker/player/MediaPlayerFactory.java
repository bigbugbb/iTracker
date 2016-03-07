package com.localytics.android.itracker.player;

/**
 * Created by bigbug on 3/6/16.
 */
public class MediaPlayerFactory {

    public static ITrackerMediaPlayer getMediaPlayer(boolean hardwareDecoding) {
        if (hardwareDecoding) {
            return TrackerMediaPlayer.getInstance();
        } else {
            return TrackerNativeMediaPlayer.getInstance();
        }
    }
}
