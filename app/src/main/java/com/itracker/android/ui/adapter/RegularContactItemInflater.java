package com.itracker.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.itracker.android.R;
import com.itracker.android.data.SettingsManager;
import com.itracker.android.data.extension.muc.MUCManager;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.ui.activity.ContactEditorActivity;
import com.itracker.android.ui.activity.ContactViewerActivity;
import com.itracker.android.ui.color.ColorManager;

public class RegularContactItemInflater {

    final Context mContext;
    final LayoutInflater mLayoutInflater;

    public RegularContactItemInflater(Context context) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(context, R.style.Theme);
        // clone the inflater using the ContextThemeWrapper
        mLayoutInflater = inflater.cloneInContext(contextThemeWrapper);
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact contact) {
        final View view;
        final ContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = mLayoutInflater.inflate(R.layout.item_contact, parent, false);
            viewHolder = new ContactListItemViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ContactListItemViewHolder) view.getTag();
        }

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }
        viewHolder.avatar.setOnClickListener(v -> onAvatarClick(contact));
        viewHolder.name.setText(contact.getName());
        viewHolder.status.setText(String.format("[%s]", mContext.getString(contact.getStatusMode().getStringID())));

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_indicator_black_16dp);
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_private_chat_indicator_black_16dp);
        } else {
            viewHolder.mucIndicator.setVisibility(View.GONE);
        }

        viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getActiveChatSeparatorColor());

        return view;
    }

    protected void onAvatarClick(AbstractContact contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            intent = ContactViewerActivity.createIntent(mContext, contact.getAccount(), contact.getUser());
        } else {
            intent = ContactEditorActivity.createIntent(mContext, contact.getAccount(), contact.getUser());
        }
        mContext.startActivity(intent);
    }
}