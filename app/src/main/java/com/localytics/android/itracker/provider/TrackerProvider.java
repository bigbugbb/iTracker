package com.localytics.android.itracker.provider;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.localytics.android.itracker.provider.TrackerContract.Activities;
import com.localytics.android.itracker.provider.TrackerContract.Backups;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.provider.TrackerContract.Locations;
import com.localytics.android.itracker.provider.TrackerContract.Motions;
import com.localytics.android.itracker.provider.TrackerContract.Tracks;
import com.localytics.android.itracker.provider.TrackerContract.Videos;
import com.localytics.android.itracker.provider.TrackerDatabase.Tables;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.SelectionBuilder;

import java.util.ArrayList;
import java.util.Arrays;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGV;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Provider that stores {@link TrackerContract} data. Data is usually inserted
 * by {@link com.localytics.android.itracker.sync.SyncHelper}, and queried by various
 * {@link Activity} instances.
 */

public class TrackerProvider extends ContentProvider {
    private static final String TAG = makeLogTag(com.localytics.android.itracker.provider.TrackerProvider.class);

    private TrackerDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int TRACKS = 100;
    private static final int TRACKS_ID = 101;

    private static final int BACKUPS = 200;
    private static final int BACKUPS_ID = 201;

    private static final int MOTIONS = 300;
    private static final int MOTIONS_ID = 301;

    private static final int LOCATIONS = 400;
    private static final int LOCATIONS_ID = 401;

    private static final int ACTIVITIES = 500;
    private static final int ACTIVITIES_ID = 501;

    private static final int VIDEOS = 600;
    private static final int VIDEOS_ID = 601;

