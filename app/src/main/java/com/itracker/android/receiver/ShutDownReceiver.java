package com.itracker.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.itracker.android.Application;

public class ShutDownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Application.getInstance().requestToClose();
    }
}
