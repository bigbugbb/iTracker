package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;

import com.localytics.android.itracker.data.model.Track;
import com.localytics.android.itracker.provider.TrackerContract;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * The base class for other fragment.
 * It defines the common communication between each fragment and the TrackerActivity.
 */
public abstract class TrackerFragment extends Fragment
        implements LoaderCallbacks<Cursor>, View.OnKeyListener {
    protected static final String TAG = makeLogTag(TrackerFragment.class);

    public static final String SELECTED_TRACK = "selected_track";
    public static final String BEGIN_DATE = "begin_date";
    public static final String END_DATE = "end_date";

    protected long mBeginTime;
    protected long mEndTime;

    protected boolean mSelected;

    protected int mPosition;

    protected Handler mHandler;
    protected HandlerThread mBackgroundThread;

    public TrackerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(this);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
//        updateSelected();
        updateTimeRange();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void onFragmentSelected() {
        mSelected = true;
    }

    public void onFragmentUnselected() {
        mSelected = false;
    }

    public static void reloadTracks(LoaderManager loaderManager, long beginTime, long endTime, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        args.putLong(BEGIN_DATE, beginTime);
        args.putLong(END_DATE, endTime);
        loaderManager.restartLoader(TracksQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadMotions(LoaderManager loaderManager, Track track, LoaderCallbacks callbacks) {
        if (track != null) {
            Bundle args = new Bundle();
            args.putParcelable(SELECTED_TRACK, track);
            loaderManager.restartLoader(MotionsQuery.TOKEN_NORMAL, args, callbacks);
        }
    }

    public static void reloadLocations(LoaderManager loaderManager, Track track, LoaderCallbacks callbacks) {
        if (track != null) {
            Bundle args = new Bundle();
            args.putParcelable(SELECTED_TRACK, track);
            loaderManager.restartLoader(LocationsQuery.TOKEN_NORMAL, args, callbacks);
        }
    }

    public static void reloadActivities(LoaderManager loaderManager, Track track, LoaderCallbacks callbacks) {
        if (track != null) {
            Bundle args = new Bundle();
            args.putParcelable(SELECTED_TRACK, track);
            loaderManager.restartLoader(ActivitiesQuery.TOKEN_NORMAL, args, callbacks);
        }
    }

    public static void reloadPhotos(LoaderManager loaderManager, long beginTime, long endTime, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        args.putLong(BEGIN_DATE, beginTime);
        args.putLong(END_DATE, endTime);
        loaderManager.restartLoader(PhotosQuery.TOKEN_NORMAL, args, callbacks);
    }

    protected void updateSelected() {
        Activity activity = getActivity();
        if (activity instanceof TrackerActivity) {
            TrackerActivity trackerActivity = (TrackerActivity) activity;
            mSelected = trackerActivity.getSelectedTab() == mPosition;
        }
    }

    protected void updateTimeRange() {
        Activity activity = getActivity();
        if (activity instanceof TrackerActivity) {
            TrackerActivity trackerActivity = (TrackerActivity) activity;
            mBeginTime = trackerActivity.getBeginDate().getMillis();
            mEndTime = trackerActivity.getEndDate().getMillis();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", args=" + args);
        Loader<Cursor> loader = null;

        switch (id) {
            case TracksQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        TrackerContract.Tracks.CONTENT_URI,
                        null,
                        TrackerContract.Tracks.SELECTION_BY_TIME_RANGE,
                        new String[]{ args.getLong(BEGIN_DATE) + "", args.getLong(END_DATE) + "" },
                        TrackerContract.Tracks.DATE + " DESC");
                break;
            }
            case MotionsQuery.TOKEN_NORMAL: {
                Track track = args.getParcelable(SELECTED_TRACK);
                loader = new CursorLoader(
                        getActivity(),
                        TrackerContract.Motions.CONTENT_URI,
                        null,
                        TrackerContract.Motions.SELECTION_BY_TRACK_ID,
                        new String[]{ track.id + "" },
                        TrackerContract.Motions.TIME + " ASC");
                break;
            }
            case LocationsQuery.TOKEN_NORMAL: {
                Track track = args.getParcelable(SELECTED_TRACK);
                loader = new CursorLoader(
                        getActivity(),
                        TrackerContract.Locations.CONTENT_URI,
                        null,
                        TrackerContract.Locations.SELECTION_BY_TRACK_ID,
                        new String[]{ track.id + "" },
                        TrackerContract.Locations.TIME + " ASC");
                break;
            }
            case ActivitiesQuery.TOKEN_NORMAL: {
                Track track = args.getParcelable(SELECTED_TRACK);
                loader = new CursorLoader(
                        getActivity(),
                        TrackerContract.Activities.CONTENT_URI,
                        null,
                        TrackerContract.Activities.SELECTION_BY_TRACK_ID,
                        new String[]{ track.id + "" },
                        TrackerContract.Activities.TIME + " ASC");
                break;
            }
            case PhotosQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        null,
                        String.format("%s >= ? AND %s <= ?", MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DATE_ADDED),
                        new String[]{ args.getLong(BEGIN_DATE) / 1000 + "", args.getLong(END_DATE) / 1000 + "" },
                        MediaStore.Images.Media.DATE_ADDED + " DESC");
                break;
            }
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected interface TracksQuery {
        int TOKEN_NORMAL = 100;
    }

    protected interface MotionsQuery {
        int TOKEN_NORMAL = 200;
    }

    protected interface LocationsQuery {
        int TOKEN_NORMAL = 300;
    }

    protected interface ActivitiesQuery {
        int TOKEN_NORMAL = 400;
    }

    protected interface PhotosQuery {
        int TOKEN_NORMAL = 500;
    }
}
