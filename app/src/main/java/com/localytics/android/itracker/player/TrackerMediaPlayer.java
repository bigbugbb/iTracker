package com.localytics.android.itracker.player;

import android.media.MediaPlayer;
import android.view.SurfaceHolder;

import java.io.IOException;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class TrackerMediaPlayer implements ITrackerMediaPlayer,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {
    public static final String TAG = makeLogTag(TrackerMediaPlayer.class);

    private MediaPlayer mMediaPlayer;
    private ITrackerMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private ITrackerMediaPlayer.OnCompletionListener mOnCompletionListener;
    private ITrackerMediaPlayer.OnErrorListener mOnErrorListener;
    private ITrackerMediaPlayer.OnInfoListener mOnInfoListener;
    private ITrackerMediaPlayer.OnPreparedListener mOnPreparedListener;
    private ITrackerMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private ITrackerMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    private static TrackerMediaPlayer sMediaPlayer = null;
    public static ITrackerMediaPlayer getInstance() {
        if (sMediaPlayer == null) {
            sMediaPlayer = new TrackerMediaPlayer();
        }
        return sMediaPlayer;
    }

    private TrackerMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public void prepare() throws IllegalStateException, IOException {
        mMediaPlayer.prepare();
    }

    @Override
    public void prepareAsync() {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void release() {

        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnPreparedListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;

        mMediaPlayer.release();

        mMediaPlayer = null;
    }

    @Override
    public void reset(boolean isPlayGone) {
        mMediaPlayer.reset();

    }

    @Override
    public void restart() {
        mMediaPlayer.start();
    }

    @Override
    public void seekTo(int msec) {
        LOGD(TAG, " NativePlayer seekTo : " + msec);
        mMediaPlayer.seekTo(msec);
    }

    @Override
    public void setDataSource(String path) throws IllegalArgumentException,
            IllegalStateException, IOException {
        mMediaPlayer.setDataSource(path);
    }

    @Override
    public void setDisplay(SurfaceHolder display) {
        mMediaPlayer.setDisplay(display);
    }

    @Override
    public void setOnBufferingUpdateListener(ITrackerMediaPlayer.OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnBufferingUpdateListener(null);
        }else {
            mMediaPlayer.setOnBufferingUpdateListener(this);
        }
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnCompletionListener(null);
        } else {
            mMediaPlayer.setOnCompletionListener(this);
        }
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnErrorListener(null);
        }else {
            mMediaPlayer.setOnErrorListener(this);
        }
    }

    @Override
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnInfoListener(null);
        }else {
            mMediaPlayer.setOnInfoListener(this);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnPreparedListener(null);
        }else {
            mMediaPlayer.setOnPreparedListener(this);
        }
    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnSeekCompleteListener(null);
        }else {
            mMediaPlayer.setOnSeekCompleteListener(this);
        }
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
        if (listener == null) {
            mMediaPlayer.setOnVideoSizeChangedListener(null);
        }else {
            mMediaPlayer.setOnVideoSizeChangedListener(this);
        }
    }

    @Override
    public void start() {
        LOGD(TAG, "start");
        mMediaPlayer.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        LOGI(TAG, " --------------- onBufferingUpdate percent = " + percent +  " ------------");
        mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mOnCompletionListener.onCompletion(this);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LOGI(TAG, " --------------- onError hard decode error what = " + what + " arg1 = " + extra + " ------------");
        return mOnErrorListener.onError(this, what, extra);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        LOGI(TAG, " --------------- onInfo what = " + what + " arg1 = " + extra + " ------------");
        return mOnInfoListener.onInfo(this, what, extra);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        LOGI(TAG, "onPrepared");
        mOnPreparedListener.onPrepared(this);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mOnSeekCompleteListener.onSeekComplete(this);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        LOGI(TAG, "onVideoSizeChanged");
        mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height);
    }

    @Override
    public void close() {
//        mMediaPlayer = null;
    }

    public int getAudioData(byte[] data, int len){
        return 0;
    }

    public void playerDetach() {

    }
}