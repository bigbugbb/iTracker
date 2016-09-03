package com.localytics.android.itracker.ui;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.account.AccountItem;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.account.StatusMode;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.Group;
import com.localytics.android.itracker.data.roster.GroupManager;
import com.localytics.android.itracker.data.roster.ShowOfflineMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public abstract class GroupedContactsAdapter extends BaseAdapter implements UpdatableAdapter {

    /**
     * List of groups used if contact has no groups.
     */
    static final Collection<Group> NO_GROUP_LIST;

    static final int TYPE_COUNT = 2;

    /**
     * View type used for contact items.
     */
    static final int TYPE_CONTACT = 0;

    /**
     * View type used for groups.
     */
    static final int TYPE_GROUP = 1;

    static {
        Collection<Group> groups = new ArrayList<>(1);
        groups.add(new Group() {
            @Override
            public String getName() {
                return GroupManager.NO_GROUP;
            }
        });
        NO_GROUP_LIST = Collections.unmodifiableCollection(groups);
    }

    final ArrayList<BaseEntity> mBaseEntities = new ArrayList<>();
    /**
     * Layout inflater
     */
    private final LayoutInflater mLayoutInflater;
    private final Activity mActivity;
    private final ContactItemInflater mContactItemInflater;
    protected Locale mLocale = Locale.getDefault();

    public GroupedContactsAdapter(Activity activity) {
        mActivity = activity;

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(activity, R.style.Theme);
        // clone the inflater using the ContextThemeWrapper
        mLayoutInflater = inflater.cloneInContext(contextThemeWrapper);

        mContactItemInflater = new ContactItemInflater(activity);
    }

    @Override
    public void onChange() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mBaseEntities.size();
    }

    @Override
    public Object getItem(int position) {
        return mBaseEntities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        Object object = getItem(position);
        if (object instanceof AbstractContact) {
            return TYPE_CONTACT;
        } else if (object instanceof GroupConfiguration) {
            return TYPE_GROUP;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case TYPE_CONTACT:
                return getContactView(position, convertView, parent);

            case TYPE_GROUP:
                return getGroupView(position, convertView, parent);

            default:
                throw new IllegalStateException();
        }
    }

    private View getGroupView(int position, View convertView, ViewGroup parent) {
        final View view;
        final GroupViewHolder viewHolder;
        if (convertView == null) {
            view = mLayoutInflater.inflate(R.layout.item_base_group, parent, false);
            viewHolder = new GroupViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (GroupViewHolder) view.getTag();
        }

        final GroupConfiguration configuration = (GroupConfiguration) getItem(position);
        final int level = AccountManager.getInstance().getColorLevel(configuration.getAccount());

        final String name = GroupManager.getInstance()
                .getGroupName(configuration.getAccount(), configuration.getUser());


        viewHolder.mIndicator.setImageLevel(configuration.isExpanded() ? 1 : 0);
        viewHolder.mGroupOfflineIndicator.setImageLevel(configuration.getShowOfflineMode().ordinal());

        int color;

        viewHolder.mGroupOfflineIndicator.setVisibility(View.GONE);
        viewHolder.mOfflineShadow.setVisibility(View.GONE);

        viewHolder.mName.setText(String.format("%s (%d/%d)", name, configuration.getOnline(), configuration.getTotal()));

        viewHolder.mGroupOfflineIndicator.setVisibility(View.VISIBLE);

        AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());

        if (accountItem != null) {
            StatusMode statusMode = accountItem.getDisplayStatusMode();
            if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
                viewHolder.mOfflineShadow.setVisibility(View.VISIBLE);
            }
        }

//        view.setBackgroundDrawable(new ColorDrawable(color));

        return view;
    }

    private View getContactView(int position, View convertView, ViewGroup parent) {
        final AbstractContact abstractContact = (AbstractContact) getItem(position);
        return mContactItemInflater.setUpContactView(convertView, parent, abstractContact);
    }

    /**
     * Gets or creates roster group in tree map.
     *
     * @param groups
     * @param name
     * @return
     */
    protected GroupConfiguration getGroupConfiguration(Map<String, GroupConfiguration> groups, String name) {
        GroupConfiguration groupConfiguration = groups.get(name);
        if (groupConfiguration != null) {
            return groupConfiguration;
        }
        groupConfiguration = new GroupConfiguration(GroupManager.NO_ACCOUNT, name, GroupManager.getInstance());
        groups.put(name, groupConfiguration);
        return groupConfiguration;
    }

    /**
     * Adds contact to there groups.
     *
     * @param abstractContact
     * @param groups
     * @param showOffline
     * @return whether contact is visible.
     */
    protected boolean addContact(AbstractContact abstractContact,
                                 boolean online,
                                 Map<String, GroupConfiguration> groups,
                                 boolean showOffline) {
        boolean hasVisible = false;

        Collection<? extends Group> abstractGroups = abstractContact.getGroups();
        if (abstractGroups.size() == 0) {
            abstractGroups = NO_GROUP_LIST;
        }
        for (Group abstractGroup : abstractGroups) {
            GroupConfiguration groupConfiguration
                    = getGroupConfiguration(groups, abstractGroup.getName());
            if (online || (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
                    || (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
                groupConfiguration.setNotEmpty();
                hasVisible = true;
                if (groupConfiguration.isExpanded()) {
                    groupConfiguration.addAbstractContact(abstractContact);
                }
            }
            groupConfiguration.increment(online);
        }

        return hasVisible;
    }

    /**
     * Sets whether group in specified account is expanded.
     */
    public void setExpanded(String account, String group, boolean expanded) {
        GroupManager.getInstance().setExpanded(account, group, expanded);
        onChange();
    }

    /**
     * Holder for views in contact list group.
     */
    private static class GroupViewHolder {
        final ImageView mIndicator;
        final TextView  mName;
        final ImageView mGroupOfflineIndicator;
        final ImageView mOfflineShadow;

        public GroupViewHolder(View view) {
            mIndicator = (ImageView) view.findViewById(R.id.indicator);
            mName = (TextView) view.findViewById(R.id.name);
            mGroupOfflineIndicator = (ImageView) view.findViewById(R.id.group_offline_indicator);
            mOfflineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        }
    }

}