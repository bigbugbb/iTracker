package com.localytics.android.itracker.ui.widget.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

public class BrightnessSeekBar extends SeekBar {

    public static final int MAX_BRIGHTNESS = 255;
    public static final int MIN_BRIGHTNESS = 0;
    public final SeekBar.OnSeekBarChangeListener mBrightnessSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int brightness, boolean fromUser) {
            setBrightness(brightness);
            setProgress(brightness);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mBrightnessListener.onBrigthnessStartedDragging();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mBrightnessListener.onBrightnessFinishedDragging();
        }
    };
    private Listener mBrightnessListener;

    public BrightnessSeekBar(Context context) {
        super(context);
    }

    public BrightnessSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrightnessSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onInitializeAccessibilityEvent(final AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(BrightnessSeekBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(final AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(BrightnessSeekBar.class.getName());
    }

    public void initialise(Listener brightnessListener) {
        this.setMax(MAX_BRIGHTNESS);
        this.setOnSeekBarChangeListener(mBrightnessSeekListener);
        this.mBrightnessListener = brightnessListener;
        manuallyUpdate(BrightnessHelper.getBrightness(getContext()));
    }

    public void setBrightness(int brightness) {
        if (brightness < MIN_BRIGHTNESS) {
            brightness = MIN_BRIGHTNESS;
        } else if (brightness > MAX_BRIGHTNESS) {
            brightness = MAX_BRIGHTNESS;
        }

        BrightnessHelper.setBrightness(getContext(), brightness);
    }

    public void manuallyUpdate(int update) {
        mBrightnessSeekListener.onProgressChanged(this, update, true);
    }

    public interface Listener {
        void onBrigthnessStartedDragging();

        void onBrightnessFinishedDragging();
    }
}