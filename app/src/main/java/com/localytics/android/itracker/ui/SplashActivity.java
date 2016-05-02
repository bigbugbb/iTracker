package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.PrefUtils;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class SplashActivity extends AppCompatActivity {
    private final static String TAG = makeLogTag(SplashActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        PrefUtils.init(getApplicationContext());

        if (!AccountUtils.hasToken(this)) {
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    AccountUtils.startAuthenticationFlow(
                            SplashActivity.this, // This must be the activity context to start the authenticate activity.
                            AccountUtils.ACCOUNT_TYPE,
                            AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS,
                            new SimpleAccountManagerCallback(SplashActivity.this)
                    );
                }
            }, 2000);
        } else {
            Intent intent = new Intent(this, TrackerActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing to prevent user closing the app from the back button
    }

    private static class SimpleAccountManagerCallback implements AccountManagerCallback<Bundle> {
        private Context mContext;

        public SimpleAccountManagerCallback(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle result = future.getResult();
                String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (authToken != null) {
                    LOGD(TAG, authToken != null ? "SUCCESS!\nToken: " + authToken : "FAIL");
                    Intent intent = new Intent();
                    intent.setClass(mContext, TrackerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(intent);
                }
                LOGD(TAG, "GetTokenForAccount Bundle is " + result);
            } catch (Exception e) {
                LOGE(TAG, "Error: " + e.getMessage());
            }
        }
    }
}
