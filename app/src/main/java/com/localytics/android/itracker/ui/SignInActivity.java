package com.localytics.android.itracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.localytics.android.itracker.R;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(SignInActivity.class);

    public static final String REQUEST_ACTIVITY_INTENT = "_request_activity_intent";

    private static final int RC_GET_TOKEN = 9000;

    private Intent mTargetIntent;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mTargetIntent = getIntent().getParcelableExtra(REQUEST_ACTIVITY_INTENT);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show an account picker to let the user choose a Google account from the device.
                // If the GoogleSignInOptions only asks for IDToken and/or profile and/or email then no
                // consent screen will be shown here.
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_GET_TOKEN);
            }
        });

        // [START configure_signin]
        // Request only the user's ID token, which can be used to identify the
        // user securely to your backend. This will contain the user's basic
        // profile (name, profile picture URL, etc) so you should not need to
        // make an additional call to personalize your application.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();
        // [END configure_signin]

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

//    private void signOut() {
//        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
//                new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(Status status) {
//                        LOGD(TAG, "signOut:onResult:" + status);
//                        updateUI(false);
//                    }
//                });
//    }
//
//    private void revokeAccess() {
//        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
//                new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(Status status) {
//                        LOGD(TAG, "revokeAccess:onResult:" + status);
//                        updateUI(false);
//                    }
//                });
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_TOKEN) {
            // [START get_id_token]
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            LOGD(TAG, "onActivityResult:GET_TOKEN:success:" + result.getStatus().isSuccess());

            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                String idToken = account.getIdToken();

                // Show signed-in UI.
                LOGD(TAG, "idToken:" + idToken);
                updateUI(true);

                // TODO(user): send token to server and validate server-side

                // Start the activity that requested the sign-in
                if (mTargetIntent != null) {
                    Intent intent = new Intent();
                    intent.setComponent(mTargetIntent.getComponent());
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, TrackerActivity.class));
                }
                finish();
            } else {
                // Show signed-out UI.
                updateUI(false);
            }
            // [END get_id_token]
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
        LOGD(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            ((TextView) findViewById(R.id.status)).setText(R.string.signed_in);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.status)).setText(R.string.signed_out);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        }
    }
}