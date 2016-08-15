package com.localytics.android.itracker.monitor;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.monitor.processor.SensorDataProcessor;
import com.localytics.android.itracker.monitor.processor.SensorDataProcessorFactory;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.Activities;
import com.localytics.android.itracker.provider.TrackerContract.Locations;
import com.localytics.android.itracker.ui.TrackerActivity;
import com.localytics.android.itracker.utils.PlayServicesUtils;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class SensorMonitorService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(SensorMonitorService.class);

    private static final int NOTIFICATION_ID = 123;

    public static final String MONITORED_SENSORS = "monitored_sensors";
    public static final String MONITORED_SENSOR_ARGS = "monitored_sensor_args";

    private static final String INTENT_ACTION_PREFIX = "com.localytics.android.itracker.intent.action";
    public static final String ACTION_LOCATION_UPDATED = INTENT_ACTION_PREFIX + ".LOCATION_UPDATED";
    public static final String ACTION_ACTIVITIES_UPDATED = INTENT_ACTION_PREFIX + ".ACTIVITIES_UPDATED";

    private Context mContext;
    private Intent mIntent;

    private SensorManager mSensorManager;

    private GoogleApiClient mGoogleApiClient;

    private Map<String, SensorDataProcessor> mProcessors;

    private Looper mEventLooper;
    private Handler mEventHandler;

    private Looper mRecordLooper;
    private Handler mRecordHandler;

    private PendingIntent mLocationPendingIntent;
    private PendingIntent mActivityPendingIntent;

    private CountDownLatch mLatch;

    private static long sLastLocationUpdateTime;
    private static long sLastActivityUpdateTime;

    public SensorMonitorService() {
        super("SensorMonitorService");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        mProcessors = new HashMap<>();

        HandlerThread sensorEventThread = new HandlerThread("SensorEventThread");
        sensorEventThread.start();
        mEventLooper = sensorEventThread.getLooper();
        mEventHandler = new Handler(mEventLooper);

        HandlerThread recordDataThread = new HandlerThread("RecordDataThread");
        recordDataThread.start();
        mRecordLooper = recordDataThread.getLooper();
        mRecordHandler = new Handler(mRecordLooper);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LOCATION_UPDATED);
        filter.addAction(ACTION_ACTIVITIES_UPDATED);
        registerReceiver(mDataUpdatedReceiver, filter, null, mRecordHandler);

        buildGoogleApiClient();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventLooper.quitSafely();
        mRecordLooper.quitSafely(); // Make sure the pending messages are handled

        mGoogleApiClient.disconnect();
        unregisterReceiver(mDataUpdatedReceiver);

        // Release the wake lock provided by the BroadcastReceiver.
        TrackerBroadcastReceiver.completeWakefulIntent(mIntent);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               Context#startService(Intent)}.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            mIntent = intent;

            // Notify user the tracker is working now.
            sendNotification(getString(R.string.tracker_message));

            // Get sensor data processor for each sensor
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            final int[] sensorTypes = intent.getIntArrayExtra(MONITORED_SENSORS);
            for (int sensorType : sensorTypes) {
                List<Sensor> sensors = mSensorManager.getSensorList(sensorType);
                if (sensors != null && sensors.size() > 0) {
                    Sensor sensor = sensors.get(0);
                    Bundle args = intent.getBundleExtra(MONITORED_SENSOR_ARGS + sensor.getType());
                    SensorDataProcessor processor = SensorDataProcessorFactory.getSensorDataProcessor(mContext, mRecordHandler, sensor, args);
                    if (processor != null) {
                        mProcessors.put(sensor.toString(), processor);
                    }
                }
            }
            mLatch = new CountDownLatch(mProcessors.size());

            // Data processors begin pre process.
            for (SensorDataProcessor processor : mProcessors.values()) {
                processor.preprocess();
            }

            // Register sensor event listener for each sensor.
            // Processor should only process data when there is no pre-process or pre-process is done.
            for (SensorDataProcessor processor : mProcessors.values()) {
                mSensorManager.registerListener(mSensorEventListener, processor.getSensor(), processor.getSamplingPeriodUs(), mEventHandler);
            }

            // Add 30% extra time to make sure all sensors have enough time to complete their work.
            long timeout = (long) (Config.MONITORING_DURATION_IN_MICROS * 1.3f);
            long maxAllowedTimeout = (long) (Config.MONITORING_INTERVAL_IN_MILLS * 0.9f * 1000);
            if (!mLatch.await(Math.min(timeout, maxAllowedTimeout), TimeUnit.MICROSECONDS)) {
                LOGD(TAG, "Sensor data are not completely processed.");
            }

            // Data processors begin post process.
            for (SensorDataProcessor processor : mProcessors.values()) {
                if (!processor.isDataProcessed()) {
                    LOGE(TAG, String.format("%s for %s got cancelled because it was still working after the timeout.", processor, processor.getSensor()));
                    processor.cancel();
                }
                processor.postprocess();
            }

        } catch (Exception e) {
            LOGD(TAG, "Caught exception in SensorMonitorService: " + e);
            e.printStackTrace();
        } finally {
            // Unregister sensor event listeners
            mSensorManager.unregisterListener(mSensorEventListener);

            // Indicate the recording is over
            cancelNotification();
        }
    }

    // Post a notification indicating the data tracker is processing.
    private void sendNotification(String msg) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(this, TrackerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.tracker_alert))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    // Cancel the notification so the notification icon won't stay in the status bar.
    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            SensorDataProcessor processor = mProcessors.get(event.sensor.toString());
            int state = processor.getState();
            if (state < SensorDataProcessor.PROCESSOR_STATE_PROCESSED) {
                try {
                    processor.process(event);
                } catch (SensorDataProcessException e) {
                    LOGE(TAG, "SensorDataProcessException - " + e);
                } finally {
                    state = processor.getState();
                    if (state == SensorDataProcessor.PROCESSOR_STATE_CANCELLED ||
                            state == SensorDataProcessor.PROCESSOR_STATE_PROCESSED) {
                        mLatch.countDown();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        LOGI(TAG, "Connected to GoogleApiClient");
        final long time = SystemClock.elapsedRealtime();

        /**
         * Request location update
         */
        if (Math.round((time - sLastLocationUpdateTime) * 1f / Config.LOCATION_REQUEST_INTERVAL) >= 1) {
            Intent locationIntent = new Intent(ACTION_LOCATION_UPDATED);
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(Config.LOCATION_REQUEST_INTERVAL);
            locationRequest.setFastestInterval(Config.LOCATION_REQUEST_INTERVAL);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mLocationPendingIntent = PendingIntent.getBroadcast(this, 0, locationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    locationRequest,
                    mLocationPendingIntent
            );
        }

        /**
         * Request activity recognition
         * It seems that the default detection interval is 16 secs if there is no significant activity changes.
         * However, we only want to get the detected data once per minute and the detected data is retrieved
         * as soon as the following request is called, so we don't care too much about the interval time.
         */
        if (Math.round((time - sLastActivityUpdateTime) * 1f / Config.ACTIVITY_REQUEST_INTERVAL) >= 1) {
            Intent activityIntent = new Intent(ACTION_ACTIVITIES_UPDATED);

            mActivityPendingIntent = PendingIntent.getBroadcast(this, 1, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    20000,
                    mActivityPendingIntent
            );
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGI(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in onConnectionFailed.
        LOGI(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    private BroadcastReceiver mDataUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final long trackId = TrackerContract.Tracks.getTrackIdOfDateTime(mContext, DateTime.now());
            ContentResolver resolver = mContext.getContentResolver();

            // Insert, insert...
            if (action.equals(ACTION_LOCATION_UPDATED)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    final Location location = result.getLastLocation();
                    LOGI(TAG, String.format("[Location] lat = %f, lon = %f, alt = %f", location.getLatitude(), location.getLongitude(), location.getAltitude()));

                    ContentValues values = new ContentValues();
                    values.put(Locations.TIME, DateTime.now().getMillis());
                    values.put(Locations.LATITUDE, location.getLatitude());
                    values.put(Locations.LONGITUDE, location.getLongitude());
                    values.put(Locations.ALTITUDE, location.getAltitude());
                    values.put(Locations.ACCURACY, location.getAccuracy());
                    values.put(Locations.SPEED, location.getSpeed());
                    values.put(Locations.TRACK_ID, trackId);
                    values.put(TrackerContract.SyncColumns.UPDATED, DateTime.now().getMillis());
                    resolver.insert(Locations.CONTENT_URI, values);

                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationPendingIntent);

                    sLastLocationUpdateTime = SystemClock.elapsedRealtime();
                }
            } else if (action.equals(ACTION_ACTIVITIES_UPDATED)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                if (result != null) {
                    DetectedActivity activity = result.getMostProbableActivity();
                    LOGI(TAG, activity.toString());

                    ContentValues values = new ContentValues();
                    values.put(Activities.TIME, DateTime.now().getMillis());
                    values.put(Activities.TYPE, PlayServicesUtils.getActivityString(getApplicationContext(), activity.getType()));
                    values.put(Activities.TYPE_ID, activity.getType());
                    values.put(Activities.CONFIDENCE, activity.getConfidence());
                    values.put(Activities.TRACK_ID, trackId);
                    values.put(TrackerContract.SyncColumns.UPDATED, DateTime.now().getMillis());
                    resolver.insert(Activities.CONTENT_URI, values);

                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityPendingIntent);

                    sLastActivityUpdateTime = SystemClock.elapsedRealtime();
                }
            }
        }
    };
}
