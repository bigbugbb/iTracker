package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;


public class Backup implements Parcelable {
    public String s3_key;
    public String data_type;
    public long   start_time;
    public long   end_time;
    public long   track_id;

    public Backup() {
    }

    public Backup(Cursor cursor) {
        s3_key = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.S3_KEY));
        data_type = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.TYPE));
        start_time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Backups.START_TIME));
        end_time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Backups.END_TIME));
        track_id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Backups.TRACK_ID));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(s3_key);
        dest.writeString(data_type);
        dest.writeLong(start_time);
        dest.writeLong(end_time);
        dest.writeLong(track_id);
    }

    private Backup(Parcel in) {
        s3_key = in.readString();
        data_type = in.readString();
        start_time = in.readLong();
        end_time = in.readLong();
        track_id = in.readLong();
    }

    public static final Parcelable.Creator<Backup> CREATOR = new Parcelable.Creator<Backup>() {

        public Backup createFromParcel(Parcel source) {
            return new Backup(source);
        }

        public Backup[] newArray(int size) {
            return new Backup[size];
        }
    };
}
