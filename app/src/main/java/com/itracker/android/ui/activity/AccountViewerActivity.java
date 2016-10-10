package com.itracker.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountItem;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.extension.blocking.BlockingManager;
import com.itracker.android.data.extension.vcard.OnVCardListener;
import com.itracker.android.data.intent.AccountIntentBuilder;
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

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class AccountViewerActivity extends ContactViewerActivity implements
        Toolbar.OnMenuItemClickListener {

    private static final String TAG = makeLogTag(AccountViewerActivity.class);

    public static final String SAVE_SHOW_ACCOUNT_INFO = "com.itracker.android.ui.activity.AccountViewerActivity.SAVE_SHOW_ACCOUNT_INFO";

    private AccountItem mAccountItem;

    public static Intent createAccountInfoIntent(Context context, String account) {
        return createIntent(context, account);
    }

    @NonNull
    private static Intent createIntent(Context context, String account) {
        final Intent intent = new AccountIntentBuilder(context, AccountViewerActivity.class).setAccount(account).build();
        intent.putExtra(INTENT_IS_FOR_ACCOUNT, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountItem = AccountManager.getInstance().getAccount(mAccount);
        if (mAccountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_account_viewer, menu);
        if (mExpandedMode) {
            updateMenuItemsColor(R.color.grey_400);
        }
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
                onEditAccountClicked();
                return true;

//            case R.id.action_block_list:
//                startActivity(BlockedListActivity.createIntent(this, account));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void updateMenuItemsColor(int colorId) {
        if (mOptionsMenu != null && mOptionsMenu.hasVisibleItems()) {
            MenuItem item = mOptionsMenu.findItem(R.id.action_edit_account_user_info);
            Drawable drawable = item.getIcon();
            if (drawable != null) {
                // If we don't mutate the drawable, then all drawable's with this id will have a color
                // filter applied to it.
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(this, colorId), PorterDuff.Mode.SRC_ATOP);
            }
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
                ContactVCardViewerFragment.onVCardReceived(mAccount, Jid.getBareAddress(mAccount), vCard);
            } else {
                ContactVCardViewerFragment.requestVCard();
            }
        }
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        super.onVCardReceived(account, bareAddress, vCard);
    }
}
