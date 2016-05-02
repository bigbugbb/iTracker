package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.provider.TrackerContract;

import org.joda.time.DateTime;

import java.util.Arrays;

public final class Motion extends BaseData implements Parcelable {

    public int data;
    public int sampling;

    public Motion() {
    }

    public Motion(Cursor cursor) {
        // Optimize by using the index number directly
        time = cursor.getLong(3);
        data = cursor.getInt(4);
        sampling = cursor.getInt(5);
        track_id = cursor.getLong(6);
    }

    // The cursor window should be larger than the whole block of data.
    public static Motion[] motionsFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Motion[] motions = new Motion[size];
            int i = 0;
            do {
                motions[i++] = new Motion(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return motions;
        } else {
            return null;
        }
    }

    public static float[] populateData(Motion[] motions, float[] outData, float baseline) {
        if (motions == null || outData == null) {
            return null;
        } else {
            Arrays.fill(outData, baseline);
        }

        int prevMinute = -1, startSecondOfMinute = -1;
        for (Motion motion : motions) {
            DateTime time = new DateTime(motion.time);
            int minuteOfDay = time.getMinuteOfDay();
            if (prevMinute != minuteOfDay) {
                prevMinute = minuteOfDay;
                startSecondOfMinute = time.getSecondOfMinute();
            }
            int offset = Math.min(minuteOfDay * Config.MONITORING_DURATION_IN_SECONDS + time.getSecondOfMinute() - startSecondOfMinute, outData.length - 1);
            outData[offset] = Math.max(baseline, Math.min(motion.data, Config.ACCELEROMETER_DATA_MAX_MAGNITUDE));
        }
        return outData;
    }

    @Override
    public String[] convertToCsvLine() {
        return new String[]{
            Long.toString(time),
            Integer.toString(data),
            Integer.toString(sampling)
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeInt(data);
        dest.writeInt(sampling);
    }

    private Motion(Parcel in) {
        time = in.readLong();
        data = in.readInt();
        sampling = in.readInt();
    }

    public static final Creator<Motion> CREATOR = new Creator<Motion>() {

        public Motion createFromParcel(Parcel source) {
            return new Motion(source);
        }

        public Motion[] newArray(int size) {
            return new Motion[size];
        }
    };
}
