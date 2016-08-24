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
package com.localytics.android.itracker.data.roster;

import android.database.Cursor;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.OnLoadListener;
import com.localytics.android.itracker.data.account.AccountItem;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.account.OnAccountRemovedListener;
import com.localytics.android.itracker.data.entity.NestedMap;
import com.localytics.android.itracker.data.entity.NestedMap.Entry;

public class GroupManager implements OnLoadListener, OnAccountRemovedListener,
        GroupStateProvider {

    /**
     * Reserved group name for the rooms.
     */
    public static final String IS_ROOM = "com.localytics.android.itracker.data.IS_ROOM";

    /**
     * Reserved group name for active chat group.
     */
    public static final String ACTIVE_CHATS = "com.localytics.android.itracker.data.ACTIVE_CHATS";

    /**
     * Reserved group name to store information about group "out of groups".
     */
    public static final String NO_GROUP = "com.localytics.android.itracker.data.NO_GROUP";

    /**
     * Group name used to store information about account itself.
     */
    public static final String IS_ACCOUNT = "com.localytics.android.itracker.data.IS_ACCOUNT";

    /**
     * Account name used to store information that don't belong to any account.
     */
    public static final String NO_ACCOUNT = "com.localytics.android.itracker.data.NO_ACCOUNT";
    private final static GroupManager instance;

    static {
        instance = new GroupManager();
        Application.getInstance().addManager(instance);
    }

    /**
     * List of settings for roster groups in accounts.
     */
    private final NestedMap<GroupConfiguration> groupConfigurations;

    private GroupManager() {
        groupConfigurations = new NestedMap<GroupConfiguration>();
    }

    public static GroupManager getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        final NestedMap<GroupConfiguration> groupConfigurations = new NestedMap<GroupConfiguration>();
        Cursor cursor = GroupTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    GroupConfiguration rosterConfiguration = new GroupConfiguration();
                    rosterConfiguration.setExpanded(GroupTable
                            .isExpanded(cursor));
                    rosterConfiguration.setShowOfflineMode(GroupTable
                            .getShowOfflineMode(cursor));
                    groupConfigurations.put(GroupTable.getAccount(cursor),
                            GroupTable.getGroup(cursor), rosterConfiguration);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(groupConfigurations);
            }
        });
    }

    private void onLoaded(NestedMap<GroupConfiguration> groupConfigurations) {
        this.groupConfigurations.addAll(groupConfigurations);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        groupConfigurations.clear(accountItem.getAccount());
    }

    /**
     * @return Group's name to be display.
     * @see {@link #IS_ROOM}, {@link #ACTIVE_CHATS}, {@link #NO_GROUP},
     * {@link #IS_ACCOUNT}, {@link #NO_ACCOUNT}.
     */
    public String getGroupName(String account, String group) {
        if (GroupManager.NO_GROUP.equals(group))
            return Application.getInstance().getString(R.string.group_none);
        else if (GroupManager.IS_ROOM.equals(group))
            return Application.getInstance().getString(R.string.group_room);
        else if (GroupManager.ACTIVE_CHATS.equals(group))
            return Application.getInstance().getString(
                    R.string.group_active_chat);
        else if (GroupManager.IS_ACCOUNT.equals(group))
            return AccountManager.getInstance().getVerboseName(account);
        return group;
    }

    @Override
    public boolean isExpanded(String account, String group) {
        GroupConfiguration configuration = groupConfigurations.get(account,
                group);
        if (configuration == null)
            return true;
        return configuration.isExpanded();
    }

    @Override
    public ShowOfflineMode getShowOfflineMode(String account, String group) {
        GroupConfiguration configuration = groupConfigurations.get(account,
                group);
        if (configuration == null)
            return ShowOfflineMode.normal;
        return configuration.getShowOfflineMode();
    }

    @Override
    public void setExpanded(String account, String group, boolean expanded) {
        GroupConfiguration configuration = groupConfigurations.get(account,
                group);
        if (configuration == null) {
            configuration = new GroupConfiguration();
            groupConfigurations.put(account, group, configuration);
        }
        configuration.setExpanded(expanded);
        requestToWriteGroup(account, group, configuration.isExpanded(),
                configuration.getShowOfflineMode());
    }

    @Override
    public void setShowOfflineMode(String account, String group,
                                   ShowOfflineMode showOfflineMode) {
        GroupConfiguration configuration = groupConfigurations.get(account,
                group);
        if (configuration == null) {
            configuration = new GroupConfiguration();
            groupConfigurations.put(account, group, configuration);
        }
        configuration.setShowOfflineMode(showOfflineMode);
        requestToWriteGroup(account, group, configuration.isExpanded(),
                configuration.getShowOfflineMode());
    }

    /**
     * Reset all show offline modes.
     */
    public void resetShowOfflineModes() {
        for (Entry<GroupConfiguration> entry : groupConfigurations)
            if (entry.getValue().getShowOfflineMode() != ShowOfflineMode.normal)
                setShowOfflineMode(entry.getFirst(), entry.getSecond(),
                        ShowOfflineMode.normal);
    }

    private void requestToWriteGroup(final String account, final String group,
                                     final boolean expanded, final ShowOfflineMode showOfflineMode) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                GroupTable.getInstance().write(account, group, expanded,
                        showOfflineMode);
            }
        });
    }

}