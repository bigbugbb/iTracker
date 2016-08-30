package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.ActivityManager;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.service.sensor.AppPersistentService;
import com.localytics.android.itracker.ui.helper.SingleActivity;
import com.localytics.android.itracker.utils.AccountUtils;
import com.localytics.android.itracker.utils.PrefUtils;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public class SplashActivity extends SingleActivity {
    private final static String TAG = makeLogTag(SplashActivity.class);

    private static final String ACTION_AUTHENTICATE_ACCOUNT = "com.localytics.android.itracker.ui.SingleActivity.ACTION_AUTHENTICATE_ACCOUNT";

    private String mAction;

    public static Intent createIntent(Context context) {
        return new Intent(context, SplashActivity.class);
    }

    public static Intent createAuthenticateAccountIntent(Context context) {
        Intent intent = createIntent(context);
        intent.setAction(ACTION_AUTHENTICATE_ACCOUNT);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        PrefUtils.init(getApplicationContext());

        mAction = getIntent().getAction();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAction = getIntent().getAction();
        getIntent().setAction(null);

        if (ACTION_AUTHENTICATE_ACCOUNT.equals(mAction)) {
            startAuthenticate();
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing to prevent user closing the app from the back button
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Application.getInstance().isClosing()) {
            startService(AppPersistentService.createIntent(this));
            update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                cancel();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void update() {
        if (Application.getInstance().isInitialized()
                && !Application.getInstance().isClosing() && !isFinishing()) {
            LogManager.i(this, "Initialized");
            startAuthenticate();
        }
    }

    private void cancel() {
        finish();
        ActivityManager.getInstance().cancelTask(this);
    }

    private void startAuthenticate() {
        if (!AccountUtils.hasToken(this)) {
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AccountUtils.startAuthenticationFlow(
                            SplashActivity.this, // This must be the activity mContext to start the authenticate activity.
                            AccountUtils.ACCOUNT_TYPE,
                            AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS,
                            new SimpleAccountManagerCallback()
                    );
                }
            });
        } else {
            Intent intent = TrackerActivity.createIntent(this);
            startActivity(intent);
        }
    }

    private static class SimpleAccountManagerCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            final Bundle result;
            try {
                result = future.getResult();
                String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (authToken != null) {
                    LOGD(TAG, authToken != null ? "SUCCESS!\nToken: " + authToken : "FAIL");
                    Intent intent = TrackerActivity.createIntent(Application.getInstance());
                    Application.getInstance().startActivity(intent);
                }
                LOGD(TAG, "GetTokenForAccount Bundle is " + result);
            } catch (Exception e) {
                LOGE(TAG, "Can't get result from future", e);
            }
        }
    }
}
