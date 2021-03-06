package com.itracker.android.ui.listener;

import android.os.Bundle;

import com.itracker.android.data.BaseUIListener;

public interface OnAuthStateChangedListener  extends BaseUIListener {

    enum AuthState {
        // Regular auth states
        REGULAR_AUTH_START,
        REGULAR_AUTH_SUCCEED,
        REGULAR_AUTH_FAIL,
        // OAuth states
        GOOGLE_AUTH_START,
        GOOGLE_AUTH_SUCCEED,
        GOOGLE_AUTH_FAIL,
    }

    /**
     * @param state The current auth state.
     * @param message The message related to the auth state.
     * @param extra The extra information for the auth state.
     */
    void onAuthStateChanged(AuthState state, String message, Bundle extra);

}