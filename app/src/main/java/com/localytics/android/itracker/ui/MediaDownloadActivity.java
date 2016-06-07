package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.google.api.services.youtube.model.Video;
import com.google.gson.Gson;
import com.localytics.android.itracker.R;

import java.util.ArrayList;
import java.util.List;

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
