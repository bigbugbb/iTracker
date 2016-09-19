package com.itracker.android.service.sensor.processor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import com.itracker.android.Config;
import com.itracker.android.data.model.Motion;
import com.itracker.android.provider.TrackerContract;
import com.itracker.android.provider.TrackerContract.Motions;
import com.itracker.android.provider.TrackerContract.Tracks;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.itracker.android.utils.LogUtils.LOGI;
import static com.itracker.android.utils.LogUtils.makeLogTag;

/**
 * Created by bigbug on 10/8/15.
 */
public class AccelerationDataProcessor extends SensorDataProcessor {

    private final static String TAG = makeLogTag(AccelerationDataProcessor.class);

    private static final int  SECOND_IN_MICROS = 1000000;
    private static final int  SAMPLING_RATE = 20;
    private static final long SAMPLING_INTERVAL_TIME_IN_MICROS = SECOND_IN_MICROS / SAMPLING_RATE;

    private float[] mGravity;
    private float[] mLinearAcceleration;

    private boolean mFirstSample;
    private long mFirstSamplingTime;
    private long mLastSamplingTime;
    private long mLastSummarySamplingTime;

    private float mSummary;
    private List<Motion> mMotions;

    private int mSummaryCount;
    private int mSamplingCount;

    public AccelerationDataProcessor(Context context, Handler handler, Sensor sensor, Bundle args) {
        super(context, handler, sensor, args);
        mMotions = new ArrayList<>(Config.MONITORING_DURATION_IN_SECONDS);
    }

    @Override
    public int getSamplingPeriodUs() {
        // Make sure we have samples more than enough.
        return (int) (SECOND_IN_MICROS / SAMPLING_RATE * 0.9f);
    }

    @Override
    protected void onPreProcess() {
        mGravity = new float[3];
        mLinearAcceleration = new float[3];

        mFirstSample = true;
        mFirstSamplingTime = 0;
        mLastSamplingTime = 0;
        mLastSummarySamplingTime = 0;

        mSummary = 0;
        mSummaryCount = 0;
        mSamplingCount = 0;

        mMotions.clear();
    }

    @Override
    protected void onProcessing(SensorEvent event) {
        final long now = SystemClock.elapsedRealtimeNanos() / 1000;

        /**
         * Sampling rate: 10 for the accelerometer data
         */
        if (mFirstSample) {
            LOGI(TAG, "/************ BEGIN MONITORING ************/");
            mFirstSample = false;
            mSamplingCount = 1;
            mFirstSamplingTime = mLastSamplingTime = mLastSummarySamplingTime = now;
            System.arraycopy(event.values, 0, mGravity, 0, 3);
            updateAccelerationSample(event.values);
        }

        final long timeSinceLastSampling = now - mLastSamplingTime;
        if (timeSinceLastSampling >= SAMPLING_INTERVAL_TIME_IN_MICROS) {
            mLastSamplingTime = now;
            int samples = (int) (timeSinceLastSampling / SAMPLING_INTERVAL_TIME_IN_MICROS);
            for (int i = 0; i < samples; ++i) {
                updateAccelerationSample(event.values);
            }
            mSamplingCount += samples;
        }

        // Store the average motion summary from the last second
        if (now - mLastSummarySamplingTime >= SECOND_IN_MICROS && mSummaryCount < Config.MONITORING_DURATION_IN_SECONDS) {
            mLastSummarySamplingTime = now;
            Motion motion = new Motion();
            motion.time = System.currentTimeMillis();
            motion.data = Math.round(mSummary / mSamplingCount);
            motion.sampling = mSamplingCount;
            mMotions.add(motion);
            mSummaryCount++;
            LOGI(TAG, "motion summary: " + motion.data + "\tcount: " + mSummaryCount);
            mSummary = mSamplingCount = 0;
        }

        // After the monitoring is completely done, unblock the handler thread.
        if (now - mFirstSamplingTime >= Config.MONITORING_DURATION_IN_MICROS && mSummaryCount >= Config.MONITORING_DURATION_IN_SECONDS) {
            LOGI(TAG, "/******************************************/");
            markDataProcessed();
        }
    }

    @Override
    protected void onPostProcess() {
        // Update the provider with the motion data.
        final ContentResolver resolver = mContext.getContentResolver();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                long trackId = Tracks.getTrackIdOfDateTime(mContext, DateTime.now());
                if (trackId != -1) {
                    ContentValues[] values = new ContentValues[mMotions.size()];
                    for (int i = 0; i < mMotions.size(); ++i) {
                        Motion motion = mMotions.get(i);
                        ContentValues cv = new ContentValues();
                        cv.put(Motions.TIME, motion.time);
                        cv.put(Motions.DATA, motion.data);
                        cv.put(Motions.SAMPLING, motion.sampling);
                        cv.put(Motions.TRACK_ID, trackId);
                        cv.put(TrackerContract.SyncColumns.UPDATED, DateTime.now().getMillis());
                        values[i] = cv;
                    }
                    resolver.bulkInsert(Motions.CONTENT_URI, values);
                }
            }
        });
    }

    private void updateAccelerationSample(final float[] values) {
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        mGravity[0] = alpha * mGravity[0] + (1 - alpha) * values[0];
        mGravity[1] = alpha * mGravity[1] + (1 - alpha) * values[1];
        mGravity[2] = alpha * mGravity[2] + (1 - alpha) * values[2];

        // Remove the gravity contribution with the high-pass filter.
        mLinearAcceleration[0] = Math.abs(values[0] - mGravity[0]);
        mLinearAcceleration[1] = Math.abs(values[1] - mGravity[1]);
        mLinearAcceleration[2] = Math.abs(values[2] - mGravity[2]);

        // Update summary data (with amplified data magnitude so it'll be easier to chunk the data)
        mSummary += (mLinearAcceleration[0] + mLinearAcceleration[1] + mLinearAcceleration[2]) * Config.ACCELEROMETER_DATA_AMPLIFIER;
    }
}
