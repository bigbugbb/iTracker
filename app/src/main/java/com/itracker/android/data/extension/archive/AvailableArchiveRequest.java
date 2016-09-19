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
package com.itracker.android.data.extension.archive;

import android.content.Intent;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.entity.AccountRelated;
import com.itracker.android.data.notification.AccountNotificationItem;
//import com.itracker.android.ui.activity.ArchiveRequest;

public class AvailableArchiveRequest extends AccountRelated implements
        AccountNotificationItem {

    public AvailableArchiveRequest(String account) {
        super(account);
    }

    @Override
    public Intent getIntent() {
//        return ArchiveRequest.createIntent(Application.getInstance(), account);
        return new Intent();
    }

    @Override
    public String getTitle() {
        return Application.getInstance().getString(R.string.archive_available_request_title);
    }

    @Override
    public String getText() {
        return AccountManager.getInstance().getVerboseName(account);
    }

}
