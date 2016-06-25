package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.ThrottledContentObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadFragment extends TrackerFragment {
    private static final String TAG = makeLogTag(MediaDownloadFragment.class);

    private MediaDownloadAdapter mMediaDownloadAdapter;

    private RecyclerView mMediaDownloadView;

    private ThrottledContentObserver mMediaDownloadsObserver;

    public MediaDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaDownloadAdapter = new MediaDownloadAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_download, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMediaDownloadView = (RecyclerView) view.findViewById(R.id.media_download_view);
        mMediaDownloadView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMediaDownloadView.setItemAnimator(new DefaultItemAnimator());
        mMediaDownloadView.setAdapter(mMediaDownloadAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mMediaDownloadsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (file downloads). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting file downloads cursor reload as a result of ContentObserver firing.");
                    reloadMediaDownloads(getLoaderManager(), MediaDownloadFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(TrackerContract.FileDownloads.CONTENT_URI, true, mMediaDownloadsObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mMediaDownloadsObserver);
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadMediaDownloads(getLoaderManager(), MediaDownloadFragment.this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case MediaDownloadsQuery.TOKEN_NORMAL: {
                MediaDownload[] downloads = MediaDownload.downloadsFromCursor(data);
                if (downloads != null && downloads.length > 0) {
                    mMediaDownloadAdapter.updateDownloads(downloads);
                }
                break;
            }
        }
    }

    private class MediaDownloadAdapter extends RecyclerView.Adapter<MediaDownloadAdapter.ViewHolder> {

        private Context mContext;
        private ArrayList<MediaDownload> mDownloads;
        private Set<String> mIdentifiers;

        public MediaDownloadAdapter(Context context) {
            mContext = context;
            mDownloads = new ArrayList<>();
            mIdentifiers = new HashSet<>();
        }

        public void updateDownloads(MediaDownload[] downloads) {
            updateDownloads(Arrays.asList(downloads));
        }

        public void updateDownloads(List<MediaDownload> downloads) {
            final int size = downloads.size();
            for (int i = size - 1; i >= 0; --i) {
                MediaDownload download = downloads.get(i);
                if (!mIdentifiers.contains(download.identifier)) {
                    mDownloads.add(download);
                    mIdentifiers.add(download.identifier);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(R.layout.item_media_download, parent, false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindData(mDownloads.get(mDownloads.size() - 1 - position));
        }

        @Override
        public int getItemCount() {
            return mDownloads.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView  title;
            TextView  downloadFileSize;
            TextView  downloadSpeed;
            TextView  downloadStatus;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail = (ImageView) itemView.findViewById(R.id.media_thumbnail);
                title     = (TextView) itemView.findViewById(R.id.media_title);
                downloadFileSize = (TextView) itemView.findViewById(R.id.media_file_size);
                downloadSpeed = (TextView) itemView.findViewById(R.id.media_download_speed);
                downloadStatus = (TextView) itemView.findViewById(R.id.media_download_status);
            }

            public void bindData(final MediaDownload download) {
                title.setText(download.title);
                downloadFileSize.setText(String.valueOf(download.total_size));
                downloadSpeed.setText("500KB/s");
                downloadStatus.setText(download.status);
                Glide.with(MediaDownloadFragment.this)
                        .load(download.thumbnail)
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);
            }
        }
    }
}
