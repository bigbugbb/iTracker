package com.localytics.android.itracker.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.localytics.android.itracker.R;

class ChatContactListItemViewHolder extends RecyclerView.ViewHolder {

    final ImageView avatar;
    final TextView  name;
    final TextView  outgoingMessageIndicator;
    final TextView  secondLineMessage;
    final TextView  smallRightText;
    final ImageView smallRightIcon;
    final ImageView statusIcon;
    final ImageView mucIndicator;
    final View separator;

    public ChatContactListItemViewHolder(View view) {
        super(view);

        avatar = (ImageView) view.findViewById(R.id.avatar);
        name = (TextView) view.findViewById(R.id.contact_list_item_name);
        outgoingMessageIndicator = (TextView) view.findViewById(R.id.outgoing_message_indicator);
        secondLineMessage = (TextView) view.findViewById(R.id.second_line_message);
        smallRightIcon = (ImageView) view.findViewById(R.id.small_right_icon);
        smallRightText = (TextView) view.findViewById(R.id.small_right_text);
        statusIcon = (ImageView) view.findViewById(R.id.contact_list_item_status_icon);
        mucIndicator = (ImageView) view.findViewById(R.id.contact_list_item_muc_indicator);
        separator = view.findViewById(R.id.contact_list_item_separator);
    }
}