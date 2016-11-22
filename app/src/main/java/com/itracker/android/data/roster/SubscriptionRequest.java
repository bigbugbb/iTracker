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
package com.itracker.android.data.roster;

import android.content.Intent;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.notification.EntityNotificationItem;
import com.itracker.android.ui.activity.TrackerActivity;
//import com.itracker.android.ui.activity.ContactList;

public class SubscriptionRequest extends BaseEntity implements EntityNotificationItem {

    public SubscriptionRequest(String account, String user) {
        super(account, user);
    }

    @Override
    public Intent getIntent() {
        return TrackerActivity.createContactSubscriptionIntent(Application.getInstance(), account, user);
    }

    @Override
    public String getText() {
        return Application.getInstance().getString(R.string.subscription_request_message);
    }

    @Override
    public String getTitle() {
        return user;
    }

    public String getConfirmation() {
        String accountName = AccountManager.getInstance().getVerboseName(account);
        return Application.getInstance().getString(R.string.contact_subscribe_confirm, accountName);
    }

}
