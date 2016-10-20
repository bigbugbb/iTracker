package com.itracker.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.ui.fragment.AccountInfoEditorFragment;

public class AccountInfoEditorActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener, AccountInfoEditorFragment.Listener {

    public static final String ARG_VCARD = "com.itracker.android.ui.activity.AccountInfoEditorActivity.ARG_VCARD";
    public static final String ARGUMENT_SAVE_BUTTON_ENABLED = "com.itracker.android.ui.activity.AccountInfoEditorActivity.ARGUMENT_SAVE_BUTTON_ENABLED";

    public static Intent createIntent(Context context, String account, String vCard) {
        Intent intent = new EntityIntentBuilder(context, AccountInfoEditorActivity.class).setAccount(account).build();
        intent.putExtra(ARG_VCARD, vCard);
        return intent;
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info_editor);

        Intent intent = getIntent();
        String account = getAccount(intent);
        String vCard = intent.getStringExtra(ARG_VCARD);

        if (AccountManager.getInstance().getAccount(account) == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            setResult(RESULT_CANCELED);
            finish();
        }

        mToolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        mToolbar.setNavigationOnClickListener(v -> finish());
        mToolbar.setTitle(R.string.edit_account_user_info);
        mToolbar.inflateMenu(R.menu.menu_save);
        mToolbar.setOnMenuItemClickListener(this);

        boolean isSaveButtonEnabled = false;
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, AccountInfoEditorFragment.newInstance(account, vCard)).commit();
        } else {
            isSaveButtonEnabled = savedInstanceState.getBoolean(ARGUMENT_SAVE_BUTTON_ENABLED);
        }
        mToolbar.getMenu().findItem(R.id.action_save).setEnabled(isSaveButtonEnabled);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARGUMENT_SAVE_BUTTON_ENABLED, mToolbar.getMenu().findItem(R.id.action_save).isEnabled());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                ((AccountInfoEditorFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).saveVCard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getFragmentManager().findFragmentById(R.id.fragment_container).onActivityResult(requestCode,
                resultCode, data);
    }

    @Override
    public void onProgressModeStarted(String message) {
        mToolbar.setTitle(message);
        mToolbar.getMenu().findItem(R.id.action_save).setEnabled(false);
    }

    @Override
    public void onProgressModeFinished() {
        mToolbar.setTitle(R.string.edit_account_user_info);
    }

    @Override
    public void enableSave() {
        mToolbar.getMenu().findItem(R.id.action_save).setEnabled(true);
    }
}