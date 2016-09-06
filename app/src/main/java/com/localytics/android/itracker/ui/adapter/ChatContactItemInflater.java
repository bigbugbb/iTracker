package com.localytics.android.itracker.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.extension.capability.ClientSoftware;
import com.localytics.android.itracker.data.extension.muc.MUCManager;
import com.localytics.android.itracker.data.message.AbstractChat;
import com.localytics.android.itracker.data.message.MessageManager;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.ui.color.ColorManager;
import com.localytics.android.itracker.utils.Emoticons;
import com.localytics.android.itracker.utils.StringUtils;

public class ChatContactItemInflater extends RegularContactItemInflater {
    public ChatContactItemInflater(Context context) {
        super(context);
    }

    public View setUpContactView(View convertView, ViewGroup parent, final AbstractContact contact) {
        final View view;
        final ChatContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = mLayoutInflater.inflate(R.layout.item_chat_contact, parent, false);
            viewHolder = new ChatContactListItemViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ChatContactListItemViewHolder) view.getTag();
        }

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(contact.getAvatarForContactList());
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAvatarClick(contact);
            }
        });

        viewHolder.name.setText(contact.getName());

        MessageManager messageManager = MessageManager.getInstance();
        if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMucPrivateChatText());
        } else if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {
            viewHolder.name.setTextColor(ColorManager.getInstance().getActiveChatTextColor());
        } else {
            viewHolder.name.setTextColor(ColorManager.getInstance().getColorMain());
        }

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_indicator_black_16dp);
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            viewHolder.mucIndicator.setVisibility(View.VISIBLE);
            viewHolder.mucIndicator.setImageResource(R.drawable.ic_muc_private_chat_indicator_black_16dp);
        } else {
            viewHolder.mucIndicator.setVisibility(View.GONE);
        }

        String statusText;

        viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);

        viewHolder.smallRightText.setVisibility(View.GONE);
        viewHolder.smallRightIcon.setVisibility(View.GONE);

        ClientSoftware clientSoftware = contact.getClientSoftware();

        if (messageManager.hasActiveChat(contact.getAccount(), contact.getUser())) {

            AbstractChat chat = messageManager.getChat(contact.getAccount(), contact.getUser());

            statusText = chat.getLastText().trim();

            view.setBackgroundColor(ColorManager.getInstance().getActiveChatBackgroundColor());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getActiveChatSeparatorColor());

            if (!statusText.isEmpty()) {
                viewHolder.smallRightText.setText(StringUtils.getSmartTimeText(mContext, chat.getLastTime()));
                viewHolder.smallRightText.setVisibility(View.VISIBLE);

                if (!chat.isLastMessageIncoming()) {
                    viewHolder.outgoingMessageIndicator.setText(mContext.getString(R.string.sender_is_you) + ": ");
                    viewHolder.outgoingMessageIndicator.setVisibility(View.VISIBLE);
                    viewHolder.outgoingMessageIndicator.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(contact.getAccount()));
                }
                viewHolder.smallRightIcon.setImageResource(R.drawable.ic_client_small);
                viewHolder.smallRightIcon.setVisibility(View.VISIBLE);
                viewHolder.smallRightIcon.setImageLevel(clientSoftware.ordinal());
            }
        } else {
            statusText = contact.getStatusText().trim();
            view.setBackgroundColor(ColorManager.getInstance().getContactBackground());
            viewHolder.separator.setBackgroundColor(ColorManager.getInstance().getContactSeparatorColor());
        }

        if (statusText.isEmpty()) {
            viewHolder.secondLineMessage.setVisibility(View.GONE);
        } else {
            viewHolder.secondLineMessage.setVisibility(View.VISIBLE);
            viewHolder.secondLineMessage.setText(Emoticons.getSmiledText(mContext, statusText, viewHolder.secondLineMessage));
        }

        viewHolder.statusIcon.setImageLevel(contact.getStatusMode().getStatusLevel());
        return view;
    }
}
