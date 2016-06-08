package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.localytics.android.itracker.R;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadActivity extends BaseActivity {
    private final static String TAG = makeLogTag(MediaDownloadActivity.class);

    public static final String EXTRA_SELECTED_VIDEOS = "extra_selected_videos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_download);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.media_download_fragment);
        if (fragment == null) {
            final String jsonSelectedVideos = getIntent().getStringExtra(EXTRA_SELECTED_VIDEOS);
            getFragmentManager().beginTransaction()
                    .replace(R.id.media_download_fragment, MediaDownloadFragment.newInstance(jsonSelectedVideos))
                    .commit();
        }
    }
}
