package com.localytics.android.itracker.ui.adapter;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.ui.listener.MediaPlaybackDelegate;
import com.localytics.android.itracker.ui.listener.OnMediaSelectModeChangedListener;
import com.localytics.android.itracker.utils.ConnectivityUtils;
import com.localytics.android.itracker.utils.YouTubeExtractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    private Activity mContext;
    private LinkedList<Video> mVideos = new LinkedList<>();
    private Set<String> mVideoIds = new HashSet<>();
    private Map<String, Video> mCheckedMap = new LinkedHashMap<>();
    private boolean mMediaSelectModeEnabled;
    private boolean mShowAsGrid;

    private MediaPlaybackDelegate mMediaPlaybackDelegate;
    private OnMediaSelectModeChangedListener mOnMediaSelectModeChangedListener;

    public MediaAdapter(Activity context, MediaPlaybackDelegate mediaPlaybackDelegate,
                        OnMediaSelectModeChangedListener onMediaSelectModeChangedListener) {
        mContext = context;
        mMediaPlaybackDelegate = mediaPlaybackDelegate;
        mOnMediaSelectModeChangedListener = onMediaSelectModeChangedListener;
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

    public void setShowAsGrid(boolean showAsGrid) {
        mShowAsGrid = showAsGrid;
    }

    public void setMediaSelectModeEnabled(boolean enabled) {
        mMediaSelectModeEnabled = enabled;
        if (!enabled) {
            mCheckedMap.clear();
        }
        if (mOnMediaSelectModeChangedListener != null) {
            mOnMediaSelectModeChangedListener.onMediaSelectModeChanged(enabled);
        }
    }

    public boolean isMediaSelectModeEnabled() {
        return mMediaSelectModeEnabled;
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

    private YouTubeExtractor.Callback createYouTubeExtractorCallback(final Video video) {
        return new YouTubeExtractor.Callback() {
            @Override
            public void onSuccess(YouTubeExtractor.Result result) {
                if (ConnectivityUtils.isOnline(mContext)) {
                    final int type = ConnectivityUtils.getNetworkType(mContext);
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        Uri uri = result.getBestAvaiableQualityVideoUri();
                        if (uri != null && mMediaPlaybackDelegate != null) {
                            mMediaPlaybackDelegate.startMediaPlayback(uri, video.title);
                        } else {
                            showMessage(R.string.media_uri_not_found);
                        }
                    } else {
                        Uri uri = result.getWorstAvaiableQualityVideoUri();
                        if (uri != null && mMediaPlaybackDelegate != null) {
                            mMediaPlaybackDelegate.startMediaPlayback(uri, video.title);
                        } else {
                            showMessage(R.string.media_uri_not_found);
                        }
                    }
                } else {
                    showMessage(R.string.network_disconnected);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        };
    }

    private void showMessage(final int resId) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, resId, Toast.LENGTH_SHORT);
            }
        });
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
            Glide.with(mContext)
                    .load(video.thumbnail)
                    .centerCrop()
                    .crossFade()
                    .into(thumbnail);

            if (mMediaSelectModeEnabled) {
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

                @Override
                public void onClick(View v) {
                    if (mMediaSelectModeEnabled) {
                        boolean isChecked = mCheckedMap.get(video.identifier) != null;
                        mCheckedMap.put(video.identifier, isChecked ? null : video);
                        selected.setChecked(!isChecked);
                        overlay.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        return;
                    }

                    YouTubeExtractor extractor = new YouTubeExtractor(video.identifier);
                    extractor.extractAsync(createYouTubeExtractorCallback(video));
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mCheckedMap.put(video.identifier, video);
                    overlay.setVisibility(View.VISIBLE);
                    setMediaSelectModeEnabled(true);
                    return true;
                }
            });
        }
    }
}