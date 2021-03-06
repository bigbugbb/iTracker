package com.itracker.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.itracker.android.R;

class ChatContactListItemViewHolder extends RecyclerView.ViewHolder {

    final ImageView avatar;
    final TextView  name;
    final TextView  outgoingMessageIndicator;
    final TextView  secondLineMessage;
    final TextView  messageTime;
    final ImageView mucIndicator;
    final View separator;

    public ChatContactListItemViewHolder(View view) {
        super(view);

        avatar = (ImageView) view.findViewById(R.id.avatar);
        name = (TextView) view.findViewById(R.id.contact_list_item_name);
        outgoingMessageIndicator = (TextView) view.findViewById(R.id.outgoing_message_indicator);
        secondLineMessage = (TextView) view.findViewById(R.id.second_line_message);
        messageTime = (TextView) view.findViewById(R.id.contact_list_item_message_time);
        mucIndicator = (ImageView) view.findViewById(R.id.contact_list_item_muc_indicator);
        separator = view.findViewById(R.id.contact_list_item_separator);
    }
}