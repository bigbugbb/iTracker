package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.VideoData;
import com.localytics.android.itracker.util.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String mChosenAccountName;

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int RESULT_PICK_IMAGE_CROP = 4;
    private static final int RESULT_VIDEO_CAP = 5;
    private static final int REQUEST_DIRECT_TAG = 6;

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
        if (TextUtils.isEmpty(Plus.AccountApi.getAccountName(mGoogleApiClient))) {
            return;
        }

        new AsyncTask<Void, Void, List<VideoData>>() {
            @Override
            protected List<VideoData> doInBackground(Void... voids) {

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

                    List<VideoData> videos = new ArrayList<>();

                    // Get videos from user's upload playlist with a playlist items list request
                    PlaylistItemListResponse pilr = youtube.playlistItems()
                            .list("id,contentDetails")
                            .setPlaylistId(uploadsPlaylistId)
                            .setMaxResults(20l).execute();
                    List<String> videoIds = new ArrayList<>();

                    // Iterate over playlist item list response to get uploaded videos' ids.
                    for (PlaylistItem item : pilr.getItems()) {
                        videoIds.add(item.getContentDetails().getVideoId());
                    }

                    // Get details of uploaded videos with a videos list request.
                    VideoListResponse vlr = youtube.videos()
                            .list("id,snippet,status")
                            .setId(TextUtils.join(",", videoIds)).execute();

                    // Add only the public videos to the local videos list.
                    for (Video video : vlr.getItems()) {
                        if ("public".equals(video.getStatus().getPrivacyStatus())) {
                            VideoData videoData = new VideoData();
                            videoData.setVideo(video);
                            videos.add(videoData);
                        }
                    }

                    // Sort videos by title
                    Collections.sort(videos, new Comparator<VideoData>() {
                        @Override
                        public int compare(VideoData videoData, VideoData videoData2) {
                            return videoData.getTitle().compareTo(videoData2.getTitle());
                        }
                    });

                    return videos;

                } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                    showGooglePlayServicesAvailabilityErrorDialog(availabilityException.getConnectionStatusCode());
                } catch (UserRecoverableAuthIOException userRecoverableException) {
                    startActivityForResult(userRecoverableException.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    LOGE(TAG, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<VideoData> videos) {
                if (isAdded() && videos == null) {
                    return;
                }
            }

        }.execute();
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
                connectionResult.startResolutionForResult(getActivity(), 0);
            } catch (IntentSender.SendIntentException e) {
                LOGE(TAG, e.toString(), e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
//            case RESULT_PICK_IMAGE_CROP:
//                if (resultCode == RESULT_OK) {
//                    mFileURI = data.getData();
//                    if (mFileURI != null) {
//                        Intent intent = new Intent(this, ReviewActivity.class);
//                        intent.setData(mFileURI);
//                        startActivity(intent);
//                    }
//                }
//                break;
//
            case RESULT_VIDEO_CAP:
                if (resultCode == Activity.RESULT_OK) {
//                    mFileURI = data.getData();
//                    if (mFileURI != null) {
//                        Intent intent = new Intent(this, ReviewActivity.class);
//                        intent.setData(mFileURI);
//                        startActivity(intent);
//                    }
                }
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        mCredential.setSelectedAccountName(accountName);
                        PrefUtils.setChosenGoogleAccountName(getActivity(), mChosenAccountName);
                    }
                }
                break;
//            case REQUEST_DIRECT_TAG:
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
                        connectionStatusCode, getActivity(), REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (TextUtils.isEmpty(mCredential.getSelectedAccountName())) {
            // ask user to choose account
            chooseAccount();
        }
    }

    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
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
        startActivityForResult(intent, RESULT_VIDEO_CAP);
    }

    private void directTag(final VideoData video) {
        final Video updateVideo = new Video();
        VideoSnippet snippet = video
                .addTags(Arrays.asList(
                        "iTracker",
                        generateKeywordFromPlaylistId(Config.YOUTUBE_UPLOAD_PLAYLIST)));
        updateVideo.setSnippet(snippet);
        updateVideo.setId(video.getYouTubeId());

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
                        .setApplicationName("iTracker")
                        .build();
                try {
                    youtube.videos().update("snippet", updateVideo).execute();
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    LOGE(TAG, e.getMessage());
                }
                return null;
            }

        }.execute();
        Toast.makeText(getActivity(), R.string.video_submitted_to_youtube, Toast.LENGTH_LONG).show();
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
}
