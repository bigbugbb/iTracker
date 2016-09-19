package com.localytics.android.itracker.ui.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.NetworkException;
import com.localytics.android.itracker.data.account.AccountType;
import com.localytics.android.itracker.ui.fragment.SignInFragment;
import com.localytics.android.itracker.ui.fragment.SignUpFragment;
import com.localytics.android.itracker.utils.AccountUtils;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements SignInFragment.OnAccountSignInListener, SignUpFragment.OnAccountSignUpListener {

    private static final String TAG = makeLogTag(AuthenticatorActivity.class);

    private final static int SIGN_IN_FRAGMENT = 0;
    private final static int SIGN_UP_FRAGMENT = 1;

    public final static String USERNAME = "username";
    public final static String CREATE_ACCOUNT = "create_account";

    private ViewAnimator mAnimator;
    private AccountManager mAccountManager;

    private LoginButton mFacebookLoginButton;
    private CallbackManager mCallbackManager;

    private ProgressBar mProgressBar;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        mAccountManager = AccountManager.get(this);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_signin_fragment, new SignInFragment());
            transaction.replace(R.id.auth_signup_fragment, new SignUpFragment());
            transaction.commit();
        }

        mProgressBar = (ProgressBar) findViewById(R.id.authenticate_progress);

        mFacebookLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        mFacebookLoginButton.setReadPermissions("email");

        // Callback registration
        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * Called when activity start-up is complete (after {@link #onStart}
     * and {@link #onRestoreInstanceState} have been called).  Applications will
     * generally not implement this method; it is intended for system
     * classes to do final initialization after application code has run.
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     * @see #onCreate
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mAnimator = (ViewAnimator) findViewById(R.id.auth_views);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAccountWillCreateNew() {
        mAnimator.setDisplayedChild(mAnimator.getDisplayedChild() == SIGN_IN_FRAGMENT ? SIGN_UP_FRAGMENT : SIGN_IN_FRAGMENT);
    }

    @Override
    public void onAccountSignInSuccess(Intent intent) {
        onFinishLogin(intent);
    }

    @Override
    public void onAccountAreadyExists() {
        mAnimator.setDisplayedChild(mAnimator.getDisplayedChild() == SIGN_IN_FRAGMENT ? SIGN_UP_FRAGMENT : SIGN_IN_FRAGMENT);
    }

    @Override
    public void onAccountSignInError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAccountStartSignIn() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAccountSignUpSuccess(Intent intent) {
        onFinishLogin(intent);
    }

    @Override
    public void onAccountSignUpError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAccountStartSignUp() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void onFinishLogin(Intent intent) {
        LOGD(TAG, "finish login");
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        String accountPassword = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        String userName = intent.getStringExtra(USERNAME);
        boolean createAccount = intent.getBooleanExtra(CREATE_ACCOUNT, false);
        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        final Account account = new Account(accountName, accountType);

        // Update preference with auth token and account name
        AccountUtils.setActiveAccount(getApplicationContext(), accountName);

        addAccount(accountName, accountPassword, createAccount);

        if (getIntent().getBooleanExtra(AccountUtils.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            LOGD(TAG, "finish login > addAccountExplicitly");
            String authTokenType = getIntent().getStringExtra(AccountUtils.ARG_AUTHTOKEN_TYPE); // we currently ignore the auth token type

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, intent.getBundleExtra(AccountManager.KEY_USERDATA));
            mAccountManager.setAuthToken(account, authTokenType, authToken);
        } else {
            LOGD(TAG, "finish login > setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void addAccount(String accountName, String password, boolean createAccount) {
        AccountType accountType = com.localytics.android.itracker.data.account.AccountManager.getInstance().getAccountTypes().get(0);

        String account;
        try {
            account = com.localytics.android.itracker.data.account.AccountManager.getInstance().addAccount(
                    jidFromAccountName(accountName), password, accountType,
                    false,
                    true,
                    false,
                    createAccount);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            return;
        }
    }

    private String jidFromAccountName(String accountName) {
        return accountName.replaceAll("@", ".") + "@" + Config.XMPP_SERVER_HOST;
    }
}
