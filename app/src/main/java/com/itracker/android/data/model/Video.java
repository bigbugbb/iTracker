package com.itracker.android.data.model;


import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.itracker.android.provider.TrackerContract;

public final class Video implements Parcelable {

    public String identifier;
    public String thumbnail;
    public String duration;
    public String title;
    public String owner;
    public String published_and_views;
    public String watched_time;

    public Video() {
    }

    public static Video fromYoutubeVideo(com.google.api.services.youtube.model.Video youtubeVideo, String watchedTime) {
        VideoSnippet snippet = youtubeVideo.getSnippet();
        ThumbnailDetails thumbnailDetails = snippet.getThumbnails();
        VideoStatistics statistics = youtubeVideo.getStatistics();
        VideoContentDetails contentDetails = youtubeVideo.getContentDetails();

        if (thumbnailDetails == null || statistics == null || contentDetails == null) {
            return null;
        }

        Video video = new Video();
        video.identifier = youtubeVideo.getId();
        video.thumbnail = thumbnailDetails.getHigh().getUrl();
        video.duration = formatDuration(contentDetails.getDuration().substring(2));
        video.title = snippet.getTitle();
        video.owner = snippet.getChannelTitle();
        video.published_and_views = formatPublishedAtAndViews(snippet.getPublishedAt(), statistics.getViewCount().longValue());
        video.watched_time = watchedTime;

        return video;
    }

    public Video(Cursor cursor) {
        identifier = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.IDENTIFIER));
        thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.THUMBNAIL));
        duration = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.DURATION));
        title = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.TITLE));
        owner = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.OWNER));
        published_and_views = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.PUBLISHED_AND_VIEWS));
        watched_time = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.LAST_OPENED_TIME));
    }

    // The cursor window should be larger than the whole block of data.
    public static Video[] videosFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Video[] videos = new Video[size];
            int i = 0;
            do {
                videos[i++] = new Video(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return videos;
        } else {
            return null;
        }
    }

    private static String formatDuration(String duration) {
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
        } else {
            sb.append("00");
        }
        return sb.toString();
    }

    private static String formatPublishedAtAndViews(DateTime time, long viewCount) {
        String publishedAt = time.toString().substring(0, 10);
        if (viewCount >= 1000000) {
            return String.format("%s · %.1fM views", publishedAt, viewCount / 1000000.0);
        } else if (viewCount >= 1000) {
            return String.format("%s · %dK views", publishedAt, viewCount / 1000);
        } else {
            return String.format("%s · %d views", publishedAt, viewCount);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(identifier);
        dest.writeString(thumbnail);
        dest.writeString(duration);
        dest.writeString(title);
        dest.writeString(owner);
        dest.writeString(published_and_views);
        dest.writeString(watched_time);
    }

    private Video(Parcel in) {
        identifier = in.readString();
        thumbnail = in.readString();
        duration = in.readString();
        title = in.readString();
        owner = in.readString();
        published_and_views = in.readString();
        watched_time = in.readString();
    }

    public static final Parcelable.Creator<Video> CREATOR = new Parcelable.Creator<Video>() {

        public Video createFromParcel(Parcel source) {
            return new Video(source);
        }

        public Video[] newArray(int size) {
            return new Video[size];
        }
    };
}