package com.localytics.android.itracker.provider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.localytics.android.itracker.provider.TrackerContract.Activities;
import com.localytics.android.itracker.provider.TrackerContract.Locations;
import com.localytics.android.itracker.provider.TrackerContract.Motions;
import com.localytics.android.itracker.provider.TrackerContract.Links;
import com.localytics.android.itracker.provider.TrackerContract.Tracks;
import com.localytics.android.itracker.provider.TrackerContract.Weathers;
import com.localytics.android.itracker.sync.SyncHelper;
import com.localytics.android.itracker.sync.TrackerDataHandler;
import com.localytics.android.itracker.util.AccountUtils;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link TrackerProvider}.
 */
public class TrackerDatabase extends SQLiteOpenHelper {
    private static final String TAG = makeLogTag(TrackerDatabase.class);

    private static final String DATABASE_NAME = "itracker.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int DATABASE_VERSION = 1;

    private final Context mContext;

    interface Tables {
        String TRACKS = "tracks";
        String LINKS = "links";
        String MOTIONS = "motions";
        String LOCATIONS = "locations";
        String ACTIVITIES = "activities";
        String WEATHERS = "weathers";
    }

    interface FOREIGN_KEY {
        String TRACK_ID = "FOREIGN KEY(track_id) ";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + TrackerContract.Tracks._ID + ")";
    }

