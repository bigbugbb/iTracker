package com.localytics.android.itracker.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Photo;
import com.localytics.android.itracker.ui.widget.CollectionView;
import com.localytics.android.itracker.ui.widget.CollectionViewCallbacks;
import com.localytics.android.itracker.ui.widget.PhotoCoordinatorLayout;

import org.joda.time.DateTime;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 3/6/16.
 */
public class MediaFragment extends TrackerFragment {
    private static final String TAG = makeLogTag(PhotoFragment.class);

    private ListView mStreamingUrlsView;
    private ArrayAdapter<AudioStreamingItem> mAdapter;

    public MediaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_media, container, false);
        mStreamingUrlsView = (ListView) root.findViewById(R.id.streaming_urls_view);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void trackTimeRange(long beginTime, long endTime) {
        super.trackTimeRange(beginTime, endTime);
    }

    public static class AudioStreamingItem {
        private String mTitle;
        private String mUrl;

        public AudioStreamingItem(String title, String url) {
            mTitle = title;
            mUrl = url;
        }
    }
}
