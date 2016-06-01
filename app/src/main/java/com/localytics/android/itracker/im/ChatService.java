package com.localytics.android.itracker.im;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.util.AccountUtils;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        String username = formatUsername(AccountUtils.getActiveAccountName(this));
        String password = encodeUsername(username);
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

    private String formatUsername(String username) {
        return username.replace('@', '.');
    }

    private String encodeUsername(String username) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
            digest.update(username.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
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
