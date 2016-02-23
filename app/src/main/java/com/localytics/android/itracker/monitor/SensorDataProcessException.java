package com.localytics.android.itracker.monitor;


/**
 * Created by bigbug on 10/9/15.
 */
public class SensorDataProcessException extends Exception {
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
