package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.utils.AccountUtils;
import com.localytics.android.itracker.utils.PrefUtils;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class SplashActivity extends ManagedActivity {
    private final static String TAG = makeLogTag(SplashActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing to prevent user closing the app from the back button
    }

    private static class SimpleAccountManagerCallback implements AccountManagerCallback<Bundle> {
        private Activity mActivity;

        public SimpleAccountManagerCallback(@NonNull Activity activity) {
            mActivity = activity;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            final Bundle result;
            try {
                result = future.getResult();
                String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (authToken != null) {
                    LOGD(TAG, authToken != null ? "SUCCESS!\nToken: " + authToken : "FAIL");
                    Intent intent = new Intent();
                    intent.setClass(mActivity, TrackerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mActivity.startActivity(intent);
                    mActivity.finish();
                }
                LOGD(TAG, "GetTokenForAccount Bundle is " + result);
            } catch (Exception e) {
                LOGE(TAG, "Can't get result from future", e);
            }
        }
    }
}
