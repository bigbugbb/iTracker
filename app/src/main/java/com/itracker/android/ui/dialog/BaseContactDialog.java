package com.itracker.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.itracker.android.R;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.color.AccountPainter;
import com.itracker.android.ui.color.ColorManager;

public abstract class BaseContactDialog extends DialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {

    public static final String ARGUMENT_ACCOUNT = "com.itracker.android.ui.mDialog.BaseContactDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_CONTACT = "com.itracker.android.ui.mDialog.BaseContactDialog.ARGUMENT_CONTACT";

    private String mAccount;
    private String mContact;
    private AccountPainter mAccountPainter;
    private AlertDialog mDialog;

    protected abstract int getDialogTitleTextResource();
    protected abstract String getMessage();
    protected abstract int getNegativeButtonTextResource();
    protected abstract int getPositiveButtonTextResource();
    protected abstract Integer getNeutralButtonTextResourceOrNull();
    protected abstract void onPositiveButtonClick();
    protected abstract void onNegativeButtonClick();
    protected abstract void onNeutralButtonClick();

    protected static void setArguments(String account, String contact, DialogFragment fragment) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_CONTACT, contact);
        fragment.setArguments(arguments);
    }

    protected String getAccount() {
        return mAccount;
    }

    protected String getContact() {
        return mContact;
    }

    protected void setUpContactTitleView(View view) {
        final AbstractContact bestContact = RosterManager.getInstance().getBestContact(mAccount, mContact);

        ((ImageView)view.findViewById(R.id.avatar)).setImageDrawable(bestContact.getAvatar());
        ((TextView)view.findViewById(R.id.name)).setText(bestContact.getName());
        ((TextView)view.findViewById(R.id.status_text)).setText(mContact);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mAccount = args.getString(ARGUMENT_ACCOUNT, null);
        mContact = args.getString(ARGUMENT_CONTACT, null);

        mAccountPainter = ColorManager.getInstance().getAccountPainter();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setCustomTitle(setUpDialogTitle())
                .setView(setUpDialogView())
                .setPositiveButton(getPositiveButtonTextResource(), this)
                .setNegativeButton(getNegativeButtonTextResource(), this);

        final Integer neutralButtonTextResource = getNeutralButtonTextResourceOrNull();
        if (neutralButtonTextResource != null) {
            builder.setNeutralButton(neutralButtonTextResource, this);
        }
        mDialog = builder.create();
        mDialog.setOnShowListener(this);

        return mDialog;
    }

    @NonNull
    private View setUpDialogTitle() {
        View dialogTitleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_title, null);
        final TextView dialogTitle = (TextView) dialogTitleView.findViewById(R.id.dialog_title_text_view);
        dialogTitle.setTextColor(mAccountPainter.getAccountTextColor(mAccount));
        dialogTitle.setText(getDialogTitleTextResource());
        return dialogTitleView;
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.contact_title_dialog, null);
        setUpContactTitleView(view);
        ((TextView) view.findViewById(R.id.dialog_message)).setText(getMessage());
        return view;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        this.mDialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(mAccountPainter.getAccountTextColor(mAccount));
        this.mDialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(mAccountPainter.getGreyMain());
        this.mDialog.getButton(Dialog.BUTTON_NEUTRAL).setTextColor(mAccountPainter.getGreyMain());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                onPositiveButtonClick();
                break;

            case Dialog.BUTTON_NEUTRAL:
                onNeutralButtonClick();
                break;

            case Dialog.BUTTON_NEGATIVE:
                onNegativeButtonClick();
                break;
        }
    }
}