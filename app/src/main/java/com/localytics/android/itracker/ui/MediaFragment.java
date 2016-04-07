package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.util.AccountUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 3/6/16.
 */
public class MediaFragment extends TrackerFragment implements
        OnTimeRangeChangedListener {
    private static final String TAG = makeLogTag(PhotoFragment.class);

    private ListView mStreamingUrlsView;
    private ArrayAdapter<AudioStreamingItem> mAdapter;

    private GoogleAccountCredential mCredential;
    private YouTube mYouTube;

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**
     * This is the directory that will be used under the user's home directory where OAuth tokens will be stored.
     */
    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    static final int REQUEST_AUTHORIZATION = 1;

    static final int REQUEST_ACCOUNT_PICKER = 2;

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

        mCredential = GoogleAccountCredential.usingOAuth2(getActivity(), Collections.singleton(YouTubeScopes.YOUTUBE));
        mCredential.setSelectedAccountName(AccountUtils.getActiveAccountName(getActivity()));
        mYouTube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential).setApplicationName("iTracker").build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_media, container, false);
        mStreamingUrlsView = (ListView) root.findViewById(R.id.streaming_urls_view);
        mStreamingUrlsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioStreamingItem item = mAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra(PlayerFragment.STREAMING_URL, item.mUrl);
                intent.putExtra(PlayerFragment.STREAMING_TITLE, item.mTitle);
                startActivity(intent);
            }
        });

        mAdapter = new StreamingUrlAdapter(getActivity());
        mStreamingUrlsView.setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<AudioStreamingItem> items = new ArrayList<>(5);
        items.add(new AudioStreamingItem("Radio 19", "http://str12.fluidstream.net/radio19.aac"));
        items.add(new AudioStreamingItem("Detskoe Radio Yekaterinburg 89.2 FM", "http://ic3.101.ru:8000/v14_1?"));
        items.add(new AudioStreamingItem("Sputnik news - The Voice of Russia 999 AM", "http://icecast.rian.cdnvideo.ru/rian.voiceeng"));
        items.add(new AudioStreamingItem("Rush FM", "http://198.50.156.36:8708/stream"));
        items.add(new AudioStreamingItem("WKWS 96.1 FM Charleston, WV\n", "http://icy1.abacast.com/wvradio-wkwsfmmp3-64"));

        mAdapter.addAll(items);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");

        if (checkGooglePlayServicesAvailable()) {
            haveGooglePlayServices();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    loadPlaylist();
                } else {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                    }
                }
                break;
        }
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (mCredential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        } else {
            // load calendars
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadPlaylist();
                }
            });
        }
    }

    private void loadPlaylist() {
        // Construct a request to retrieve the current user's channel ID.
        // See https://developers.google.com/youtube/v3/docs/channels/list
        YouTube.Channels.List channelRequest = null;
        try {
            channelRequest = mYouTube.channels().list("contentDetails");
            channelRequest.setMine(true);

            // In the API response, only include channel information needed
            // for this use case.
            channelRequest.setFields("items/contentDetails");
            ChannelListResponse channelResult = channelRequest.execute();

            List<Channel> channelsList = channelResult.getItems();

            if (channelsList != null) {
                // The user's default channel is the first item in the list.
                String channelId = channelsList.get(0).getId();
                LOGD(TAG, channelId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
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

    private static class StreamingUrlAdapter extends ArrayAdapter<AudioStreamingItem> {

        public StreamingUrlAdapter(Context context)
        {
            super(context, 0, new ArrayList<AudioStreamingItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_audio_streaming, parent, false);
                convertView.setTag(convertView.findViewById(R.id.steaming_title));
            }

            TextView title = (TextView) convertView.getTag();
            AudioStreamingItem dataItem = getItem(position);
            if (title != null && dataItem != null) {
                title.setText(dataItem.mTitle);
            }

            return convertView;
        }
    }

    public static class AudioStreamingItem {
        String mTitle;
        String mUrl;

        public AudioStreamingItem(String title, String url) {
            mTitle = title;
            mUrl = url;
        }
    }
}
