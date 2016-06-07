package com.localytics.android.itracker.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
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
        /** Whether the item needs to sync or not. */
        String DIRTY = "dirty";
        /** Whether the item is syncing or not. Each sync has a unique id. */
        String SYNC = "sync";
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    public interface BaseDataColumns {
        String TIME = "time";
        String TRACK_ID = "track_id";
    }

    interface TrackColumns {
        String DATE = "date";
        String VERSION = "version";
    }

    interface BackupColumns {
        String S3_KEY = "s3_key";
        String CATEGORY = "category";
        String DATE = "date";
        String HOUR = "hour";
    }

    interface MotionColumns extends BaseDataColumns {
        String DATA = "data";
        String SAMPLING = "sampling";
    }

    interface LocationColumns extends BaseDataColumns {
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String ALTITUDE = "altitude";
        String ACCURACY = "accuracy";
        String SPEED = "speed";
    }

    interface ActivityColumns extends BaseDataColumns {
        String TYPE = "type";
        String TYPE_ID = "type_id";
        String CONFIDENCE = "confidence";
    }

    interface VideoColumns {
        String IDENTIFIER = "identifier";
        String THUMBNAIL = "thumbnail";
        String DURATION = "duration";
        String TITLE = "title";
        String OWNER = "owner";
        String PUBLISHED_AND_VIEWS = "published_and_views";
        String WATCHED_TIME = "watched_time";
        String FILE_SIZE = "file_size";
    }

    interface Paths {
        String TRACKS = "tracks";
        String BACKUPS = "backups";
        String MOTIONS = "motions";
        String LOCATIONS = "locations";
        String ACTIVITIES = "activities";
        String VIDEOS = "videos";
        String UNKNOWN = "unknown";
    }

    public static final String CONTENT_AUTHORITY = "com.localytics.itracker";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final Uri UNKNOWN_CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.UNKNOWN).build();

    public static final String[] TOP_LEVEL_PATHS = new String[] {
            Paths.TRACKS
    };

    public enum SyncState {
        PENDING ("pending"),    // data needs to be synced
        SYNCED  ("synced"),     // data has been synced
        SYNCING ("syncing");    // data is syncing

        private final String mState;

        SyncState(String state) {
            mState = state;
        }

        public String state() {
            return mState;
        }
    }

    public static final String DATA_CATEGORY_MOTION   = "motion";
    public static final String DATA_CATEGORY_LOCATION = "location";
    public static final String DATA_CATEGORY_ACTIVITY = "activity";

    public static final String SELECTION_BY_SYNC = String.format("%s = ?", SyncColumns.SYNC);
    public static final String SELECTION_BY_DIRTY = String.format("%s = ?", SyncColumns.DIRTY);
    public static final String SELECTION_BY_INTERVAL = String.format("%s >= ? AND %s < ?", BaseDataColumns.TIME, BaseDataColumns.TIME);
    public static final String ORDER_BY_TIME_ASC = BaseDataColumns.TIME + " ASC";

    public static class Tracks implements TrackColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.TRACKS).build();

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

        public static long getTrackIdOfDateTime(Context context, DateTime datetime) {
            long trackId = -1;

            Cursor cursor = null;
            try {
                ContentResolver resolver = context.getContentResolver();
                long startOfDay = datetime.withTimeAtStartOfDay().getMillis();
                cursor = resolver.query(Tracks.CONTENT_URI, null, Tracks.DATE + " = ?", new String[]{"" + startOfDay}, null);
                if (cursor != null && cursor.moveToFirst()) {
                    trackId = cursor.getLong(0);
                } else {
                    final ContentValues values = new ContentValues();
                    values.put(Tracks.DATE, startOfDay);
                    values.put(SyncColumns.UPDATED, DateTime.now().getMillis());
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

    public static class Backups implements BackupColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.BACKUPS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.backup";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.backup";

        public static final String SELECTION_BY_S3_KEY = String.format("%s = ?", S3_KEY);

        /** Build a {@link Uri} that references a given motion. */
        public static Uri buildBackupUri(String backupId) {
            return CONTENT_URI.buildUpon().appendPath(backupId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getBackupId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Motions implements MotionColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.MOTIONS).build();

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
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.LOCATIONS).build();

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
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.ACTIVITIES).build();

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

    public static class Videos implements VideoColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(Paths.VIDEOS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.itracker.video";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.itracker.video";

        /** Build a {@link Uri} that references a given video. */
        public static Uri buildVideoUri(String videoId) {
            return CONTENT_URI.buildUpon().appendPath(videoId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getVideoId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static String categoryFromUri(@NonNull Uri uri) {
        if (uri == Activities.CONTENT_URI) {
            return DATA_CATEGORY_ACTIVITY;
        } else if (uri == Locations.CONTENT_URI) {
            return DATA_CATEGORY_LOCATION;
        } else if (uri == Motions.CONTENT_URI) {
            return DATA_CATEGORY_MOTION;
        }
        return "unknown";
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
