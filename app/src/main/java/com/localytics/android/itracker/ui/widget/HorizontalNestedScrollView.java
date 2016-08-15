package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * Created by bigbug on 11/7/15.
 */
public class HorizontalNestedScrollView extends HorizontalScrollView implements NestedScrollingChild {
    private static final String TAG = makeLogTag(HorizontalNestedScrollView.class);

    private final NestedScrollingChildHelper mNestedScrollingChildHelper;

    private int mLastMotionX;
    private int mActivePointerId;

    public HorizontalNestedScrollView(Context context) {
        this(context, null);
    }

    public HorizontalNestedScrollView(Context context, @Nullable AttributeSet attrs) {
        this(context, null, 0);
    }

    public HorizontalNestedScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mNestedScrollingChildHelper.onDetachedFromWindow();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int actionMasked = MotionEventCompat.getActionMasked(event);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mLastMotionX = (int) event.getX();
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mActivePointerId = -1; // INVALID_POINTER
                stopNestedScroll();
                break;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
            return false;
        }

        final int actionMasked = MotionEventCompat.getActionMasked(event);

        // We only concern about the methods from NestedScrollingChild are called,
        // so the fab in the CoordinateLayout can get notified and perform its behavior.
        switch (actionMasked) {
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int x = (int) MotionEventCompat.getX(event, activePointerIndex);
                int deltaX = mLastMotionX - x;
                dispatchNestedPreScroll(0, deltaX, null, null);
                dispatchNestedScroll(0, deltaX, 0, 0, null);
                break;
        }

        return true;
    }
}
