package com.localytics.android.itracker.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.intent.EntityIntentBuilder;
import com.localytics.android.itracker.data.roster.RosterContact;
import com.localytics.android.itracker.data.roster.RosterManager;
import com.localytics.android.itracker.ui.dialog.ContactDeleteDialogFragment;

public class ContactEditorActivity extends ContactViewerActivity implements Toolbar.OnMenuItemClickListener {

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactEditorActivity.class)
                .setAccount(account).setUser(user).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = getToolbar();

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        if (rosterContact != null) {
            toolbar.inflateMenu(R.menu.menu_contact_viewer);
            toolbar.setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        if (rosterContact != null) {
            getMenuInflater().inflate(R.menu.menu_contact_viewer, menu);
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
            case R.id.action_edit_alias:
                editAlias();
                return true;

            case R.id.action_edit_groups:
//                startActivity(GroupEditor.createIntent(this, getAccount(), getBareAddress()));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialogFragment.newInstance(getAccount(), getBareAddress())
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RosterManager.getInstance().setName(getAccount(), getBareAddress(), input.getText().toString());
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}