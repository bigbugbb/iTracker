package com.itracker.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountItem;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.extension.blocking.BlockingManager;
import com.itracker.android.data.intent.AccountIntentBuilder;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.adapter.ContactViewerHeaderInflater;
import com.itracker.android.ui.fragment.AccountEditorFragment;
import com.itracker.android.ui.fragment.AccountInfoEditorFragment;
import com.itracker.android.ui.fragment.ContactVCardViewerFragment;
import com.itracker.android.xmpp.address.Jid;
import com.itracker.android.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AccountViewerActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener,
        ContactVCardViewerFragment.Listener, AccountEditorFragment.AccountEditorFragmentInteractionListener {

    public static final int ACCOUNT_VIEWER_MENU = R.menu.menu_account_viewer;
    public static final int ACCOUNT_INFO_EDITOR_REQUEST_CODE = 1;
    public static final String INTENT_SHOW_ACCOUNT_INFO = "com.itracker.android.ui.activity.AccountViewerActivity.INTENT_SHOW_ACCOUNT_INFO";
    public static final String SAVE_SHOW_ACCOUNT_INFO = "com.itracker.android.ui.activity.AccountViewerActivity.SAVE_SHOW_ACCOUNT_INFO";

    private String account;
    private AccountItem accountItem;
    private View contactTitleView;
    private AbstractContact bestContact;
    private View statusIcon;
    private View preferencesFragmentContainer;
    private View vCardFragmentContainer;
    private Toolbar toolbar;
    private boolean showAccountInfo;
    private TextView statusText;

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    public static Intent createAccountInfoIntent(Context context, String account) {
        return createIntent(context, account, true);
    }

    public static Intent createAccountPreferencesIntent(Context context, String account) {
        return createIntent(context, account, false);
    }

    @NonNull
    private static Intent createIntent(Context context, String account, boolean showAccountInfo) {
        final Intent intent = new AccountIntentBuilder(context, AccountViewerActivity.class).setAccount(account).build();
        intent.putExtra(INTENT_SHOW_ACCOUNT_INFO, showAccountInfo);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        if (intent != null) {
            showAccountInfo = intent.getBooleanExtra(INTENT_SHOW_ACCOUNT_INFO, true);
        }

        if (!accountItem.getFactualStatusMode().isOnline()) {
            showAccountInfo = false;
        }

        setContentView(R.layout.activity_account_viewer);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.inflateMenu(ACCOUNT_VIEWER_MENU);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(AccountViewerActivity.this);
            }
        });

        bestContact = RosterManager.getInstance().getBestContact(account, Jid.getBareAddress(account));

        contactTitleView = findViewById(R.id.contact_title_expanded);
        contactTitleView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        statusIcon = findViewById(R.id.status_icon);
        statusText = (TextView) findViewById(R.id.status_text);

        preferencesFragmentContainer = findViewById(R.id.fragment_container);
        vCardFragmentContainer = findViewById(R.id.scrollable_container);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.scrollable_container, ContactVCardViewerFragment.newInstance(account, Jid.getBareAddress(account)))
                    .add(R.id.fragment_container, new AccountEditorFragment())
                    .commit();
        } else {
            showAccountInfo = savedInstanceState.getBoolean(SAVE_SHOW_ACCOUNT_INFO);
        }

        if (showAccountInfo) {
            showAccountInfo();
        } else {
            showAccountSettings();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTitle();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_SHOW_ACCOUNT_INFO, showAccountInfo);
    }

    private void updateTitle() {
        ContactViewerHeaderInflater.updateHeader(contactTitleView, this, bestContact);
//        statusIcon.setVisibility(View.GONE);
//        statusText.setText(Jid.getBareAddress(account));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(ACCOUNT_VIEWER_MENU, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_account_settings).setVisible(showAccountInfo);
        menu.findItem(R.id.action_edit_account_user_info).setVisible(showAccountInfo);
        menu.findItem(R.id.action_account_user_info).setVisible(!showAccountInfo);
        boolean showBlockListAction = BlockingManager.getInstance().isSupported(account) && showAccountInfo;
        menu.findItem(R.id.action_block_list).setVisible(showBlockListAction);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_account_user_info:
                editAccountInfo();
                return true;
            case R.id.action_account_settings:
                showAccountSettings();
                return true;

            case R.id.action_account_user_info:
                showAccountInfo();
                return true;

//            case R.id.action_block_list:
//                startActivity(BlockedListActivity.createIntent(this, account));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAccountInfo() {
        setUpFragment(true);
    }

    private void showAccountSettings() {
        setUpFragment(false);
    }

    private void setUpFragment(boolean vCardVisible) {
        showAccountInfo = vCardVisible;
        preferencesFragmentContainer.setVisibility(vCardVisible ? View.GONE : View.VISIBLE);
        vCardFragmentContainer.setVisibility(vCardVisible ? View.VISIBLE : View.GONE);
        onPrepareOptionsMenu(toolbar.getMenu());

        toolbar.setTitle(getString(vCardVisible ? R.string.account_user_info : R.string.account_settings));
    }

    private void editAccountInfo() {
        VCard vCard = ((ContactVCardViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container)).getVCard();
        if (vCard != null) {
            Intent intent = AccountInfoEditorActivity.createIntent(this, account, vCard.getChildElementXML().toString());
            startActivityForResult(intent, ACCOUNT_INFO_EDITOR_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ACCOUNT_INFO_EDITOR_REQUEST_CODE) {
            return;
        }

        final ContactVCardViewerFragment ContactVCardViewerFragment
                = (ContactVCardViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container);

        if (resultCode == AccountInfoEditorFragment.REQUEST_NEED_VCARD) {
            ContactVCardViewerFragment.requestVCard();
        }

        if (resultCode == RESULT_OK) {
            String vCardXml = data.getStringExtra(AccountInfoEditorFragment.ARGUMENT_VCARD);

            VCard vCard = null;
            if (vCardXml != null) {
                try {
                    vCard = ContactVCardViewerFragment.parseVCard(vCardXml);
                } catch (XmlPullParserException | IOException | SmackException e) {
                    e.printStackTrace();
                }
            }

            if (vCard != null) {
                vCard.getField(VCardProperty.NICKNAME.name());
                ContactVCardViewerFragment.onVCardReceived(account, Jid.getBareAddress(account), vCard);
            } else {
                ContactVCardViewerFragment.requestVCard();
            }
        }
    }

    @Override
    public void onVCardReceived() {
        updateTitle();
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public AccountItem getAccountItem() {
        return accountItem;
    }

    @Override
    public void onColorChange(String colorName) {

    }
}
