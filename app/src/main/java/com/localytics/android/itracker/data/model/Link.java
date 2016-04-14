package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;


public class Link implements Parcelable {
    public String link;
    public String data_type;
    public long   start_time;
    public long   end_time;
    public long   track_id;

    public Link() {
    }

    public Link(Cursor cursor) {
        link = cursor.getString(cursor.getColumnIndex(TrackerContract.Links.LINK));
        data_type = cursor.getString(cursor.getColumnIndex(TrackerContract.Links.TYPE));
        start_time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Links.START_TIME));
        end_time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Links.END_TIME));
        track_id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Links.TRACK_ID));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(link);
        dest.writeString(data_type);
        dest.writeLong(start_time);
        dest.writeLong(end_time);
        dest.writeLong(track_id);
    }

    private Link(Parcel in) {
        link = in.readString();
        data_type = in.readString();
        start_time = in.readLong();
        end_time = in.readLong();
        track_id = in.readLong();
    }

    public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {

        public Link createFromParcel(Parcel source) {
            return new Link(source);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };
}