    private static final int FILE_DOWNLOADS = 700;
    private static final int FILE_DOWNLOADS_ID = 701;
    private static final int FILE_DOWNLOADS_MEDIA = 702;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = TrackerContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "tracks", TRACKS);
        matcher.addURI(authority, "tracks/*", TRACKS_ID);

        matcher.addURI(authority, "backups", BACKUPS);
        matcher.addURI(authority, "backups/*", BACKUPS_ID);

        matcher.addURI(authority, "motions", MOTIONS);
        matcher.addURI(authority, "motions/*", MOTIONS_ID);

        matcher.addURI(authority, "locations", LOCATIONS);
        matcher.addURI(authority, "locations/*", LOCATIONS_ID);

        matcher.addURI(authority, "activities", ACTIVITIES);
        matcher.addURI(authority, "activities/*", ACTIVITIES_ID);

        matcher.addURI(authority, "videos", VIDEOS);
        matcher.addURI(authority, "videos/*", VIDEOS_ID);

        matcher.addURI(authority, "file_downloads", FILE_DOWNLOADS);
        matcher.addURI(authority, "file_downloads/#", FILE_DOWNLOADS_ID);
        matcher.addURI(authority, "file_downloads/media", FILE_DOWNLOADS_MEDIA);

        return matcher;
    }

    /**
     * Implement this to initialize your content provider on startup.
     * This method is called for all registered content providers on the
     * application main thread at application launch time.  It must not perform
     * lengthy operations, or application startup will be delayed.
     * <p/>
     * <p>You should defer nontrivial initialization (such as opening,
     * upgrading, and scanning databases) until the content provider is used
     * (via {@link #query}, {@link #insert}, etc).  Deferred initialization
     * keeps application startup fast, avoids unnecessary work if the provider
     * turns out not to be needed, and stops database errors (such as a full
     * disk) from halting application launch.
     * <p/>
     * <p>If you use SQLite, {@link android.database.sqlite.SQLiteOpenHelper}
     * is a helpful utility class that makes it easy to manage databases,
     * and will automatically defer opening until first use.  If you do use
     * SQLiteOpenHelper, make sure to avoid calling
     * {@link android.database.sqlite.SQLiteOpenHelper#getReadableDatabase} or
     * {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase}
     * from this method.  (Instead, override
     * {@link android.database.sqlite.SQLiteOpenHelper#onOpen} to initialize the
     * database when it is first opened.)
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new TrackerDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        TrackerDatabase.deleteDatabase(context);
        mOpenHelper = new TrackerDatabase(getContext());
    }

    /** Returns a tuple of question marks. For example, if count is 3, returns "(?,?,?)". */
    private String makeQuestionMarkTuple(int count) {
        if (count < 1) {
            return "()";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(?");
        for (int i = 1; i < count; i++) {
            stringBuilder.append(",?");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);

        // avoid the expensive string concatenation below if not loggable
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            LOGV(TAG, "uri=" + uri + " match=" + match + " proj=" + Arrays.toString(projection) +
                    " selection=" + selection + " args=" + Arrays.toString(selectionArgs) + ")");
        }

        SelectionBuilder builder = buildExpandedSelection(uri, match);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                if (builder == null) {
                    builder = buildSimpleSelection(uri);
                }

                boolean distinct = !TextUtils.isEmpty(uri.getQueryParameter(TrackerContract.QUERY_PARAMETER_DISTINCT));

                try {
                    Cursor cursor = builder
                            .where(selection, selectionArgs)
                            .query(db, distinct, projection, sortOrder, null);
                    Context context = getContext();
                    if (null != context) {
                        cursor.setNotificationUri(context.getContentResolver(), uri);
                    }
                    return cursor;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private String getCurrentAccountName(Uri uri, boolean sanitize) {
        String accountName = AccountUtils.getActiveAccountName(getContext());
        if (sanitize) {
            // sanitize accountName when concatenating (http://xkcd.com/327/)
            accountName = (accountName != null) ? accountName.replace("'", "''") : null;
        }
        return accountName;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKS:
                return Tracks.CONTENT_TYPE;
            case TRACKS_ID:
                return Tracks.CONTENT_ITEM_TYPE;
            case BACKUPS:
                return Backups.CONTENT_TYPE;
            case BACKUPS_ID:
                return Backups.CONTENT_ITEM_TYPE;
            case MOTIONS:
                return Motions.CONTENT_TYPE;
            case MOTIONS_ID:
                return Motions.CONTENT_ITEM_TYPE;
            case LOCATIONS:
                return Locations.CONTENT_TYPE;
            case LOCATIONS_ID:
                return Locations.CONTENT_ITEM_TYPE;
            case ACTIVITIES:
                return Activities.CONTENT_TYPE;
            case ACTIVITIES_ID:
                return Activities.CONTENT_ITEM_TYPE;
            case VIDEOS:
                return Videos.CONTENT_TYPE;
            case VIDEOS_ID:
                return Videos.CONTENT_ITEM_TYPE;
            case FILE_DOWNLOADS:
                return FileDownloads.CONTENT_TYPE;
            case FILE_DOWNLOADS_ID:
                return FileDownloads.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // the next line works because SQLiteDatabase
        // uses a thread local SQLiteSession object for all manipulations
        db.beginTransaction();
        try {
            final ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
//            getContext().getContentResolver().notifyChange(LentItemsContract.CONTENT_URI, null);
            return results;
        } finally {
            db.endTransaction();
        }
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        boolean syncToNetwork = !TrackerContract.hasCallerIsSyncAdapterParameter(uri);
        if (syncToNetwork) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

            // Widgets can't register content observers so we refresh widgets separately.
//            context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        LOGV(TAG, "bulkInsert(uri=" + uri + ", values=" + values.toString() + ", account=" + getCurrentAccountName(uri, false) + ")");
        int numInserted = 0;
        String table;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKS: {
                table = Tables.TRACKS;
                break;
            }
            case BACKUPS: {
                table = Tables.BACKUPS;
                break;
            }
            case MOTIONS: {
                table = Tables.MOTIONS;
                break;
            }
            case LOCATIONS: {
                table = Tables.LOCATIONS;
                break;
            }
            case ACTIVITIES: {
                table = Tables.ACTIVITIES;
                break;
            }
            case VIDEOS: {
                table = Tables.VIDEOS;
                break;
            }
            case FILE_DOWNLOADS: {
                table = Tables.FILE_DOWNLOADS;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }

        db.beginTransaction();
        try {
            for (ContentValues cv : values) {
                long newID = db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                if (newID <= 0) {
                    LOGD(TAG, "Failed to insert row into " + uri);
                }
            }
            db.setTransactionSuccessful();
            notifyChange(uri);
            numInserted = values.length;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        return numInserted;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ", account=" + getCurrentAccountName(uri, false) + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case TRACKS: {
                long newId = db.insertWithOnConflict(Tables.TRACKS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                notifyChange(uri);
                return Tracks.buildTrackUri("" + newId);
            }
            case BACKUPS: {
                long newId = db.insertWithOnConflict(Tables.BACKUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                notifyChange(uri);
                return Backups.buildBackupUri("" + newId);
            }
            case MOTIONS: {
                long newId = db.insertOrThrow(Tables.MOTIONS, null, values);
                notifyChange(uri);
                return Motions.buildMotionUri("" + newId);
            }
            case LOCATIONS: {
                long newId = db.insertOrThrow(Tables.LOCATIONS, null, values);
                notifyChange(uri);
                return Locations.buildLocationUri("" + newId);
            }
            case ACTIVITIES: {
                long newId = db.insertOrThrow(Tables.ACTIVITIES, null, values);
                notifyChange(uri);
                return Activities.buildActivityUri("" + newId);
            }
            case VIDEOS: {
                long newId = -1;
                try {
                    newId = db.insertWithOnConflict(Tables.VIDEOS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    notifyChange(uri);
                } catch (SQLException e) {
                    LOGE(TAG, "Error inserting " + values, e);
                }
                return Videos.buildVideoUri("" + newId);
            }
            case FILE_DOWNLOADS: {
                long newId = -1;
                try {
                    newId = db.insertWithOnConflict(Tables.FILE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    notifyChange(uri);
                } catch (SQLException e) {
                    LOGE(TAG, "Error inserting " + values, e);
                }
                return FileDownloads.buildFileDownloadUri("" + newId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "delete(uri=" + uri + ", account=" + accountName + ")");
        if (uri.equals(TrackerContract.BASE_CONTENT_URI)) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ", account=" + accountName + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACKS: {
                return builder.table(Tables.TRACKS);
            }
            case TRACKS_ID: {
                final String id = Tracks.getTrackId(uri);
                return builder.table(Tables.TRACKS).where(Tracks._ID + "=?", id);
            }
            case BACKUPS: {
                return builder.table(Tables.BACKUPS);
            }
            case BACKUPS_ID: {
                final String id = Backups.getBackupId(uri);
                return builder.table(Tables.BACKUPS).where(Backups._ID + "=?", id);
            }
            case MOTIONS: {
                return builder.table(Tables.MOTIONS);
            }
            case MOTIONS_ID: {
                final String id = Motions.getMotionId(uri);
                return builder.table(Tables.MOTIONS).where(Motions._ID + "=?", id);
            }
            case LOCATIONS: {
                return builder.table(Tables.LOCATIONS);
            }
            case LOCATIONS_ID: {
                final String id = Locations.getLocationId(uri);
                return builder.table(Tables.LOCATIONS).where(Locations._ID + "=?", id);
            }
            case ACTIVITIES: {
                return builder.table(Tables.ACTIVITIES);
            }
            case ACTIVITIES_ID: {
                final String id = Activities.getActivityId(uri);
                return builder.table(Tables.ACTIVITIES).where(Activities._ID + "=?", id);
            }
            case VIDEOS: {
                return builder.table(Tables.VIDEOS);
            }
            case VIDEOS_ID: {
                final String id = Videos.getVideoId(uri);
                return builder.table(Tables.VIDEOS).where(Videos._ID + "=?", id);
            }
            case FILE_DOWNLOADS: {
                return builder.table(Tables.FILE_DOWNLOADS);
            }
            case FILE_DOWNLOADS_ID: {
                final String id = FileDownloads.getFileDownloadId(uri);
                return builder.table(Tables.FILE_DOWNLOADS).where(FileDownloads._ID + "=?", id);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case BACKUPS: {
                return builder.table(Tables.BACKUPS)
                        .groupBy(Backups.DATE + "," + Backups.HOUR + ", " + Backups.CATEGORY);
            }
            case FILE_DOWNLOADS_MEDIA: {
                // A Left Join returns all rows from the left table even if they don't exist in the right table.
                return builder.table(Tables.MEDIA_DOWNLOADS)
                        .mapToTable(FileDownloads._ID, Tables.FILE_DOWNLOADS);
            }
        }
        return null;
    }
}
