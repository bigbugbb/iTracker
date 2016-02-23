package com.localytics.android.itracker.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

import org.joda.time.DateTime;

/**
 * Contract class for interacting with {@link TrackerProvider}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class TrackerContract {
    /**
     * Query parameter to create a distinct query.
     */
    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    public interface SyncColumns {
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface TrackColumns {
        String DATE = "date";
    }

    interface MotionColumns {
        String TIME = "time";
        String DATA = "data";
        String SAMPLING = "sampling";
        String DEVICE_ID = "device_id";
        String TRACK_ID = "track_id";
    }

    interface LocationColumns {
        String TIME = "time";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String ALTITUDE = "altitude";
        String ACCURACY = "accuracy";
        String SPEED = "speed";
        String DEVICE_ID = "device_id";
        String TRACK_ID = "track_id";
    }

    interface ActivityColumns {
        String TIME = "time";
        String TYPE = "type";
        String TYPE_ID = "type_id";
        String CONFIDENCE = "confidence";
        String DEVICE_ID = "device_id";
        String TRACK_ID = "track_id";
    }

    interface WeatherColumns {
        String TIME = "time";
        String CITY = "city";
        String WEATHER = "weather";
        String TEMPERATURE = "temperature";
        String TRACK_ID = "track_id";
    }

    public static final String CONTENT_AUTHORITY = "com.localytics.itracker";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class Tracks implements TrackColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("tracks").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.track";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.track";

        public static final String SELECTION_BY_TIME_RANGE = String.format("%s >= ? AND %s <= ?", DATE, DATE);

        /** Build a {@link Uri} that references a given track. */
        public static Uri buildTrackUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).build();
        }

        public static Uri buildTrackUriByDate(String startOfDateInMills) {
            return CONTENT_URI.buildUpon().appendQueryParameter("date", startOfDateInMills).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getTrackId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getTrackIdToday(Context context) {
            long trackId = -1;

            Cursor cursor = null;
            try {
                ContentResolver resolver = context.getContentResolver();
                long startOfToday = DateTime.now().withTimeAtStartOfDay().getMillis();
                cursor = resolver.query(Tracks.CONTENT_URI, null, Tracks.DATE + "=?", new String[]{"" + startOfToday}, null);
                if (cursor != null && cursor.moveToFirst()) {
                    trackId = cursor.getLong(0);
                } else {
                    ContentValues values = new ContentValues();
                    values.put(Tracks.DATE, startOfToday);
                    Uri uri = resolver.insert(Tracks.CONTENT_URI, values);
                    if (uri != null) {
                        trackId = Long.parseLong(Tracks.getTrackId(uri));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return trackId;
        }
    }

    public static class Motions implements MotionColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("motions").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.motion";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.motion";

        public static final String SELECTION_BY_TRACK_ID = String.format("%s = ?", TRACK_ID);

        /** Build a {@link Uri} that references a given motion. */
        public static Uri buildMotionUri(String motionId) {
            return CONTENT_URI.buildUpon().appendPath(motionId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getMotionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Locations implements LocationColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("locations").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.location";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.location";

        public static final String SELECTION_BY_TRACK_ID = String.format("%s = ?", TRACK_ID);

        /** Build a {@link Uri} that references a given location. */
        public static Uri buildLocationUri(String locationId) {
            return CONTENT_URI.buildUpon().appendPath(locationId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getLocationId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Activities implements ActivityColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("activities").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.activity";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.activity";

        public static final String SELECTION_BY_TRACK_ID = String.format("%s = ?", TRACK_ID);

        /** Build a {@link Uri} that references a given activity. */
        public static Uri buildActivityUri(String activityId) {
            return CONTENT_URI.buildUpon().appendPath(activityId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getActivityId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Weathers implements WeatherColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("weathers").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.weather";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.weather";

        /** Build a {@link Uri} that references a given weather. */
        public static Uri buildWeatherUri(String weatherId) {
            return CONTENT_URI.buildUpon().appendPath(weatherId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getWeatherId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true", uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private TrackerContract() {
    }
}
