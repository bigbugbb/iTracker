package com.localytics.android.itracker.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.localytics.android.itracker.data.message.AbstractChat;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends BaseAdapter {

    private List<AbstractChat> mChats;

    private final RegularContactItemInflater mContactItemInflater;

    public ChatListAdapter(Context context) {
        mChats = new ArrayList<>();
        mContactItemInflater = new RegularContactItemInflater(context);
    }

    public void updateChats(List<AbstractChat> chats) {
        mChats.clear();
        mChats.addAll(chats);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mChats.size();
    }

    @Override
    public Object getItem(int position) {
        return mChats.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AbstractChat abstractChat = (AbstractChat) getItem(position);
        final AbstractContact abstractContact = RosterManager.getInstance()
                .getBestContact(abstractChat.getAccount(), abstractChat.getUser());
        return mContactItemInflater.setUpContactView(convertView, parent, abstractContact);
    }
}