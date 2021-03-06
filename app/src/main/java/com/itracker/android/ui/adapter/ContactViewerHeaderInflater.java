package com.itracker.android.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itracker.android.R;
import com.itracker.android.data.SettingsManager;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.utils.AccountUtils;
import com.itracker.android.xmpp.address.Jid;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;


public class ContactViewerHeaderInflater {

    public static void updateHeader(View titleView, final Context context, AbstractContact abstractContact, VCard vCard) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.avatar);

        nameView.setText(vCard.getNickName());
        if (vCard.getAvatar() != null) {
            Glide.with(context).load(vCard.getAvatar()).asBitmap().dontAnimate().into(avatarView);
        } else {
            avatarView.setImageDrawable(abstractContact.getAvatar());
        }

        final TextView emailView = (TextView) titleView.findViewById(R.id.email);
        emailView.setText(vCard.getEmailHome() != null ? vCard.getEmailHome() : vCard.getEmailWork());
        Linkify.addLinks(emailView, Linkify.ALL);
    }

    public static void updateHeader(View titleView, final Context context, AbstractContact abstractContact) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final TextView emailView = (TextView) titleView.findViewById(R.id.email);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.avatar);

        nameView.setText(abstractContact.getName());

        // if it is account, not simple user contact
        if (Jid.getBareAddress(abstractContact.getUser()).equals(Jid.getBareAddress(abstractContact.getAccount()))) {
            nameView.setText(SettingsManager.contactsDefaultUsername());
            emailView.setText(AccountUtils.getActiveAccountName(context));
            Linkify.addLinks(emailView, Linkify.ALL);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user.getPhotoUrl() != null) {
                Glide.with(avatarView.getContext()).load(user.getPhotoUrl()).dontAnimate().into(avatarView);
            } else {
                Uri avatar = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.baozou_avatar);
                Glide.with(avatarView.getContext()).load(avatar).dontAnimate().into(avatarView);
            }
        } else {
            avatarView.setImageDrawable(abstractContact.getAvatar());
        }
    }
}
