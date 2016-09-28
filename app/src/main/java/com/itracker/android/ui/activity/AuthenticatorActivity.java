package com.itracker.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.account.AccountType;
import com.itracker.android.ui.fragment.SignInFragment;
import com.itracker.android.ui.fragment.SignUpFragment;
import com.itracker.android.ui.listener.OnAuthStateChangedListener;
import com.itracker.android.ui.listener.OnSignInSignUpSwitchedListener;
import com.itracker.android.utils.AccountUtils;
import com.itracker.android.utils.RequestUtils;

import org.json.JSONException;
import org.json.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.LOGI;
import static com.itracker.android.utils.LogUtils.LOGW;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class AuthenticatorActivity extends AccountAuthenticatorActivity implements
        SignInFragment.OnAccountSignInListener,
        SignUpFragment.OnAccountSignUpListener,
        OnAuthStateChangedListener,
        OnSignInSignUpSwitchedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = makeLogTag(AuthenticatorActivity.class);

    private final static int SIGN_IN_FRAGMENT = 0;
    private final static int SIGN_UP_FRAGMENT = 1;

    private final static int GOOGLE_SIGN_IN = 100;

    public final static String USERNAME = "username";
    public final static String CREATE_ACCOUNT = "create_account";

    private final static String FIREBASE_CUSTOM_TOKEN = "firebase_custom_token";

    private ViewAnimator mAnimator;
    private AccountManager mAccountManager;

    private Button mGoogleSignInButton;
    private GoogleApiClient mGoogleApiClient;

    private ProgressBar mProgressBar;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_signin_fragment, new SignInFragment());
            transaction.replace(R.id.auth_signup_fragment, new SignUpFragment());
            transaction.commit();
        }

        mProgressBar = (ProgressBar) findViewById(R.id.authenticate_progress);

        mAccountManager = AccountManager.get(this);

        // Setup google sign in button
        mGoogleSignInButton = (Button) findViewById(R.id.google_sign_in_button);
        mGoogleSignInButton.setOnClickListener(this);

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    public void onStart() {
        super.onStart();

        Application.getInstance().addUIListener(OnAuthStateChangedListener.class, this);

        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Application.getInstance().removeUIListener(OnAuthStateChangedListener.class, this);

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onAuthStateChanged(AuthState state, Bundle extra) {

    }

    @Override
    public void onAccountSignInSuccess(Intent intent) {
        onFinishLogin(intent);
    }

    @Override
    public void onSwitchToSignIn() {
        mAnimator.setDisplayedChild(SIGN_IN_FRAGMENT);
    }

    @Override
    public void onSwitchToSignUp() {
        mAnimator.setDisplayedChild(SIGN_UP_FRAGMENT);
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

    // User sign in/up by typing username and password
    private void onFinishLogin(final Intent intent) {
        LOGD(TAG, "finish login");
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        Bundle userdata = intent.getBundleExtra(AccountManager.KEY_USERDATA);
        boolean createAccount = intent.getBooleanExtra(CREATE_ACCOUNT, false);
        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

        updateChatAccount(accountName, null, createAccount);
        updateAppAccount(accountName, accountPassword, authToken, userdata);

        // Sign in firebase with the custom token from the app api
        JSONObject jsonRequest = createFirebaseTokenRequestParams(accountName);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Config.FIREBASE_TOKENS_URL, jsonRequest, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String customToken = response.getString(FIREBASE_CUSTOM_TOKEN);
                    FirebaseAuth.getInstance().signInWithCustomToken(customToken)
                            .addOnCompleteListener(AuthenticatorActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    LOGD(TAG, "signInWithCustomToken:onComplete:" + task.isSuccessful());
                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.
                                    if (!task.isSuccessful()) {
                                        LOGW(TAG, "signInWithCustomToken", task.getException());
                                        Toast.makeText(AuthenticatorActivity.this, "Firebase authentication failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    finishAuthenticatorActivity(intent);
                                }
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                LOGD(TAG, "Failed to get firebase custom token.");
                finishAuthenticatorActivity(intent);
            }
        });

        RequestUtils.addToRequestQueue(jsObjRequest);
    }

    private JSONObject createFirebaseTokenRequestParams(String uid) {
        JSONObject params = new JSONObject();
        try {
            params.put("uid", uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    private JSONObject createGoogleSignInRequestParams(String jwt) {
        JSONObject params = new JSONObject();
        try {
            params.put("jwt", jwt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    private String jidFromAccountName(String accountName) {
        return accountName.replaceAll("@", ".") + "@" + Config.XMPP_SERVER_HOST;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOGI(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGI(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in onConnectionFailed.
        LOGI(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onClick(View v) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        LOGD(TAG, "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        LOGD(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            LOGW(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(AuthenticatorActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            createSessionFromGoogleAccount(account);
                        }
                    }
                });
    }

    private void createSessionFromGoogleAccount(final GoogleSignInAccount account) {
        // Build the jwt with the google sign in account
        JSONObject payload = new JSONObject();
        try {
            payload.put("email", account.getEmail());
            payload.put("display_name", account.getDisplayName());
            payload.put("family_name", account.getFamilyName());
            payload.put("given_name", account.getGivenName());
            payload.put("photo_url", account.getPhotoUrl());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String secretKey = getString(R.string.google_session_jwt_secret);
        String base64EncodedSecretKey = new String(Base64.encode(secretKey.getBytes(), Base64.NO_PADDING));
        final String compactJwt = Jwts.builder()
                .setPayload(payload.toString())
                .signWith(SignatureAlgorithm.HS256, base64EncodedSecretKey)
                .compact();

        // Create a google sign in session with the generated jwt.
        // If the user identified by the email doesn't exist, the api will create the user.
        JSONObject params = createGoogleSignInRequestParams(compactJwt);
        int method = com.android.volley.Request.Method.POST;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (method, Config.GOOGLE_SESSIONS_URL, params, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            handleGoogleSignInSessionResponse(response);
                        } catch (JSONException e) {
                            LOGD(TAG, "Failed to parse json object: ", e);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LOGD(TAG, "Failed to create the google sign in session.");
                    }
                });

        RequestUtils.addToRequestQueue(jsObjRequest);
    }

    private void handleGoogleSignInSessionResponse(JSONObject response) throws JSONException {
        // The api will response with a new auth token and it should be stored locally until expired.
        // If the user is newly created, the app have to register an ejabberd account as well.
        String authToken = response.getString("auth_token");
        String accountName = response.getString("email");
        String accountType = AccountUtils.getAccountType(Application.getInstance());
        boolean createAccount = response.getBoolean("new_account");

        updateChatAccount(accountName, null, createAccount);
        updateAppAccount(accountName, null, authToken, null);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
        finishAuthenticatorActivity(intent);
    }

    private void updateChatAccount(String accountName, String accountPassword, boolean createAccount) {
        String secret = getString(R.string.ejabberd_account_secret);
        String password = TextUtils.isEmpty(accountPassword) ? (accountName + secret).hashCode() + "" : accountPassword;
        AccountType accountType = com.itracker.android.data.account.AccountManager.getInstance().getAccountTypes().get(0);

        final String account;
        try {
            account = com.itracker.android.data.account.AccountManager.getInstance().addAccount(
                    jidFromAccountName(accountName), password, accountType,
                    false,
                    true,
                    false,
                    createAccount);
            LOGD(TAG, "addAcount: " + account);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void updateAppAccount(String accountName, String accountPassword,
                                  String authToken, Bundle userdata) {
        final String accountType = AccountUtils.getAccountType(Application.getInstance());
        final Account account = new Account(accountName, accountType);

        // Update preference with auth token and account name
        AccountUtils.setActiveAccount(getApplicationContext(), accountName);

        if (getIntent().getBooleanExtra(AccountUtils.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            final String authTokenType = AccountUtils.getAuthTokenType(Application.getInstance());
            mAccountManager.addAccountExplicitly(account, accountPassword, userdata);
            mAccountManager.setAuthToken(account, authTokenType, authToken);
        } else {
            mAccountManager.setPassword(account, accountPassword);
        }
    }

    private void finishAuthenticatorActivity(Intent resultIntent) {
        setAccountAuthenticatorResult(resultIntent.getExtras());
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
