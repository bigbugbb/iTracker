package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.util.AppQueryHandler;
import com.localytics.android.itracker.util.PrefUtils;
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

    private class DownloadProgressBroadcastReceiver extends FileDownloadBroadcastReceiver {

        protected void onDownloading(FileDownloadRequest request,
                                     long currentFileSize, long totalFileSize, long downloadSpeed, Bundle extra) {
            MediaDownloadAdapter.ViewHolder viewHolder = findViewHolderByRequestId(request.getId());
            if (viewHolder != null) {
                viewHolder.updateDownloadSpeed(downloadSpeed);
                viewHolder.updateDownloadProgress(currentFileSize, totalFileSize);
            }
        }

        protected void onCanceled(FileDownloadRequest request, Bundle extra) {
            AsyncQueryHandler handler = new AppQueryHandler(getActivity().getContentResolver());
            handler.startDelete(0, null, FileDownloads.CONTENT_URI, FileDownloads.FILE_ID + " = ?", new String[]{ request.getId() });
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
                downloadProgress.setMax(100);
            }

            public void bindData(final MediaDownload download) {
                long currentFileSize = PrefUtils.getCurrentDownloadFileSize(mContext, download.identifier);
                long currentDownloadSpeed = PrefUtils.getCurrentDownloadSpeed(mContext, download.identifier);

                Glide.with(MediaDownloadFragment.this)
                        .load(download.thumbnail)
                        .centerCrop()
                        .crossFade()
                        .into(thumbnail);
                title.setText(download.title);

                DownloadStatus status = download.getStatus();
                if (status == DownloadStatus.DOWNLOADING) {
                    downloadSpeed.setText(formatDownloadSpeed(currentDownloadSpeed));
                    downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, download.total_size));
                } else if (status == DownloadStatus.COMPLETED) {
                    downloadSpeed.setText("");
                    downloadFileSize.setText(formatFileTotalSize(download.total_size));
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

                SpannableString statusText = new SpannableString(status.value().toLowerCase());
                if (status == DownloadStatus.COMPLETED) {
                    statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.colorAccent)), 0, statusText.length(), 0);
                } else if (status == DownloadStatus.CONNECTING) {
                    statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, android.R.color.holo_orange_dark)), 0, statusText.length(), 0);
                } else if (status == DownloadStatus.FAILED) {
                    statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, android.R.color.holo_red_light)), 0, statusText.length(), 0);
                }
                downloadStatus.setText(statusText);

                if (status == DownloadStatus.PENDING || status == DownloadStatus.PREPARING ||
                        status == DownloadStatus.DOWNLOADING || status == DownloadStatus.CONNECTING) {
                    if (status == DownloadStatus.CONNECTING) {
                        downloadProgress.setIndeterminate(true);
                    }
                    downloadProgress.setVisibility(View.VISIBLE);
                } else {
                    downloadProgress.setVisibility(View.INVISIBLE);
                }

                final PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FileDownloadManager fdm = FileDownloadManager.getInstance(getActivity());
                        switch (item.getItemId()) {
                            case R.id.action_start_download: {
                                fdm.startDownload(download.identifier);
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
                };

                itemView.setTag(download.identifier);
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(getActivity(), downloadFileSize);
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

                        popupMenu.setOnMenuItemClickListener(menuItemClickListener);

                        popupMenu.show();

                        return true;
                    }
                });
            }

            void updateDownloadSpeed(long bytesPerSecond) {
                downloadSpeed.setText(formatDownloadSpeed(bytesPerSecond));
            }

            void updateDownloadProgress(long currentFileSize, long totalFileSize) {
                final float progress = currentFileSize / (float) totalFileSize;
                downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, totalFileSize));
                downloadProgress.setIndeterminate(false);
                downloadProgress.setProgress((int) (progress * 100));
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
        }
    }
}
