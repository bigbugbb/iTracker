package com.localytics.android.itracker.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import com.localytics.android.itracker.data.model.User;
import com.localytics.android.itracker.ui.AuthenticatorActivity;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.LogUtils;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static com.localytics.android.itracker.util.LogUtils.LOGE;

public class AuthenticateService extends Service {

    private static final Object sAuthenticatorLock = new Object();
    private static AccountAuthenticator sAuthenticator = null;

    @Override
    public void onCreate() {
        synchronized (sAuthenticatorLock) {
            if (sAuthenticator == null) {
                sAuthenticator = new AccountAuthenticator(getApplicationContext());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sAuthenticator.getIBinder();
    }

    public class AccountAuthenticator extends AbstractAccountAuthenticator {

        private final String TAG = LogUtils.makeLogTag(AccountAuthenticator.class);

        private final Context mContext;

        public AccountAuthenticator(Context context) {
            super(context);
            // Fuck Google - set mContext as protected!
            mContext = context;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            LogUtils.LOGD(TAG, "addAccount");

            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountUtils.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountUtils.ARG_AUTHTOKEN_TYPE, authTokenType);
            intent.putExtra(AccountUtils.ARG_IS_ADDING_NEW_ACCOUNT, true);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            LogUtils.LOGD(TAG, "getAuthToken");

            // If the caller requested an authToken type we don't support, then
            // return an error
            if (!authTokenType.equals(AccountUtils.AUTHTOKEN_TYPE_READ_ONLY) && !authTokenType.equals(AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
                return result;
            }

            // Extract the username and password from the Account Manager, and ask
            // the server for an appropriate AuthToken.
            final AccountManager am = AccountManager.get(mContext);

            String authToken = am.peekAuthToken(account, authTokenType);
            LogUtils.LOGD(TAG, "peekAuthToken returned - " + authToken);

            // Let's give another try to authenticate the user
            if (AccountUtils.isAuthTokenExpired(mContext)) {
                final String password = am.getPassword(account);
                if (!TextUtils.isEmpty(password)) {
                    try {
                        User user = AccountUtils.signInUser(account.name, password);
                        authToken = user != null ? user.auth_token : null;
                    } catch (Exception e) {
                        LOGE(TAG, "Exception: " + e.getMessage());
                        authToken = null;
                    }
                }
                AccountUtils.clearAuthTokenExpiredState(mContext);
            }

            // If we get an authToken - we return it
            if (!TextUtils.isEmpty(authToken)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                return result;
            }

            // If we get here, then we couldn't access the user's password - so we
            // need to re-prompt them for their credentials. We do that by creating
            // an intent to display our AuthenticatorActivity.
            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(AccountUtils.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(AccountUtils.ARG_AUTHTOKEN_TYPE, authTokenType);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }


        @Override
        public String getAuthTokenLabel(String authTokenType) {
            if (AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS.equals(authTokenType))
                return AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;
            else if (AccountUtils.AUTHTOKEN_TYPE_READ_ONLY.equals(authTokenType))
                return AccountUtils.AUTHTOKEN_TYPE_READ_ONLY_LABEL;
            else
                return authTokenType + " (Label)";
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
            final Bundle result = new Bundle();
            result.putBoolean(KEY_BOOLEAN_RESULT, false);
            return result;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }
    }

}
