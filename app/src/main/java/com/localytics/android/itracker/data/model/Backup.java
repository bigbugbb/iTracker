package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;


public class Backup implements Parcelable {
    public String s3_key;
    public String category;
    public String state;
    public String date;
    public int    hour;

    public Backup() {
    }

    public Backup(Cursor cursor) {
        s3_key = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.S3_KEY));
        category = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.CATEGORY));
        state = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.STATE));
        date = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.DATE));
        hour = cursor.getInt(cursor.getColumnIndex(TrackerContract.Backups.HOUR));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(s3_key);
        dest.writeString(category);
        dest.writeString(state);
        dest.writeString(date);
        dest.writeInt(hour);
    }

    private Backup(Parcel in) {
        s3_key = in.readString();
        category = in.readString();
        state = in.readString();
        date = in.readString();
        hour = in.readInt();
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
