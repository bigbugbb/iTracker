package com.localytics.android.itracker.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.account.CommonState;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.message.OnChatChangedListener;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.OnContactChangedListener;

import java.util.Collection;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class ContactFragment extends TrackerFragment implements
        OnContactChangedListener,
        OnChatChangedListener,
        ContactsAdapter.OnContactListChangedListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener {
    private static final String TAG = makeLogTag(ContactFragment.class);

    private ListView mContactsView;
    private ContactsAdapter mContactsAdapter;

//    private ProgressBar mProgressView;

    public ContactFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // to avoid strange bug on some 4.x androids
//        view.setBackgroundColor(ColorManager.getInstance().getContactListBackgroundColor());

        mContactsView = (ListView) view.findViewById(android.R.id.list);
        mContactsView.setOnItemClickListener(this);
        mContactsView.setItemsCanFocus(true);
        registerForContextMenu(mContactsView);
        mContactsAdapter = new ContactsAdapter(getActivity(), this);
        mContactsView.setAdapter(mContactsAdapter);

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Theme);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        ViewGroup contactsViewHeader = (ViewGroup) localInflater.inflate(R.layout.header_contact_list, mContactsView, false);
        mContactsView.addHeaderView(contactsViewHeader, null, false);
        contactsViewHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ImageView imageAddContacts = (ImageView) contactsViewHeader.findViewById(R.id.image_add_contacts);
        imageAddContacts.setColorFilter(ContextCompat.getColor(getActivity(), R.color.orange_500), PorterDuff.Mode.SRC_OUT);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        mProgressView = (ProgressBar) view.findViewById(R.id.progress_view);
//        mProgressView.setVisibility(mContactsAdapter.getCount() > 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        mContactsAdapter.onChange();
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        mContactsAdapter.removeRefreshRequests();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        BaseEntity baseEntity = (BaseEntity) mContactsView.getItemAtPosition(info.position);
        if (baseEntity instanceof AbstractContact) {
//            ContextMenuHelper.createContactContextMenu(
//                    getActivity(), mContactsAdapter, (AbstractContact) baseEntity, menu);
        } else if (baseEntity instanceof GroupConfiguration) {
//            ContextMenuHelper.createGroupContextMenu(getActivity(), mContactsAdapter,
//                    baseEntity.getAccount(), baseEntity.getUser(), menu);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onChatChanged(String account, String user, boolean incoming) {

    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getAdapter().getItem(position);
        if (object instanceof AbstractContact) {
//            contactListFragmentListener.onContactClick((AbstractContact) object);
        } else if (object instanceof GroupConfiguration) {
            GroupConfiguration groupConfiguration = (GroupConfiguration) object;
            mContactsAdapter.setExpanded(groupConfiguration.getAccount(), groupConfiguration.getUser(),
                    !groupConfiguration.isExpanded());
        }
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts, boolean hasVisibleContacts, boolean isFilterEnabled) {

    }
}
