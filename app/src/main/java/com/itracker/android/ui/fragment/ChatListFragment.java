package com.itracker.android.ui.fragment;


import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.message.OnChatChangedListener;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.OnContactChangedListener;
import com.itracker.android.ui.activity.ChatViewerActivity;
import com.itracker.android.ui.adapter.ChatsAdapter;

import java.util.Collection;

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class ChatListFragment extends TrackerFragment implements
        OnContactChangedListener,
        OnChatChangedListener,
        AdapterView.OnItemClickListener {
    private static final String TAG = makeLogTag(ChatListFragment.class);

    private ListView mChatsView;
    private ChatsAdapter mChatsAdapter;

    public ChatListFragment() {
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
        mChatsView.setEmptyView(view.findViewById(R.id.empty_view));

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
        if (incoming) {
            mChatsAdapter.refreshRequest();
        }
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getAdapter().getItem(position);
        if (object instanceof AbstractContact) {
            AbstractContact contact = (AbstractContact) object;
            startActivity(ChatViewerActivity.createSpecificChatIntent(getActivity(),
                    contact.getAccount(), contact.getUser()));
        }
    }
}
