package com.localytics.android.itracker.data.model;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.DetectedActivity;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.provider.TrackerContract;


public final class Activity extends BaseData implements Parcelable {

    public String type;
    public int    type_id;
    public int    confidence;
    public String device_id;

    public Activity() {
    }

    public Activity(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndex(TrackerContract.Activities.TIME));
        type = cursor.getString(cursor.getColumnIndex(TrackerContract.Activities.TYPE));
        type_id = cursor.getInt(cursor.getColumnIndex(TrackerContract.Activities.TYPE_ID));
        confidence = cursor.getInt(cursor.getColumnIndex(TrackerContract.Activities.CONFIDENCE));
        device_id = cursor.getString(cursor.getColumnIndex(TrackerContract.Activities.DEVICE_ID));
        track_id = cursor.getLong(cursor.getColumnIndex(TrackerContract.Activities.TRACK_ID));
    }

    public Drawable getActivityIcon(Context context) {
        Resources resources = context.getResources();
        switch (type_id) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getDrawable(R.drawable.ic_activity_in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getDrawable(R.drawable.ic_activity_on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getDrawable(R.drawable.ic_activity_on_foot);
            case DetectedActivity.RUNNING:
                return resources.getDrawable(R.drawable.ic_activity_running);
            case DetectedActivity.STILL:
                return resources.getDrawable(R.drawable.ic_activity_still);
            case DetectedActivity.TILTING:
                return resources.getDrawable(R.drawable.ic_activity_tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getDrawable(R.drawable.ic_activity_unknown);
            case DetectedActivity.WALKING:
                return resources.getDrawable(R.drawable.ic_activity_walking);
            default:
                return resources.getDrawable(R.drawable.ic_activity_unknown);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeString(type);
        dest.writeInt(type_id);
        dest.writeInt(confidence);
        dest.writeString(device_id);
    }

    private Activity(Parcel in) {
        time = in.readLong();
        type = in.readString();
        type_id = in.readInt();
        confidence = in.readInt();
        device_id = in.readString();
    }

    public static Activity[] activitiesFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Activity[] activities = new Activity[size];
            int i = 0;
            do {
                activities[i++] = new Activity(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return activities;
        } else {
            return null;
        }
    }

    public static final Parcelable.Creator<Activity> CREATOR = new Parcelable.Creator<Activity>() {

        public Activity createFromParcel(Parcel source) {
            return new Activity(source);
        }

        public Activity[] newArray(int size) {
            return new Activity[size];
        }
    };
}
