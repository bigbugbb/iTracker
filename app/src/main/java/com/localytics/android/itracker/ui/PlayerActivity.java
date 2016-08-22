package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.widget.player.controller.PlayerControllerVisibilityListener;

public class PlayerActivity extends BaseActivity implements PlayerControllerVisibilityListener {

    public static final String MEDIA_PLAYER_TITLE = "media_player_title";

    public static Intent createStartPlaybackIntent(Context context, Uri uri, String title) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.setData(uri);
        intent.putExtra(PlayerActivity.MEDIA_PLAYER_TITLE, title);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

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

    @Override
    protected boolean isOrientationIgnored() {
        return true;
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
        int newVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (!visible) {
            newVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(newVisibility);
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


