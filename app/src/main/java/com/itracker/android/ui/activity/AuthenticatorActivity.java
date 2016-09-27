package com.itracker.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.account.AccountType;
import com.itracker.android.ui.fragment.SignInFragment;
import com.itracker.android.ui.fragment.SignUpFragment;
import com.itracker.android.ui.listener.OnAuthenticateResult;
import com.itracker.android.utils.AccountUtils;
import com.itracker.android.utils.PasswordAuthentication;
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
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = makeLogTag(AuthenticatorActivity.class);

    private final static int SIGN_IN_FRAGMENT = 0;
    private final static int SIGN_UP_FRAGMENT = 1;

    private final static int GOOGLE_SIGN_IN = 100;

    public final static String USERNAME = "username";
    public final static String CREATE_ACCOUNT = "create_account";

    private final static String UID = "uid";
    private final static String FIREBASE_CUSTOM_TOKEN = "firebase_custom_token";

    private ViewAnimator mAnimator;
    private AccountManager mAccountManager;

    private Button mGoogleSignInButton;
    private GoogleApiClient mGoogleApiClient;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private OnAuthenticateResult[] mAuthenticateResult;

    private ProgressBar mProgressBar;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        mAccountManager = AccountManager.get(this);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    LOGD(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    LOGD(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_signin_fragment, new SignInFragment());
            transaction.replace(R.id.auth_signup_fragment, new SignUpFragment());
            transaction.commit();
        }

        mAuthenticateResult = new OnAuthenticateResult[2];
        mAuthenticateResult[0] = (OnAuthenticateResult) getFragmentManager().findFragmentById(R.id.auth_signin_fragment);
        mAuthenticateResult[1] = (OnAuthenticateResult) getFragmentManager().findFragmentById(R.id.auth_signup_fragment);

        mProgressBar = (ProgressBar) findViewById(R.id.authenticate_progress);

        // Setup google sign in button
        mGoogleSignInButton = (Button) findViewById(R.id.google_sign_in_button);
        mGoogleSignInButton.setOnClickListener(this);

        initializeGoogleSignIn();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private void initializeGoogleSignIn() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        if (mGoogleApiClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            // Build a GoogleApiClient with access to the Google Sign-In API and the
            // options specified by gso.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
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
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthListener);
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthListener);
        }
        mGoogleApiClient.disconnect();
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

    private void onFinishLogin(final Intent intent) {
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

        String secret = getString(R.string.ejabberd_account_secret);
        String xmppAccountPassword = (accountName + secret).hashCode() + "";
        addAccount(accountName, xmppAccountPassword, createAccount);

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

        // Sign in firebase with the custom token
        JSONObject params = createFirebaseTokenRequestParams(accountName);
        int method = com.android.volley.Request.Method.POST;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (method, Config.FIREBASE_TOKENS_URL, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String customToken = response.getString(FIREBASE_CUSTOM_TOKEN);
                    mFirebaseAuth.signInWithCustomToken(customToken)
                            .addOnCompleteListener(AuthenticatorActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    LOGD(TAG, "signInWithCustomToken:onComplete:" + task.isSuccessful());

                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.
                                    if (!task.isSuccessful()) {
                                        LOGW(TAG, "signInWithCustomToken", task.getException());
                                        Toast.makeText(AuthenticatorActivity.this, "Authentication failed.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        setAccountAuthenticatorResult(intent.getExtras());
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    }
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
            }
        });

        RequestUtils.addToRequestQueue(jsObjRequest);
    }

    public void addAccount(String accountName, String password, boolean createAccount) {
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

    private JSONObject createFirebaseTokenRequestParams(String uid) {
        JSONObject params = new JSONObject();
        try {
            params.put("uid", uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    private JSONObject createGoogleSessionRequestParams(String jwt) {
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
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
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

//                mAuthenticateResult[0].onAuthenticateSuccess();
            } else {
                // Google Sign In failed, update UI appropriately
//                mAuthenticateResult[0].onAuthenticateFailed();
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        LOGD(TAG, "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
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
        JSONObject params = createGoogleSessionRequestParams(compactJwt);
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
        boolean createAccount = response.getBoolean("new_account");

        // Update preference with auth token and account name
        AccountUtils.setActiveAccount(getApplicationContext(), accountName);

        String secret = getString(R.string.ejabberd_account_secret);
        String accountPassword = (accountName + secret).hashCode() + "";
        addAccount(accountName, accountPassword, createAccount);

        final String accountType = AccountUtils.getAccountType(Application.getInstance());
        final Account account = new Account(accountName, accountType);
        if (getIntent().getBooleanExtra(AccountUtils.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            final String authTokenType = AccountUtils.getAuthTokenType(Application.getInstance());
            mAccountManager.addAccountExplicitly(account, null, null);
            mAccountManager.setAuthToken(account, authTokenType, authToken);
        } else {
            mAccountManager.setPassword(account, null);
        }

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
