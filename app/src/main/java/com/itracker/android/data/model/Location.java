package com.itracker.android.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.itracker.android.provider.TrackerContract;


public final class Location extends BaseData implements Parcelable {

    public float  latitude;
    public float  longitude;
    public float  altitude;
    public float  accuracy;
    public float  speed;

    public Location() {
    }

    public Location(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Locations.TIME));
        latitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.LATITUDE));
        longitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.LONGITUDE));
        altitude = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.ALTITUDE));
        accuracy = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.ACCURACY));
        speed = cursor.getFloat(cursor.getColumnIndex(TrackerContract.Locations.SPEED));
        track_id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Locations.TRACK_ID));
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
    public String[] convertToCsvLine() {
        return new String[]{
            Long.toString(time),
            Float.toString(latitude),
            Float.toString(longitude),
            Float.toString(altitude),
            Float.toString(accuracy),
            Float.toString(speed)
        };
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
    }

    private Location(Parcel in) {
        time = in.readLong();
        latitude = in.readFloat();
        longitude = in.readFloat();
        altitude = in.readFloat();
        accuracy = in.readFloat();
        speed = in.readFloat();
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
