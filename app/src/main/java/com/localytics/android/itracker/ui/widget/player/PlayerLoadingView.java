package com.localytics.android.itracker.ui.widget.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.localytics.android.itracker.R;


public class PlayerLoadingView extends FrameLayout {

    public PlayerLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerLoadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(getContext()).inflate(R.layout.media_player_loading, this);
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

}