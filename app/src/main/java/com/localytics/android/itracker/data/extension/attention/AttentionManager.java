/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.localytics.android.itracker.data.extension.attention;

import android.media.AudioManager;
import android.net.Uri;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.NetworkException;
import com.localytics.android.itracker.data.OnLoadListener;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.account.AccountItem;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.connection.ConnectionItem;
import com.localytics.android.itracker.data.connection.ConnectionManager;
import com.localytics.android.itracker.data.connection.ConnectionThread;
import com.localytics.android.itracker.data.connection.OnPacketListener;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.extension.capability.CapabilitiesManager;
import com.localytics.android.itracker.data.extension.capability.ClientInfo;
import com.localytics.android.itracker.data.message.AbstractChat;
import com.localytics.android.itracker.data.message.ChatAction;
import com.localytics.android.itracker.data.message.MessageManager;
import com.localytics.android.itracker.data.message.RegularChat;
import com.localytics.android.itracker.data.notification.EntityNotificationProvider;
import com.localytics.android.itracker.data.notification.NotificationManager;
import com.localytics.android.itracker.data.roster.RosterManager;
import com.localytics.android.xmpp.address.Jid;
import com.localytics.android.xmpp.attention.Attention;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * XEP-0224: Attention.
 *
 * @author alexander.ivanov
 */
public class AttentionManager implements OnPacketListener, OnLoadListener {

    private final static Object enabledLock;

    private final static AttentionManager instance;

    static {
        instance = new AttentionManager();
        Application.getInstance().addManager(instance);

        enabledLock = new Object();
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                synchronized (enabledLock) {
                    if (SettingsManager.chatsAttention())
                        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(Attention.NAMESPACE);
                }
            }
        });
    }

    private final EntityNotificationProvider<AttentionRequest> attentionRequestProvider = new EntityNotificationProvider<AttentionRequest>(
            R.drawable.ic_stat_error) {

        @Override
        public Uri getSound() {
            return SettingsManager.chatsAttentionSound();
        }

        @Override
        public int getStreamType() {
            return AudioManager.STREAM_RING;
        }

    };

    public AttentionManager() {
    }

    public static AttentionManager getInstance() {
        return instance;
    }

    public void onSettingsChanged() {
        synchronized (enabledLock) {
            for (String account : AccountManager.getInstance().getAccounts()) {
                ConnectionThread connectionThread = AccountManager
                        .getInstance().getAccount(account)
                        .getConnectionThread();
                if (connectionThread == null)
                    continue;
                XMPPConnection xmppConnection = connectionThread
                        .getXMPPConnection();
                if (xmppConnection == null)
                    continue;
                ServiceDiscoveryManager manager = ServiceDiscoveryManager
                        .getInstanceFor(xmppConnection);
                if (manager == null)
                    continue;
                boolean contains = false;
                for (String feature : manager.getFeatures()) {
                    if (Attention.NAMESPACE.equals(feature)) {
                        contains = true;
                    }
                }
                if (SettingsManager.chatsAttention() == contains)
                    continue;
                if (SettingsManager.chatsAttention())
                    manager.addFeature(Attention.NAMESPACE);
                else
                    manager.removeFeature(Attention.NAMESPACE);
            }
            AccountManager.getInstance().resendPresence();
        }
    }

    @Override
    public void onLoad() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded();
            }
        });
    }

    private void onLoaded() {
        NotificationManager.getInstance().registerNotificationProvider(
                attentionRequestProvider);
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem))
            return;
        if (!(packet instanceof Message))
            return;
        if (!SettingsManager.chatsAttention())
            return;
        final String account = ((AccountItem) connection).getAccount();
        if (bareAddress == null)
            return;
        for (ExtensionElement packetExtension : packet.getExtensions()) {
            if (packetExtension instanceof Attention) {
                MessageManager.getInstance().openChat(account, bareAddress);
                MessageManager.getInstance()
                        .getOrCreateChat(account, bareAddress)
                        .newAction(null, null, ChatAction.attention_requested);
                attentionRequestProvider.add(new AttentionRequest(account,
                        bareAddress), true);
            }
        }
    }

    public void sendAttention(String account, String user) throws NetworkException {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        if (!(chat instanceof RegularChat)) {
            throw new NetworkException(R.string.ENTRY_IS_NOT_FOUND);
        }
        String to = chat.getTo();
        if (Jid.getResource(to) == null || "".equals(Jid.getResource(to))) {
            final Presence presence = RosterManager.getInstance().getPresence(account, user);
            if (presence == null) {
                to = null;
            } else {
                to = presence.getFrom();
            }
        }

        if (to == null) {
            throw new NetworkException(R.string.ENTRY_IS_NOT_AVAILABLE);
        }

        ClientInfo clientInfo = CapabilitiesManager.getInstance().getClientInfo(account, to);
        if (clientInfo == null)
            throw new NetworkException(R.string.ENTRY_IS_NOT_AVAILABLE);
        if (!clientInfo.getFeatures().contains(Attention.NAMESPACE))
            throw new NetworkException(R.string.ATTENTION_IS_NOT_SUPPORTED);
        Message message = new Message();
        message.setTo(to);
        message.setType(Message.Type.headline);
        message.addExtension(new Attention());
        ConnectionManager.getInstance().sendStanza(account, message);
        chat.newAction(null, null, ChatAction.attention_called);
    }

    public void removeAccountNotifications(BaseEntity chat) {
        attentionRequestProvider.remove(chat.getAccount(), chat.getUser());
    }

}
