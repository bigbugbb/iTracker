package com.localytics.android.itracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.localytics.android.itracker.Application;

public class ShutDownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Application.getInstance().requestToClose();
    }
}
