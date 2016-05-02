package com.localytics.android.itracker.ui.widget.player;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * A custom layout we put as a layout root to get notified about any screen touches.
 */
public final class PlayerTouchRoot extends FrameLayout {

    public static final int MIN_INTERCEPTION_TIME = 1000;
    private long mLastInterception;

    private OnTouchReceiver mTouchReceiver;

    public PlayerTouchRoot(final Context context) {
        super(context);
    }

    public PlayerTouchRoot(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerTouchRoot(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        if (mTouchReceiver != null) {
            final long timeStamp = SystemClock.elapsedRealtime();
            // we throttle the touch event dispatch to avoid event spam
            if (timeStamp - mLastInterception > MIN_INTERCEPTION_TIME) {
                mLastInterception = timeStamp;
                mTouchReceiver.onControllerUiTouched();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setOnTouchReceiver(final OnTouchReceiver receiver) {
        this.mTouchReceiver = receiver;
    }

    public interface OnTouchReceiver {
        void onControllerUiTouched();
    }
}