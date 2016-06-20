package com.localytics.android.itracker.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.localytics.android.itracker.data.model.Backup;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Backups;
import com.localytics.android.itracker.provider.TrackerContract.Tracks;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class BackupsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(BackupsHandler.class);

    private Set<Backup> mBackups = new HashSet<>();

    public BackupsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Backup backup : new Gson().fromJson(element, Backup[].class)) {
            mBackups.add(backup);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(Backups.CONTENT_URI);

        Set<Backup> localBackups = new HashSet<>();

        // Get local backups
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    localBackups.add(new Backup(cursor));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Remove existing local backups from remote backups
        mBackups.removeAll(localBackups);

        Set<String> dates = new HashSet<>();
        long now = DateTime.now().getMillis();

        // Add insert ops for remote backups, so later we can grab the data file from s3 with the keys
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        for (Backup backup : mBackups) {
            list.add(ContentProviderOperation.newInsert(uri)
                    .withValue(Backups.S3_KEY, backup.s3_key)
                    .withValue(Backups.CATEGORY, backup.category)
                    .withValue(Backups.DATE, backup.date)
                    .withValue(Backups.HOUR, backup.hour)
                    .withValue(Backups.UPDATED, now)
                    .withValue(Backups.DIRTY, 0)
                    .withValue(Backups.SYNC, TrackerContract.SyncState.PENDING.value())
                    .build());

            dates.add(backup.date);
        }

        // Make the tracks from the backups, so user can see them from the action screen,
        // and later the tracks are used to link the data restored from data files downloaded from s3.
        uri = TrackerContract.addCallerIsSyncAdapterParameter(Tracks.CONTENT_URI);
        for (String date : dates) {
            list.add(ContentProviderOperation.newInsert(uri)
                    .withValue(Tracks.DATE, formatter.parseDateTime(date).getMillis())
                    .withValue(Tracks.UPDATED, now)
                    .build());
        }
    }
}
