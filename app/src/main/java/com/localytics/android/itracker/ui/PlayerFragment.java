package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.TransportMediator;
import android.support.v4.media.TransportPerformer;
import android.support.v7.app.ActionBar;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.widget.MediaController;

import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class PlayerFragment extends Fragment {
    private static final String TAG = makeLogTag(PlayerFragment.class);

    private static final String MEDIA_PLAYER_URI = "media_player_uri";

    private Context mContext;

    private Content mContent;
    private TransportMediator mTransportMediator;
    private MediaController mMediaController;

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

        setRetainInstance(true);
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

        // Find the video player in our UI.
        mContent = (Content) view.findViewById(R.id.media_content);

        // Create transport controller to control video, giving the callback
        // interface to receive actions from.
        mTransportMediator = new TransportMediator(getActivity(), mTransportPerformer);

        // Create and initialize the media control UI.
        mMediaController = (MediaController) view.findViewById(R.id.media_controller);
        mMediaController.setMediaPlayer(mTransportMediator);

        // We're just playing a built-in demo video.
        mContent.init((BaseActivity) getActivity(), mTransportMediator, mMediaController);
        mContent.setVideoURI(mUri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final View decorView = getActivity().getWindow().getDecorView();

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

//        decorView.setOnSystemUiVisibilityChangeListener(
//                new View.OnSystemUiVisibilityChangeListener() {
//                    @Override
//                    public void onSystemUiVisibilityChange(int visibility) {
//                        LOGI(TAG, "onSystemUiVisibilityChange: " + visibility);
//                        decorView.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                enterImmersiveMode();
//                            }
//                        }, 3000);
//
//                    }
//                }
//        );
    }

    @Override
    public void onStart() {
        super.onStart();
        mContent.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mContent.onStop();
    }

    /**
     * Detects and toggles immersive mode (also known as "hide bar" mode).
     */
    public void enterImmersiveMode() {
        if (!isAdded()) {
            return;
        }

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return mTransportMediator.dispatchKeyEvent(event);
    }

    /**
     * Handle actions from on-screen media controls.  Most of these are simple re-directs
     * to the VideoView, some we need to capture to update our state.
     */
    TransportPerformer mTransportPerformer = new TransportPerformer() {
        @Override
        public void onStart() {
            mContent.start();
        }

        @Override
        public void onStop() {
            mContent.pause();
        }

        @Override
        public void onPause() {
            mContent.pause();
        }

        @Override
        public long onGetDuration() {
            return mContent.getDuration();
        }

        @Override
        public long onGetCurrentPosition() {
            return mContent.getCurrentPosition();
        }

        @Override
        public void onSeekTo(long pos) {
            mContent.seekTo((int)pos);
        }

        @Override
        public boolean onIsPlaying() {
            return mContent.isPlaying();
        }

        @Override public int onGetBufferPercentage() {
            return mContent.getBufferPercentage();
        }

        @Override
        public int onGetTransportControlFlags() {
            int flags = TransportMediator.FLAG_KEY_MEDIA_PLAY
                    | TransportMediator.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | TransportMediator.FLAG_KEY_MEDIA_STOP;
            if (mContent.canPause()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_PAUSE;
            }
            if (mContent.canSeekBackward()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_REWIND;
            }
            if (mContent.canSeekForward()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_FAST_FORWARD;
            }
            return flags;
        }
    };

    /**
     * This is the actual video player.  It is the top-level content of
     * the activity's view hierarchy, going under the status bar and nav bar areas.
     */
    public static class Content extends VideoView implements
            View.OnSystemUiVisibilityChangeListener, View.OnClickListener,
            ActionBar.OnMenuVisibilityListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        BaseActivity mActivity;
        TransportMediator mTransportMediator;
        MediaController mMediaController;
        boolean mAddedMenuListener;
        boolean mMenusOpen;
        boolean mPaused;
        boolean mNavVisible;
        int mLastSystemUiVis;

        Runnable mNavHider = new Runnable() {
            @Override
            public void run() {
                setNavVisibility(false);
            }
        };

        Runnable mProgressUpdater = new Runnable() {
            @Override
            public void run() {
                mMediaController.updateProgress();
                getHandler().postDelayed(this, 1000);
            }
        };

        public Content(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnSystemUiVisibilityChangeListener(this);
            setOnClickListener(this);
            setOnPreparedListener(this);
            setOnCompletionListener(this);
            setOnErrorListener(this);
        }

        public void init(BaseActivity activity, TransportMediator transportMediator,
                         MediaController mediaController) {
            // This called by the containing activity to supply the surrounding
            // state of the video player that it will interact with.
            mActivity = activity;
            mTransportMediator = transportMediator;
            mMediaController = mediaController;
            pause();
        }

        public void onStart() {
            if (mActivity != null) {
                mAddedMenuListener = true;
                mActivity.getSupportActionBar().addOnMenuVisibilityListener(this);
            }
        }

        public void onStop() {
            if (mAddedMenuListener) {
                mActivity.getSupportActionBar().removeOnMenuVisibilityListener(this);
            }
            mNavVisible = false;
        }

        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            // Detect when we go out of nav-hidden mode, to clear our state
            // back to having the full UI chrome up.  Only do this when
            // the state is changing and nav is no longer hidden.
            int diff = mLastSystemUiVis ^ visibility;
            mLastSystemUiVis = visibility;
            if ((diff & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                setNavVisibility(true);
            }
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);

            // When we become visible or invisible, play is paused.
            pause();
        }

        @Override
        public void onClick(View v) {
            // Clicking anywhere makes the navigation visible.
            setNavVisibility(true);
        }

        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mMenusOpen = isVisible;
            setNavVisibility(true);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mMediaController.setEnabled(true);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            mTransportMediator.pausePlaying();
            pause();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mTransportMediator.pausePlaying();
            pause();
            return false;
        }

        @Override
        public void start() {
            super.start();
            mPaused = false;
            setKeepScreenOn(true);
            setNavVisibility(true);
            mMediaController.refresh();
            scheduleProgressUpdater();
        }

        @Override
        public void pause() {
            super.pause();
            mPaused = true;
            setKeepScreenOn(false);
            setNavVisibility(true);
            mMediaController.refresh();
            scheduleProgressUpdater();
        }

        void scheduleProgressUpdater() {
            Handler h = getHandler();
            if (h != null) {
                if (mNavVisible && !mPaused) {
                    h.removeCallbacks(mProgressUpdater);
                    h.post(mProgressUpdater);
                } else {
                    h.removeCallbacks(mProgressUpdater);
                }
            }
        }

        void setNavVisibility(boolean visible) {
            int newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!visible) {
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // If we are now visible, schedule a timer for us to go invisible.
            if (visible) {
                Handler h = getHandler();
                if (h != null) {
                    h.removeCallbacks(mNavHider);
                    if (!mMenusOpen && !mPaused) {
                        // If the menus are open or play is paused, we will not auto-hide.
                        h.postDelayed(mNavHider, 3000);
                    }
                }
            }

            // Set the new desired visibility.
            setSystemUiVisibility(newVis);
            mNavVisible = visible;
            mMediaController.setVisibility(visible ? VISIBLE : INVISIBLE);
            scheduleProgressUpdater();
        }
    }
}
