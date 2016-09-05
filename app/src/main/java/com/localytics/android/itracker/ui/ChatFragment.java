package com.localytics.android.itracker.ui;


import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.account.CommonState;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.message.OnChatChangedListener;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.OnContactChangedListener;

import java.util.Collection;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class ChatFragment extends TrackerFragment implements
        OnContactChangedListener,
        OnChatChangedListener,
        ContactsAdapter.OnContactListChangedListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener {
    private static final String TAG = makeLogTag(ChatFragment.class);

    private ListView mChatsView;
    private ChatsAdapter mChatsAdapter;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // to avoid strange bug on some 4.x androids
//        view.setBackgroundColor(ColorManager.getInstance().getContactListBackgroundColor());

        mChatsView = (ListView) view.findViewById(android.R.id.list);
        mChatsView.setOnItemClickListener(this);
        mChatsView.setItemsCanFocus(true);
        registerForContextMenu(mChatsView);
        mChatsAdapter = new ChatsAdapter(getActivity());
        mChatsView.setAdapter(mChatsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnChatChangedListener.class, this);
        mChatsAdapter.onChange();
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnChatChangedListener.class, this);
        mChatsAdapter.removeRefreshRequests();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        BaseEntity baseEntity = (BaseEntity) mChatsView.getItemAtPosition(info.position);
        if (baseEntity instanceof AbstractContact) {
//            ContextMenuHelper.createContactContextMenu(
//                    getActivity(), mContactsAdapter, (AbstractContact) baseEntity, menu);
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
        }
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts, boolean hasVisibleContacts, boolean isFilterEnabled) {

    }
}
