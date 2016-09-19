package com.itracker.android.player;

import android.media.MediaPlayer;
import android.view.SurfaceHolder;

import java.io.IOException;

public interface ITrackerMediaPlayer {
    public final static int MEDIA_INFO_BUFFERING_START = MediaPlayer.MEDIA_INFO_BUFFERING_START;
    public final static int MEDIA_INFO_BUFFERING_END = MediaPlayer.MEDIA_INFO_BUFFERING_END;
    public final static int MEDIA_INFO_SERACH = 0x9999;
    public final static Object LOCK = new Object();

    interface OnBufferingUpdateListener {
        void onBufferingUpdate(ITrackerMediaPlayer p, int percent);
    }

    interface OnCompletionListener {
        void onCompletion(ITrackerMediaPlayer p);
    }

    interface OnErrorListener {
        boolean onError(ITrackerMediaPlayer p, int what, int extra);
    }

    interface OnInfoListener {
        boolean onInfo(ITrackerMediaPlayer ip, int what, int extra);
    }

    interface OnPreparedListener {
        void onPrepared(ITrackerMediaPlayer p);
    }

    interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(ITrackerMediaPlayer p, int width, int height);
    }

    interface OnSeekCompleteListener {
        void onSeekComplete(ITrackerMediaPlayer p);
    }

    interface OnCreatePreviewCompleteListener {
        void OnCreatePreviewComplete();
    }

    int getCurrentPosition();

    int getDuration();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void pause();

    void prepare() throws IllegalStateException, IOException;

    void prepareAsync();

    void release();

    void reset(boolean isPlayGone);

    void restart();

    void seekTo(int msec);

    void setDataSource(String path) throws IllegalArgumentException,
            IllegalStateException, IOException;

    void setDisplay(SurfaceHolder sh);

    void setOnBufferingUpdateListener(ITrackerMediaPlayer.OnBufferingUpdateListener listener);

    void setOnCompletionListener(ITrackerMediaPlayer.OnCompletionListener listener);

    void setOnErrorListener(ITrackerMediaPlayer.OnErrorListener listener);

    void setOnInfoListener(ITrackerMediaPlayer.OnInfoListener listener);

    void setOnPreparedListener(ITrackerMediaPlayer.OnPreparedListener listener);

    void setOnSeekCompleteListener(ITrackerMediaPlayer.OnSeekCompleteListener listener);

    void setOnVideoSizeChangedListener(ITrackerMediaPlayer.OnVideoSizeChangedListener listener);

    void start();

    void close();

    int getAudioData(byte[] data, int arrayLen);

//    void setPlayerService(IQvodPlayer service);

    void playerDetach();
}