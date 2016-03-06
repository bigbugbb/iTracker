package com.localytics.android.itracker.ui;

import android.app.Activity;
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

import com.localytics.android.itracker.R;

import java.util.ArrayList;
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
        items.add(new AudioStreamingItem("九九音乐台", "http://www.zueiai.net/asx3/guangbo_wl_36.asx"));
        items.add(new AudioStreamingItem("WKWS 96.1 FM Charleston, WV\n", "http://icy1.abacast.com/wvradio-wkwsfmmp3-64"));

        mAdapter.addAll(items);
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
