package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;

/**
 * Created by bigbug on 1/16/16.
 */
public final class Weather extends BaseData implements Parcelable {

    public String city;
    public String weather;
    public int    temperature;

    public Weather() {
    }

    public Weather(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Weathers.TIME));
        city = cursor.getString(cursor.getColumnIndex(TrackerContract.Weathers.CITY));
        weather = cursor.getString(cursor.getColumnIndex(TrackerContract.Weathers.WEATHER));
        temperature = cursor.getInt(cursor.getColumnIndex(TrackerContract.Weathers.TEMPERATURE));
        track_id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Weathers.TRACK_ID));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeString(city);
        dest.writeString(weather);
        dest.writeInt(temperature);
    }

    private Weather(Parcel in) {
        time = in.readLong();
        city = in.readString();
        weather = in.readString();
        temperature = in.readInt();
    }

    public static final Creator<Weather> CREATOR = new Creator<Weather>() {

        public Weather createFromParcel(Parcel source) {
            return new Weather(source);
        }

        public Weather[] newArray(int size) {
            return new Weather[size];
        }
    };
}