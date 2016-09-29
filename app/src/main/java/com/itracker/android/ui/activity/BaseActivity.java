package com.itracker.android.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.itracker.android.R;
import com.itracker.android.provider.TrackerContract;
import com.itracker.android.service.fcm.AppInstanceIdService;
import com.itracker.android.utils.AccountUtils;
import com.itracker.android.utils.LogUtils;
import com.itracker.android.utils.PlayServicesUtils;
import com.itracker.android.utils.PrefUtils;

import java.util.ArrayList;

import static com.itracker.android.utils.LogUtils.LOGD;

public class BaseActivity extends ManagedActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(BaseActivity.class);

    // Permission request codes
    private static final int REQUEST_BASIC_PERMISSIONS = 100;

    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Primary toolbar and drawer toggle
    protected Toolbar mToolbar;

    protected Menu mOptionsMenu;

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;

    private boolean mManualSyncRequest;

    private boolean mServiceBound;

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
            // do something here in some time
        }

        if (!PrefUtils.hasSentPushTokenToServer(context)) {
            AppInstanceIdService.updateFcmToken();
        }

        // The player activity needs the landscape orientation, otherwise if the orientation changes,
        // from portrait to landscape, there will be a terrible lagging before opening the video.
        // The solution is not be set the orientation to portrait, but only set it once to landscape.
        if (!isOrientationIgnored()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // On some phones it doesn't work if only set this through xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!requestPermissions()) {
            LOGD(TAG, "Got location permission.");
        }
    }

    protected boolean isOrientationIgnored() {
        return false;
    }

    protected boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && !(this instanceof SplashActivity)) {

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
        if (ab != null) {
//            ab.setHomeAsUpIndicator(R.drawable.ic_menu);
            ab.setDisplayHomeAsUpEnabled(true);
//            ab.setDisplayShowHomeEnabled(true);
//            ab.setDisplayUseLogoEnabled(true);
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
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            MyBinder myBinder = (MyBinder) service;
//            mBoundService = myBinder.getService();
            mServiceBound = true;
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefUtils.PREF_SDK_TEST_MODE_ENABLED)) {
            if (mOptionsMenu != null) {

            }
        }
    }

    protected void onRefreshingStateChanged(boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(() -> {
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
            });
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
        LOGD(TAG, "onConnectionFailed:" + connectionResult);
    }
}
