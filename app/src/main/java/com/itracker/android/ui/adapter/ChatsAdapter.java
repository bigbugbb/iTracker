package com.itracker.android.ui.adapter;

import android.app.Activity;
import android.os.Handler;
import android.widget.Filter;
import android.widget.Filterable;

import com.itracker.android.data.SettingsManager;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.account.CommonState;
import com.itracker.android.data.extension.blocking.BlockingManager;
import com.itracker.android.data.extension.muc.MUCManager;
import com.itracker.android.data.extension.muc.RoomChat;
import com.itracker.android.data.extension.muc.RoomContact;
import com.itracker.android.data.message.AbstractChat;
import com.itracker.android.data.message.ChatContact;
import com.itracker.android.data.message.MessageManager;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.GroupManager;
import com.itracker.android.data.roster.RosterContact;
import com.itracker.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class ChatsAdapter extends GroupedContactsAdapter implements Runnable, Filterable {

    /**
     * Number of milliseconds between lazy refreshes.
     */
    private static final long REFRESH_INTERVAL = 1000;

    /**
     * Handler for deferred refresh.
     */
    private final Handler mHandler;

    /**
     * Lock for refresh requests.
     */
    private final Object mRefreshLock;

    /**
     * Whether refresh was requested.
     */
    private boolean mRefreshRequested;

    /**
     * Whether refresh is in progress.
     */
    private boolean mRefreshInProgress;

    /**
     * Minimal time when next refresh can be executed.
     */
    private Date mNextRefresh;

    /**
     * Contact filter.
     */
    ContactFilter mContactFilter;

    /**
     * Filter string. Can be <code>null</code> if filter is disabled.
     */
    String mFilterString;

    public ChatsAdapter(Activity activity) {
        super(activity);
        mHandler = new Handler();
        mRefreshLock = new Object();
        mRefreshRequested = false;
        mRefreshInProgress = false;
        mNextRefresh = new Date();
    }

    /**
     * Requests refresh in some time in future.
     */
    public void refreshRequest() {
        synchronized (mRefreshLock) {
            if (mRefreshRequested) {
                return;
            }
            if (mRefreshInProgress) {
                mRefreshRequested = true;
            } else {
                long delay = mNextRefresh.getTime() - new Date().getTime();
                mHandler.postDelayed(this, delay > 0 ? delay : 0);
            }
        }
    }

    /**
     * Remove refresh requests.
     */
    public void removeRefreshRequests() {
        synchronized (mRefreshLock) {
            mRefreshRequested = false;
            mRefreshInProgress = false;
            mHandler.removeCallbacks(this);
        }
    }

    @Override
    public void onChange() {
        synchronized (mRefreshLock) {
            mRefreshRequested = false;
            mRefreshInProgress = true;
            mHandler.removeCallbacks(this);
        }

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getContacts();

        Map<String, Collection<String>> blockedContacts = new TreeMap<>();
        for (String account : AccountManager.getInstance().getAccounts()) {
            blockedContacts.put(account, BlockingManager.getInstance().getBlockedContacts(account));
        }

        final Collection<RosterContact> rosterContacts = new ArrayList<>();
        for (RosterContact contact : allRosterContacts) {
            if (blockedContacts.containsKey(contact.getAccount())) {
                if (!blockedContacts.get(contact.getAccount()).contains(contact.getUser())) {
                    rosterContacts.add(contact);
                }
            }
        }

        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();
        final String selectedAccount = AccountManager.getInstance().getSelectedAccount();

        /**
         * Groups.
         */
        final Map<String, GroupConfiguration> groups;

        /**
         * List of active chats.
         */
        final GroupConfiguration activeChats;

        /**
         * Whether there is at least one visible contact.
         */
        boolean hasVisibleContacts = false;

        final Map<String, AccountConfiguration> accounts = new TreeMap<>();

        for (String account : AccountManager.getInstance().getAccounts()) {
            accounts.put(account, null);
        }

        /**
         * List of rooms and active chats grouped by users inside accounts.
         */
        final Map<String, Map<String, AbstractChat>> abstractChats = new TreeMap<>();

        for (AbstractChat abstractChat : MessageManager.getInstance().getChats()) {
            if ((abstractChat instanceof RoomChat || abstractChat.isActive())
                    && accounts.containsKey(abstractChat.getAccount())) {
                final String account = abstractChat.getAccount();
                Map<String, AbstractChat> users = abstractChats.get(account);
                if (users == null) {
                    users = new TreeMap<>();
                    abstractChats.put(account, users);
                }
                users.put(abstractChat.getUser(), abstractChat);
            }
        }

        if (mFilterString == null) {
            activeChats = new GroupConfiguration(GroupManager.NO_ACCOUNT, GroupManager.ACTIVE_CHATS, GroupManager.getInstance());

            // Create arrays.
            groups = new TreeMap<>();

            for (Map<String, AbstractChat> users : abstractChats.values()) {
                for (AbstractChat abstractChat : users.values()) {
                    final AbstractContact abstractContact;
                    if (abstractChat instanceof RoomChat) {
                        abstractContact = new RoomContact((RoomChat) abstractChat);
                    } else {
                        abstractContact = new ChatContact(abstractChat);
                    }
                    if (abstractChat.isActive()) {
                        activeChats.setNotEmpty();
                        hasVisibleContacts = true;
                        if (activeChats.isExpanded()) {
                            activeChats.addAbstractContact(abstractContact);
                        }
                        activeChats.increment(false);
                    }
                    if (selectedAccount != null && !selectedAccount.equals(abstractChat.getAccount())) {
                        continue;
                    }
                    final boolean online;
                    if (abstractChat instanceof RoomChat) {
                        online = abstractContact.getStatusMode().isOnline();
                    } else if (MUCManager.getInstance().isMucPrivateChat(abstractChat.getAccount(), abstractChat.getUser())) {
                        online = abstractContact.getStatusMode().isOnline();
                    } else {
                        online = false;
                    }
                    hasVisibleContacts = true;
                    addContact(abstractContact, online, groups, true);
                }
            }

            // Remove empty groups, sort and apply structure.
            mBaseEntities.clear();
            if (hasVisibleContacts) {
                for (GroupConfiguration rosterConfiguration : groups.values()) {
                    if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
//                        mBaseEntities.add(rosterConfiguration);
//                        rosterConfiguration.sortAbstractContacts(comparator);
                        mBaseEntities.addAll(rosterConfiguration.getAbstractContacts());
                    }
                }
            }
        } else { // Search
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator);
            mBaseEntities.clear();
            mBaseEntities.addAll(baseEntities);
        }

        super.onChange();

        synchronized (mRefreshLock) {
            mNextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
            mRefreshInProgress = false;
            mHandler.removeCallbacks(this); // Just to be sure.
            if (mRefreshRequested) {
                mHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    }

    private ArrayList<AbstractContact> getSearchResults(Collection<RosterContact> rosterContacts,
                                                        Comparator<AbstractContact> comparator) {
        final ArrayList<AbstractContact> baseEntities = new ArrayList<>();

        // Build structure.
        for (RosterContact rosterContact : rosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            if (rosterContact.getName().toLowerCase(mLocale).contains(mFilterString)) {
                baseEntities.add(rosterContact);
            }
        }
        Collections.sort(baseEntities, comparator);
        return baseEntities;
    }

    @Override
    public void run() {
        onChange();
    }

    /**
     * Listener for contact list appearance changes.
     *
     * @author alexander.ivanov
     */
    public interface OnContactListChangedListener {

        void onContactListChanged(CommonState commonState, boolean hasContacts,
                                  boolean hasVisibleContacts, boolean isFilterEnabled);

    }

    @Override
    public Filter getFilter() {
        if (mContactFilter == null) {
            mContactFilter = new ContactFilter();
        }
        return mContactFilter;
    }

    private class ContactFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            if (constraint == null || constraint.length() == 0) {
                mFilterString = null;
            } else {
                mFilterString = constraint.toString().toLowerCase(mLocale);
            }
            onChange();
        }

    }
}