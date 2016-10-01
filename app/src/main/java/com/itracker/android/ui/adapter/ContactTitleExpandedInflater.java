package com.itracker.android.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.itracker.android.R;
import com.itracker.android.data.extension.avatar.AvatarManager;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.xmpp.address.Jid;


public class ContactTitleExpandedInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final TextView emailView = (TextView) titleView.findViewById(R.id.email);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.avatar);

        nameView.setText(abstractContact.getName());

        // if it is account, not simple user contact
        if (Jid.getBareAddress(abstractContact.getUser()).equals(Jid.getBareAddress(abstractContact.getAccount()))) {
            avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(abstractContact.getAccount()));
        } else {
            avatarView.setImageDrawable(abstractContact.getAvatar());
        }
    }
}
