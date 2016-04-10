package com.localytics.android.itracker.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.util.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class MediaFragment extends TrackerFragment implements
        OnTimeRangeChangedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = makeLogTag(MediaFragment.class);

    private GoogleApiClient mGoogleApiClient;
    private GoogleAccountCredential mCredential;

    private RecyclerView mMediaView;
    private MediaAdapter mMediaAdapter;

    private ProgressBar mProgressView;

    private static final int REQUEST_PERMISSIONS_TO_GET_ACCOUNTS = 100;

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
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        mMediaView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMediaView.setItemAnimator(new DefaultItemAnimator());
        mMediaView.setAdapter(mMediaAdapter);

        mProgressView = (ProgressBar) view.findViewById(R.id.progress_view);
        mProgressView.setVisibility(mMediaAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);

//        YouTubeExtractor extractor = new YouTubeExtractor("1pyfMnF6j_g");
//        extractor.extract(new YouTubeExtractor.Callback() {
//            @Override
//            public void onSuccess(YouTubeExtractor.Result result) {
//                LOGD(TAG, result.getHd720VideoUri().toString());
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                t.printStackTrace();
//            }
//        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSelected) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSelected) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onFragmentUnselected() {
        super.onFragmentUnselected();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (!requestGetAccountsPermission()) {
            final String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            mChosenAccountName = accountName;
            mCredential.setSelectedAccountName(accountName);
            PrefUtils.setChosenGoogleAccountName(getActivity(), mChosenAccountName);
        }

        reloadYouTubeMedia();
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

    private void reloadYouTubeMedia() {
        final String accountName = mCredential.getSelectedAccountName();

        if (TextUtils.isEmpty(accountName)) {
            return;
        }

        new AsyncTask<Void, Void, List<Video>>() {
            @Override
            protected List<Video> doInBackground(Void... voids) {

                YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
                        .setApplicationName("iTracker")
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
                    String uploadsPlaylistId = clr.getItems().get(0)
                            .getContentDetails().getRelatedPlaylists()
                            .getWatchHistory();

                    List<Video> videos = new ArrayList<>();

                    // Get videos from user's upload playlist with a playlist items list request
                    PlaylistItemListResponse pilr = youtube.playlistItems()
                            .list("id,contentDetails")
                            .setPlaylistId(uploadsPlaylistId)
                            .setMaxResults(50l).execute();
                    List<String> videoIds = new ArrayList<>();

                    // Iterate over playlist item list response to get uploaded videos' ids.
                    for (PlaylistItem item : pilr.getItems()) {
                        videoIds.add(item.getContentDetails().getVideoId());
                    }

                    // Get details of uploaded videos with a videos list request.
                    VideoListResponse vlr = youtube.videos()
                            .list("id,snippet,status,statistics,contentDetails")
                            .setId(TextUtils.join(",", videoIds)).execute();

                    // Add only the public videos to the local videos list.
                    for (Video video : vlr.getItems()) {
                        if ("public".equals(video.getStatus().getPrivacyStatus())) {
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
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<Video> videos) {
                if (isAdded() && videos != null) {
                    LOGD(TAG, "Load videos successfully: " + videos);
                    mMediaAdapter.updateVideos(videos);
                    mProgressView.setVisibility(View.GONE);
                }
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

    @Override
    public void trackTimeRange(long beginTime, long endTime) {
        super.trackTimeRange(beginTime, endTime);
    }

    @Override
    public void onBeginTimeChanged(long begin) {
        mBeginTime = begin;
    }

    @Override
    public void onEndTimeChanged(long end) {
        mEndTime = end;
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

    private class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

        private Context mContext;
        private List<Video> mVideos = new ArrayList<>();

        public MediaAdapter(Context context) {
            mContext = context;
        }

        public void updateVideos(List<Video> videos) {
            mVideos.clear();
            mVideos.addAll(videos);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(R.layout.item_media, parent, false);
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

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail = (ImageView) itemView.findViewById(R.id.media_thumbnail);
                duration  = (TextView) itemView.findViewById(R.id.media_duration);
                title     = (TextView) itemView.findViewById(R.id.media_title);
                owner     = (TextView) itemView.findViewById(R.id.media_owner);
                published = (TextView) itemView.findViewById(R.id.media_published_at_and_views);
            }

            public void bindData(Video video) {
                VideoSnippet snippet = video.getSnippet();
                VideoStatistics statistics = video.getStatistics();
                VideoContentDetails contentDetails = video.getContentDetails();

                duration.setText(formatDuration(contentDetails.getDuration().substring(2)));
                title.setText(snippet.getTitle());
                owner.setText(snippet.getChannelTitle());
                published.setText(formatPublishedAtAndViews(snippet.getPublishedAt(), statistics.getViewCount().longValue()));
                Glide.with(MediaFragment.this)
                        .load(snippet.getThumbnails().getDefault().getUrl())
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: play the media in another activity or the floating view
                    }
                });
            }

            private String formatDuration(String duration) {
                StringBuilder sb = new StringBuilder();
                int startIndex = 0;
                int indexH = duration.indexOf("H");
                if (indexH != -1) {
                    sb.append(duration.substring(startIndex, indexH))
                            .append(':');
                    startIndex = indexH + 1;
                }
                int indexM = duration.indexOf("M");
                if (indexM != -1) {
                    sb.append(indexM - startIndex < 2 && startIndex > 0 ? "0" : "")
                            .append(duration.substring(startIndex, indexM))
                            .append(':');
                    startIndex = indexM + 1;
                }
                int indexS = duration.indexOf("S");
                if (indexS != -1) {
                    if (startIndex == 0) {
                        sb.append("0:");
                    }
                    sb.append(indexS - startIndex < 2 ? "0" : "").append(duration.substring(startIndex, indexS));
                }
                return sb.toString();
            }

            private String formatPublishedAtAndViews(DateTime time, long viewCount) {
                String publishedAt = time.toString().substring(0, 10);
                if (viewCount >= 1000000) {
                    return String.format("%s · %.1fM views", publishedAt, viewCount / 1000000.0);
                } else if (viewCount >= 1000) {
                    return String.format("%s · %dK views", publishedAt, viewCount / 1000);
                } else {
                    return String.format("%s · %d views", publishedAt, viewCount);
                }
            }
        }
    }
}
