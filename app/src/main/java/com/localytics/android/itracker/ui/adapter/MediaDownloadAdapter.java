package com.localytics.android.itracker.ui.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
import com.localytics.android.itracker.utils.PrefUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.localytics.android.itracker.utils.LogUtils.LOGI;

public class MediaDownloadAdapter extends RecyclerView.Adapter<MediaDownloadAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<MediaDownload> mDownloads;
    private Map<String, MediaDownload> mExistingDownloads;

    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;

    public MediaDownloadAdapter(Context context, View.OnClickListener onClickListener,
                                View.OnLongClickListener onLongClickListener) {
        mContext = context;
        mDownloads = new ArrayList<>();
        mExistingDownloads = new HashMap<>();
        mOnClickListener = onClickListener;
        mOnLongClickListener = onLongClickListener;
    }

    public void updateDownloads(MediaDownload[] downloads) {
        updateDownloads(Arrays.asList(downloads));
    }

    public void updateDownloads(List<MediaDownload> downloads) {
        // remove outdated downloads
        HashSet<String> oldDownloads = new HashSet<>();
        HashSet<String> newDownloads = new HashSet<>();
        for (MediaDownload download : mDownloads) {
            oldDownloads.add(download.identifier);
        }
        for (MediaDownload download : downloads) {
            newDownloads.add(download.identifier);
        }
        if (oldDownloads.removeAll(newDownloads) || newDownloads.isEmpty()) {
            for (String identifier : oldDownloads) {
                MediaDownload outdatedDownload = mExistingDownloads.put(identifier, null);
                mDownloads.remove(outdatedDownload);
            }
        }

        // add new downloads
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

    public MediaDownload getItem(String downloadId) {
        for (MediaDownload download : mDownloads) {
            if (download.identifier.equals(downloadId)) {
                return download;
            }
        }
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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

            Glide.with(mContext)
                    .load(download.thumbnail)
                    .centerCrop()
                    .crossFade()
                    .into(thumbnail);
            title.setText(download.title);

            TrackerContract.DownloadStatus status = download.getStatus();
            if (status == TrackerContract.DownloadStatus.DOWNLOADING) {
                downloadSpeed.setText(formatDownloadSpeed(currentDownloadSpeed));
                downloadFileSize.setText(formatCurrentFileSizeAndFileTotalSize(currentFileSize, download.total_size));
            } else if (status == TrackerContract.DownloadStatus.COMPLETED) {
                downloadSpeed.setText("");
                downloadFileSize.setText(formatFileTotalSize(download.total_size));
            } else {
                downloadSpeed.setText("");
                downloadFileSize.setText("");
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
            if (status == TrackerContract.DownloadStatus.COMPLETED) {
                statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.colorAccent)), 0, statusText.length(), 0);
            } else if (status == TrackerContract.DownloadStatus.CONNECTING) {
                statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, android.R.color.holo_orange_dark)), 0, statusText.length(), 0);
            } else if (status == TrackerContract.DownloadStatus.FAILED) {
                statusText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(mContext, android.R.color.holo_red_light)), 0, statusText.length(), 0);
            }
            downloadStatus.setText(statusText);

            if (status == TrackerContract.DownloadStatus.PENDING || status == TrackerContract.DownloadStatus.PREPARING ||
                    status == TrackerContract.DownloadStatus.DOWNLOADING || status == TrackerContract.DownloadStatus.CONNECTING) {
                if (status == TrackerContract.DownloadStatus.CONNECTING) {
                    downloadProgress.setIndeterminate(true);
                }
                downloadProgress.setVisibility(View.VISIBLE);
            } else {
                downloadProgress.setVisibility(View.INVISIBLE);
            }

            itemView.setTag(download.identifier);
            itemView.setOnClickListener(mOnClickListener);
            itemView.setOnLongClickListener(mOnLongClickListener);
        }

        public void updateDownloadSpeed(long bytesPerSecond) {
            downloadSpeed.setText(formatDownloadSpeed(bytesPerSecond));
        }

        public void updateDownloadProgress(long currentFileSize, long totalFileSize) {
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
