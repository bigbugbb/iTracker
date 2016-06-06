package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.data.model.Track;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.ui.widget.ActionFrameLayout;
import com.localytics.android.itracker.ui.widget.MotionsView;
import com.localytics.android.itracker.ui.widget.TimelineItem;
import com.localytics.android.itracker.ui.widget.TimelinesView;
import com.localytics.android.itracker.ui.widget.TrackItemAdapter;
import com.localytics.android.itracker.util.ThrottledContentObserver;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class ActionFragment extends TrackerFragment implements
        OnTrackItemSelectedListener {

    private static final String TAG = makeLogTag(ActionFragment.class);

    private RecyclerView  mTracksView;
    private TextView      mShowTimelinesView;
    private TimelinesView mTimelinesView;
    private MotionsView   mMotionsView;
    private ProgressBar   mLoadingView;

    private TimeRangeController mTimeRangeController;

    private TrackItemAdapter mTrackItemAdapter;

    private Track mSelectedTrack;

    private ThrottledContentObserver mTracksObserver;
    private ThrottledContentObserver mMotionsObserver;
    private ThrottledContentObserver mActivitiesObserver;

    private boolean mSearchEnabled;

    public ActionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mTimeRangeController = new TimeRangeController(this);

        mTracksObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (tracks). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting motions cursor reload as a result of ContentObserver firing.");
                    long beginTime = mTimeRangeController.getBeginDate().getMillis();
                    long endtime = mTimeRangeController.getEndDate().getMillis();
                    reloadTracks(getLoaderManager(), beginTime, endtime, ActionFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(TrackerContract.Tracks.CONTENT_URI, true, mTracksObserver);

        mMotionsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (motions). Content changed.");
                if (isAdded()) {
                    if (mSelectedTrack != null) {
                        LOGD(TAG, "Requesting motions cursor reload as a result of ContentObserver firing.");
                        reloadMotions(getLoaderManager(), mSelectedTrack, ActionFragment.this);
                    }
                }
            }
        });
        activity.getContentResolver().registerContentObserver(TrackerContract.Motions.CONTENT_URI, true, mMotionsObserver);

        mActivitiesObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (activities). Content changed.");
                if (isAdded()) {
                    if (mSelectedTrack != null) {
                        LOGD(TAG, "Requesting motions cursor reload as a result of ContentObserver firing.");
                        reloadActivities(getLoaderManager(), mSelectedTrack, ActionFragment.this);
                    }
                }
            }
        });
        activity.getContentResolver().registerContentObserver(TrackerContract.Activities.CONTENT_URI, true, mActivitiesObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mSelectedTrack = null;

        getActivity().getContentResolver().unregisterContentObserver(mTracksObserver);
        getActivity().getContentResolver().unregisterContentObserver(mMotionsObserver);
        getActivity().getContentResolver().unregisterContentObserver(mActivitiesObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = 0;
        mTimeRangeController.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ActionFrameLayout layout = (ActionFrameLayout) inflater.inflate(R.layout.fragment_action, container, false);

        mTracksView    = (RecyclerView) layout.findViewById(R.id.tracks_view);
        mTimelinesView = (TimelinesView) layout.findViewById(R.id.activities_view);
        mMotionsView   = (MotionsView) layout.findViewById(R.id.motions_view);
        mLoadingView   = (ProgressBar) layout.findViewById(R.id.motions_loading_progress);
        mShowTimelinesView = (TextView) layout.findViewById(R.id.show_timeline);

        layout.setOnFootprintFabClickedListener(new ActionFrameLayout.OnFootprintFabClickedListener() {
            @Override
            public void onFootprintFabClicked() {
                Intent intent = new Intent(getActivity(), FootprintActivity.class);
                intent.putExtra(SELECTED_TRACK, mSelectedTrack);
                startActivity(intent);
            }
        });

        mTrackItemAdapter = new TrackItemAdapter(getActivity());
        mTrackItemAdapter.addOnItemSelectedListener(this);

        mTracksView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        mTracksView.setItemAnimator(new DefaultItemAnimator());
        mTracksView.setAdapter(mTrackItemAdapter);

        ListView listView = mTimelinesView.getListView();
        listView.setAdapter(new TimelinesView.TimelineItemAdapter(getActivity()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TimelinesView.TimelineItemAdapter adapter = (TimelinesView.TimelineItemAdapter) parent.getAdapter();
                adapter.selectItem(position);

                for (int i = 0; i < parent.getChildCount(); ++i) {
                    TimelineItem item = (TimelineItem) parent.getChildAt(i);
                    item.setTimelineDrawable(item == view);
                }

                TimelineItem.Timeline timeline = adapter.getItem(position);
                mMotionsView.moveViewport(timeline.getStartTime(), timeline.getStopTime());
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShowTimelinesView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ripple_effect));
        }

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_action, menu);

        if (mSearchEnabled) {
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            toolbar.addView(mTimeRangeController.getTimeRange(),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            menu.findItem(R.id.action_clear).setVisible(true);
            menu.findItem(R.id.action_search).setVisible(false);
        }

        mMenu = menu;
    }

    @Override
    public void onDestroyOptionsMenu() {
        if (mSearchEnabled) {
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            toolbar.removeView(mTimeRangeController.getTimeRange());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        switch (item.getItemId()) {
            case R.id.action_search: {
                mSearchEnabled = true;
                item.setVisible(false);
                toolbar.addView(mTimeRangeController.getTimeRange(),
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                mMenu.findItem(R.id.action_clear).setVisible(true);
                return true;
            }
            case R.id.action_clear: {
                mSearchEnabled = false;
                item.setVisible(false);
                toolbar.removeView(mTimeRangeController.getTimeRange());
                mMenu.findItem(R.id.action_search).setVisible(true);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mTimeRangeController.updateTimeRange();
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");
        long beginTime = mTimeRangeController.getBeginDate().getMillis();
        long endtime = mTimeRangeController.getEndDate().getMillis();
        reloadTracks(getLoaderManager(), beginTime, endtime, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTracksObserver.cancelPendingCallback();
        mMotionsObserver.cancelPendingCallback();
        mActivitiesObserver.cancelPendingCallback();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mTimeRangeController.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
    }

    @Override
    public void onFragmentUnselected() {
        super.onFragmentUnselected();
    }

    @Override
    public void onTrackItemSelected(View view, int position) {
        showLoadingProgress();
        mSelectedTrack = mTrackItemAdapter.getItem(position);
        reloadMotions(getLoaderManager(), mSelectedTrack, ActionFragment.this);
        reloadActivities(getLoaderManager(), mSelectedTrack, ActionFragment.this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case TracksQuery.TOKEN_NORMAL: {
                mTrackItemAdapter.changeCursor(data);
                break;
            }
            case MotionsQuery.TOKEN_NORMAL: {
                Motion[] motions = Motion.motionsFromCursor(data);
                mMotionsView.updateMotions(motions, new MotionsView.OnMotionsUpdatedListener() {
                    @Override
                    public void onMotionsUpdated() {
                        hideLoadingProgress();
                    }
                });
                break;
            }
            case ActivitiesQuery.TOKEN_NORMAL: {
                TimelinesView.TimelineItemAdapter adapter =
                        (TimelinesView.TimelineItemAdapter) mTimelinesView.getListView().getAdapter();
                adapter.setNotifyOnChange(false);
                adapter.clear();
                adapter.addAll(TimelineItem.Timeline.fromActivities(data));
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case TracksQuery.TOKEN_NORMAL: {
                mTrackItemAdapter.changeCursor(null);
                break;
            }
            case MotionsQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    private void showLoadingProgress() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.setVisibility(View.VISIBLE);
            }
        }, 50);
    }

    private void hideLoadingProgress() {
        mHandler.removeCallbacksAndMessages(null);
        mLoadingView.setVisibility(View.INVISIBLE);
    }
}
