package com.localytics.android.itracker.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Motions;

import java.util.ArrayList;
import java.util.HashMap;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MotionsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(MotionsHandler.class);

    private HashMap<String, Motion> mMotions = new HashMap<String, Motion>();

    public MotionsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Motion motion : new Gson().fromJson(element, Motion[].class)) {
//            mMotions.put(motion.id, motion);
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

    public HashMap<String, Motion> getMotionMap() {
        return mMotions;
    }
}
