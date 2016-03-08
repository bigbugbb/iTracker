package com.localytics.android.itracker.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

class AudioTrackManager {
    final static String TAG = makeLogTag(AudioTrackManager.class);

    private TrackerNativeMediaPlayer mMediaPlayer;
    private AudioThread mAudioThread;

    public AudioTrackManager(TrackerNativeMediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    public void pause() {
        if (mAudioThread != null) {
            mAudioThread.pause();
        }
    }

    public void play() {
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.setPriority(6);
            mAudioThread.init();
            mAudioThread.start();
        }
        mAudioThread.play();
    }

    public void stop() {
        if (mAudioThread != null) {
            mAudioThread.stopPlay();
        }
    }

    public void release() {
        if(mAudioThread != null ) {
            LOGI(TAG, "********* release start");
            LOGI(TAG, "release mAudioThread.release() called");
            mAudioThread.release();
            mAudioThread = null;
            LOGI(TAG, "********** release end");
        }
    }

    public void flush() {
        if (mAudioThread != null) {
            mAudioThread.flush();
        }
    }

    private int getAudioData(byte[] data, int arrayLen) {
        return mMediaPlayer.getAudioData(data, arrayLen);
    }

    private int getSamplingRate() {
        return mMediaPlayer.getSamplingRate();
    }

    private int getChannelCount() {
        return mMediaPlayer.getChannelCount();
    }

    private int getBytePerSample() {
        return mMediaPlayer.getBytePerSample();
    }

    class AudioThread extends Thread {
        protected AudioTrack mAudioTrack;
        protected int mOutBufSize;
        protected byte[] mOutBytes;
        protected boolean mRun;
        protected boolean mPause;

        public void init() {
            try {
                mRun = true;
                int sampleRate = getSamplingRate();
                int channelCount = getChannelCount();
                int bytePerSample = getBytePerSample();
                int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                int formatConfig = AudioFormat.ENCODING_PCM_16BIT;

                if (channelCount == 2) {
                    channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
                }
                if (bytePerSample == 1) {
                    formatConfig = AudioFormat.ENCODING_PCM_8BIT;
                }
                mOutBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, formatConfig);

                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                        channelConfig, formatConfig, mOutBufSize, AudioTrack.MODE_STREAM);

                mOutBytes = new byte[mOutBufSize];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        OnPlaybackPositionUpdateListener mListener = new OnPlaybackPositionUpdateListener(){
//            @Override
//            public void onMarkerReached(AudioTrack track) {
//                LOGE("---------", "onMarkerReached called!");
//            }
//
//            @Override
//            public void onPeriodicNotification(AudioTrack track) {
//                LOGE("*********", "onPeriodicNotification called!");
//            }
//        };

        @Override
        public void run() {
            if (mAudioTrack == null) {
                return;
            }
            mAudioTrack.play();

            while (mRun) {
                try {
                    int length = getAudioData(mOutBytes, mOutBytes.length);
                    if (length != -1) {
                        mAudioTrack.write(mOutBytes, 0, length);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mPause) {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void stopPlay() {
            mRun = false;
            if (mAudioTrack != null) {
                mAudioTrack.stop();
            }
        }

        public void play() {
            if (mAudioTrack != null) {
                mAudioTrack.play();
                mPause = false;
            }
        }

        public void pause() {
            if (mAudioTrack != null) {
                mAudioTrack.pause();
                mPause = true;
            }
        }

        public void release() {
            if (mAudioTrack != null) {
                mRun = false;
                try {
                    join(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mAudioTrack.release();
            }
        }

        public void flush() {
            if (mAudioTrack != null) {
                mAudioTrack.flush();
            }
        }
    }
}


