package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.View;

import com.localytics.android.itracker.R;

public class PlayerActivity extends BaseActivity {

    public static final String MEDIA_PLAYER_TITLE = "media_player_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String title = getIntent().getStringExtra(MEDIA_PLAYER_TITLE);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(true);
        ab.setTitle(title);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.player_fragment);
        if (fragment == null) {
            final Uri uri = getIntent().getData();
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_fragment, PlayerFragment.newInstance(uri))
                    .commit();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // We first dispatch keys to the transport controller -- we want it
        // to get to consume any media keys rather than letting whoever has focus
        // in the view hierarchy to potentially eat it.
        PlayerFragment fragment = (PlayerFragment) getFragmentManager().findFragmentById(R.id.player_fragment);
        if (fragment != null) {
            fragment.dispatchKeyEvent(event);
        }

        return super.dispatchKeyEvent(event);
    }
}


