package com.itracker.android.ui.listener;

import android.net.Uri;

public interface MediaPlaybackDelegate {
    void startMediaPlayback(Uri uri, String title);
}