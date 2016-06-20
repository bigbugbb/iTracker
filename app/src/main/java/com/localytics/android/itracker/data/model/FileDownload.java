package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;

/**
 * Created by bigbug on 6/20/16.
 */
public class FileDownload implements Parcelable {

    public String media_id;
    public long   media_size;
    public String media_type;
    public String media_desc;
    public String target_url;
    public String status;
    public String start_time;
    public String finish_time;

    public FileDownload() {
    }

    public FileDownload(Cursor cursor) {
        media_id = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.MEDIA_ID));
        media_size = cursor.getLong(cursor.getColumnIndexOrThrow(FileDownloads.MEDIA_SIZE));
        media_type = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.MEDIA_TYPE));
        media_desc = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.MEDIA_DESC));
        target_url = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.TARGET_URL));
        status = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.STATUS));
        start_time = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.START_TIME));
        finish_time = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.FINISH_TIME));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(media_id);
        dest.writeLong(media_size);
        dest.writeString(media_type);
        dest.writeString(media_desc);
        dest.writeString(target_url);
        dest.writeString(status);
        dest.writeString(start_time);
        dest.writeString(finish_time);
    }

    private FileDownload(Parcel in) {
        media_id = in.readString();
        media_size = in.readLong();
        media_type = in.readString();
        media_desc = in.readString();
        target_url = in.readString();
        status = in.readString();
        start_time = in.readString();
        finish_time = in.readString();
    }

    public static final Parcelable.Creator<FileDownload> CREATOR = new Parcelable.Creator<FileDownload>() {

        public FileDownload createFromParcel(Parcel source) {
            return new FileDownload(source);
        }

        public FileDownload[] newArray(int size) {
            return new FileDownload[size];
        }
    };
}
