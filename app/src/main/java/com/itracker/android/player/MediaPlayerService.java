package com.itracker.android.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;

public class MediaPlayerService extends Service {
    private static final String TAG = makeLogTag(MediaPlayerService.class);

    /*
     * Constants of message sent to player command handler.
     */
    static final int MSG_EXEC = 100;
    static final int MSG_CONNECT = 200;
    static final int MSG_DISCONNECT = 300;

    /*
     * Constants of intent action sent to the service.
     */
    public static final String INTENT_ACTION_PLAYER_OPEN   = "play_open";
    public static final String INTENT_ACTION_PLAYER_CLOSE  = "player_close";
    public static final String INTENT_ACTION_PLAYER_PLAY   = "player_play";
    public static final String INTENT_ACTION_PLAYER_PAUSE  = "player_pause";
    public static final String INTENT_ACTION_PLAYER_SEEK   = "player_seek";

    public static final String PLAYER_PARAMETER_OPEN_URL = "player_open_url";

    private HandlerThread mHandlerThread;
    private Handler mServiceHandler;

    private ITrackerMediaPlayer mMediaPlayer;

    /*
     * registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private NetworkInfoReceiver mNetworkInfoReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Can't bind to MediaPlayerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LOGD(TAG, "Starting MediaPlayerService");

        mHandlerThread = new HandlerThread(TAG + "MediaPlayerService Thread");
        mHandlerThread.start();
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());
        mNetworkInfoReceiver = new NetworkInfoReceiver(getApplicationContext(), mServiceHandler);
        registerReceiver(mNetworkInfoReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mMediaPlayer = MediaPlayerFactory.getMediaPlayer(false);
    }

    /**
     * A Broadcast receiver to receive network connection change events.
     */
    static class NetworkInfoReceiver extends BroadcastReceiver {
        boolean mNetworkConnected;
        private final Handler mHandler;

        /**
         * Constructs a NetworkInfoReceiver.
         *
         * @param handler a handle to send message to
         */
        public NetworkInfoReceiver(Context context, Handler handler) {
            mHandler = handler;
            mNetworkConnected = getNetworkStatus(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                mNetworkConnected = getNetworkStatus(context);
                LOGD(TAG, "Network connected: " + mNetworkConnected);
                mHandler.sendEmptyMessage(mNetworkConnected ? MSG_CONNECT : MSG_DISCONNECT);
            }
        }

        /**
         * Gets the status of network connectivity.
         *
         * @return true if network is connected, false otherwise.
         */
        boolean isNetworkConnected() {
            return mNetworkConnected;
        }

        private boolean getNetworkStatus(Context context) {
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        if (something wrong) {
//            LOGW(TAG, "Something is wrong.");
//            stopSelf(startId);
//            return START_NOT_STICKY;
//        }

        mServiceHandler.sendMessage(mServiceHandler.obtainMessage(MSG_EXEC, intent));
        /*
         * The service will not restart if it's killed by system.
         */
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mNetworkInfoReceiver);
        mHandlerThread.quitSafely();
        super.onDestroy();
    }

    class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_EXEC) {
                execCommand((Intent) msg.obj);
            } else if (msg.what == MSG_DISCONNECT) {
                // TODO: prompt the user about the network disconnection
            } else {
                Log.e(TAG, "Unknown command: " + msg.what);
            }
        }
    }

    /**
     * Executes command received by the service.
     *
     * @param intent received intent
     */
    void execCommand(Intent intent) {
        final String action = intent.getAction();

        try {
            if (action.equals(INTENT_ACTION_PLAYER_OPEN)) {
                final String url = intent.getStringExtra(PLAYER_PARAMETER_OPEN_URL);
                mMediaPlayer.setDataSource(url);
            } else if (action.equals(INTENT_ACTION_PLAYER_CLOSE)) {
                mMediaPlayer.close();
            } else if (action.equals(INTENT_ACTION_PLAYER_PLAY)) {
                mMediaPlayer.start();
            } else if (action.equals(INTENT_ACTION_PLAYER_PAUSE)) {
                mMediaPlayer.pause();
            } else if (action.equals(INTENT_ACTION_PLAYER_SEEK)) {
            }
        } catch (Exception e) {
            mMediaPlayer.close();
            mMediaPlayer.release();
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        // only available when the application is debuggable
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return;
        }

        // TODO: write something
        writer.flush();
    }
}
