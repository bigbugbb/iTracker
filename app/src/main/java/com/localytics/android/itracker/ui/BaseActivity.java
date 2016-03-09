package com.localytics.android.itracker.ui;

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.gcm.RegistrationIntentService;
import com.localytics.android.itracker.monitor.TrackerBroadcastReceiver;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.AccountUtils;
import com.localytics.android.itracker.util.LogUtils;
import com.localytics.android.itracker.util.PlayServicesUtils;
import com.localytics.android.itracker.util.PrefUtils;

import java.util.ArrayList;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGW;

public class BaseActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(BaseActivity.class);

    // Permission request codes
    private static final int REQUEST_BASIC_PERMISSIONS = 100;

    private GoogleApiClient mGoogleApiClient;

    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Primary toolbar and drawer toggle
    protected Toolbar mToolbar;

    protected Menu mOptionsMenu;

    // AsyncTask that performs GCM registration in the background
    private AsyncTask<Void, Void, Void> mGCMRegisterTask;

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;

    private boolean mManualSyncRequest;

    static {
        System.loadLibrary("media_player");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();

        PrefUtils.init(context);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        // Verifies the proper version of Google Play Services exists on the device.
        PlayServicesUtils.checkGooglePlaySevices(this);
        if (savedInstanceState == null) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (checkBasicPermissions()) {
            setupBackgroundMonitor();
        } else {
            requestBasicPermissions();
        }
    }

    private void requestBasicPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionList.add(Manifest.permission.READ_PHONE_STATE);
        if (Config.USE_EXTERNAL_DIRECTORY /*&& !SdkVersionUtils.hasKitKat()*/) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        final String[] permissions = permissionList.toArray(new String[permissionList.size()]);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_BASIC_PERMISSIONS);
    }

    private boolean checkSignIn() {
        // First validate the server client id format.
        String serverClientId = getString(R.string.server_client_id);
        String suffix = ".apps.googleusercontent.com";
        if (!serverClientId.trim().endsWith(suffix)) {
            throw new RuntimeException("Invalid server client ID in strings.xml, must end with " + suffix);
        }

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

        // Try to make a silent sign in.
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            LOGD(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            GoogleSignInAccount account = result.getSignInAccount();

            final String IdToken = account.getIdToken();
            final String personEmail = account.getEmail();
            final String personName = account.getDisplayName();
            final String personId = account.getId();
            final Uri personPhoto = account.getPhotoUrl();

            return true;
        } else {
            return false;
        }
    }

    private boolean checkBasicPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Config.USE_EXTERNAL_DIRECTORY /*&& !SdkVersionUtils.hasKitKat()*/) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void setupBackgroundMonitor() {
        if (!PrefUtils.isAlarmSetupDone(getApplicationContext())) {
            Intent intent = new Intent(TrackerBroadcastReceiver.ACTION_BOOTSTRAP_MONITOR_ALARM);
            sendBroadcast(intent);
            PrefUtils.markAlarmSetupDone(getApplicationContext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // It is possible that the permissions request interaction with the user is interrupted.
        // In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
        if (permissions.length == 0) {
            requestBasicPermissions();
            return;
        }

        if (requestCode == REQUEST_BASIC_PERMISSIONS) {
            boolean v1 = permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean v2 = permissions[1].equals(Manifest.permission.READ_PHONE_STATE) && grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (v1 && v2) {
                if (Config.USE_EXTERNAL_DIRECTORY /*&& !SdkVersionUtils.hasKitKat()*/) {
                    if (permissions[2].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        setupBackgroundMonitor();
                    } else {
                        // Permission was denied. Display an error message.
                    }
                } else {
                    setupBackgroundMonitor();
                }
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayUseLogoEnabled(true);

//        ab.setDisplayShowCustomEnabled(true);
//        ab.setDisplayShowTitleEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        PlayServicesUtils.checkGooglePlaySevices(this);

        // Watch for sync state changes
        mSyncStatusObserver.onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGCMRegisterTask != null) {
            LOGD(TAG, "Cancelling GCM registration task.");
            mGCMRegisterTask.cancel(true);
        }

        try {
            GCMRegistrar.onDestroy(this);
        } catch (Exception e) {
            LOGW(TAG, "C2DM unregistration error", e);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefUtils.PREF_SDK_TEST_MODE_ENABLED)) {
            if (mOptionsMenu != null) {

            }
        }
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    protected void onRefreshingStateChanged(boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String accountName = AccountUtils.getActiveAccountName(BaseActivity.this);
                    if (TextUtils.isEmpty(accountName)) {
                        onRefreshingStateChanged(false);
                        mManualSyncRequest = false;
                        return;
                    }

                    Account account = new Account(accountName, AccountUtils.ACCOUNT_TYPE);
                    boolean syncActive = ContentResolver.isSyncActive(account, TrackerContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(account, TrackerContract.CONTENT_AUTHORITY);
                    if (!syncActive && !syncPending) {
                        mManualSyncRequest = false;
                    }
                    onRefreshingStateChanged(syncActive || (mManualSyncRequest && syncPending));
                }
            });
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
        LOGD(TAG, "onConnectionFailed:" + connectionResult);
    }
}
