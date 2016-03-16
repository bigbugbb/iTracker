package com.localytics.android.itracker.sync;

import java.io.IOException;

public class NotSupportedDataException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * Construct a NotSupportedDataException for the sync data not
     * yet supported.
     *
     * @param s message describing the issue
     */
    public NotSupportedDataException(final String s) {
        super(s);
    }

    /**
     * Construct a NotSupportedDataException for the sync data not
     * yet supported.
     *
     * @param s   message describing the issue
     * @param why a lower level implementation specific issue.
     */
    public NotSupportedDataException(final String s, final Throwable why) {
        super(s);
        initCause(why);
    }
}