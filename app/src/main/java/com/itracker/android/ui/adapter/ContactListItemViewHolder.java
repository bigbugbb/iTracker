package com.itracker.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.itracker.android.R;

class ContactListItemViewHolder extends RecyclerView.ViewHolder {

    final ImageView avatar;
    final TextView  name;
    final TextView  status;
    final ImageView mucIndicator;
    final View separator;

    public ContactListItemViewHolder(View view) {
        super(view);
        avatar = (ImageView) view.findViewById(R.id.avatar);
        name = (TextView) view.findViewById(R.id.contact_list_item_name);
        status = (TextView) view.findViewById(R.id.contact_list_item_status);
        mucIndicator = (ImageView) view.findViewById(R.id.contact_list_item_muc_indicator);
        separator = view.findViewById(R.id.contact_list_item_separator);
    }
}