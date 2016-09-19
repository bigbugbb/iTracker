package com.itracker.android.ui.widget.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

public class VolumeSeekBar extends SeekBar {

    public final OnSeekBarChangeListener mVolumeSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int vol, boolean fromUser) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mVolumeListener.onVolumeStartedDragging();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mVolumeListener.onVolumeFinishedDragging();
        }
    };

    private AudioManager mAudioManager;
    private Listener mVolumeListener;
    private BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVolumeProgress();
        }
    };

    public VolumeSeekBar(Context context) {
        super(context);
    }

    public VolumeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VolumeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onInitializeAccessibilityEvent(final AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(VolumeSeekBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(final AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(VolumeSeekBar.class.getName());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerVolumeReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterVolumeReceiver();
        super.onDetachedFromWindow();
    }

    public void initialise(final Listener volumeListener) {
        this.mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        this.mVolumeListener = volumeListener;

        this.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        this.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        this.setOnSeekBarChangeListener(mVolumeSeekListener);
    }

    private void updateVolumeProgress() {
        this.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private void registerVolumeReceiver() {
        getContext().registerReceiver(mVolumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    private void unregisterVolumeReceiver() {
        getContext().unregisterReceiver(mVolumeReceiver);
    }

    public void manuallyUpdate(int update) {
        mVolumeSeekListener.onProgressChanged(this, update, true);
    }

    public interface Listener {
        void onVolumeStartedDragging();

        void onVolumeFinishedDragging();
    }

}