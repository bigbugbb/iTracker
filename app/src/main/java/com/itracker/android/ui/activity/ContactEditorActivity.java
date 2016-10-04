package com.itracker.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.itracker.android.R;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.data.roster.RosterContact;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.dialog.ContactDeleteDialogFragment;

public class ContactEditorActivity extends ContactViewerActivity {

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactEditorActivity.class)
                .setAccount(account).setUser(user).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        if (rosterContact != null) {
            mToolbar.inflateMenu(R.menu.menu_contact_viewer);
            mToolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_edit_alias:
//                editAlias();
//                return true;

//            case R.id.action_edit_groups:
//                startActivity(GroupEditor.createIntent(this, getAccount(), getBareAddress()));
//                return true;

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

        builder.setPositiveButton(android.R.string.ok, (dialog, which) ->
            RosterManager.getInstance().setName(getAccount(), getBareAddress(), input.getText().toString()));

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }
}