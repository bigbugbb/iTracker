package com.itracker.android.data;

import java.io.IOException;

/**
 * General {@link IOException} that indicates a problem occurred while parsing or applying a {@link
 * JSONHandler}.
 */
public class HandlerException extends IOException {

    public HandlerException() {
        super();
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    @Override
    public String toString() {
        if (getCause() != null) {
            return getLocalizedMessage() + ": " + getCause();
        } else {
            return getLocalizedMessage();
        }
    }
}