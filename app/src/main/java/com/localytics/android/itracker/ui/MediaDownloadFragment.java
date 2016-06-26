package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.util.ThrottledContentObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
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
        mMediaDownloadsObserver.setThrottleDelay(100);
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
        private Map<String, MediaDownload> mExistingDownloads;

        public MediaDownloadAdapter(Context context) {
            mContext = context;
            mDownloads = new ArrayList<>();
            mExistingDownloads = new HashMap<>();
        }

        public void updateDownloads(MediaDownload[] downloads) {
            updateDownloads(Arrays.asList(downloads));
        }

        public void updateDownloads(List<MediaDownload> downloads) {
            final int size = downloads.size();
            for (int i = size - 1; i >= 0; --i) {
                MediaDownload newDownload = downloads.get(i);
                MediaDownload existingdownload = mExistingDownloads.get(newDownload.identifier);
                if (existingdownload == null) {
                    mDownloads.add(newDownload);
                    mExistingDownloads.put(newDownload.identifier, newDownload);
                } else {
                    LOGI("newDownload", newDownload.title + ":" + newDownload.status);
                    existingdownload.update(newDownload);
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
            ImageView   thumbnail;
            TextView    title;
            TextView    downloadFileSize;
            TextView    downloadSpeed;
            TextView    downloadStatus;
            ProgressBar downloadProgress;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail = (ImageView) itemView.findViewById(R.id.media_thumbnail);
                title     = (TextView) itemView.findViewById(R.id.media_title);
                downloadFileSize = (TextView) itemView.findViewById(R.id.media_file_size);
                downloadSpeed = (TextView) itemView.findViewById(R.id.media_download_speed);
                downloadStatus = (TextView) itemView.findViewById(R.id.media_download_status);
                downloadProgress = (ProgressBar) itemView.findViewById(R.id.media_download_progress);
            }

            public void bindData(final MediaDownload download) {
                Glide.with(MediaDownloadFragment.this)
                        .load(download.thumbnail)
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);
                title.setText(download.title);
                downloadFileSize.setText(formatTotalSize(download.total_size));
                downloadSpeed.setText(isDownloading(download.status) ? "200KB/s" : "");
                LOGI("status_test", download.status);
                downloadStatus.setText(download.status);
                if (shouldShowProgress(download.status)) {
                    downloadProgress.setVisibility(View.VISIBLE);
                    if (isDownloading(download.status)) {
                        downloadProgress.setIndeterminate(false);
                        downloadProgress.setProgress(60);
                    }
                } else {
                    downloadProgress.setVisibility(View.INVISIBLE);
                }
            }

            private String formatTotalSize(long totalSize) {
                float sizeInMb = totalSize / 1024f / 1024f;
                if (sizeInMb < 1) {
                    return String.format("%.2fMB", sizeInMb);
                } else if (sizeInMb > 1024) {
                    float sizeInGb = totalSize / 1024f;
                    return String.format("%.2fGB", sizeInGb);
                }
                return String.format("%.1fMB", sizeInMb);
            }

            private boolean isDownloading(String status) {
                String downloadStatus = TrackerContract.DownloadStatus.DOWNLOADING.value();
                return TextUtils.equals(status, downloadStatus);
            }

            private boolean shouldShowProgress(String status) {
                return status.equalsIgnoreCase(DownloadStatus.INITIALIZED.value()) ||
                        status.equalsIgnoreCase(DownloadStatus.PREPARING.value()) ||
                        status.equalsIgnoreCase(DownloadStatus.DOWNLOADING.value());
            }
        }
    }
}
