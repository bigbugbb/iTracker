package com.itracker.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.activity.ContactViewerActivity;

public class ContactDeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.itracker.android.ui.dialog.ContactDeleteDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.itracker.android.ui.dialog.ContactDeleteDialogFragment.ARGUMENT_USER";

    private String mUser;
    private String mAccount;

    public static ContactDeleteDialogFragment newInstance(String account, String user) {
        ContactDeleteDialogFragment fragment = new ContactDeleteDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mAccount = args.getString(ARGUMENT_ACCOUNT, null);
        mUser = args.getString(ARGUMENT_USER, null);

        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getActivity().getString(R.string.contact_delete_confirm),
                        RosterManager.getInstance().getName(mAccount, mUser),
                        AccountManager.getInstance().getVerboseName(mAccount)))
                .setPositiveButton(R.string.contact_delete, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            RosterManager.getInstance().removeContact(mAccount, mUser);

            if (getActivity() instanceof ContactViewerActivity) {
//                startAnimationActivity(ContactList.createIntent(getActivity()));
            }
        }
    }
}