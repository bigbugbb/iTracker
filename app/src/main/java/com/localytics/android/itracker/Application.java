package com.localytics.android.itracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.multidex.MultiDex;

import com.localytics.android.itracker.data.BaseManagerInterface;
import com.localytics.android.itracker.data.BaseUIListener;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.data.NetworkException;
import com.localytics.android.itracker.data.OnCloseListener;
import com.localytics.android.itracker.data.OnInitializedListener;
import com.localytics.android.itracker.data.OnLoadListener;
import com.localytics.android.itracker.data.OnLowMemoryListener;
import com.localytics.android.itracker.data.OnTimerListener;
import com.localytics.android.itracker.data.OnUnloadListener;
import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.monitor.TrackerBroadcastReceiver;
import com.localytics.android.itracker.service.XabberService;
import com.localytics.android.itracker.utils.LogUtils;

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

public class Application extends android.app.Application {

    private final static String TAG = LogUtils.makeLogTag(Application.class);

    private static Application sInstance;

    private final ArrayList<Object> mRegisteredManagers;

    /**
     * Thread to execute tasks in background..
     */
    private final ExecutorService backgroundExecutor;

    /**
     * Handler to execute runnable in UI thread.
     */
    private final Handler handler;

    /**
     * Unmodifiable collections of managers that implement some common
     * interface.
     */
    private Map<Class<? extends BaseManagerInterface>, Collection<? extends BaseManagerInterface>> mManagerInterfaces;
    private Map<Class<? extends BaseUIListener>, Collection<? extends BaseUIListener>> mUiListeners;

    /**
     * Where data load was requested.
     */
    private boolean serviceStarted;

    /**
     * Whether application was initialized.
     */
    private boolean initialized;

    /**
     * Whether user was notified about some action in contact list activity
     * after application initialization.
     */
    private boolean notified;

    /**
     * Whether application is to be closed.
     */
    private boolean closing;

    /**
     * Whether {@link #onServiceDestroy()} has been called.
     */
    private boolean closed;

    private final Runnable mTimerRunnable = new Runnable() {

        @Override
        public void run() {
            for (OnTimerListener listener : getManagers(OnTimerListener.class))
                listener.onTimer();
            if (!closing)
                startTimer();
        }

    };

    /**
     * Future for loading process.
     */
    private Future<Void> loadFuture;

    public Application() {
        sInstance = this;
        serviceStarted = false;
        initialized = false;
        notified = false;
        closing = false;
        closed = false;
        mUiListeners = new HashMap<>();
        mManagerInterfaces = new HashMap<>();
        mRegisteredManagers = new ArrayList<>();

        handler = new Handler();
        backgroundExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "Background executor service");
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }
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

        // Bootstrap the monitor here in case the app is opened again after crashed or killed.
        bootstrapBackgroundMonitor();

        bootstrapFileDownloadService();

        if (BuildConfig.DEBUG) {
//            Config.enableStrictMode();
        }

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
        if (closed) {
            return;
        }
        onClose();
        runInBackground(new Runnable() {
            @Override
            public void run() {
                onUnload();
            }
        });
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
        return initialized;
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
        initialized = true;
        XabberService.getInstance().changeForeground();
        startTimer();
    }

    private void onClose() {
        LogManager.i(this, "onClose");
        for (Object manager : mRegisteredManagers) {
            if (manager instanceof OnCloseListener) {
                ((OnCloseListener) manager).onClose();
            }
        }
        closed = true;
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
     * @return <code>true</code> only once per application life. Subsequent
     * calls will always returns <code>false</code>.
     */
    public boolean doNotify() {
        if (notified) {
            return false;
        }
        notified = true;
        return true;
    }

    /**
     * Starts data loading in background if not started yet.
     *
     * @return
     */
    public void onServiceStarted() {
        if (serviceStarted) {
            return;
        }
        serviceStarted = true;
        LogManager.i(this, "onStart");
        loadFuture = backgroundExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    onLoad();
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Throw exceptions in UI thread if any.
                            try {
                                loadFuture.get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                            onInitialized();
                        }
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
        closing = true;
        stopService(XabberService.createIntent(this));
    }

    /**
     * @return Whether application is to be closed.
     */
    public boolean isClosing() {
        return closing;
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
        if (closed) {
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

    private void bootstrapBackgroundMonitor() {
        Intent intent = new Intent(TrackerBroadcastReceiver.ACTION_BOOTSTRAP_MONITOR_ALARM);
        sendBroadcast(intent);
    }

    private void bootstrapFileDownloadService() {
        FileDownloadManager fdm = FileDownloadManager.getInstance(this);
        fdm.recoverStatus();
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseUIListener> Collection<T> getOrCreateUIListeners(Class<T> cls) {
        Collection<T> collection = (Collection<T>) mUiListeners.get(cls);
        if (collection == null) {
            collection = new ArrayList<T>();
            mUiListeners.put(cls, collection);
        }
        return collection;
    }

    /**
     * @param cls Requested class of listeners.
     * @return List of registered UI listeners.
     */
    public <T extends BaseUIListener> Collection<T> getUIListeners(Class<T> cls) {
        if (closed) {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                for (OnErrorListener onErrorListener : getUIListeners(OnErrorListener.class)) {
//                    onErrorListener.onError(resourceId);
//                }
            }
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
        backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LogManager.exception(runnable, e);
                }
            }
        });
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    /**
     * Submits request to be executed in UI thread.
     */
    public void runOnUiThreadDelay(final Runnable runnable, long delayMillis) {
        handler.postDelayed(runnable, delayMillis);
    }
}