    public TrackerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL("CREATE TABLE " + Tables.TRACKS + " ("
                + Tracks._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Tracks.DIRTY + " INTEGER DEFAULT 1,"
                + Tracks.SYNC + " TEXT,"
                + Tracks.UPDATED + " INTEGER NOT NULL,"
                + Tracks.DATE + " INTEGER UNIQUE NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.LINKS + " ("
                + Links._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Links.DIRTY + " INTEGER DEFAULT 1,"
                + Links.SYNC + " TEXT,"
                + Links.UPDATED + " INTEGER NOT NULL,"
                + Links.LINK + " INTEGER UNIQUE NOT NULL,"
                + Links.TYPE + " TEXT NOT NULL,"
                + Links.START_TIME + " INTEGER NOT NULL,"
                + Links.END_TIME + " INTEGER NOT NULL,"
                + Links.DEVICE_ID + " TEXT NOT NULL,"
                + Links.TRACK_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.TRACK_ID + References.TRACK_ID + " ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + Tables.MOTIONS + " ("
                + Motions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Motions.DIRTY + " INTEGER DEFAULT 1,"
                + Motions.SYNC + " TEXT,"
                + Motions.UPDATED + " INTEGER NOT NULL,"
                + Motions.TIME + " INTEGER NOT NULL,"
                + Motions.DATA + " INTEGER NOT NULL,"
                + Motions.SAMPLING + " INTEGER NOT NULL,"
                + Motions.DEVICE_ID + " TEXT NOT NULL,"
                + Motions.TRACK_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.TRACK_ID + References.TRACK_ID + " ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + Tables.LOCATIONS + " ("
                + Locations._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Locations.DIRTY + " INTEGER DEFAULT 1,"
                + Locations.SYNC + " TEXT,"
                + Locations.UPDATED + " INTEGER NOT NULL,"
                + Locations.TIME + " INTEGER NOT NULL,"
                + Locations.LATITUDE + " REAL NOT NULL,"
                + Locations.LONGITUDE + " REAL NOT NULL,"
                + Locations.ALTITUDE + " REAL NOT NULL,"
                + Locations.ACCURACY + " REAL NOT NULL,"
                + Locations.SPEED + " REAL NOT NULL,"
                + Locations.DEVICE_ID + " TEXT NOT NULL,"
                + Locations.TRACK_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.TRACK_ID + References.TRACK_ID + " ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + Tables.ACTIVITIES + " ("
                + Activities._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Activities.DIRTY + " INTEGER DEFAULT 1,"
                + Activities.SYNC + " TEXT,"
                + Activities.UPDATED + " INTEGER NOT NULL,"
                + Activities.TIME + " INTEGER NOT NULL,"
                + Activities.TYPE + " TEXT NOT NULL,"
                + Activities.TYPE_ID + " INTEGER NOT NULL,"
                + Activities.CONFIDENCE + " INTEGER NOT NULL,"
                + Activities.DEVICE_ID + " TEXT NOT NULL,"
                + Activities.TRACK_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.TRACK_ID + References.TRACK_ID + " ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + Tables.WEATHERS + " ("
                + Weathers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Weathers.DIRTY + " INTEGER DEFAULT 1,"
                + Weathers.SYNC + " TEXT,"
                + Weathers.UPDATED + " INTEGER NOT NULL,"
                + Weathers.TIME + " INTEGER NOT NULL,"
                + Weathers.CITY + " TEXT NOT NULL,"
                + Weathers.WEATHER + " TEXT NOT NULL,"
                + Weathers.TEMPERATURE + " INTEGER NOT NULL,"
                + Weathers.TRACK_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.TRACK_ID + References.TRACK_ID + " ON DELETE CASCADE);");

        // Create indexes on track_id
        db.execSQL("CREATE INDEX motion_track_id_index ON " + Tables.MOTIONS + "(" + Motions.TRACK_ID + ");");
        db.execSQL("CREATE INDEX link_track_id_index ON " + Tables.LINKS + "(" + Links.TRACK_ID + ");");
        db.execSQL("CREATE INDEX location_track_id_index ON " + Tables.LOCATIONS + "(" + Locations.TRACK_ID + ");");
        db.execSQL("CREATE INDEX activity_track_id_index ON " + Tables.ACTIVITIES + "(" + Activities.TRACK_ID + ");");
        db.execSQL("CREATE INDEX weather_track_id_index ON " + Tables.WEATHERS + "(" + Weathers.TRACK_ID + ");");

        // Create indexes on dirty
        db.execSQL("CREATE INDEX motion_dirty_index ON " + Tables.MOTIONS + "(" + Motions.DIRTY + ");");
        db.execSQL("CREATE INDEX link_dirty_index ON " + Tables.LINKS + "(" + Links.DIRTY + ");");
        db.execSQL("CREATE INDEX location_dirty_index ON " + Tables.LOCATIONS + "(" + Locations.DIRTY + ");");
        db.execSQL("CREATE INDEX activity_dirty_index ON " + Tables.ACTIVITIES + "(" + Activities.DIRTY + ");");
        db.execSQL("CREATE INDEX weather_dirty_index ON " + Tables.WEATHERS + "(" + Weathers.DIRTY + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        Account account = AccountUtils.getActiveAccount(mContext);
        if (account != null) {
            LOGI(TAG, "Cancelling any pending syncs for account");
            ContentResolver.cancelSync(account, TrackerContract.CONTENT_AUTHORITY);
        }

        // Current DB version. We update this variable as we perform upgrades to reflect
        // the current version we are in.
        int version = oldVersion;

        // Indicates whether the data we currently have should be invalidated as a
        // result of the db upgrade. Default is true (invalidate); if we detect that this
        // is a trivial DB upgrade, we set this to false.
        boolean dataInvalidated = true;

        LOGD(TAG, "After upgrade logic, at version " + version);

        // at this point, we ran out of upgrade logic, so if we are still at the wrong
        // version, we have no choice but to delete everything and create everything again.
        if (version != DATABASE_VERSION) {
            // Do something here
            version = DATABASE_VERSION;
        }

        if (dataInvalidated) {
            LOGD(TAG, "Data invalidated; resetting our data timestamp.");
            TrackerDataHandler.resetDataTimestamp(mContext);
            if (account != null) {
                LOGI(TAG, "DB upgrade complete. Requesting resync.");
                SyncHelper.requestManualSync(account);
            }
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}