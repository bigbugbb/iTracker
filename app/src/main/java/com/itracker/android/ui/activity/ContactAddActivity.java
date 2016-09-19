package com.itracker.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.itracker.android.R;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.ui.fragment.ContactAddFragment;

public class ContactAddActivity extends BaseActivity {

    public static Intent createIntent(Context context) {
        return createIntent(context, null);
    }

    public static Intent createIntent(Context context, String account) {
        return createIntent(context, account, null);
    }

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactAddActivity.class).setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_add);

        Intent intent = getIntent();

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.contact_add_fragment, ContactAddFragment.newInstance(getAccount(intent), getUser(intent)))
                    .commit();
        }

    }
}