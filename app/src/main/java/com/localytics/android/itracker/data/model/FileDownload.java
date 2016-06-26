package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;

/**
 * Created by bigbug on 6/20/16.
 */
public class FileDownload implements Parcelable {

    public String file_id;
    public long   total_size;
    public String target_url;
    public String status;
    public String start_time;
    public String finish_time;

    public FileDownload() {
    }

    public FileDownload(Cursor cursor) {
        file_id = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.FILE_ID));
        total_size = cursor.getLong(cursor.getColumnIndexOrThrow(FileDownloads.TOTAL_SIZE));
        target_url = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.TARGET_URL));
        status = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.STATUS));
        start_time = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.START_TIME));
        finish_time = cursor.getString(cursor.getColumnIndexOrThrow(FileDownloads.FINISH_TIME));
    }

    public void update(FileDownload download) {
        if (!TextUtils.equals(file_id, download.file_id)) {
            return;
        }

        total_size = download.total_size;
        target_url = download.target_url;
        status = download.status;
        start_time = download.start_time;
        finish_time = download.finish_time;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(file_id);
        dest.writeLong(total_size);
        dest.writeString(target_url);
        dest.writeString(status);
        dest.writeString(start_time);
        dest.writeString(finish_time);
    }

    protected FileDownload(Parcel in) {
        file_id = in.readString();
        total_size = in.readLong();
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
