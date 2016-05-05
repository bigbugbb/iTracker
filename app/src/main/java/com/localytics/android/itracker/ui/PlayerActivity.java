package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.View;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.widget.player.controller.PlayerControllerVisibilityListener;

public class PlayerActivity extends Activity implements PlayerControllerVisibilityListener {

    public static final String MEDIA_PLAYER_TITLE = "media_player_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String title = getIntent().getStringExtra(MEDIA_PLAYER_TITLE);

//        ActionBar ab = getSupportActionBar();
//        ab.setDisplayHomeAsUpEnabled(true);
//        ab.setDisplayShowTitleEnabled(true);
//        ab.setTitle(title);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.player_fragment);
        if (fragment == null) {
            final Uri uri = getIntent().getData();
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_fragment, PlayerFragment.newInstance(uri))
                    .commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        PlayerFragment fragment = (PlayerFragment) getFragmentManager().findFragmentById(R.id.player_fragment);
        if (fragment != null) {
            fragment.setVisibilityListener(this);
            fragment.playVideo();
        }
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        // We first dispatch keys to the transport controller -- we want it
//        // to get to consume any media keys rather than letting whoever has focus
//        // in the view hierarchy to potentially eat it.
//        PlayerFragment fragment = (PlayerFragment) getFragmentManager().findFragmentById(R.id.player_fragment);
//        if (fragment != null) {
//            fragment.dispatchKeyEvent(event);
//        }
//
//        return super.dispatchKeyEvent(event);
//    }

    @Override
    public void onControlsVisibilityChange(boolean visible) {
        int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (!visible) {
            newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(newVis);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
                    PlayerFragment fragment = (PlayerFragment) getFragmentManager().findFragmentById(R.id.player_fragment);
                    if (fragment != null) {
                        fragment.showPlayerController();
                    }
                }
            }
        });
    }
}


