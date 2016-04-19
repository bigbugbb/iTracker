package com.localytics.android.itracker.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.localytics.android.itracker.data.model.Backup;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Motions;

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
            mBackups.put(backup.s3_key, backup);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = TrackerContract.addCallerIsSyncAdapterParameter(Motions.CONTENT_URI);

        // since the number of tags is very small, for simplicity we delete them all and reinsert
        list.add(ContentProviderOperation.newDelete(uri).build());
//        for (Motion motion : mMotions.values()) {
//            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
//            builder.withValue(Motions.MOTION_ID, motion.id);
//            builder.withValue(Motions.MOTION_DATE, motion.date);
//            builder.withValue(Motions.MOTION_NOTE, motion.note);
//            list.add(builder.build());
//        }
    }
}
