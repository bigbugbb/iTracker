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
        GOOGLE_SIGN_IN_START,
        GOOGLE_SIGN_IN_SUCCEED,
        GOOGLE_SIGN_IN_FAIL,
    }

    /**
     * @param state The current auth state.
     * @param extra The extra information for the auth state.
     */
    void onAuthStateChanged(AuthState state, Bundle extra);

}