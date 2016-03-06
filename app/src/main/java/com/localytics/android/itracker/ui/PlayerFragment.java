package com.localytics.android.itracker.ui;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.util.LogUtils;


public class PlayerFragment extends TrackerFragment {
    private static final String TAG = LogUtils.makeLogTag(PlayerFragment.class);

    public static final String STREAMING_TITLE = "streaming_title";
    public static final String STREAMING_URL = "streaming_url";

    private String mUrl;
    private String mTitle;

    private Context mContext;
    private Handler mMainHandler;

    public PlayerFragment() {
        mMainHandler = new Handler();
    }

    public static PlayerFragment newInstance(String url, String title) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(STREAMING_URL, url);
        args.putString(STREAMING_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        if (getArguments() != null) {
            mUrl = getArguments().getString(STREAMING_URL);
            mTitle = getArguments().getString(STREAMING_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player, container, false);
    }
}
