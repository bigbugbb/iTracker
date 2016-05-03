package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.widget.player.PlayerLoadingView;
import com.localytics.android.itracker.ui.widget.player.PlayerVideoView;
import com.localytics.android.itracker.ui.widget.player.VideoStateListener;
import com.localytics.android.itracker.ui.widget.player.controller.PlayerController;
import com.localytics.android.itracker.ui.widget.player.controller.PlayerControllerVisibilityListener;
import com.localytics.android.itracker.ui.widget.player.controller.SimpleMediaPlayerController;
import com.localytics.android.itracker.ui.widget.player.controller.MediaPlayerController;

import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class PlayerFragment extends Fragment implements VideoStateListener {
    private static final String TAG = makeLogTag(PlayerFragment.class);

    private static final String MEDIA_PLAYER_URI = "media_player_uri";

    private Context mContext;

    private PlayerVideoView    mTextureView;
    private PlayerController   mPlayerController;
    private PlayerLoadingView  mPlayerLoadingView;

    private Uri mUri;

    public static PlayerFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(MEDIA_PLAYER_URI, uri);

        PlayerFragment fragment = new PlayerFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        if (getArguments() != null) {
            mUri = getArguments().getParcelable(MEDIA_PLAYER_URI);
        } else {
            throw new RuntimeException("PlayerFragment must have a uri to playback.");
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = (PlayerVideoView) view.findViewById(R.id.player_video_texture);
        mPlayerController = (MediaPlayerController) view.findViewById(R.id.player_video_controller);
        mPlayerLoadingView = (PlayerLoadingView) view.findViewById(R.id.player_video_loading);

        mTextureView.setMediaController(mPlayerController);
        mTextureView.setOnPlayStateListener(this);
    }

//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        final View decorView = getActivity().getWindow().getDecorView();
//
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//    }

    public void playVideo() {
        mTextureView.setVideo(mUri, SimpleMediaPlayerController.DEFAULT_VIDEO_START);
        mTextureView.start();
    }

    public void setVisibilityListener(PlayerControllerVisibilityListener visibilityListener) {
        mPlayerController.setVisibilityListener(visibilityListener);
    }

    public void showPlayerController() {
        mPlayerLoadingView.hide();
        mPlayerController.show();
    }

    private void showLoadingView(){
        mPlayerLoadingView.show();
        mPlayerController.hide();
    }

    @Override
    public void onFirstVideoFrameRendered() {
        mPlayerController.show();
    }

    @Override
    public void onPlay() {
        showPlayerController();
    }

    @Override
    public void onBuffer() {
        showLoadingView();
    }

    @Override
    public boolean onStopWithExternalError(int position) {
        return false;
    }
}
