package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.download.FileDownloadBroadcastReceiver;
import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.download.FileDownloadRequest;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.util.ExternalStorageUtils;
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

    private LocalBroadcastManager mBroadcastManager;
    private DownloadProgressBroadcastReceiver mDownloadProgressBroadcastReceiver;

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

        mDownloadProgressBroadcastReceiver = new DownloadProgressBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileDownloadBroadcastReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);

        mBroadcastManager = LocalBroadcastManager.getInstance(activity);
        mBroadcastManager.registerReceiver(mDownloadProgressBroadcastReceiver, filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mMediaDownloadsObserver);
        mBroadcastManager.unregisterReceiver(mDownloadProgressBroadcastReceiver);
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

    private class DownloadProgressBroadcastReceiver extends FileDownloadBroadcastReceiver {

        protected void onPreparing(FileDownloadRequest request, Bundle extra) {
        }

        protected void onPaused(FileDownloadRequest request, Bundle extra) {
        }

        protected void onDownloading(FileDownloadRequest request, long currentFileSize, long totalFileSize, long downloadSpeed, Bundle extra) {
            LOGI("test_speed", request.getId() + "-" + downloadSpeed);
            MediaDownloadAdapter.ViewHolder viewHolder = findViewHolderByRequestId(request.getId());
            if (viewHolder != null) {
                viewHolder.updateDownloadSpeed(downloadSpeed);
                viewHolder.updateDownloadProgress(currentFileSize, totalFileSize);
            }
        }

        protected void onCanceled(FileDownloadRequest request, Bundle extra) {
        }

        protected void onCompleted(FileDownloadRequest request, Uri downloadedFileUri, Bundle extra) {
            MediaDownloadAdapter.ViewHolder viewHolder = findViewHolderByRequestId(request.getId());
            if (viewHolder != null) {
                viewHolder.clearCachedData();
            }
        }

        protected void onFailed(FileDownloadRequest request, Bundle extra) {
        }

        private MediaDownloadAdapter.ViewHolder findViewHolderByRequestId(String requestId) {
            for (int i = 0; i < mMediaDownloadView.getChildCount(); ++i) {
                View childView = mMediaDownloadView.getChildAt(i);
                String fileId = (String) childView.getTag();
                if (TextUtils.equals(fileId, requestId)) {
                    return (MediaDownloadAdapter.ViewHolder) mMediaDownloadView.getChildViewHolder(childView);
                }
            }
            return null;
        }
    }

    private class MediaDownloadAdapter extends RecyclerView.Adapter<MediaDownloadAdapter.ViewHolder> {

        private Context mContext;
        private ArrayList<MediaDownload> mDownloads;
        private Map<String, MediaDownload> mExistingDownloads;

        private SharedPreferences mSharedPref;

        private static final String PREF_CURRENT_FILE_SIZE = "_pref_current_file_size_";

        public MediaDownloadAdapter(Context context) {
            mContext = context;
            mDownloads = new ArrayList<>();
            mExistingDownloads = new HashMap<>();
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
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
                downloadProgress.setMax(100);
            }

            public void bindData(final MediaDownload download) {
                long currentFileSize = mSharedPref.getLong(PREF_CURRENT_FILE_SIZE + download.file_id, 0);
                Glide.with(MediaDownloadFragment.this)
                        .load(download.thumbnail)
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);
                title.setText(download.title);
                if (isDownloading(download.status)) {
                    downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, download.total_size));
                } else {
                    downloadSpeed.setText("");
                    if (download.total_size > 0) {
                        if (currentFileSize > 0) {
                            // The task has been paused previously
                            downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, download.total_size));
                        } else {
                            downloadFileSize.setText(formatFileTotalSize(download.total_size));
                        }
                    }
                }
                downloadStatus.setText(download.status);
                downloadProgress.setVisibility(shouldShowProgress(download.status) ? View.VISIBLE : View.INVISIBLE);

                itemView.setTag(download.file_id);
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(getActivity(), itemView);
                        Menu menu = popupMenu.getMenu();
                        popupMenu.getMenuInflater().inflate(R.menu.popup_menu_download, menu);
                        DownloadStatus status = DownloadStatus.valueOf(download.status.toUpperCase());

                        if (status == DownloadStatus.PENDING || status == DownloadStatus.PREPARING ||
                                status == DownloadStatus.DOWNLOADING) {
                            menu.findItem(R.id.action_start_download).setVisible(false);
                        } else if (status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED) {
                            menu.findItem(R.id.action_pause_download).setVisible(false);
                        } else if (status == DownloadStatus.COMPLETED) {
                            menu.findItem(R.id.action_start_download).setVisible(false);
                            menu.findItem(R.id.action_pause_download).setVisible(false);
                            menu.findItem(R.id.action_cancel_download).setVisible(false);
                        }

                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                FileDownloadManager fdm = FileDownloadManager.getInstance(getActivity());
                                switch (item.getItemId()) {
                                    case R.id.action_start_download: {
                                        Uri srcUri = Uri.parse(download.target_url);
                                        Uri destUri = Uri.parse(ExternalStorageUtils.getSdCardPath() + "/iTracker/downloads/" + download.identifier);
                                        fdm.startDownload(download.identifier, srcUri, destUri);
                                        break;
                                    }
                                    case R.id.action_pause_download: {
                                        fdm.pauseDownload(download.identifier);
                                        break;
                                    }
                                    case R.id.action_cancel_download: {
                                        fdm.cancelDownload(download.identifier);
                                        break;
                                    }
                                    case R.id.action_show_property: {
                                        // TODO: show a fragment dialog with the download file property
                                        break;
                                    }
                                }
                                return true;
                            }
                        });

                        popupMenu.show();

                        return true;
                    }
                });
            }

            void clearCachedData() {
                final String fileId = (String) itemView.getTag();
                mSharedPref.edit().remove(PREF_CURRENT_FILE_SIZE + fileId).apply();
            }

            void updateDownloadSpeed(long bytesPerSecond) {
                downloadSpeed.setText(formatDownloadSpeed(bytesPerSecond));
            }

            void updateDownloadProgress(long currentFileSize, long totalFileSize) {
                final String fileId = (String) itemView.getTag();
                final float progress = currentFileSize / (float) totalFileSize;
                downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, totalFileSize));
                downloadProgress.setIndeterminate(false);
                downloadProgress.setProgress((int) (progress * 100));
                mSharedPref.edit().putLong(PREF_CURRENT_FILE_SIZE + fileId, currentFileSize).apply();
            }

            private String formatDownloadSpeed(long bytesPerSecond) {
                float kbPerSecond = bytesPerSecond / 1000f;
                if (kbPerSecond < 1) {
                    return String.format("%.1fKB/s", kbPerSecond);
                } else if (kbPerSecond > 1000) {
                    float mbPerSecond = kbPerSecond / 1000f;
                    return String.format("%.1fMB/s", mbPerSecond);
                }
                return String.format("%dKB/s", (int) kbPerSecond);
            }

            private String formatCurrentFileSizeAndFileTotalSize(long currentSize, long totalSize) {
                float currentSizeInMb = currentSize / 1024f / 1024f;
                float totalSizeInMb = totalSize / 1024f / 1024f;
                if (totalSizeInMb < 1) {
                    return String.format("%.2f/%.2fMB", currentSizeInMb, totalSizeInMb);
                } else if (totalSizeInMb > 1024) {
                    float currentSizeInGb = currentSizeInMb / 1024f;
                    float totalSizeInGb = totalSizeInMb / 1024f;
                    return String.format("%.2f/%.2fGB", currentSizeInGb, totalSizeInGb);
                }
                return String.format("%.1f/%.1fMB", currentSizeInMb, totalSizeInMb);
            }

            private String formatFileTotalSize(long totalSize) {
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
                return status.equalsIgnoreCase(DownloadStatus.PENDING.value()) ||
                        status.equalsIgnoreCase(DownloadStatus.PREPARING.value()) ||
                        status.equalsIgnoreCase(DownloadStatus.DOWNLOADING.value());
            }
        }
    }
}
