package com.localytics.android.itracker.ui.widget.player.gestures;

import android.view.MotionEvent;

public interface PlayerGestureEventListener {
    void onTap();

    void onHorizontalScroll(MotionEvent event, float delta);

    void onVerticalScroll(MotionEvent event, float delta);

    void onSwipeRight();

    void onSwipeLeft();

    void onSwipeBottom();

    void onSwipeTop();
}
