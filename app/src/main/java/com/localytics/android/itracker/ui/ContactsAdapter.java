package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.os.Handler;
import android.widget.Filter;
import android.widget.Filterable;

import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.account.CommonState;
import com.localytics.android.itracker.data.extension.blocking.BlockingManager;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.RosterContact;
import com.localytics.android.itracker.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class ContactsAdapter extends GroupedContactsAdapter implements Runnable, Filterable {

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

    private final OnContactListChangedListener mListener;

    public ContactsAdapter(Activity activity, OnContactListChangedListener listener) {
        super(activity);
        mListener = listener;
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

        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();
        final CommonState commonState = AccountManager.getInstance().getCommonState();
        final String selectedAccount = AccountManager.getInstance().getSelectedAccount();


        /**
         * Groups.
         */
        final Map<String, GroupConfiguration> groups;

        /**
         * Whether there is at least one contact.
         */
        boolean hasContacts = false;

        /**
         * Whether there is at least one visible contact.
         */
        boolean hasVisibleContacts = false;

        if (mFilterString == null) {
            // Create arrays.
            groups = new TreeMap<>();

            // Build structure.
            for (RosterContact rosterContact : rosterContacts) {
                if (!rosterContact.isEnabled()) {
                    continue;
                }
                hasContacts = true;
                final boolean online = rosterContact.getStatusMode().isOnline();
                final String account = rosterContact.getAccount();

                if (selectedAccount != null && !selectedAccount.equals(account)) {
                    continue;
                }
                if (addContact(rosterContact, online, groups, showOffline)) {
                    hasVisibleContacts = true;
                }
            }

            // Remove empty groups, sort and apply structure.
            mBaseEntities.clear();
            if (hasVisibleContacts) {
                for (GroupConfiguration rosterConfiguration : groups.values()) {
                    if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                        mBaseEntities.add(rosterConfiguration);
                        rosterConfiguration.sortAbstractContacts(comparator);
                        mBaseEntities.addAll(rosterConfiguration.getAbstractContacts());
                    }
                }
            }
        } else { // Search
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator);
            mBaseEntities.clear();
            mBaseEntities.addAll(baseEntities);
            hasVisibleContacts = baseEntities.size() > 0;
        }

        super.onChange();
        mListener.onContactListChanged(commonState, hasContacts, hasVisibleContacts, mFilterString != null);

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