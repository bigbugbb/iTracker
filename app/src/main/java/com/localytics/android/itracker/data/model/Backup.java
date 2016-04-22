package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class Backup implements Parcelable {
    public String s3_key;
    public String category;
    public String date;
    public int    hour;

    private static DateTimeFormatter sFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    public Backup() {
    }

    public Backup(Cursor cursor) {
        s3_key = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.S3_KEY));
        category = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.CATEGORY));
        date = cursor.getString(cursor.getColumnIndex(TrackerContract.Backups.DATE));
        hour = cursor.getInt(cursor.getColumnIndex(TrackerContract.Backups.HOUR));
    }

    public long timestamp() {
        DateTime datetime = sFormatter.parseDateTime(date);
        datetime.plusHours(hour);
        return datetime.getMillis();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(s3_key);
        dest.writeString(category);
        dest.writeString(date);
        dest.writeInt(hour);
    }

    private Backup(Parcel in) {
        s3_key = in.readString();
        category = in.readString();
        date = in.readString();
        hour = in.readInt();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Backup)) {
            return false;
        }
        if (s3_key.equals(((Backup) object).s3_key)) {
            return true;
        }
        return super.equals(object);
    }

    // equals will not be called if two objects have different hashCodes
    @Override
    public int hashCode() {
        return s3_key.hashCode();
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
