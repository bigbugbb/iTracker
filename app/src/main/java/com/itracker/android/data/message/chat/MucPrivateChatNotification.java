package com.itracker.android.data.message.chat;

import android.content.Intent;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.notification.EntityNotificationItem;
import com.itracker.android.data.roster.RosterManager;
//import com.itracker.android.ui.activity.ContactList;

public class MucPrivateChatNotification extends BaseEntity implements EntityNotificationItem {

    public MucPrivateChatNotification(String account, String user) {
        super(account, user);
    }

    @Override
    public Intent getIntent() {
//        return ContactList.createMucPrivateChatInviteIntent(Application.getInstance(), account, user);
        return new Intent();
    }

    @Override
    public String getTitle() {
        return RosterManager.getInstance().getBestContact(account, user).getName();
    }

    @Override
    public String getText() {
        return Application.getInstance().getString(R.string.conference_private_chat);
    }

}
