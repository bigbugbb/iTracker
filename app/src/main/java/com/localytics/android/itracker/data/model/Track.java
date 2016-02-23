package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;

/**
 * Created by bbo on 1/15/16.
 */
public final class Track implements Parcelable {

    public long id;
    public long date;

    public Track() {
    }

    public Track(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Tracks._ID));
        date = cursor.getLong(cursor.getColumnIndex(TrackerContract.Tracks.DATE));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(date);
    }

    private Track(Parcel in) {
        id = in.readLong();
        date = in.readLong();
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {

        public Track createFromParcel(Parcel source) {
            return new Track(source);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
}
