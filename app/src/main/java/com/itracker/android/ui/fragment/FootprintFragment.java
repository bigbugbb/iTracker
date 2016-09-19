package com.itracker.android.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.itracker.android.R;
import com.itracker.android.data.model.Activity;
import com.itracker.android.data.model.Location;
import com.itracker.android.data.model.Motion;
import com.itracker.android.data.model.Track;
import com.itracker.android.ui.widget.GLMotionsView;
import com.itracker.android.ui.widget.GLMotionsView.OnViewportChangedListener;
import com.itracker.android.utils.LogUtils;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A placeholder fragment containing a simple view.
 */
public class FootprintFragment extends TrackerFragment implements
        OnMapReadyCallback,
        OnViewportChangedListener,
        OnMarkerClickListener,
        OnMarkerDragListener,
        OnInfoWindowClickListener,
        OnInfoWindowLongClickListener,
        OnInfoWindowCloseListener {

    private static final String TAG = LogUtils.makeLogTag(FootprintFragment.class);

    private static final String UNKNOWN_ACTIVITY_TYPE = "unknown";

    private Context mContext;

    private MapFragment mMapFragment;
    private Handler mMainHandler;

    private TextView mMapTimeRange;
    private GLMotionsView mGLMotionsView;

    private GoogleMap mMap;
    private Track mTrack;

    private FootprintInfoWindowAdapter mInfoWindowAdapter;

    private List<Location> mLocations;
    private List<Activity> mActivities;

    private long mStartTime;
    private long mStopTime;

    public static FootprintFragment newInstance(Track track) {
        Bundle args = new Bundle();
        args.putParcelable(SELECTED_TRACK, track);

        FootprintFragment fragment = new FootprintFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public FootprintFragment() {
        mMainHandler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        if (getArguments() != null) {
            mTrack = getArguments().getParcelable(SELECTED_TRACK);
        } else {
            throw new RuntimeException("FootprintFragment must have a motion argument.");
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_footprint, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.footprint_map);
        mMapFragment.getMapAsync(this);

        mGLMotionsView = (GLMotionsView) view.findViewById(R.id.summary_view);
        mMapTimeRange = (TextView) view.findViewById(R.id.map_time_range);
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadMotions(getLoaderManager(), mTrack, this);
        mGLMotionsView.setOnViewportChangedListener(this);

        RectF viewport = mGLMotionsView.getCurrentViewport();
        updateMapTimeRange(viewport.left, viewport.right);
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLMotionsView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLMotionsView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMainHandler.removeCallbacksAndMessages(null);
        mGLMotionsView.setOnViewportChangedListener(null);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mInfoWindowAdapter = new FootprintInfoWindowAdapter();
        mMap.setInfoWindowAdapter(mInfoWindowAdapter);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowLongClickListener(this);

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: request permission
            return;
        } else {
            mMap.setMyLocationEnabled(true);
        }

        // Load location and activity data of the tracked date
        reloadLocations(getLoaderManager(), mTrack, this);
        reloadActivities(getLoaderManager(), mTrack, this);
    }

    @Override
    public void onViewportChanged(RectF prevViewport, RectF currViewport) {
        if (mLocations != null) {
            if (updateMapTimeRange(currViewport.left, currViewport.right)) {
                // Update location under throttle control
                mMainHandler.removeCallbacksAndMessages(null);
                mMainHandler.postDelayed(mUpdateFootprintRunnable, 150);
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case MotionsQuery.TOKEN_NORMAL: {
                mGLMotionsView.updateMotions(Motion.motionsFromCursor(data));
                break;
            }
            case LocationsQuery.TOKEN_NORMAL: {
                Location[] locations = Location.locationsFromCursor(data);
                if (locations != null) {
                    mLocations = Arrays.asList(locations);
                    if (mActivities != null) {
                        RectF currViewport = mGLMotionsView.getCurrentViewport();
                        if (updateMapTimeRange(currViewport.left, currViewport.right)) {
                            mUpdateFootprintRunnable.run();
                        }
                    }
                }
                break;
            }
            case ActivitiesQuery.TOKEN_NORMAL: {
                Activity[] activities = Activity.activitiesFromCursor(data);
                if (activities != null) {
                    mActivities = Arrays.asList(activities);
                    if (mLocations != null) {
                        RectF currViewport = mGLMotionsView.getCurrentViewport();
                        if (updateMapTimeRange(currViewport.left, currViewport.right)) {
                            mUpdateFootprintRunnable.run();
                        }
                    }
                }
            }
        }
    }

    private Runnable mUpdateFootprintRunnable = new Runnable() {
        @Override
        public void run() {
            Collection<Location> locations = getLocationsInTimeRange(); // the locations are in time order
            if (locations.size() == 0) {
                return;
            }

            // Convert Location to LatLng
            Map<Long, LatLng> latLngs = new TreeMap<>();
            for (Location location : locations) {
                latLngs.put(location.time, new LatLng(location.latitude, location.longitude));
            }

            // Removes all markers, overlays, and poly lines from the map.
            mMap.clear();

            // Add markers
            Marker lastMarker = null;
            for (Map.Entry<Long, LatLng> entry : latLngs.entrySet()) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(entry.getValue())
                        .draggable(true)
                        .alpha(0.6f));
                long time = entry.getKey();
                Activity activity = searchTargetActivity(mActivities, time);
                MarkerInfo mi = new MarkerInfo(
                        activity != null ? activity.getActivityIcon(mContext) : mContext.getResources().getDrawable(R.drawable.ic_activity_unknown),
                        time,
                        activity != null ? activity.type : "Unknown activity");
                mInfoWindowAdapter.setMarkerInfo(marker.hashCode(), mi);
                lastMarker = marker;
            }

            // Bring the last marker to the top
            lastMarker.setAlpha(1);
            lastMarker.setIcon(BitmapDescriptorFactory.defaultMarker(210));
            lastMarker.showInfoWindow();

            // Move the camera to the last location
            CameraPosition position = new CameraPosition.Builder().target(lastMarker.getPosition())
                    .zoom(15f)
                    .bearing(0)
                    .tilt(35)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    // TODO:
                }

                @Override
                public void onCancel() {
                    // TODO:
                }
            });
        }
    };

    private Collection<Location> getLocationsInTimeRange() {
        TreeMap<Long, Location> map = new TreeMap<>();
        Location previous = null, next = null;
        for (Location location : mLocations) {
            if (location.time >= mStartTime && location.time < mStopTime) {
                map.put(location.time, location);
            } else if (location.time < mStartTime) {
                previous = location; // location closest to the time before the start time
            } else if (location.time >= mStopTime) {
                if (next == null) {
                    next = location; // location closest to the time after the stop time
                }
            }
        }
        if (map.size() == 0) {
            if (previous != null) {
                map.put(previous.time, previous);
            } else if (next != null) {
                map.put(next.time, next);
            }
        }
        return map.values();
    }

    private boolean updateMapTimeRange(float start, float stop) {
        if (mTrack == null) {
            return false;
        }
        mStartTime = mTrack.date + (long) (DateUtils.DAY_IN_MILLIS * start);
        mStopTime  = mStartTime + (long) (DateUtils.DAY_IN_MILLIS * (stop - start));
        String startTimeString = DateFormatUtils.format(mStartTime, "HH:mm");
        String stopTimeString  = DateFormatUtils.format(mStopTime, "HH:mm");
        mMapTimeRange.setText(String.format("%s - %s", startTimeString, stopTimeString));
        return true;
    }

    private Activity searchTargetActivity(List<Activity> activities, long time) {
        int size = activities.size();

        if (size == 0 || time < activities.get(0).time || time > activities.get(size - 1).time) {
            return null;
        }

        // Activities are sorted from the query, so just use binary search here.
        int beg = 0, end = activities.size() - 1;
        int mid = (beg + end) >> 1;
        while (end - beg > 1) {
            Activity begActivity = activities.get(beg);
            Activity midActivity = activities.get(mid);
            Activity endActivity = activities.get(end);
            if (time >= begActivity.time && time < midActivity.time) {
                end = mid;
            } else if (time > midActivity.time && time <= endActivity.time) {
                beg = mid;
            }
            mid = (beg + end) >> 1;
        }

        // Find the closer one
        Activity begActivity = activities.get(beg);
        Activity endActivity = activities.get(end);
        Activity target = begActivity;
        if (Math.abs(begActivity.time - time) > Math.abs(endActivity.time - time)) {
            target = endActivity;
        }

        return target;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onInfoWindowClose(Marker marker) {

    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        final BounceInterpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 2 * t);

                if (t > 0.0) {
                    handler.postDelayed(this, 20);
                }
            }
        });

        // We return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    public class MarkerInfo {
        Drawable mBadge;
        long     mTime;
        String   mActivity;

        public MarkerInfo(Drawable badge, long time, String activity) {
            mBadge = badge;
            mTime = time;
            mActivity = activity;
        }
    }

    private class FootprintInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mWindow;
        private final Map<Integer, MarkerInfo> mMarkerInfoMap;

        FootprintInfoWindowAdapter() {
            mWindow = LayoutInflater.from(getActivity()).inflate(R.layout.footprint_info_window, null);
            InfoWindowHolder holder = new InfoWindowHolder();
            holder.badge = (ImageView) mWindow.findViewById(R.id.badge);
            holder.time = (TextView) mWindow.findViewById(R.id.time);
            holder.activity = (TextView) mWindow.findViewById(R.id.activity);
            mWindow.setTag(holder);

            mMarkerInfoMap = new HashMap<>();
        }

        public void setMarkerInfo(int key, MarkerInfo mi) {
            mMarkerInfoMap.put(key, mi);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            if (isAdded()) {
                render(marker);
            }
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker) {
            InfoWindowHolder holder = (InfoWindowHolder) mWindow.getTag();
            MarkerInfo mi = mMarkerInfoMap.get(marker.hashCode());

            holder.badge.setImageDrawable(mi.mBadge);

            final String time = "Time: " + DateFormatUtils.format(mi.mTime, "HH:mm");
            if (!TextUtils.isEmpty(time)) {
                SpannableString timeText = new SpannableString(time);
                timeText.setSpan(new StyleSpan(Typeface.BOLD), 0, 6, 0);
                timeText.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(getActivity(), R.color.body_text_2)), 6, timeText.length(), 0);
                holder.time.setText(timeText);
            } else {
                holder.time.setText("");
            }

            final String activity = "Activity: " + mi.mActivity;
            if (!TextUtils.isEmpty(activity)) {
                SpannableString activityText = new SpannableString(activity);
                activityText.setSpan(new StyleSpan(Typeface.BOLD), 0, 10, 0);
                activityText.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(getActivity(), R.color.body_text_2)), 10, activityText.length(), 0);
                holder.activity.setText(activityText);
            } else {
                holder.activity.setText("");
            }
        }

        private class InfoWindowHolder {
            ImageView badge;
            TextView  time;
            TextView  activity;
        }
    }
}
