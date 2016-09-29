package com.itracker.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itracker.android.data.BaseManagerInterface;
import com.itracker.android.data.BaseUIListener;
import com.itracker.android.data.FileDownloadManager;
import com.itracker.android.data.LogManager;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.OnCloseListener;
import com.itracker.android.data.OnInitializedListener;
import com.itracker.android.data.OnLoadListener;
import com.itracker.android.data.OnLowMemoryListener;
import com.itracker.android.data.OnTimerListener;
import com.itracker.android.data.OnUnloadListener;
import com.itracker.android.data.connection.ConnectionManager;
import com.itracker.android.receiver.SensorMonitorReceiver;
import com.itracker.android.service.sensor.AppPersistentService;
import com.itracker.android.ui.activity.SplashActivity;
import com.itracker.android.utils.LogUtils;
import com.localytics.android.Localytics;

import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static com.itracker.android.utils.LogUtils.LOGD;

public class Application extends android.support.multidex.MultiDexApplication {

    private final static String TAG = LogUtils.makeLogTag(Application.class);

    private static Application sInstance;

    private final ArrayList<Object> mRegisteredManagers;

    /**
     * Thread to execute tasks in background..
     */
    private final ExecutorService mBackgroundExecutor;

    /**
     * Handler to execute runnable in UI thread.
     */
    private final Handler mHandler;

    /**
     * Firebase auth and auth state listener.
     */
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    /**
     * Unmodifiable collections of managers that implement some common
     * interface.
     */
    private Map<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>> mManagerInterfaces;
    private Map<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>> mUiListeners;

    /**
     * Where data load was requested.
     */
    private boolean mServiceStarted;

    /**
     * Whether application was initialized.
     */
    private boolean mInitialized;

    /**
     * Whether application is to be closed.
     */
    private boolean mClosing;

    /**
     * Whether {@link #onServiceDestroy()} has been called.
     */
    private boolean mClosed;

    private final Runnable mTimerRunnable = () -> {
        for (OnTimerListener listener : getManagers(OnTimerListener.class)) {
            listener.onTimer();
        }
        if (!mClosing) {
            startTimer();
        }
    };

    /**
     * Future for loading process.
     */
    private Future<Void> mLoadFuture;

    public Application() {
        sInstance = this;
        mServiceStarted = false;
        mInitialized = false;
        mClosing = false;
        mClosed = false;
        mUiListeners = new HashMap<>();
        mManagerInterfaces = new HashMap<>();
        mRegisteredManagers = new ArrayList<>();

        mHandler = new Handler();
        mBackgroundExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Background executor service");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });
    }

