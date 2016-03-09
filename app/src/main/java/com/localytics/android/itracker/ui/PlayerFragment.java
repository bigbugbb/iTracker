package com.localytics.android.itracker.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.player.MediaPlayerService;
import com.localytics.android.itracker.util.LogUtils;


public class PlayerFragment extends TrackerFragment {
    private static final String TAG = LogUtils.makeLogTag(PlayerFragment.class);

    public static final String STREAMING_TITLE = "streaming_title";
    public static final String STREAMING_URL = "streaming_url";

    private String mUrl;
    private String mTitle;

    private Context mContext;
    private Handler mMainHandler;

    private ImageButton mPlayPauseButton;

    private boolean mStarted;

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
        View root = inflater.inflate(R.layout.fragment_player, container, false);
        mPlayPauseButton = (ImageButton) root.findViewById(R.id.play_pause);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlayerService.class);
                intent.setAction(mStarted ? MediaPlayerService.INTENT_ACTION_PLAYER_PAUSE
                        : MediaPlayerService.INTENT_ACTION_PLAYER_PLAY);
                getActivity().startService(intent);

                Drawable drawable = getResources().getDrawable(mStarted ? R.drawable.play_streaming
                        : R.drawable.pause_streaming);
                mPlayPauseButton.setImageDrawable(drawable);

                mStarted = !mStarted;
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        intent.setAction(MediaPlayerService.INTENT_ACTION_PLAYER_OPEN);
        intent.putExtra(MediaPlayerService.PLAYER_PARAMETER_OPEN_URL, mUrl);
        getActivity().startService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        intent.setAction(MediaPlayerService.INTENT_ACTION_PLAYER_CLOSE);
        getActivity().startService(intent);
    }
}
