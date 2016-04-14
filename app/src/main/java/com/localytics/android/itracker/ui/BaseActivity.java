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
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
        if (!requestPermissions()) {
            setupBackgroundMonitor();
        }
    }

    protected boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ArrayList<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

            // No explanation needed, we always have to request these permissions.
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]),
                    REQUEST_BASIC_PERMISSIONS);

            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // It is possible that the permissions request interaction with the user is interrupted.
        // In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
        if (permissions.length == 0) {
            requestPermissions();
            return;
        }

        if (requestCode == REQUEST_BASIC_PERMISSIONS) {
            boolean granted = true;

            for (int i = 0; i < permissions.length; ++i) {
                final int grantResult = grantResults[i];
                final String permission = permissions[i];

                switch (permission) {
                    case Manifest.permission.ACCESS_FINE_LOCATION:
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            granted = false;
                        }
                        break;
                }
            }

            if (!granted) {
                Toast.makeText(this, R.string.require_all_basic_permissions, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Yeah! We've got all the permissions, let's start background monitoring.
                setupBackgroundMonitor();
            }
        }
    }

    private void setupBackgroundMonitor() {
        if (!PrefUtils.isAlarmSetupDone(getApplicationContext())) {
            Intent intent = new Intent(TrackerBroadcastReceiver.ACTION_BOOTSTRAP_MONITOR_ALARM);
            sendBroadcast(intent);
            PrefUtils.markAlarmSetupDone(getApplicationContext());
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
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayUseLogoEnabled(true);
        }

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
