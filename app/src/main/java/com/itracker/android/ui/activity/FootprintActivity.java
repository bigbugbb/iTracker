package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.itracker.android.R;
import com.itracker.android.data.model.Track;
import com.itracker.android.ui.fragment.FootprintFragment;
import com.itracker.android.ui.fragment.TrackerFragment;

public class FootprintActivity extends BaseActivity {

    public static Intent createIntent(Context context, Track selectedTrack) {
        Intent intent = new Intent(context, FootprintActivity.class);
        intent.putExtra(TrackerFragment.SELECTED_TRACK, selectedTrack);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_footprint);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.footprint_fragment);
        if (fragment == null) {
            final Track track = getIntent().getParcelableExtra(TrackerFragment.SELECTED_TRACK);
            getFragmentManager().beginTransaction()
                    .replace(R.id.footprint_fragment, FootprintFragment.newInstance(track))
                    .commit();
        }
    }
}
