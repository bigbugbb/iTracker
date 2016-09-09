package com.localytics.android.itracker.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.account.OnAccountChangedListener;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.extension.muc.MUCManager;
import com.localytics.android.itracker.data.extension.vcard.VCardManager;
import com.localytics.android.itracker.data.intent.AccountIntentBuilder;
import com.localytics.android.itracker.data.intent.EntityIntentBuilder;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.GroupManager;
import com.localytics.android.itracker.data.roster.OnContactChangedListener;
import com.localytics.android.itracker.data.roster.RosterContact;
import com.localytics.android.itracker.data.roster.RosterManager;
import com.localytics.android.itracker.ui.adapter.ContactTitleInflater;
import com.localytics.android.itracker.ui.color.ColorManager;
import com.localytics.android.itracker.ui.fragment.ConferenceInfoFragment;
import com.localytics.android.itracker.ui.fragment.ContactVcardViewerFragment;
import com.localytics.android.xmpp.address.Jid;

import java.util.Collection;
import java.util.List;

public class ContactViewerActivity extends BaseActivity implements
        OnContactChangedListener, OnAccountChangedListener, ContactVcardViewerFragment.Listener {

    private String account;
    private String bareAddress;
    private Toolbar toolbar;
    private View contactTitleView;
    private AbstractContact bestContact;
    private CollapsingToolbarLayout collapsingToolbar;

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewerActivity.class)
                .setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    protected Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // View information about contact from system contact list
            Uri data = getIntent().getData();
            if (data != null && "content".equals(data.getScheme())) {
                List<String> segments = data.getPathSegments();
                if (segments.size() == 2 && "data".equals(segments.get(0))) {
                    Long id;
                    try {
                        id = Long.valueOf(segments.get(1));
                    } catch (NumberFormatException e) {
                        id = null;
                    }
                    if (id != null)
                        // FIXME: Will be empty while application is loading
                        for (RosterContact rosterContact : RosterManager.getInstance().getContacts())
                            if (id.equals(rosterContact.getViewId())) {
                                account = rosterContact.getAccount();
                                bareAddress = rosterContact.getUser();
                                break;
                            }
                }
            }
        } else {
            account = getAccount(getIntent());
            bareAddress = getUser(getIntent());
        }

        if (bareAddress != null && bareAddress.equalsIgnoreCase(GroupManager.IS_ACCOUNT)) {
            bareAddress = Jid.getBareAddress(AccountManager.getInstance().getAccount(account).getRealJid());
        }

        if (account == null || bareAddress == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        setContentView(R.layout.activity_contact_viewer);

        if (savedInstanceState == null) {

            Fragment fragment;
            if (MUCManager.getInstance().hasRoom(account, bareAddress)) {
                fragment = ConferenceInfoFragment.newInstance(account, bareAddress);
            } else {
                fragment = ContactVcardViewerFragment.newInstance(account, bareAddress);
            }

            getFragmentManager().beginTransaction().add(R.id.scrollable_container, fragment).commit();


        }

        bestContact = RosterManager.getInstance().getBestContact(account, bareAddress);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ContactViewerActivity.this);
            }
        });


//        StatusBarPainter statusBarPainter = new StatusBarPainter(this);
//        statusBarPainter.updateWithAccountName(account);

        final int accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        contactTitleView = findViewById(R.id.contact_title_expanded);
        findViewById(R.id.status_icon).setVisibility(View.GONE);
        contactTitleView.setBackgroundColor(accountMainColor);
        TextView contactNameView = (TextView) findViewById(R.id.name);
        contactNameView.setVisibility(View.INVISIBLE);

        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(bestContact.getName());

        collapsingToolbar.setBackgroundColor(accountMainColor);
        collapsingToolbar.setContentScrimColor(accountMainColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
        updateName();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, bareAddress)) {
                updateName();
                break;
            }
        }
    }

    private void updateName() {
        if (MUCManager.getInstance().isMucPrivateChat(account, bareAddress)) {
            String vCardName = VCardManager.getInstance().getName(bareAddress);
            if (!"".equals(vCardName)) {
                collapsingToolbar.setTitle(vCardName);
            } else {
                collapsingToolbar.setTitle(Jid.getResource(bareAddress));
            }

        } else {
            collapsingToolbar.setTitle(RosterManager.getInstance().getBestContact(account, bareAddress).getName());
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account)) {
            updateName();
        }
    }

    protected String getAccount() {
        return account;
    }

    protected String getBareAddress() {
        return bareAddress;
    }

    @Override
    public void onVCardReceived() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
    }
}