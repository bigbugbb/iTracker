package com.localytics.android.itracker.ui.widget.player.gestures;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class PlayerGestureControllerView extends View{

    private GestureDetector mGestureDetector;
    private PlayerGestureEventListener mListener;

    public PlayerGestureControllerView(Context context) {
        super(context);
    }

    public PlayerGestureControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerGestureControllerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setClickable(true);
        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mayNotifyGestureDetector(event);
        return true;
    }

    private void mayNotifyGestureDetector(MotionEvent event){
        mGestureDetector.onTouchEvent(event);
    }

    public void setPlayerGestureEventsListener(PlayerGestureEventListener listener){
        mGestureDetector = new GestureDetector(getContext(), new PlayerGestureListener(listener, ViewConfiguration.get(getContext())));
    }

}