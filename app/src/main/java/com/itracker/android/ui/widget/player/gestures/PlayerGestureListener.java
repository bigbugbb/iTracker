package com.itracker.android.ui.widget.player.gestures;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import static com.itracker.android.utils.LogUtils.LOGI;

public class PlayerGestureListener implements GestureDetector.OnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private final int mMinFlingVelocity;

    public static final String TAG = "PlayerGestureListener";
    private final PlayerGestureEventListener mListener;

    public PlayerGestureListener(PlayerGestureEventListener listener, ViewConfiguration viewConfiguration) {
        mListener = listener;
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mListener.onTap();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        LOGI(TAG, "Long Press");
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.i(TAG, "Scroll");

        float deltaY = e2.getY() - e1.getY();
        float deltaX = e2.getX() - e1.getX();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                mListener.onHorizontalScroll(e2, deltaX);
                if (deltaX > 0) {
                    LOGI(TAG, "Slide right");
                } else {
                    LOGI(TAG, "Slide left");
                }
            }
        } else {
            if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                mListener.onVerticalScroll(e2, deltaY);
                if (deltaY > 0) {
                    LOGI(TAG, "Slide down");
                } else {
                    LOGI(TAG, "Slide up");
                }
            }
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Fling event occurred.  Notification of this one happens after an "up" event.
        LOGI(TAG, "Fling");
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > mMinFlingVelocity) {
                    if (diffX > 0) {
                        mListener.onSwipeRight();
                    } else {
                        mListener.onSwipeLeft();
                    }
                }
                result = true;
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > mMinFlingVelocity) {
                if (diffY > 0) {
                    mListener.onSwipeBottom();
                } else {
                    mListener.onSwipeTop();
                }
            }
            result = true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        LOGI(TAG, "Show Press");
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.i(TAG, "Down");
        return false;
    }

}