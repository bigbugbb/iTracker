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
package com.localytics.android.itracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.service.ImService;

/**
 * Android boot receiver.
 *
 * @author alexander.ivanov
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SettingsManager.connectionStartAtBoot()) {
            context.startService(ImService.createIntent(context));
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}
