package com.itracker.android.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;
import com.itracker.android.Config;
import com.itracker.android.data.model.User;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.itracker.android.utils.LogUtils.LOGD;

public class AccountUtils {
    private static final String TAG = LogUtils.makeLogTag(AccountUtils.class);

    /** account type id */
    public static final String ACCOUNT_TYPE = "com.itracker.android";

    /** user data */
    public static final String USERDATA_USER_ID = "userId";
    public static final String USERDATA_AUTHTOKEN_STATE = "auth_token_state";
    public static final String AUTHTOKEN_STATE_EXPIRED = "expired";

    /** current active account */
    private static final String PREF_ACTIVE_ACCOUNT = "_chosen_account";

    private static final String PREF_ACCOUNT_TYPE = "account_type";
    private static final String PREF_AUTHTOKEN_TYPE = "authtoken_type";

    /** these names are are prefixes; the account is appended to them */
    private static final String PREFIX_PREF_AUTH_TOKEN = "auth_token_";
    private static final String PREFIX_PREF_GCM_KEY = "gcm_key_";

    /** auth token types */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an show account";

    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an show account";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTHTOKEN_TYPE = "AUTH_TYPE";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    private static SharedPreferences getSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean hasActiveAccount(final Context context) {
        return !TextUtils.isEmpty(getActiveAccountName(context));
    }

    public static String getActiveAccountName(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_ACTIVE_ACCOUNT, null);
    }

    public static Account getActiveAccount(final Context context) {
        String account = getActiveAccountName(context);
        if (account != null) {
            return new Account(account, ACCOUNT_TYPE);
        } else {
            return null;
        }
    }

    public static boolean setActiveAccount(final Context context, final String accountName) {
        LOGD(TAG, "Set active account to: " + accountName);
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_ACTIVE_ACCOUNT, accountName).commit();
        return true;
    }

    public static String getAccountType(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_ACCOUNT_TYPE, "com.itracker.android");
    }

    public static boolean setAccountType(final Context context, final String accountType) {
        LOGD(TAG, "Set account type to: " + accountType);
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_ACCOUNT_TYPE, accountType).commit();
        return true;
    }

    public static String getAuthTokenType(final Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PREF_AUTHTOKEN_TYPE, "com.itracker.android");
    }

    public static boolean setAuthTokenType(final Context context, final String authTokenType) {
        LOGD(TAG, "Set auth token type to: " + authTokenType);
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(PREF_AUTHTOKEN_TYPE, authTokenType).commit();
        return true;
    }

    private static String makeAccountSpecificPrefKey(Context context, String prefix) {
        return hasActiveAccount(context) ? makeAccountSpecificPrefKey(getActiveAccountName(context), prefix) : null;
    }

    private static String makeAccountSpecificPrefKey(String accountName, String prefix) {
        return prefix + accountName;
    }

    public static String getAuthToken(final Context context) {
        Account account = getActiveAccount(context);
        String authToken = null;
        if (account != null) {
            authToken = AccountManager.get(context).peekAuthToken(account, AUTHTOKEN_TYPE_FULL_ACCESS);
        }
        return authToken;
    }

    public static String getAuthToken(final Context context, final String authTokenType) {
        Account account = getActiveAccount(context);
        String authToken = null;
        if (account != null) {
            authToken = AccountManager.get(context).peekAuthToken(account, authTokenType);
        }
        return authToken;
    }

    public static boolean hasToken(final Context context) {
        return hasToken(context, AUTHTOKEN_TYPE_FULL_ACCESS);
    }

    public static boolean hasToken(final Context context, final String authTokenType) {
        return !TextUtils.isEmpty(getAuthToken(context, authTokenType));
    }

    public static void refreshAuthToken(final Context context) {
        refreshAuthToken(context, AUTHTOKEN_TYPE_FULL_ACCESS);
    }

    // Refresh the expired auth token
    public static void refreshAuthToken(final Context context, final String authTokenType) {
        final AccountManager am = AccountManager.get(context);
        final Account account = getActiveAccount(context);
        if (account != null) {
            String authToken = AccountManager.get(context).peekAuthToken(account, AUTHTOKEN_TYPE_FULL_ACCESS);
            am.invalidateAuthToken(account.type, authToken); // Must be called or the authenticator won't be triggered.
            am.setUserData(account, USERDATA_AUTHTOKEN_STATE, AUTHTOKEN_STATE_EXPIRED);
            try {
                am.blockingGetAuthToken(account, authTokenType, true);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isAuthTokenExpired(final Context context) {
        AccountManager am = AccountManager.get(context);
        Account account = getActiveAccount(context);
        String state = am.getUserData(account, USERDATA_AUTHTOKEN_STATE);
        return !TextUtils.isEmpty(state) && state.equals(AUTHTOKEN_STATE_EXPIRED);
    }

    public static void clearAuthTokenExpiredState(final Context context) {
        AccountManager am = AccountManager.get(context);
        Account account = getActiveAccount(context);
        am.setUserData(account, USERDATA_AUTHTOKEN_STATE, null);
    }

    // When another device login with the same user account, we invalidate the auth token on the current device.
    // So the users have to re-enter their passwords again.
    public static void invalidateAuthToken(final Context context) {
        invalidateAuthToken(context, AUTHTOKEN_TYPE_FULL_ACCESS);
    }

    public static void invalidateAuthToken(final Context context, final String authTokenType) {
        AccountManager am = AccountManager.get(context);
        Account account = getActiveAccount(context);
        if (account != null) {
            final String authToken = am.peekAuthToken(account, authTokenType);
            am.invalidateAuthToken(account.type, authToken);
        }
    }

    public static void setGcmKey(final Context context, final String accountName, final String gcmKey) {
        SharedPreferences sp = getSharedPreferences(context);
        sp.edit().putString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_GCM_KEY), gcmKey).commit();
        LOGD(TAG, "GCM key of account " + accountName + " set to: " + sanitizeFcmKey(gcmKey));
    }

    public static String getGcmKey(final Context context, final String accountName) {
        SharedPreferences sp = getSharedPreferences(context);
        String gcmKey = sp.getString(makeAccountSpecificPrefKey(accountName, PREFIX_PREF_GCM_KEY), null);

        // if there is no current GCM key, generate a new random one
        if (TextUtils.isEmpty(gcmKey)) {
            gcmKey = UUID.randomUUID().toString();
            LOGD(TAG, "No GCM key on account " + accountName + ". Generating random one: " + sanitizeFcmKey(gcmKey));
            setGcmKey(context, accountName, gcmKey);
        }

        return gcmKey;
    }

    public static String sanitizeFcmKey(String key) {
        if (key == null) {
            return "(null)";
        } else if (key.length() > 8) {
            return key.substring(0, 4) + "........" + key.substring(key.length() - 4);
        } else {
            return "........";
        }
    }

    /**
     * Get an auth token for the account.
     * If not exist - add it and then return its auth token.
     * If one exist - return its auth token.
     * If more than one exists - show a picker and return the select account's auth token.
     * @param activity
     * @param accountType
     * @param authTokenType
     */
    public static void startAuthenticationFlow(final Activity activity,
                                               final String accountType,
                                               final String authTokenType,
                                               final AccountManagerCallback<Bundle> callback) {
        AccountManager am = AccountManager.get(activity);
        am.getAuthTokenByFeatures(accountType, authTokenType, null, activity, null, null, callback, null);
    }

    public static User signInUser(String email, String password) throws Exception {
        LOGD(TAG, "user sign in");

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("email", email);
        jsonRequest.put("password", password);

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Config.SESSIONS_URL, jsonRequest, future, future) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/vnd.itracker.v1");
                return params;
            }
        };
        RequestUtils.addToRequestQueue(jsObjRequest);

        JSONObject response = future.get(1500, TimeUnit.SECONDS);

        return new Gson().fromJson(response.toString(), User.class);
    }

    public static User signUpUser(String email, String username, String password, String passwordConfirmation) throws Exception {
        LOGD(TAG, "user sign up");

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("email", email);
        jsonRequest.put("username", username);
        jsonRequest.put("password", password);
        jsonRequest.put("password_confirmation", passwordConfirmation);

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Config.USERS_URL, jsonRequest, future, future) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/vnd.itracker.v1");
                return params;
            }
        };
        RequestUtils.addToRequestQueue(jsObjRequest);

        JSONObject response = future.get(1500, TimeUnit.SECONDS);

        return new Gson().fromJson(response.toString(), User.class);
    }
}