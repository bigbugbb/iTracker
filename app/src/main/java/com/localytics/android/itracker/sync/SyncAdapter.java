package com.localytics.android.itracker.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.localytics.android.itracker.BuildConfig;
import com.localytics.android.itracker.utils.LogUtils;

import java.util.regex.Pattern;

import static com.localytics.android.itracker.utils.LogUtils.LOGI;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = LogUtils.makeLogTag(SyncAdapter.class);

    private static final Pattern sSanitizeAccountNamePattern = Pattern.compile("(.).*?(.?)@");

    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;

        // noinspection ConstantConditions, PointlessBooleanExpression
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    LogUtils.LOGE(TAG, "Uncaught sync exception, suppressing UI in release build.", throwable);
                }
            });
        }
    }

    @Override
    public void onPerformSync(final Account account, Bundle extras, String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {
        final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);

        final String logSanitizedAccountName = sSanitizeAccountNamePattern.matcher(account.name).replaceAll("$1...$2@");

        LOGI(TAG, "Beginning sync for account " + logSanitizedAccountName + "," +
                " uploadOnly=" + uploadOnly +
                " manualSync=" + manualSync +
                " initialize=" + initialize);

        // Sync from bootstrap and remote data, as needed
        new SyncHelper(mContext).performSync(syncResult, account, extras);
    }
}