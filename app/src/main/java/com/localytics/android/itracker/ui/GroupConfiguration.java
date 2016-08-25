package com.localytics.android.itracker.ui;

import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.GroupManager;
import com.localytics.android.itracker.data.roster.GroupStateProvider;
import com.localytics.android.itracker.data.roster.ShowOfflineMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Group representation in the contact list.
 */
public class GroupConfiguration extends BaseEntity {

    /**
     * List of contacts in group.
     */
    private final ArrayList<AbstractContact> abstractContacts;

    /**
     * Whether group has no contacts to display in expanded mode.
     */
    private boolean empty;

    /**
     * Whether group is expanded.
     */
    private final boolean expanded;

    /**
     * Total number of contacts in group.
     */
    private int total;

    /**
     * Number of online contacts in group.
     */
    private int online;

    /**
     * Mode of showing offline contacts.
     */
    private final ShowOfflineMode showOfflineMode;

    public GroupConfiguration(String account, String group,
                              GroupStateProvider groupStateProvider) {
        super(account, group);
        abstractContacts = new ArrayList<>();
        expanded = groupStateProvider.isExpanded(account, group);
        showOfflineMode = groupStateProvider.getShowOfflineMode(account, group);
        empty = true;
        total = 0;
        online = 0;
    }

    /**
     * Adds new contact.
     *
     * @param abstractContact
     */
    public void addAbstractContact(AbstractContact abstractContact) {
        abstractContacts.add(abstractContact);
    }

    /**
     * Gets list of contacts.
     *
     * @return
     */
    public Collection<AbstractContact> getAbstractContacts() {
        return abstractContacts;
    }

    /**
     * Sorts list of abstract contacts.
     *
     * @param comparator
     */
    public void sortAbstractContacts(Comparator<AbstractContact> comparator) {
        Collections.sort(abstractContacts, comparator);
    }

    /**
     * Increments number of contacts in group.
     *
     * @param online whether contact is online.
     */
    public void increment(boolean online) {
        this.total++;
        if (online) {
            this.online++;
        }
    }

    /**
     * @return Whether there is no one contact to be displayed in expanded mode.
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Set that there is at least one contact to be displayed in expanded mode.
     */
    public void setNotEmpty() {
        empty = false;
    }

    /**
     * @return Whether group is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @return Total number of contacts in group.
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return Number of online contacts in group.
     */
    public int getOnline() {
        return online;
    }

    /**
     * @return Mode of showing offline contacts.
     */
    public ShowOfflineMode getShowOfflineMode() {
        return showOfflineMode;
    }

    @Override
    public int compareTo(BaseEntity another) {
        final String anotherUser = another.getUser();
        int result = account.compareTo(another.getAccount());
        if (result != 0) {
            if (user.compareTo(another.getUser()) != 0) {
                if (user.equals(GroupManager.ACTIVE_CHATS)) {
                    return -1;
                }
                if (anotherUser.equals(GroupManager.ACTIVE_CHATS)) {
                    return 1;
                }
            }
            return result;
        }
        result = user.compareTo(anotherUser);
        if (result != 0) {
            if (user.equals(GroupManager.ACTIVE_CHATS)) {
                return -1;
            }
            if (anotherUser.equals(GroupManager.ACTIVE_CHATS)) {
                return 1;
            }
            if (user.equals(GroupManager.IS_ACCOUNT)) {
                return -1;
            }
            if (anotherUser.equals(GroupManager.IS_ACCOUNT)) {
                return 1;
            }
            if (user.equals(GroupManager.NO_GROUP)) {
                return -1;
            }
            if (anotherUser.equals(GroupManager.NO_GROUP)) {
                return 1;
            }
            if (user.equals(GroupManager.IS_ROOM)) {
                return -1;
            }
            if (anotherUser.equals(GroupManager.IS_ROOM)) {
                return 1;
            }
            return result;
        }
        return 0;
    }

}