package com.itracker.android.service.download;

import java.io.IOException;

/**
 * Thrown when a download is interrupted.
 */
class DownloadInterruptedException extends IOException {

    private String mReason;

    /**
     * Constructs a new {@code FileDownloadInterruptedException} with its stack trace
     * filled in.
     */
    public DownloadInterruptedException() {
    }

    /**
     * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
     * detail message filled in.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    public DownloadInterruptedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
     * detail message filled in.
     *
     * @param detailMessage
     *            the detail message for this exception.
     * @param reason
     *            the reason for this interruption
     */
    public DownloadInterruptedException(String detailMessage, String reason) {
        super(detailMessage);
        mReason = reason;
    }

    public String getReason() {
        return mReason;
    }
}