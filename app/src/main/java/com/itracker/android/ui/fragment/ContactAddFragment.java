package com.itracker.android.ui.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.message.MessageManager;
import com.itracker.android.data.roster.PresenceManager;
import com.itracker.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.ArrayList;
import java.util.Collection;

public class ContactAddFragment extends Fragment {

    private static final String ARG_ACCOUNT = "com.itracker.android.ui.fragment.ContactAddFragment.ARG_ACCOUNT";
    private static final String ARG_USER = "com.itracker.android.ui.fragment.ContactAddFragment.ARG_USER";
    private static final String SAVED_NAME = "com.itracker.android.ui.fragment.ContactAddFragment.SAVED_NAME";
    private static final String SAVED_ACCOUNT = "com.itracker.android.ui.fragment.ContactAddFragment.SAVED_ACCOUNT";
    private static final String SAVED_USER = "com.itracker.android.ui.fragment.ContactAddFragment.SAVED_USER";

    private EditText mUserView;
    private EditText mNameView;

    private String mAccount;
    private String mUser;
    private String mName;

    public static ContactAddFragment newInstance(String account, String user) {
        ContactAddFragment fragment = new ContactAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_add, container, false);

        if (savedInstanceState != null) {
            mName = savedInstanceState.getString(SAVED_NAME);
            setAccount(savedInstanceState.getString(SAVED_ACCOUNT));
            setUser(savedInstanceState.getString(SAVED_USER));
        } else {
            if (getAccount() == null || getUser() == null) {
                mName = null;
            } else {
                mName = RosterManager.getInstance().getName(getAccount(), getUser());
                if (getUser().equals(mName)) {
                    mName = null;
                }
            }
        }

        if (getAccount() == null) {
            Collection<String> accounts = AccountManager.getInstance().getAccounts();
            if (accounts.size() == 1) {
                setAccount(accounts.iterator().next());
            }
        }

        mUserView = (EditText) view.findViewById(R.id.contact_user);
        mNameView = (EditText) view.findViewById(R.id.contact_name);

        if (getUser() != null) {
            mUserView.setText(getUser());
        }
        if (mName != null) {
            mNameView.setText(mName);
        }

        view.findViewById(R.id.btn_add_contact).setOnClickListener(v -> addContact());

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_ACCOUNT, getAccount());
        outState.putString(SAVED_USER, mUserView.getText().toString());
        outState.putString(SAVED_NAME, mNameView.getText().toString());

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private String jidFromUserAccount(String accountName) {
        return accountName.replaceAll("@", ".") + "@" + getString(R.string.xmpp_server_host);
    }

    private void addContact() {
        if (getAccount() == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_LONG).show();
            return;
        }

        String user = mUserView.getText().toString();
        if ("".equals(user)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_USER_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String name = mNameView.getText().toString();

        user = jidFromUserAccount(user);

        try {
            RosterManager.getInstance().createContact(mAccount, user, name, new ArrayList<>());
            PresenceManager.getInstance().requestSubscription(mAccount, user);
            MessageManager.getInstance().openChat(mAccount, user);
        } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException e) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
        } catch (XMPPException.XMPPErrorException e) {
            Application.getInstance().onError(R.string.XMPP_EXCEPTION);
        } catch (SmackException.NoResponseException e) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }

        getActivity().finish();
    }

    protected String getAccount() {
        return mAccount;
    }

    protected void setAccount(String account) {
        mAccount = account;
    }

    protected String getUser() {
        return mUser;
    }

    protected void setUser(String user) {
        mUser = user;
    }
}