    public static Application getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException();
        }
        return sInstance;
    }

    /**
     * Returns whether system contact storage is supported.
     * <p/>
     * Note:
     * <p/>
     * Please remove *_CONTACTS, *_ACCOUNTS, *_SETTINGS permissions,
     * SyncAdapterService and AccountAuthenticatorService together from manifest
     * file.
     *
     * @return
     */
    public boolean isContactsSupported() {
        return checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                LOGD(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
            } else {
                // User is signed out
                LOGD(TAG, "onAuthStateChanged:signed_out");
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthListener);

        ArrayList<String> contactManager = new ArrayList<>();
        TypedArray contactManagerClasses = getResources().obtainTypedArray(R.array.contact_managers);
        for (int index = 0; index < contactManagerClasses.length(); index++) {
            contactManager.add(contactManagerClasses.getString(index));
        }
        contactManagerClasses.recycle();

        TypedArray managerClasses = getResources().obtainTypedArray(R.array.managers);
        for (int index = 0; index < managerClasses.length(); index++) {
            if (isContactsSupported() || !contactManager.contains(managerClasses.getString(index))) {
                try {
                    Class.forName(managerClasses.getString(index));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        managerClasses.recycle();

        TypedArray tableClasses = getResources().obtainTypedArray(R.array.tables);
        for (int index = 0; index < tableClasses.length(); index++) {
            try {
                Class.forName(tableClasses.getString(index));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        tableClasses.recycle();

//        Localytics.autoIntegrate(this);
//        Localytics.setTestModeEnabled(true);
    }

    @Override
    public void onLowMemory() {
        for (OnLowMemoryListener listener : getManagers(OnLowMemoryListener.class)) {
            listener.onLowMemory();
        }
        super.onLowMemory();
    }

    /**
     * Service have been destroyed.
     */
    public void onServiceDestroy() {
        if (mClosed) {
            return;
        }
        onClose();
        runInBackground(() -> onUnload());
    }

    @Override
    public void onTerminate() {
        requestToClose();
        super.onTerminate();
    }

    /**
     * Whether application is initialized.
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    private void onLoad() {
        ProviderManager.addLoader(new ProviderFileLoader(getResources().openRawResource(R.raw.smack)));

        for (OnLoadListener listener : getManagers(OnLoadListener.class)) {
            LogManager.i(listener, "onLoad");
            listener.onLoad();
        }
    }

    private void onInitialized() {
        for (OnInitializedListener listener : getManagers(OnInitializedListener.class)) {
            LogManager.i(listener, "onInitialized");
            listener.onInitialized();
        }
        mInitialized = true;
        AppPersistentService.getInstance().changeForeground();
        FileDownloadManager.getInstance().recoverStatus();
        sendBroadcast(SensorMonitorReceiver.createBootstrapIntent(this));
        startActivity(SplashActivity.createAuthenticateAccountIntent(this));
        startTimer();

        ConnectionManager.getInstance().updateConnections(true);
    }

    private void onClose() {
        LogManager.i(this, "onClose");
        for (Object manager : mRegisteredManagers) {
            if (manager instanceof OnCloseListener) {
                ((OnCloseListener) manager).onClose();
            }
        }
        mClosed = true;

        if (mAuthListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void onUnload() {
        LogManager.i(this, "onUnload");
        for (Object manager : mRegisteredManagers) {
            if (manager instanceof OnUnloadListener) {
                ((OnUnloadListener) manager).onUnload();
            }
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * Starts data loading in background if not started yet.
     *
     * @return
     */
    public void onServiceStarted() {
        if (mServiceStarted) {
            return;
        }
        mServiceStarted = true;
        LogManager.i(this, "onStart");
        mLoadFuture = mBackgroundExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    onLoad();
                } finally {
                    runOnUiThread(() -> {
                        // Throw exceptions in UI thread if any.
                        try {
                            mLoadFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        onInitialized();
                    });
                }
                return null;
            }
        });
    }

    /**
     * Requests to close application in some time in future.
     */
    public void requestToClose() {
        mClosing = true;
        stopService(AppPersistentService.createIntent(this));
    }

    /**
     * @return Whether application is to be closed.
     */
    public boolean isClosing() {
        return mClosing;
    }

    /**
     * Start periodically callbacks.
     */
    private void startTimer() {
        runOnUiThreadDelay(mTimerRunnable, OnTimerListener.DELAY);
    }

    /**
     * Register new manager.
     */
    public void addManager(Object manager) {
        mRegisteredManagers.add(manager);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * @param cls Requested class of managers.
     * @return List of registered manager.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseManagerInterface> Collection<T> getManagers(Class<T> cls) {
        if (mClosed) {
            return Collections.emptyList();
        }
        Collection<T> collection = (Collection<T>) mManagerInterfaces.get(cls);
        if (collection == null) {
            collection = new ArrayList<>();
            for (Object manager : mRegisteredManagers) {
                if (cls.isInstance(manager)) {
                    collection.add((T) manager);
                }
            }
            collection = Collections.unmodifiableCollection(collection);
            mManagerInterfaces.put(cls, collection);
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseUIListener> Collection<T> getOrCreateUIListeners(Class<T> cls) {
        Collection<T> collection = (Collection<T>) mUiListeners.get(cls);
        if (collection == null) {
            collection = new ArrayList<>();
            mUiListeners.put(cls, collection);
        }
        return collection;
    }

    /**
     * @param cls Requested class of listeners.
     * @return List of registered UI listeners.
     */
    public <T extends BaseUIListener> Collection<T> getUIListeners(Class<T> cls) {
        if (mClosed) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(getOrCreateUIListeners(cls));
    }

    /**
     * Register new listener.
     * <p/>
     * Should be called from {@link Activity#onResume()}.
     */
    public <T extends BaseUIListener> void addUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).add(listener);
    }

    /**
     * Unregister listener.
     * <p/>
     * Should be called from {@link Activity#onPause()}.
     */
    public <T extends BaseUIListener> void removeUIListener(Class<T> cls, T listener) {
        getOrCreateUIListeners(cls).remove(listener);
    }

    /**
     * Notify about error.
     */
    public void onError(final int resourceId) {
        runOnUiThread(() -> {
//            for (OnErrorListener onErrorListener : getUIListeners(OnErrorListener.class)) {
//                onErrorListener.onError(resourceId);
//            }
        });
    }

    /**
     * Notify about error.
     */
    public void onError(NetworkException networkException) {
        LogManager.exception(this, networkException);
        onError(networkException.getResourceId());
    }

    /**
     * Submits request to be executed in background.
     */
    public void runInBackground(final Runnable runnable) {
        mBackgroundExecutor.submit(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                LogManager.exception(runnable, e);
            }
        });
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThread(final Runnable runnable) {
        mHandler.post(runnable);
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThreadDelay(final Runnable runnable, long delayMillis) {
        mHandler.postDelayed(runnable, delayMillis);
    }
}
