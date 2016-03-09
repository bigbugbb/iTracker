package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.provider.TrackerContract;

/**
 * Created by bigbug on 1/10/16.
 */
public final class Location extends BaseData implements Parcelable {

    public float  latitude;
    public float  longitude;
    public float  altitude;
    public float  accuracy;
    public float  speed;
    public String device_id;

    public Location() {
    }

    public Location(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Locations.TIME));
        latitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.LATITUDE));
        longitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.LONGITUDE));
        altitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.ALTITUDE));
        accuracy = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.ACCURACY));
        speed = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.SPEED));
        device_id = cursor.getString(cursor.getColumnIndex(TrackerContract.Locations.DEVICE_ID));
    }

    // The cursor window should be larger than the whole block of data.
    public static Location[] locationsFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Location[] locations = new Location[size];
            int i = 0;
            do {
                locations[i++] = new Location(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return locations;
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
        dest.writeLong(time);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
        dest.writeFloat(altitude);
        dest.writeFloat(accuracy);
        dest.writeFloat(speed);
        dest.writeString(device_id);
    }

    private Location(Parcel in) {
        time = in.readLong();
        latitude = in.readFloat();
        longitude = in.readFloat();
        altitude = in.readFloat();
        accuracy = in.readFloat();
        speed = in.readFloat();
        device_id = in.readString();
    }

    public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {

        public Location createFromParcel(Parcel source) {
            return new Location(source);
        }

        public Location[] newArray(int size) {
            return new Location[size];
        }
    };
}
