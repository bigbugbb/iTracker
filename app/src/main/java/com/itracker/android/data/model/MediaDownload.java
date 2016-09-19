package com.itracker.android.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.itracker.android.provider.TrackerContract;

/**
 * Created by bigbug on 6/24/16.
 */
public class MediaDownload extends FileDownload {

    public String identifier;
    public String thumbnail;
    public String duration;
    public String title;

    public MediaDownload() {
    }

    public MediaDownload(Cursor cursor) {
        super(cursor);
        identifier = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.IDENTIFIER));
        thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.THUMBNAIL));
        duration = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.DURATION));
        title = cursor.getString(cursor.getColumnIndexOrThrow(TrackerContract.Videos.TITLE));
    }

    public static MediaDownload[] downloadsFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            MediaDownload[] downloads = new MediaDownload[size];
            int i = 0;
            do {
                downloads[i++] = new MediaDownload(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return downloads;
        } else {
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(identifier);
        dest.writeString(thumbnail);
        dest.writeString(duration);
        dest.writeString(title);
    }

    private MediaDownload(Parcel in) {
        super(in);
        identifier = in.readString();
        thumbnail = in.readString();
        duration = in.readString();
        title = in.readString();
    }

    public static final Parcelable.Creator<MediaDownload> CREATOR = new Parcelable.Creator<MediaDownload>() {

        public MediaDownload createFromParcel(Parcel source) {
            return new MediaDownload(source);
        }

        public MediaDownload[] newArray(int size) {
            return new MediaDownload[size];
        }
    };
}
