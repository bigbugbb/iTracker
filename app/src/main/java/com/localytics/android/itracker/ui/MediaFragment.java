package com.localytics.android.itracker.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.commit451.youtubeextractor.YouTubeExtractor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.PrefUtils;
import com.localytics.android.itracker.util.ThrottledContentObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class MediaFragment extends TrackerFragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = makeLogTag(MediaFragment.class);

    private GoogleApiClient mGoogleApiClient;
    private GoogleAccountCredential mCredential;

    private RecyclerView mMediaView;
    private MediaAdapter mMediaAdapter;

    private SwipeRefreshLayout mMediaSwipeRefresh;

    private View mLoadingPannel;
    private ProgressBar mProgressView;

    private Set<String> mTokenSet = Collections.synchronizedSet(new HashSet<String>());
    private volatile String mNextPageToken;
    private volatile boolean mLoading;

    private boolean mSelectMode;
    private boolean mShowAsGrid;

    private ThrottledContentObserver mVideosObserver;

    private static final long MAX_NUMBER_OF_ITEMS = 50l;
    private static final int REQUEST_PERMISSIONS_TO_GET_ACCOUNTS = 100;

    private static final String SHOW_MEDIA_GRID = "show_media_grid";
    private static final String NEXT_PAGE_TOKEN = "next_page_token";

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String mChosenAccountName;

    // Register an API key here: https://console.developers.google.com
    public static final String KEY = "AIzaSyBKpl_2cbamvaAd8xbCE9KzW5KYEz-DEZo";

    public static final String[] SCOPES = {Scopes.PROFILE, YouTubeScopes.YOUTUBE};

    public MediaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Should be triggered after we taking a new photo
        mVideosObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (videos). Content changed.");
                // do nothing here
            }
        });
        activity.getContentResolver().registerContentObserver(TrackerContract.Videos.CONTENT_URI, true, mVideosObserver);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mVideosObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = 2;

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();

        mCredential = GoogleAccountCredential.usingOAuth2(getActivity(), Arrays.asList(SCOPES));
        mCredential.setBackOff(new ExponentialBackOff());

        mChosenAccountName = PrefUtils.getChosenGoogleAccountName(getActivity());
        mCredential.setSelectedAccountName(mChosenAccountName);

        mMediaAdapter = new MediaAdapter(getActivity());

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mShowAsGrid = sp.getBoolean(SHOW_MEDIA_GRID, false);
        mNextPageToken = sp.getString(NEXT_PAGE_TOKEN, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_media, container, false);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMediaView = (RecyclerView) view.findViewById(R.id.media_view);
        mMediaView.setLayoutManager(mShowAsGrid ? new GridLayoutManager(getActivity(), 2) : new LinearLayoutManager(getActivity()));
        mMediaView.setItemAnimator(new DefaultItemAnimator());
        mMediaView.setAdapter(mMediaAdapter);
        mMediaView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) mMediaView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                LOGD(TAG, String.format("visibleItemCount: %d, firstVisibleItem: %d, totalItemCount: %d",
                        visibleItemCount, firstVisibleItem, totalItemCount));
                if (visibleItemCount + firstVisibleItem >= totalItemCount) {
                    if (!mLoading) {
                        if (mNextPageToken != null && mTokenSet.contains(mNextPageToken)) {
                            return; // All watch histories have been loaded
                        }
                        mLoading = true;
                        mLoadingPannel.setVisibility(View.VISIBLE);
                        mMediaView.setPadding(mMediaView.getPaddingLeft(), mMediaView.getPaddingTop(),
                                mMediaView.getPaddingRight(), mMediaView.getPaddingBottom() + mLoadingPannel.getHeight());
                        reloadYouTubeMedia(false);
                    }
                }
            }
        });

        mMediaSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.media_swipe_refresh);
        mMediaSwipeRefresh.setColorSchemeResources(R.color.colorAccent);
        mMediaSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLoading = true;
                reloadYouTubeMedia(true);
            }
        });

        mProgressView = (ProgressBar) view.findViewById(R.id.progress_view);
        mProgressView.setVisibility(mMediaAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);

        mLoadingPannel = view.findViewById(R.id.loading_pannel);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSelectMode) {
            inflater.inflate(R.menu.menu_media_select, menu);
        } else {
            inflater.inflate(R.menu.menu_media, menu);
            menu.findItem(R.id.action_grid).setVisible(!mShowAsGrid);
            menu.findItem(R.id.action_list).setVisible(mShowAsGrid);
        }

        mMenu = menu;
    }

    @Override
    public void onDestroyOptionsMenu() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_current_downloads: {
                Intent intent = new Intent(getActivity(), MediaDownloadActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_grid: {
                showMediaAsGridOrList(true);
                return true;
            }
            case R.id.action_list: {
                showMediaAsGridOrList(false);
                return true;
            }
            case R.id.action_download: {
                List<Video> videos = mMediaAdapter.getSelectedVideos();
                if (videos.size() == 0) {
                    Toast.makeText(getActivity(), R.string.download_without_selection, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(getActivity(), MediaDownloadActivity.class);
//                intent.putParcelableArrayListExtra(MediaDownloadActivity.EXTRA_VIDEOS_TO_DOWNLOAD, new ArrayList<>(videos));
                startActivity(intent);
                return true;
            }
            case R.id.action_share: {
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMediaAsGridOrList(boolean showAsGrid) {
        mShowAsGrid = showAsGrid;
        mMenu.findItem(R.id.action_grid).setVisible(!mShowAsGrid);
        mMenu.findItem(R.id.action_list).setVisible(mShowAsGrid);
        mMediaView.setLayoutManager(mShowAsGrid ?
                new GridLayoutManager(getActivity(), 2) : new LinearLayoutManager(getActivity()));
        mMediaView.setAdapter(mMediaAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();

        // http://stackoverflow.com/questions/5166201/android-onsaveinstancestate-and-onpause
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(SHOW_MEDIA_GRID, mShowAsGrid);
        editor.putString(NEXT_PAGE_TOKEN, mNextPageToken);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case VideosQuery.TOKEN_NORMAL: {
                Video[] videos = Video.videosFromCursor(data);
                if (videos != null && videos.length > 0) {
                    mMediaAdapter.updateVideos(Arrays.asList(videos));
                    mProgressView.setVisibility(View.GONE);
                } else {
                    reloadYouTubeMedia(true);
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        if (isAdded()) {
            if (mGoogleApiClient != null && !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onFragmentUnselected() {
        super.onFragmentUnselected();
        if (isAdded()) {
            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (!requestGetAccountsPermission()) {
            final String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            mChosenAccountName = accountName;
            mCredential.setSelectedAccountName(accountName);
            PrefUtils.setChosenGoogleAccountName(getActivity(), mChosenAccountName);
        }

        // Check the local cache first for the first time
        if (mMediaAdapter.size() == 0) {
            reloadVideos(getLoaderManager(), MediaFragment.this);
        } else {
            reloadYouTubeMedia(true);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGI(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Toast.makeText(getActivity(), R.string.connection_to_google_play_failed, Toast.LENGTH_SHORT).show();

            LOGE(TAG, String.format("Connection to Play Services Failed, error: %d, reason: %s",
                    connectionResult.getErrorCode(), connectionResult.toString()));
            try {
                connectionResult.startResolutionForResult(getActivity(), TrackerActivity.REQUEST_AUTHORIZATION);
            } catch (IntentSender.SendIntentException e) {
                LOGE(TAG, e.toString(), e);
            }
        }
    }

    private void reloadYouTubeMedia(final boolean loadLatest) {
        final String accountName = mCredential.getSelectedAccountName();

        if (TextUtils.isEmpty(accountName)) {
            mLoading = false;
            return;
        }

        new AsyncTask<Void, Void, List<Video>>() {
            @Override
            protected List<Video> doInBackground(Void... voids) {
                YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
                        .setApplicationName(getString(R.string.app_name))
                        .build();

                try {
                    /*
                     * Now that the user is authenticated, the app makes a channels list request
					 * to get the authenticated user's channel. Returned with that data is the
					 * playlist id for the uploaded videos.
					 * https://developers.google.com/youtube/v3/docs/channels/list
					 */
                    ChannelListResponse clr = youtube.channels()
                            .list("contentDetails").setMine(true).execute();

                    // Get the user's uploads playlist's id from channel list response
                    String watchHistoryPlaylistIds = clr.getItems().get(0)
                            .getContentDetails().getRelatedPlaylists()
                            .getWatchHistory();

                    // Get videos from user's upload playlist with a playlist items list request
                    PlaylistItemListResponse pilr = youtube.playlistItems()
                            .list("id,contentDetails,snippet")
                            .setPlaylistId(watchHistoryPlaylistIds)
                            .setPageToken(loadLatest ? null : mNextPageToken)
                            .setMaxResults(MAX_NUMBER_OF_ITEMS).execute();

                    // Get page token for further loading
                    if (loadLatest) {
                        // First load
                        if (mNextPageToken == null) {
                            mNextPageToken = pilr.getNextPageToken();
                        }
                    } else {
                        mTokenSet.add(mNextPageToken);
                        mNextPageToken = pilr.getNextPageToken();
                    }

                    // Iterate over playlist item list response to get uploaded videos' ids and last watched time
                    LinkedHashMap<String, String> watchedVideos = new LinkedHashMap<>();
                    for (PlaylistItem item : pilr.getItems()) {
                        watchedVideos.put(item.getContentDetails().getVideoId(), item.getSnippet().getPublishedAt().toString());
                    }

                    // Get details of uploaded videos with a videos list request.
                    VideoListResponse vlr = youtube.videos()
                            .list("id,snippet,status,statistics,contentDetails")
                            .setId(TextUtils.join(",", watchedVideos.keySet())).execute();

                    // Add only the public videos to the local videos list.
                    List<Video> videos = new ArrayList<>();
                    for (com.google.api.services.youtube.model.Video youtubeVideo : vlr.getItems()) {
                        if ("public".equals(youtubeVideo.getStatus().getPrivacyStatus())) {
                            Video video = Video.fromYoutubeVideo(youtubeVideo, watchedVideos.get(youtubeVideo.getId()));
                            videos.add(video);
                        }
                    }

                    return videos;

                } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                    showGooglePlayServicesAvailabilityErrorDialog(availabilityException.getConnectionStatusCode());
                } catch (UserRecoverableAuthIOException userRecoverableException) {
                    startActivityForResult(userRecoverableException.getIntent(), TrackerActivity.REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    LOGE(TAG, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mLoading = false;
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<Video> videos) {
                if (isAdded() && videos != null) {
                    LOGD(TAG, "Load videos successfully: " + videos);
                    mMediaAdapter.updateVideos(videos, loadLatest);
                    cacheLocalVideos(videos);
                    if (mLoadingPannel.getVisibility() == View.VISIBLE) {
                        mMediaView.setPadding(mMediaView.getPaddingLeft(), mMediaView.getPaddingTop(),
                                mMediaView.getPaddingRight(), mMediaView.getPaddingBottom() - mLoadingPannel.getHeight());
                    }
                    mProgressView.setVisibility(View.GONE);
                    mLoadingPannel.setVisibility(View.GONE);
                    mMediaSwipeRefresh.setRefreshing(false);
                }
            }

            private void cacheLocalVideos(final List<Video> videos) {
                /**
                 *  Build and apply the operations to insert remote youtube videos as local cache.
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<ContentProviderOperation> ops = new ArrayList<>(videos.size());
                        for (Video video : videos) {
                            ops.add(ContentProviderOperation
                                    .newInsert(TrackerContract.Videos.CONTENT_URI)
                                    .withValue(TrackerContract.Videos.IDENTIFIER, video.identifier)
                                    .withValue(TrackerContract.Videos.THUMBNAIL, video.thumbnail)
                                    .withValue(TrackerContract.Videos.DURATION, video.duration)
                                    .withValue(TrackerContract.Videos.TITLE, video.title)
                                    .withValue(TrackerContract.Videos.OWNER, video.owner)
                                    .withValue(TrackerContract.Videos.PUBLISHED_AND_VIEWS, video.published_and_views)
                                    .withValue(TrackerContract.Videos.WATCHED_TIME, video.watched_time)
                                    .withValue(TrackerContract.Videos.FILE_SIZE, video.file_size)
                                    .build());
                        }
                        try {
                            getActivity().getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                        } catch (RemoteException | OperationApplicationException e) {
                            LOGE(TAG, "Fail to save local youtube video cache: " + e);
                        }
                    }
                }).start();
            }

        }.execute();
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected boolean requestGetAccountsPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            ArrayList<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.GET_ACCOUNTS);

            requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS_TO_GET_ACCOUNTS);

            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // It is possible that the permissions request interaction with the user is interrupted.
        // In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
        if (permissions.length == 0) {
            requestGetAccountsPermission();
            return;
        }

        switch (requestCode) {
            case REQUEST_PERMISSIONS_TO_GET_ACCOUNTS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGoogleApiClient.connect();
                } else {
                    Toast.makeText(getActivity(), R.string.require_get_accounts_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TrackerActivity.REQUEST_GMS_ERROR_DIALOG:
                break;
            case TrackerActivity.REQUEST_VIDEO_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
//                    mFileURI = data.getData();
//                    if (mFileURI != null) {
//                        Intent intent = new Intent(this, ReviewActivity.class);
//                        intent.setData(mFileURI);
//                        startActivity(intent);
//                    }
                }
                break;
            case TrackerActivity.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case TrackerActivity.REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case TrackerActivity.REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        mCredential.setSelectedAccountName(accountName);
                        PrefUtils.setChosenGoogleAccountName(getActivity(), mChosenAccountName);
                    }
                }
                break;
//            case TrackerActivity.REQUEST_DIRECT_TAG:
//                if (resultCode == Activity.RESULT_OK && data != null
//                        && data.getExtras() != null) {
//                    String youtubeId = data.getStringExtra(YOUTUBE_ID);
//                    if (youtubeId.equals(mVideoData.getYouTubeId())) {
//                        directTag(mVideoData);
//                    }
//                }
//                break;
        }
    }

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode, getActivity(), TrackerActivity.REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), TrackerActivity.REQUEST_ACCOUNT_PICKER);
    }

    public void recordVideo(View view) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        // Workaround for Nexus 7 Android 4.3 Intent Returning Null problem
        // create a file to save the video in specific folder (this works for
        // video only)
        // mFileURI = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileURI);

        // set the video image quality to high
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        // start the Video Capture Intent
        startActivityForResult(intent, TrackerActivity.REQUEST_VIDEO_CAPTURE);
    }

    private void directTag(final Video video) {
//        final Video updateVideo = new Video();
//        VideoSnippet snippet = video
//                .addTags(Arrays.asList(
//                        "iTracker",
//                        generateKeywordFromPlaylistId(Config.YOUTUBE_UPLOAD_PLAYLIST)));
//        updateVideo.setSnippet(snippet);
//        updateVideo.setId(video.getYouTubeId());
//
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
//                        .setApplicationName("iTracker")
//                        .build();
//                try {
//                    youtube.videos().update("snippet", updateVideo).execute();
//                } catch (UserRecoverableAuthIOException e) {
//                    startActivityForResult(e.getIntent(), TrackerActivity.REQUEST_AUTHORIZATION);
//                } catch (IOException e) {
//                    LOGE(TAG, e.getMessage());
//                }
//                return null;
//            }
//
//        }.execute();
//
//        Toast.makeText(getActivity(), R.string.video_submitted_to_youtube, Toast.LENGTH_LONG).show();
    }

    public String generateKeywordFromPlaylistId(String playlistId) {
        if (playlistId == null) playlistId = "";
        if (playlistId.indexOf("PL") == 0) {
            playlistId = playlistId.substring(2);
        }
        playlistId = playlistId.replaceAll("\\W", "");
        String keyword = "iTracker".concat(playlistId);
        if (keyword.length() > 30) {
            keyword = keyword.substring(0, 30);
        }
        return keyword;
    }

    private void startMediaPlayback(Uri uri, Video video) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.setData(uri);
        intent.putExtra(PlayerActivity.MEDIA_PLAYER_TITLE, video.title);
        startActivity(intent);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mSelectMode) {
                mSelectMode = false;
                getActivity().invalidateOptionsMenu();
                mMediaAdapter.clearSelectedVideos();
                return true;
            }
        }
        return false;
    }

    private class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

        private Context mContext;
        private LinkedList<Video> mVideos = new LinkedList<>();
        private Set<String> mVideoIds = new HashSet<>();
        private Map<String, Video> mCheckedMap = new LinkedHashMap<>();

        public MediaAdapter(Context context) {
            mContext = context;
        }

        public void updateVideos(List<Video> videos) {
            updateVideos(videos, false);
        }

        public void updateVideos(List<Video> videos, boolean prepend) {
            if (prepend) {
                ListIterator<Video> back = videos.listIterator(videos.size());
                while (back.hasPrevious()) {
                    Video previous = back.previous();
                    if (!mVideoIds.contains(previous.identifier)) {
                        mVideos.push(previous);
                        mVideoIds.add(previous.identifier);
                    }
                }
            } else {
                for (Video video : videos) {
                    if (!mVideoIds.contains(video.identifier)) {
                        mVideos.offer(video);
                        mVideoIds.add(video.identifier);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public List<Video> getVideos() {
            return mVideos;
        }

        public List<Video> getSelectedVideos() {
            List videos = new ArrayList<>();
            for (Video video : mCheckedMap.values()) {
                if (video != null) {
                    videos.add(video);
                }
            }
            return videos;
        }

        public void clearSelectedVideos() {
            mCheckedMap.clear();
            notifyDataSetChanged();
        }

        public int size() {
            return mVideos.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(mShowAsGrid ?
                    R.layout.item_media_grid : R.layout.item_media, parent, false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindData(mVideos.get(position));
        }

        @Override
        public int getItemCount() {
            return mVideos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView  duration;
            TextView  title;
            TextView  owner;
            TextView  published;
            CheckBox  selected;
            View      overlay;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail = (ImageView) itemView.findViewById(R.id.media_thumbnail);
                duration  = (TextView) itemView.findViewById(R.id.media_duration);
                title     = (TextView) itemView.findViewById(R.id.media_title);
                owner     = (TextView) itemView.findViewById(R.id.media_owner);
                published = (TextView) itemView.findViewById(R.id.media_published_at_and_views);
                selected  = (CheckBox) itemView.findViewById(R.id.media_selected);
                overlay   = itemView.findViewById(R.id.selection_overlay);
            }

            public void bindData(final Video video) {
                duration.setText(video.duration);
                title.setText(video.title);
                owner.setText(video.owner);
                published.setText(video.published_and_views);
                Glide.with(MediaFragment.this)
                        .load(video.thumbnail)
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);

                if (mSelectMode) {
                    selected.setVisibility(View.VISIBLE);
                    selected.setChecked(mCheckedMap.get(video.identifier) != null);
                    overlay.setVisibility(mCheckedMap.get(video.identifier) != null ? View.VISIBLE : View.INVISIBLE);
                } else {
                    selected.setVisibility(View.INVISIBLE);
                    overlay.setVisibility(View.INVISIBLE);
                }
                selected.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isChecked = mCheckedMap.get(video.identifier) != null;
                        mCheckedMap.put(video.identifier, isChecked ? null : video);
                        overlay.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                    }
                });

                itemView.setOnClickListener(new View.OnClickListener() {

                    @Nullable
                    public Uri getWorstAvaiableQualityVideoUri(YouTubeExtractor.Result result) {
                        Uri uri = result.getSd240VideoUri();
                        if (uri != null) {
                            return uri;
                        } else {
                            uri = result.getSd360VideoUri();
                            if (uri != null) {
                                return uri;
                            } else {
                                uri = result.getHd720VideoUri();
                                if (uri != null) {
                                    return uri;
                                } else {
                                    uri = result.getHd1080VideoUri();
                                    return uri != null ? uri : null;
                                }
                            }
                        }
                    }

                    @Override
                    public void onClick(View v) {
                        if (mSelectMode) {
                            boolean isChecked = mCheckedMap.get(video.identifier) != null;
                            mCheckedMap.put(video.identifier, isChecked ? null : video);
                            selected.setChecked(!isChecked);
                            overlay.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                            return;
                        }

                        // TODO: play the media in another activity or the floating view
                        YouTubeExtractor extractor = new YouTubeExtractor(video.identifier);
                        extractor.extract(new YouTubeExtractor.Callback() {
                            @Override
                            public void onSuccess(YouTubeExtractor.Result result) {
                                ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                                    final int type = networkInfo.getType();
                                    if (type == ConnectivityManager.TYPE_WIFI) {
                                        Uri uri = result.getBestAvaiableQualityVideoUri();
                                        if (uri != null) {
                                            startMediaPlayback(uri, video);
                                        } else {
                                            Toast.makeText(getActivity(), R.string.media_uri_not_found, Toast.LENGTH_SHORT);
                                        }
                                    } else {
                                        Uri uri = getWorstAvaiableQualityVideoUri(result);
                                        if (uri != null) {
                                            startMediaPlayback(uri, video);
                                        } else {
                                            Toast.makeText(getActivity(), R.string.media_uri_not_found, Toast.LENGTH_SHORT);
                                        }
                                    }
                                } else {
                                    Toast.makeText(getActivity(), R.string.network_disconnected, Toast.LENGTH_SHORT);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                t.printStackTrace();
                            }
                        });
                    }
                });

                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (!mSelectMode) {
                            mSelectMode = true;
                            mCheckedMap.put(video.identifier, video);
                            overlay.setVisibility(View.VISIBLE);
                            getActivity().invalidateOptionsMenu();
                            mMediaAdapter.notifyDataSetChanged();
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
    }
}
