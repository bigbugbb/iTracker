package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.account.OnAccountChangedListener;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.extension.muc.MUCManager;
import com.itracker.android.data.extension.vcard.VCardManager;
import com.itracker.android.data.intent.AccountIntentBuilder;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.GroupManager;
import com.itracker.android.data.roster.OnContactChangedListener;
import com.itracker.android.data.roster.RosterContact;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.adapter.ContactTitleInflater;
import com.itracker.android.ui.color.ColorManager;
import com.itracker.android.ui.fragment.ConferenceInfoFragment;
import com.itracker.android.ui.fragment.ContactVcardViewerFragment;
import com.itracker.android.xmpp.address.Jid;

import java.util.Collection;
import java.util.List;

public class ContactViewerActivity extends BaseActivity implements
        OnContactChangedListener, OnAccountChangedListener, ContactVcardViewerFragment.Listener {

    private String mAccount;
    private String mBareAddress;
    private View mContactTitleView;
    private AbstractContact mBestContact;
    private CollapsingToolbarLayout mCollapsingToolbar;

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
                                mAccount = rosterContact.getAccount();
                                mBareAddress = rosterContact.getUser();
                                break;
                            }
                }
            }
        } else {
            mAccount = getAccount(getIntent());
            mBareAddress = getUser(getIntent());
        }

        if (mBareAddress != null && mBareAddress.equalsIgnoreCase(GroupManager.IS_ACCOUNT)) {
            mBareAddress = Jid.getBareAddress(AccountManager.getInstance().getAccount(mAccount).getRealJid());
        }

        if (mAccount == null || mBareAddress == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        setContentView(R.layout.activity_contact_viewer);

        if (savedInstanceState == null) {
            Fragment fragment;
            if (MUCManager.getInstance().hasRoom(mAccount, mBareAddress)) {
                fragment = ConferenceInfoFragment.newInstance(mAccount, mBareAddress);
            } else {
                fragment = ContactVcardViewerFragment.newInstance(mAccount, mBareAddress);
            }

            getFragmentManager().beginTransaction().add(R.id.scrollable_container, fragment).commit();
        }

        mBestContact = RosterManager.getInstance().getBestContact(mAccount, mBareAddress);

        mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ContactViewerActivity.this));

//        StatusBarPainter statusBarPainter = new StatusBarPainter(this);
//        statusBarPainter.updateWithAccountName(account);

        final int accountMainColor = ContextCompat.getColor(this, R.color.colorPrimary);

        mContactTitleView = findViewById(R.id.contact_title_expanded);
        findViewById(R.id.status_icon).setVisibility(View.GONE);
        mContactTitleView.setBackgroundColor(accountMainColor);
        TextView contactNameView = (TextView) findViewById(R.id.name);
        contactNameView.setVisibility(View.INVISIBLE);

        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setTitle(mBestContact.getName());
        mCollapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.primary_text_default_material_dark));
        mCollapsingToolbar.setBackgroundColor(accountMainColor);
        mCollapsingToolbar.setContentScrimColor(accountMainColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        ContactTitleInflater.updateTitle(mContactTitleView, this, mBestContact);
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
            if (entity.equals(mAccount, mBareAddress)) {
                updateName();
                break;
            }
        }
    }

    private void updateName() {
        if (MUCManager.getInstance().isMucPrivateChat(mAccount, mBareAddress)) {
            String vCardName = VCardManager.getInstance().getName(mBareAddress);
            if (!"".equals(vCardName)) {
                mCollapsingToolbar.setTitle(vCardName);
            } else {
                mCollapsingToolbar.setTitle(Jid.getResource(mBareAddress));
            }

        } else {
            mCollapsingToolbar.setTitle(RosterManager.getInstance().getBestContact(mAccount, mBareAddress).getName());
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(mAccount)) {
            updateName();
        }
    }

    protected String getAccount() {
        return mAccount;
    }

    protected String getBareAddress() {
        return mBareAddress;
    }

    @Override
    public void onVCardReceived() {
        ContactTitleInflater.updateTitle(mContactTitleView, this, mBestContact);
    }
}