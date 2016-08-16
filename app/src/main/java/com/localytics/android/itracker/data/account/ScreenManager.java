/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.localytics.android.itracker.data.account;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.data.OnCloseListener;
import com.localytics.android.itracker.data.OnInitializedListener;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.connection.ConnectionManager;
import com.localytics.android.itracker.data.extension.csi.ClientStateManager;
import com.localytics.android.itracker.receiver.GoAwayReceiver;
import com.localytics.android.itracker.receiver.GoXaReceiver;
import com.localytics.android.itracker.receiver.ScreenReceiver;

/**
 * Manage screen on / off.
 *
 * @author alexander.ivanov
 */
public class ScreenManager implements OnInitializedListener, OnCloseListener {

    private final ScreenReceiver screenReceiver;
    private final AlarmManager alarmManager;
    private final PendingIntent goAwayPendingIntent;
    private final PendingIntent goXaPendingIntent;

    private final static ScreenManager instance;

    static {
        instance = new ScreenManager();
        Application.getInstance().addManager(instance);
    }

    public static ScreenManager getInstance() {
        return instance;
    }

    private ScreenManager() {
        screenReceiver = new ScreenReceiver();
        goAwayPendingIntent = PendingIntent.getBroadcast(
                Application.getInstance(), 0,
                GoAwayReceiver.createIntent(Application.getInstance()), 0);
        goXaPendingIntent = PendingIntent.getBroadcast(
                Application.getInstance(), 0,
                GoXaReceiver.createIntent(Application.getInstance()), 0);
        alarmManager = (AlarmManager) Application.getInstance()
                .getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void onInitialized() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        Application.getInstance().registerReceiver(screenReceiver, filter);
    }

    @Override
    public void onClose() {
        alarmManager.cancel(goAwayPendingIntent);
        alarmManager.cancel(goXaPendingIntent);
        Application.getInstance().unregisterReceiver(screenReceiver);
    }

    private long getTime(int milliSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MILLISECOND, milliSeconds);
        return calendar.getTimeInMillis();
    }

    public void onScreen(Intent intent) {
        int goAway = SettingsManager.connectionGoAway();
        int goXa = SettingsManager.connectionGoXa();
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            ConnectionManager.getInstance().updateConnections(false);
            alarmManager.cancel(goAwayPendingIntent);
            alarmManager.cancel(goXaPendingIntent);
            AccountManager.getInstance().wakeUp();

            // notify server(s) that client is now active
            ClientStateManager.setActive();
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            if (goAway >= 0)
                alarmManager.set(AlarmManager.RTC_WAKEUP, getTime(goAway),
                        goAwayPendingIntent);
            if (goXa >= 0)
                alarmManager.set(AlarmManager.RTC_WAKEUP, getTime(goXa),
                        goXaPendingIntent);

            // notify server(s) that client is now inactive
            ClientStateManager.setInactive();
        }
    }

}
