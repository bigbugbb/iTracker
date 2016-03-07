package com.localytics.android.itracker.ui;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.player.MediaPlayerService;

public class PlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        if (null == savedInstanceState) {
            Intent intent = getIntent();
            final String url = intent.getStringExtra(PlayerFragment.STREAMING_URL);
            final String title = intent.getStringExtra(PlayerFragment.STREAMING_TITLE);
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_fragment, PlayerFragment.newInstance(url, title))
                    .commit();
        }
    }
}
