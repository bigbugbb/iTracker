package com.itracker.android.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.itracker.android.R;
import com.itracker.android.data.extension.cs.ChatStateManager;
import com.itracker.android.data.roster.AbstractContact;

import org.jivesoftware.smackx.chatstates.ChatState;

public class ContactTitleInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);

        nameView.setText(abstractContact.getName());

//        // if it is account, not simple user contact
//        if (Jid.getBareAddress(abstractContact.getUser()).equals(Jid.getBareAddress(abstractContact.getAccount()))) {
//            avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(abstractContact.getAccount()));
//        } else {
//            avatarView.setImageDrawable(abstractContact.getAvatar());
//        }
        setStatus(context, titleView, abstractContact);
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact) {
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.status_icon);

        int statusLevel = abstractContact.getStatusMode().getStatusLevel();
        if (isContactOffline(statusLevel)) {
            statusModeView.setVisibility(View.GONE);
        } else {
            statusModeView.setVisibility(View.VISIBLE);
            statusModeView.setImageLevel(statusLevel);
        }

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);


        ChatState chatState = ChatStateManager.getInstance().getChatState(
                abstractContact.getAccount(), abstractContact.getUser());

        CharSequence statusText;
        if (chatState == ChatState.composing) {
            statusText = context.getString(R.string.chat_state_composing);
        } else if (chatState == ChatState.paused) {
            statusText = context.getString(R.string.chat_state_paused);
        } else {
            statusText = abstractContact.getStatusText().trim();
            if (statusText.toString().isEmpty()) {
                statusText = context.getString(abstractContact.getStatusMode().getStringID());
            }
        }
        statusTextView.setText(statusText);
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

}
