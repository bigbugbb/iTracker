package com.itracker.android.service.sensor.processor;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by bigbug on 10/11/15.
 */
public class SensorDataProcessorFactory {

    public static synchronized SensorDataProcessor getSensorDataProcessor(Context context, Handler handler, Sensor sensor, Bundle args) {
        SensorDataProcessor processor = null;

        switch (sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            processor = new AccelerationDataProcessor(context, handler, sensor, args);
            break;
        }

        return processor;
    }
}
