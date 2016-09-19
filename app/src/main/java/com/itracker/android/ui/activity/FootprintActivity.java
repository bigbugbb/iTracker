package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.itracker.android.R;
import com.itracker.android.data.model.Track;
import com.itracker.android.ui.fragment.FootprintFragment;
import com.itracker.android.ui.fragment.TrackerFragment;

public class FootprintActivity extends BaseActivity {

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
