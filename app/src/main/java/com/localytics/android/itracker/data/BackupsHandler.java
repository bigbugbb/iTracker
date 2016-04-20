package com.localytics.android.itracker.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.localytics.android.itracker.data.model.Backup;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Backups;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class BackupsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(BackupsHandler.class);

    private HashMap<String, Backup> mBackups = new HashMap<>();

    public BackupsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Backup backup : new Gson().fromJson(element, Backup[].class)) {
            backup.state = TrackerContract.BackupState.IDLE.state();
            mBackups.put(backup.s3_key, backup);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(Backups.CONTENT_URI);
        long now = DateTime.now().getMillis();
        for (Backup backup : mBackups.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(Backups.S3_KEY, backup.s3_key);
            builder.withValue(Backups.CATEGORY, backup.category);
            builder.withValue(Backups.STATE, backup.state);
            builder.withValue(Backups.DATE, backup.date);
            builder.withValue(Backups.HOUR, backup.hour);
            builder.withValue(Backups.UPDATED, now);
            list.add(builder.build());
        }
    }
}
