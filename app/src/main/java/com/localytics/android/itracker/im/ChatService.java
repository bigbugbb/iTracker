package com.localytics.android.itracker.im;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.util.AccountUtils;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class ChatService extends Service {
    private final static String TAG = makeLogTag(ChatService.class);

    private XMPP mXMPP;

    @Override
    public IBinder onBind(final Intent intent) {
        return new LocalBinder<>(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String username = AccountUtils.getActiveAccountName(this);
        String password = toHex(username);
        mXMPP = new XMPP(this, Config.XMPP_SERVER_URL, username, password);
        mXMPP.connect(AccountUtils.getActiveAccountName(this));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mXMPP.disconnect();
    }

    private String toHex(String origin) {
        try {
            return new BigInteger(1, origin.getBytes("UTF-8")).toString(16);
        } catch (UnsupportedEncodingException e) {
            LOGE(TAG, "Fail to convert original string to hex form: " + e.getMessage());
        }
        return origin;
    }

    private class LocalBinder<T> extends Binder {

        private final WeakReference<T> mService;

        public LocalBinder(final T service) {
            mService = new WeakReference<>(service);
        }

        public T getService() {
            return mService.get();
        }
    }
}
