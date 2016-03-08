package com.localytics.android.itracker.player;

import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class TrackerNativeMediaPlayer implements ITrackerMediaPlayer {
    public static final String TAG = makeLogTag(TrackerNativeMediaPlayer.class);

    static {
        System.loadLibrary("media_player");
        classInitNative();
    }

    private static TrackerNativeMediaPlayer sMediaPlayer = null;
    public static TrackerNativeMediaPlayer getInstance() {
        if (sMediaPlayer == null) {
            sMediaPlayer = new TrackerNativeMediaPlayer();
        }
        return sMediaPlayer;
    }

    private int mVideoWidth  = 0;
    private int mVideoHeight = 0;
    private Surface mSurface = null;
    private LinkedBlockingQueue<Integer> mEventBlockingQueue = new LinkedBlockingQueue<>();

    AudioTrackManager mAudioTrackManager = new AudioTrackManager(this);

    private ITrackerMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private ITrackerMediaPlayer.OnCompletionListener mOnCompletionListener;
    private ITrackerMediaPlayer.OnErrorListener mOnErrorListener;
    private ITrackerMediaPlayer.OnInfoListener mOnInfoListener;
    private ITrackerMediaPlayer.OnPreparedListener mOnPreparedListener;
    private ITrackerMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private ITrackerMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private static ITrackerMediaPlayer.OnCreatePreviewCompleteListener mOnCreatePreviewCompleteListener;

    // -----------------------------------------------------------------------------
    private native int playergetCurrentTime();

    private native int playergetDuration();

    private native int playergetVideoHeight();

    private native int playergetVideoWidth();

    private native boolean playerIsPlaying();

    public native int playercreate();

    private native int playerpause();

    private native int playerclose();

    private native int playerdetach();

    private native int playerseek(double time);

    private native int playeropen(String url,double offset, int remote);

    private native int playerattach(Surface surf);

    private native int playersetVideoMode(int mode);

    private native int playerplay();

    private native void playernative_setup();

    private native void playernative_release();

    private native boolean playerIsOpened();

    private native boolean playerIsPaused();

    private native boolean playerIsClosed();

    private native boolean playerIsPreviewing();

    private native static void classInitNative();

    private native static int respondEvent(int event);

    public native int startpreview(String url, double offset, int framecount);

    private native int stoppreview();

    public native static int getSamplingRate();
    public native static int getChannelCount();
    public native static int getBytePerSample();

    private native int getNativeAudioData(byte[] data, int arrayLen);
    private native int getNativePreviewData(byte[] data, int arrayLen);

    private TrackerNativeMediaPlayer() {
        playernative_setup();
        playercreate();
    }

    public boolean isPlayerClosed() {
        return playerIsClosed();
    }

    public boolean isPlayerPreviewing() {
        return playerIsPreviewing();
    }

    @Override
    protected void finalize() {
        playernative_release();
    }

    public int getCurrentPosition() {
        return playergetCurrentTime() * 1000;
    }

    public int getDuration() {
        return playergetDuration() * 1000;
    }

    public int getVideoHeight() {
        return playergetVideoHeight();
    }

    public int getVideoWidth() {
        return playergetVideoWidth();
    }

    public boolean isPlaying() {
        return playerIsPlaying();
    }

    public void pause() {
        playerpause();
        mAudioTrackManager.pause();
    }

    public void prepare() {
    }

    public void prepareAsync() {
    }

    public void release() {
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnPreparedListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;

        playerdetach();

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(this);
        }
    }

    public void reset(boolean isPlayGone) {
        mHandler.removeCallbacksAndMessages(this);
//        this.isPlayGone = isPlayGone;
//        if (!isPlayGone) {
//            LOGI(TAG, "reset");
//            playerdetach();
//        }
    }

    public void restart() {
        playerplay();
        mAudioTrackManager.play();
    }

    public void seekTo(int msec) {
        LOGD(TAG, " seekTo : " + msec / 1000.0);
        playerseek(msec / 1000.0);
    }

    public void setDataSource(String path) {
        playeropen(path, 0.0, 1);
    }

    public void setOnBufferingUpdateListener(ITrackerMediaPlayer.OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public static void setOnCreatePreviewCompleteListener(OnCreatePreviewCompleteListener listener) {
        mOnCreatePreviewCompleteListener = listener;
    }

    public void start() {
        playerplay();
        mAudioTrackManager.play();
    }

    @Override
    public void setDisplay(SurfaceHolder surfaceHolder) {
        LOGI(TAG, "setDisplay");
        if (surfaceHolder != null) {
            mSurface = surfaceHolder.getSurface();
            LOGI(TAG, "setDisplay playerattach called");
            playerattach(surfaceHolder.getSurface());
        } else {
            LOGI(TAG, "SurfaceHolder is null");
        }
        LOGI(TAG, "setDisplay end");
    }

    public void postEventFromNative(int what, int arg1, int arg2) {
        LOGD(TAG, "what = " + what + " arg1 = " + arg1 + " arg2 = "+ arg2);

//        if (what == ON_PREVIEW_STARTED) {
//            try {
//                mEventBlockingQueue.put(ON_PREVIEW_STARTED);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            LOGD(TAG, "ON_PREVIEW_STARTED  ****************** what = " + what + " arg1 = " + arg1 + " arg2 = "+ arg2);
//            return;
//        }
//
//        if (what == ON_PREVIEW_CAPTURED) {
//            LOGD(TAG, "ON_PREVIEW_CAPTURED  ****************** what = " + what + " arg1 = " + arg1 + " arg2 = "+ arg2);
//            try {
//                mEventBlockingQueue.put(ON_PREVIEW_CAPTURED);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            return;
//        }
//        if (what == ON_PREVIEW_STOPPED) {
//            LOGD(TAG, "ON_PREVIEW_STOPPED  ****************** what = " + what + " arg1 = " + arg1 + " arg2 = "+ arg2);
//            try {
//                mEventBlockingQueue.put(ON_PREVIEW_STOPPED);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
//
//        if(what == ON_ERROR && arg1 == E_BADPREVIEW) {
//            LOGD(TAG, "ON_ERROR E_BADPREVIEW  ****************** what = " + what + " arg1 = " + arg1 + " arg2 = "+ arg2);
//            try {
//                mEventBlockingQueue.put(E_BADPREVIEW);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
//
//        if (mHandler != null) {
//            Message m = mHandler.obtainMessage(what, arg1, arg2);
//            mHandler.sendMessage(m);
//        }
//
//        if(what == ON_VIDEO_SIZE_CHANGED) {
//            synchronized (ITrackerMediaPlayer.LOCK) {
//                try {
//                    LOGI(TAG, "--------------Recieve video size change event from native! ------------------");
//                    LOGI(TAG, "--------------waiting begine ------------------");
//                    ITrackerMediaPlayer.LOCK.wait(5000);
//                    LOGI(TAG, "--------------waiting end ------------------");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    public void clearEvent() {
        mEventBlockingQueue.clear();
    }

    public int getEvent() {
        try {
            Integer eventInteger = mEventBlockingQueue.poll(5, TimeUnit.SECONDS);
            if(eventInteger != null) {
                return eventInteger;
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

//    public int getEvent(long timeout, TimeUnit unit) {
//        try {
//            Integer eventInteger = mEventBlockingQueue.poll(timeout, unit);
//            if(eventInteger != null) {
//                return eventInteger;
//            }
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return -1;
//    }

    public static final int ON_PREPARED 			= 0x00000001;
    public static final int ON_CLOSED 			    = 0x00000002;
    public static final int ON_COMPLETION 			= 0x00000003;
    public static final int ON_BEGIN_BUFFERING 		= 0x00000004;
    public static final int ON_END_BUFFERING 		= 0x00000005;
    public static final int ON_BUFFERING_UPDATE 	= 0x00000006;
    public static final int ON_VIDEO_SIZE_CHANGED 	= 0x00000007;
    public static final int ON_PREVIEW_STARTED 		= 0x00000008;
    public static final int ON_PREVIEW_CAPTURED 	= 0x00000009;
    public static final int ON_PREVIEW_STOPPED 		= 0x0000000A;
    public static final int ON_NOTIFY_SEEK_POSITION = 0x0000000B;
    public static final int ON_NOTIFY_READ_INDEX 	= 0x0000000C;
    public static final int ON_ERROR 				= 0x0000000D;
    public static final int ON_SEEK_COMPLETE 		= 0x0000000E;
    public static final int E_BADPREVIEW = -8;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ON_BUFFERING_UPDATE:
                    if (mOnBufferingUpdateListener != null) {
                        mOnBufferingUpdateListener.onBufferingUpdate(TrackerNativeMediaPlayer.this, msg.arg1);
                    }
                    break;
                case ON_COMPLETION:
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(TrackerNativeMediaPlayer.this);
                    }
                    break;
                case ON_ERROR:
                    if (mOnErrorListener != null) {
                        LOGI(TAG, "ON_ERROR  @@@@@@@@@@@@@@@ ON_ERROR = " + ON_ERROR + " arg1 = " + msg.arg1);
                        //mOnErrorListener.onError(NativePlayerN.this, msg.arg1, msg.arg2);
                        close();
                    }
                    break;
                //case ON_INFO:
                case ON_BEGIN_BUFFERING:
                    if (mOnInfoListener != null) {
                        LOGI(TAG, "Buffering infor debug MEDIA_INFO_BUFFERING_START  NativePlayerN  ******************  ON_BEGIN_BUFFERING = " + ON_BEGIN_BUFFERING + " arg1 = " + msg.arg1);
                        mOnInfoListener.onInfo(TrackerNativeMediaPlayer.this, ITrackerMediaPlayer.MEDIA_INFO_BUFFERING_START/*msg.arg1*/, msg.arg2);
                    }
                    break;
                case ON_END_BUFFERING:
                    if (mOnInfoListener != null) {
                        LOGI(TAG, "Buffering infor debug MEDIA_INFO_BUFFERING_END  NativePlayerN  ******************  ON_END_BUFFERING = " + ON_END_BUFFERING + " arg1 = " + msg.arg1);
                        mOnInfoListener.onInfo(TrackerNativeMediaPlayer.this, ITrackerMediaPlayer.MEDIA_INFO_BUFFERING_END/*msg.arg1*/, msg.arg2);
                    }
                    break;
                case ON_CLOSED:
                    if (mOnInfoListener != null) {
                        LOGI(TAG, "ON_CLOSED  !!!!!!!!!!!!!!!!!!!!");
                        mOnInfoListener.onInfo(TrackerNativeMediaPlayer.this, msg.what, msg.arg1);
                    }
                    break;
                case ON_PREPARED:
                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onPrepared(TrackerNativeMediaPlayer.this);
                    }
                    break;
                case ON_SEEK_COMPLETE:
                    if (mOnSeekCompleteListener != null) {
                        mOnSeekCompleteListener.onSeekComplete(TrackerNativeMediaPlayer.this);
                    }
                    break;
                case ON_VIDEO_SIZE_CHANGED:
                    if (mOnVideoSizeChangedListener != null) {
                        mVideoWidth = msg.arg1;
                        mVideoHeight = msg.arg2;
                        mOnVideoSizeChangedListener.onVideoSizeChanged(TrackerNativeMediaPlayer.this, msg.arg1, msg.arg2);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void close() {
        mHandler.removeCallbacksAndMessages(this);
        LOGI(TAG, "close() called!!!!!!!!!!!!!!!!!!!!");
        mAudioTrackManager.release();
        playerclose();
    }

    public int getAudioData(byte[] data, int arrayLen) {
        return getNativeAudioData(data, arrayLen);
    }

    public void playerDetach() {
        playerdetach();
    }
}