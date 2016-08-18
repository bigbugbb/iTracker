package com.localytics.android.itracker.service.sensor.processor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * The base generic data processor class for any specific implementation of sensor data processor.
 * It deals with the processor state and the calls the following delegate methods that user has to implemented:
 *
 * @see #onPreProcess
 * @see #onProcessing(SensorEvent event)
 * @see #onPostProcess
 *
 * User must handle the incoming specific data in their {@link #onProcessing(SensorEvent event)}.
 * When they finish the data handling, markDataProcessed should be called to notify the processor that
 * the data has been processed so the post-process can start.
 *
 * Created by bigbug on 10/8/15.
 */
abstract public class SensorDataProcessor {
    private final static String TAG = makeLogTag(SensorDataProcessor.class);

    protected Context mContext;
    protected Handler mHandler;
    protected Sensor mSensor;
    protected Bundle mArguments;

    private int mState;

    /** @hide */
    @IntDef({
            PROCESSOR_STATE_IDLE,
            PROCESSOR_STATE_PREPROCESS,
            PROCESSOR_STATE_PROCESSING,
            PROCESSOR_STATE_PROCESSED,
            PROCESSOR_STATE_POSTPROCESS,
            PROCESSOR_STATE_CANCELLED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensorDataProcessorState {
    }

    public static final int PROCESSOR_STATE_IDLE        = 0;
    public static final int PROCESSOR_STATE_PREPROCESS  = 1;
    public static final int PROCESSOR_STATE_PROCESSING  = 2;
    public static final int PROCESSOR_STATE_PROCESSED   = 3;
    public static final int PROCESSOR_STATE_POSTPROCESS = 4;
    public static final int PROCESSOR_STATE_CANCELLED   = 5;

    public SensorDataProcessor(Context context, Handler handler, Sensor sensor, Bundle args) {
        mContext = context;
        mHandler = handler;
        mSensor = sensor;
        mArguments = args;
        mState = PROCESSOR_STATE_IDLE;
    }

    @SensorDataProcessorState
    public int getState() {
        synchronized (this) {
            return mState;
        }
    }

    public void cancel() {
        synchronized (this) {
            mState = PROCESSOR_STATE_CANCELLED;
        }
    }

    public void reset() {
        synchronized (this) {
            mState = PROCESSOR_STATE_IDLE;
        }
    }

    public Sensor getSensor() {
        return mSensor;
    }

    public Bundle getArguments() {
        return mArguments;
    }

    public void setSensor(Sensor sensor) {
        mSensor = sensor;
    }

    public void setArguments(Bundle args) {
        mArguments = args;
    }

    public int getSamplingPeriodUs() {
        return 1000000;
    }

    public final void preprocess() throws SensorDataProcessException {
        synchronized (this) {
            if (mState == PROCESSOR_STATE_CANCELLED || mState == PROCESSOR_STATE_PROCESSED) return;
            if (mState != PROCESSOR_STATE_IDLE) {
                throw new SensorDataProcessException("Sensor data processor state is not PROCESSOR_STATE_IDLE");
            }
            mState = PROCESSOR_STATE_PREPROCESS;
        }
        onPreProcess();
    }

    public final void process(SensorEvent event) throws SensorDataProcessException {
        synchronized (this) {
            if (mState == PROCESSOR_STATE_CANCELLED || mState == PROCESSOR_STATE_PROCESSED) return;
            if (mState != PROCESSOR_STATE_PREPROCESS && mState != PROCESSOR_STATE_PROCESSING) {
                throw new SensorDataProcessException(
                        "Sensor data processor state is neither PROCESSOR_STATE_PREPROCESS nor PROCESSOR_STATE_PROCESSING");
            }
            mState = PROCESSOR_STATE_PROCESSING;
        }
        onProcessing(event);
    }

    public final void postprocess() throws SensorDataProcessException {
        synchronized (this) {
            if (mState == PROCESSOR_STATE_CANCELLED) return;
            if (mState != PROCESSOR_STATE_PROCESSED) {
                throw new SensorDataProcessException(
                        "Sensor data processor state is not PROCESSOR_STATE_PROCESSED, you must call markDataProcessed.");
            }
            mState = PROCESSOR_STATE_POSTPROCESS;
        }
        onPostProcess();
    }

    public boolean isDataProcessed() {
        synchronized (this) {
            return mState == PROCESSOR_STATE_PROCESSED;
        }
    }

    protected void markDataProcessed() {
        synchronized (this) {
            if (mState != PROCESSOR_STATE_CANCELLED) {
                mState = PROCESSOR_STATE_PROCESSED;
            }
        }
    }

    public static class SensorDataProcessException extends Exception {
        private static final long serialVersionUID = 1400623677490280711L;

        /**
         * Constructs a {@code SensorDataProcessException} with no specified detail
         * message.
         */
        public SensorDataProcessException() {}

        /**
         * Constructs a {@code SensorDataProcessException} with the specified detail
         * message.
         *
         * @param message the detail message
         */
        public SensorDataProcessException(String message) {
            super(message);
        }
    }

    protected abstract void onPreProcess();

    protected abstract void onProcessing(SensorEvent event);

    protected abstract void onPostProcess();

}
