package com.localytics.android.itracker.sync;

import android.content.Context;

import com.localytics.android.itracker.Config;

import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class EventFeedbackApi {
    private static final String TAG = makeLogTag(EventFeedbackApi.class);

    private static final String PARAMETER_EVENT_CODE = "code";
    private static final String PARAMETER_API_KEY = "apikey";

    private static final String PARAMETER_SESSION_ID = "objectid";
    private static final String PARAMETER_SURVEY_ID = "surveyId";
    private static final String PARAMETER_REGISTRANT_ID = "registrantKey";
    private final Context mContext;
    private final String mUrl;

    public EventFeedbackApi(Context context) {
        mContext = context;
        mUrl = Config.FEEDBACK_URL;
    }

    /**
     * Posts a session to the event server.
     *
     * @param sessionId The ID of the session that was reviewed.
     * @return whether or not updating succeeded
     */
    public boolean sendSessionToServer(String sessionId, List<String> questions) {

//        BasicHttpClient httpClient = new BasicHttpClient();
//        httpClient.addHeader(PARAMETER_EVENT_CODE, Config.FEEDBACK_API_CODE);
//        httpClient.addHeader(PARAMETER_API_KEY, Config.FEEDBACK_API_KEY);
//
//        ParameterMap parameterMap = httpClient.newParams();
//        parameterMap.add(PARAMETER_SESSION_ID, sessionId);
//        parameterMap.add(PARAMETER_SURVEY_ID, Config.FEEDBACK_SURVEY_ID);
//        parameterMap.add(PARAMETER_REGISTRANT_ID, Config.FEEDBACK_DUMMY_REGISTRANT_ID);
//        int i = 1;
//        for (String question : questions) {
//            parameterMap.add("q" + i, question);
//            i++;
//        }
//
//        HttpResponse response = httpClient.get(mUrl, parameterMap);
//
//        if (response != null && response.getStatus() == HttpURLConnection.HTTP_OK) {
//            LOGD(TAG, "Server returned HTTP_OK, so session posting was successful.");
//            return true;
//        } else {
//            LOGE(TAG, "Error posting session: HTTP status " + response.getStatus());
//            return false;
//        }
        return false;
    }

}
