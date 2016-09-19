package com.itracker.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.ActivityManager;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.extension.archive.MessageArchiveManager;
import com.itracker.android.data.extension.attention.AttentionManager;
import com.itracker.android.data.extension.blocking.BlockingManager;
import com.itracker.android.data.extension.blocking.OnBlockedListChangedListener;
import com.itracker.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.data.message.MessageManager;
import com.itracker.android.data.message.OnChatChangedListener;
import com.itracker.android.data.notification.NotificationManager;
import com.itracker.android.data.roster.OnContactChangedListener;
import com.itracker.android.ui.fragment.ChatViewerFragment;

import java.util.Collection;

public class ChatViewerActivity extends BaseActivity implements
        OnChatChangedListener,
        OnContactChangedListener,
        ChatViewerFragment.ChatViewerFragmentListener,
        OnBlockedListChangedListener {

    /**
     * Attention request.
     */
    private static final String ACTION_ATTENTION = "com.itracker.android.data.ATTENTION";
    private static final String ACTION_SPECIFIC_CHAT = "com.itracker.android.data.ACTION_SPECIFIC_CHAT";

    private static final String SAVED_SELECTED_ACCOUNT = "com.itracker.android.ui.activity.ChatViewerActivity.SAVED_SELECTED_ACCOUNT";
    private static final String SAVED_SELECTED_USER = "com.itracker.android.ui.activity.ChatViewerActivity.SAVED_SELECTED_USER";

    private static final String SAVED_EXIT_ON_SEND = "com.itracker.android.ui.activity.ChatViewerActivity.EXIT_ON_SEND";

    ChatViewerFragment mRegisteredChat;
    private boolean mExitOnSend;
    private String mExtraText;
    private BaseEntity mSelectedChat;

    private boolean mIsVisible;

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private static String getAccount(Intent intent) {
        String value = EntityIntentBuilder.getAccount(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.account");
    }

    private static String getUser(Intent intent) {
        String value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.user");
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
    }

    public static Intent createSpecificChatIntent(Context context, String account, String user) {
        Intent intent = new EntityIntentBuilder(context, ChatViewerActivity.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
        return intent;
    }

    public static Intent createClearTopIntent(Context context, String account, String user) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /**
     * Create intent to send message.
     * <p/>
     * Contact list will not be shown on when chat will be closed.
     *
     * @param context
     * @param account
     * @param user
     * @param text    if <code>null</code> then user will be able to send a number
     *                of messages. Else only one message can be send.
     * @return
     */
    public static Intent createSendIntent(Context context, String account, String user, String text) {
        Intent intent = ChatViewerActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, String account, String user) {
        Intent intent = ChatViewerActivity.createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_chat_viewer);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getSelectedChatFromIntent();

        if (savedInstanceState != null) {
            mSelectedChat = new BaseEntity(savedInstanceState.getString(SAVED_SELECTED_ACCOUNT),
                    savedInstanceState.getString(SAVED_SELECTED_USER));
            mExitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);
        } else {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.chat_viewer_fragment, ChatViewerFragment.newInstance(mSelectedChat.getAccount(), mSelectedChat.getUser()))
                    .commit();
        }
    }

    private void getSelectedChatFromIntent() {
        Intent intent = getIntent();

        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_SPECIFIC_CHAT:
            case ACTION_ATTENTION:
            case Intent.ACTION_SEND:
                mSelectedChat = new BaseEntity(getAccount(intent), getUser(intent));
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isFinishing()) {
            return;
        }

        setIntent(intent);

        getSelectedChatFromIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsVisible = true;

        Application.getInstance().addUIListener(OnChatChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);

        updateWithSelectedChat();

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(mSelectedChat);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            mExtraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (mExtraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                mExitOnSend = true;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_SELECTED_ACCOUNT, mSelectedChat.getAccount());
        outState.putString(SAVED_SELECTED_USER, mSelectedChat.getUser());
        outState.putBoolean(SAVED_EXIT_ON_SEND, mExitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
        mIsVisible = false;
    }

    private void updateWithSelectedChat() {
        hideKeyboard(this);

        if (mIsVisible) {
            MessageManager.getInstance().setVisibleChat(mSelectedChat);
        }

        MessageArchiveManager.getInstance().requestHistory(mSelectedChat.getAccount(), mSelectedChat.getUser(), 0,
                MessageManager.getInstance().getChat(mSelectedChat.getAccount(), mSelectedChat.getUser()).getRequiredMessageCount());

        NotificationManager.getInstance().removeMessageNotification(mSelectedChat.getAccount(), mSelectedChat.getUser());
    }

    @Override
    public void onChatChanged(final String account, final String user, final boolean incoming) {
        if (mRegisteredChat.isEqual(mSelectedChat)) {
            mRegisteredChat.updateChat();
            if (incoming) {
                mRegisteredChat.playIncomingAnimation();
            }
        }
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity contact : entities) {
            if (mRegisteredChat.isEqual(contact)) {
                mRegisteredChat.updateChat();
            }
        }
    }

    private void insertExtraText() {
        if (mExtraText == null) {
            return;
        }

        boolean isExtraTextInserted = false;

        if (mRegisteredChat.isEqual(mSelectedChat)) {
            mRegisteredChat.setInputText(mExtraText);
            isExtraTextInserted = true;
        }

        if (isExtraTextInserted) {
            mExtraText = null;
        }
    }

    @Override
    public void onCloseChat() {
        close();
    }

    @Override
    public void onMessageSent() {
        if (mExitOnSend) {
            close();
        }
    }

    @Override
    public void registerChat(ChatViewerFragment chat) {
        mRegisteredChat = chat;
    }

    @Override
    public void unregisterChat(ChatViewerFragment chat) {
        mRegisteredChat = null;
    }

    private void close() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
        }
    }

    @Override
    public void onBlockedListChanged(String account) {
        // if chat of blocked contact is currently opened, it should be closed
        if (mSelectedChat != null) {
            final Collection<String> blockedContacts = BlockingManager.getInstance().getBlockedContacts(account);
            if (blockedContacts.contains(mSelectedChat.getUser())) {
                close();
            }

            final Collection<String> blockedMucContacts = PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account);
            if (blockedMucContacts.contains(mSelectedChat.getUser())) {
                close();
            }
        }
    }
}