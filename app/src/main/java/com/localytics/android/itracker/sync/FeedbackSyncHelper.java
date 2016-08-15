package com.localytics.android.itracker.sync;

import android.content.Context;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * Created by bigbug on 5/9/15.
 */
public class FeedbackSyncHelper {
    private static final String TAG = makeLogTag(FeedbackSyncHelper.class);

    Context mContext;
    EventFeedbackApi mEventFeedbackApi;

    FeedbackSyncHelper(Context context) {
        mContext = context;
        mEventFeedbackApi = new EventFeedbackApi(context);
    }

    public void sync() {
//        final ContentResolver cr = mContext.getContentResolver();
//        final Uri newFeedbackUri = TrackerContract.Feedback.CONTENT_URI;
//        Cursor c = cr.query(newFeedbackUri, null, TrackerContract.Feedback.SYNCED + " = 0", null, null);
//        LOGD(TAG, "Number of unsynced feedbacks: " + c.getCount());
//        List<String> updatedSessions = Lists.newArrayList();
//
//        while (c.moveToNext()) {
//            String sessionId = c.getString(c.getColumnIndex(TrackerContract.Feedback.SESSION_ID));
//
//            List<String> questions = Lists.newArrayList();
//            questions.add(c.getString(c.getColumnIndex(TrackerContract.Feedback.SESSION_RATING)));
//            questions.add(c.getString(c.getColumnIndex(TrackerContract.Feedback.ANSWER_RELEVANCE)));
//            questions.add(c.getString(c.getColumnIndex(TrackerContract.Feedback.ANSWER_CONTENT)));
//            questions.add(c.getString(c.getColumnIndex(TrackerContract.Feedback.ANSWER_SPEAKER)));
//            questions.add(c.getString(c.getColumnIndex(TrackerContract.Feedback.COMMENTS)));
//
//            if (mEventFeedbackApi.sendSessionToServer(sessionId, questions)) {
//                LOGI(TAG, "Successfully updated session " + sessionId);
//                updatedSessions.add(sessionId);
//            } else {
//                LOGE(TAG, "Couldn't update session " + sessionId);
//            }
//        }
//
//        c.close();
//
//        // Flip the "synced" flag to true for any successfully updated sessions, but leave them
//        // in the database to prevent duplicate feedback
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(TrackerContract.Feedback.SYNCED, 1);
//        for (String sessionId : updatedSessions) {
//            cr.update(TrackerContract.Feedback.buildFeedbackUri(sessionId), contentValues, null, null);
//        }

    }
}