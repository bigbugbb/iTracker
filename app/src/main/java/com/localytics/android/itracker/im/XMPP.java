package com.localytics.android.itracker.im;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;
import com.localytics.android.itracker.BuildConfig;
import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.data.model.ChatMessage;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager.AutoReceiptMode;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.Jid;

import java.io.IOException;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


class XMPP {
    private static final String TAG = makeLogTag(XMPP.class);

    private String mServerUrl;
    private String mUsername;
    private String mPassword;

    private Gson mGson;

    private Handler mMainHandler;

    private Context mContext;
    private XMPPTCPConnection mConnection;

    private boolean mChatCreated = false;

    public XMPP(ChatService context, String serverUrl, String username, String password) {
        this.mContext   = context;
        this.mServerUrl = serverUrl;
        this.mUsername  = username;
        this.mPassword  = password;
        init();
    }

    public Chat mChat;

    AppChatMessageListener mChatMessageListener;
    ChatManagerListenerImpl mChatManagerListener;

    static {
        try {
            Class.forName("org.jivesoftware.smack.ReconnectionManager");
        } catch (ClassNotFoundException e) {
            // problem loading reconnection manager
            LOGE(TAG, "Can't find ReconnectionManager: " + e.getMessage());
        }
    }

    public void init() {
        mGson = new Gson();
        mMainHandler = new Handler(Looper.getMainLooper());
        mChatMessageListener = new AppChatMessageListener(mContext);
        mChatManagerListener = new ChatManagerListenerImpl();
        initializeConnection();
    }

    private void initializeConnection() {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
//                .setServiceName(mServerUrl)
                .setHost(mServerUrl)
                .setPort(Config.XMPP_CLIENT_PORT)
                .setDebuggerEnabled(true)
                .build();
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        mConnection = new XMPPTCPConnection(config);
        mConnection.addConnectionListener(new XMPPConnectionListener());
    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConnection.disconnect();
            }
        }).start();
    }

    public void connect(final String caller) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected synchronized Boolean doInBackground(Void... arg0) {
                if (mConnection.isConnected()) {
                    return false;
                }
                showToastMessage(caller + "=>connecting....");
                LOGD("Connect() Function", caller + "=>connecting....");

                try {
                    mConnection.connect();
                    DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mConnection);
                    dm.setAutoReceiptMode(AutoReceiptMode.always);
                    dm.addReceiptReceivedListener(new ReceiptReceivedListener() {

                        @Override
                        public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {

                        }
                    });
                } catch (IOException e) {
                    showToastMessage("(" + caller + ")" + "IOException: ");
                    LOGE(TAG, "IOException: " + e.getMessage());
                } catch (SmackException e) {
                    showToastMessage("(" + caller + ")" + "SMACKException: ");
                    LOGE(TAG, "SMACKException: " + e.getMessage());
                } catch (XMPPException e) {
                    showToastMessage("(" + caller + ")" + "XMPPException: ");
                    LOGE(TAG, "XMPPException: " + e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        }.execute();
    }

    public void login() {
        try {
            mConnection.login(mUsername, mPassword);
            LOGI("LOGIN", "Yey! We're connected to the Xmpp server!");
        } catch (XMPPException | SmackException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ChatManagerListenerImpl implements ChatManagerListener {
        @Override
        public void chatCreated(final Chat chat, final boolean createdLocally) {
            if (!createdLocally) {
                chat.addMessageListener(mChatMessageListener);
            }
        }
    }

    public void sendMessage(ChatMessage chatMessage) {
        String body = mGson.toJson(chatMessage);

        if (!mChatCreated) {
//            mChat = ChatManager.getInstanceFor(connection).createChat(
//                    chatMessage.receiver + "@"
//                            + mContext.getString(R.string.server),
//                    mChatMessageListener);
            mChatCreated = true;
        }
        final Message message = new Message();
        message.setBody(body);
        message.setStanzaId(chatMessage.msgid);
        message.setType(Message.Type.chat);

        try {
            if (mConnection.isAuthenticated()) {
                mChat.sendMessage(message);
            } else {
                login();
            }
        } catch (SmackException.NotConnectedException e) {
            LOGE(TAG, "msg Not sent!-Not Connected!");
        } catch (Exception e) {
            LOGE(TAG, "msg Not sent!" + e.getMessage());
        }

    }

    public class XMPPConnectionListener implements ConnectionListener {
        @Override
        public void connected(final XMPPConnection connection) {
            LOGD(TAG, "Connected!");
            if (!connection.isAuthenticated()) {
                login();
            }
        }

        @Override
        public void connectionClosed() {
            showToastMessage("ConnectionClosed!");
            LOGD(TAG, "ConnectionClosed!");
            mChatCreated = false;
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            showToastMessage("ConnectionClosedOn Error!!");
            LOGD(TAG, "ConnectionClosedOn Error!");
            mChatCreated = false;
        }

        @Override
        public void reconnectingIn(int seconds) {
            LOGD(TAG, "Reconnectingin " + seconds);
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
            showToastMessage("ReconnectionFailed!");
            LOGD(TAG, "ReconnectionFailed!");
            mChatCreated = false;
        }

        @Override
        public void reconnectionSuccessful() {
            showToastMessage("REConnected!");
            LOGD(TAG, "ReconnectionSuccessful");
            mChatCreated = false;
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            LOGD(TAG, "Authenticated!");
            ChatManager.getInstanceFor(mConnection).addChatListener(mChatManagerListener);
            mChatCreated = false;
            showToastMessage("Connected!");
        }
    }

    private class AppChatMessageListener implements ChatMessageListener {

        public AppChatMessageListener(Context context) {
        }

        @Override
        public void processMessage(final Chat chat, final Message message) {
            LOGI(TAG, "Xmpp message received: '" + message);

            if (message.getType() == Message.Type.chat && !TextUtils.isEmpty(message.getBody())) {
                final ChatMessage chatMessage = mGson.fromJson(message.getBody(), ChatMessage.class);

                processMessage(chatMessage);
            }
        }

        private void processMessage(final ChatMessage chatMessage) {
            chatMessage.isMine = false;
            // TODO: update ui
        }

    }

    private void showToastMessage(final String message) {
        if (BuildConfig.DEBUG) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
