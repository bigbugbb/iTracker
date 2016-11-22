package com.itracker.android.ui.dialog;

import android.app.DialogFragment;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.PresenceManager;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.activity.ContactAddActivity;
import com.itracker.android.xmpp.address.Jid;

public class ContactSubscriptionDialog extends BaseContactDialog {

    public static DialogFragment newInstance(String account, String contact) {
        DialogFragment fragment = new ContactSubscriptionDialog();
        setArguments(account, contact, fragment);
        return fragment;
    }

    @Override
    protected int getDialogTitleTextResource() {
        return R.string.subscription_request_message;
    }

    @Override
    protected String getMessage() {
        final AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(), getContact());
        return getString(R.string.contact_subscribe_confirm, bestContact.getName());
    }

    @Override
    protected int getNegativeButtonTextResource() {
        return R.string.decline_contact;
    }

    @Override
    protected int getPositiveButtonTextResource() {
        return R.string.accept_contact;
    }

    @Override
    protected Integer getNeutralButtonTextResourceOrNull() {
        return null;
    }

    @Override
    protected void onPositiveButtonClick() {
        onAccept();
    }

    @Override
    protected void onNegativeButtonClick() {
        onDecline();
    }

    @Override
    protected void onNeutralButtonClick() {

    }

    public void onAccept() {
        try {
            PresenceManager.getInstance().acceptSubscription(getAccount(), getContact());
            PresenceManager.getInstance().requestSubscription(getAccount(), getContact());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    public void onDecline() {
        try {
            PresenceManager.getInstance().discardSubscription(getAccount(), getContact());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }
}