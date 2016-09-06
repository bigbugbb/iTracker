package com.localytics.android.itracker.ui.adapter;

import com.localytics.android.itracker.data.roster.GroupStateProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Account representation in the contact list.
 */
public class AccountConfiguration extends GroupConfiguration {

    private final TreeMap<String, GroupConfiguration> mGroups;

    public AccountConfiguration(String account, String user,
                                GroupStateProvider groupStateProvider) {
        super(account, user, groupStateProvider);
        mGroups = new TreeMap<>();
    }

    /**
     * Gets group by name.
     *
     * @param group
     * @return <code>null</code> will be returns if there is no such group.
     */
    public GroupConfiguration getGroupConfiguration(String group) {
        return mGroups.get(group);
    }

    /**
     * Adds new group.
     *
     * @param groupConfiguration
     */
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        mGroups.put(groupConfiguration.getUser(), groupConfiguration);
    }

    /**
     * Returns sorted list of groups.
     *
     * @return
     */
    public Collection<GroupConfiguration> getSortedGroupConfigurations() {
        ArrayList<GroupConfiguration> groups = new ArrayList<GroupConfiguration>(
                this.mGroups.values());
        Collections.sort(groups);
        return Collections.unmodifiableCollection(groups);
    }

}