package com.itracker.android.sync;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.itracker.android.provider.TrackerContract;
import com.itracker.android.utils.AccountUtils;

/**
 * A simple {@link BroadcastReceiver} that triggers a sync. This is used by the GCM code to trigger
 * jittered syncs using {@link android.app.AlarmManager}.
 */
public class TriggerSyncReceiver extends BroadcastReceiver {
    public static final String EXTRA_USER_DATA_SYNC_ONLY = "com.itracker.android.EXTRA_USER_DATA_SYNC_ONLY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String accountName = AccountUtils.getActiveAccountName(context);
        if (TextUtils.isEmpty(accountName)) {
            return;
        }
        Account account = AccountUtils.getActiveAccount(context);
        if (account != null) {
            if (intent.getBooleanExtra(EXTRA_USER_DATA_SYNC_ONLY, false) ) {
                // this is a request to sync user data only, so do a manual sync right now
                // with the userDataOnly == true.
                SyncHelper.requestManualSync(account);
            } else {
                // this is a request to sync everything
                ContentResolver.requestSync(account, TrackerContract.CONTENT_AUTHORITY, new Bundle());
            }
        }
    }
}