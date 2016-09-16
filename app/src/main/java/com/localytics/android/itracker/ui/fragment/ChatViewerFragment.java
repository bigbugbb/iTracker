package com.localytics.android.itracker.ui.fragment;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.data.NetworkException;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.entity.BaseEntity;
import com.localytics.android.itracker.data.extension.archive.MessageArchiveManager;
import com.localytics.android.itracker.data.extension.attention.AttentionManager;
import com.localytics.android.itracker.data.extension.cs.ChatStateManager;
import com.localytics.android.itracker.data.extension.file.FileManager;
import com.localytics.android.itracker.data.extension.file.FileUtils;
import com.localytics.android.itracker.data.extension.httpfileupload.HttpFileUploadManager;
import com.localytics.android.itracker.data.extension.httpfileupload.HttpUploadListener;
import com.localytics.android.itracker.data.extension.muc.MUCManager;
import com.localytics.android.itracker.data.extension.muc.RoomChat;
import com.localytics.android.itracker.data.extension.muc.RoomState;
import com.localytics.android.itracker.data.extension.otr.OTRManager;
import com.localytics.android.itracker.data.message.AbstractChat;
import com.localytics.android.itracker.data.message.MessageItem;
import com.localytics.android.itracker.data.message.MessageManager;
import com.localytics.android.itracker.data.message.RegularChat;
import com.localytics.android.itracker.data.message.chat.ChatManager;
import com.localytics.android.itracker.data.notification.NotificationManager;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.RosterManager;
import com.localytics.android.itracker.ui.activity.ChatViewerActivity;
import com.localytics.android.itracker.ui.activity.ContactEditorActivity;
import com.localytics.android.itracker.ui.activity.ContactViewerActivity;
import com.localytics.android.itracker.ui.adapter.ChatMessageAdapter;
import com.localytics.android.itracker.ui.adapter.ContactTitleInflater;
import com.localytics.android.itracker.ui.color.ColorManager;
import com.localytics.android.itracker.ui.helper.PermissionsRequester;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;

public class ChatViewerFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener,
        ChatMessageAdapter.Message.MessageClickListener, HttpUploadListener, ChatMessageAdapter.Listener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final int MINIMUM_MESSAGES_TO_LOAD = 10;
    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 23;
    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 24;
    private static final int PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS = 25;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 26;
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 27;
    boolean mIsInputEmpty = true;
    private EditText mInputView;
    private ChatMessageAdapter mChatMessageAdapter;
    private boolean mSkipOnTextChanges = false;
    private String mAccount;
    private String mUser;
    private ImageButton mSendButton;
    private Toolbar mToolbar;

    private ChatViewerFragmentListener mListener;
    private Animation mShakeAnimation = null;
    private RecyclerView mRecyclerView;
    private View mContactTitleView;
    private AbstractContact mAbstractContact;
    private LinearLayoutManager mLayoutManager;
    private MessageItem mClickedMessageItem;

    private Timer mStopTypingTimer = new Timer();
    private final long STOP_TYPING_DELAY = 4000; // in ms
    private ImageButton mAttachButton;

    public static ChatViewerFragment newInstance(String account, String user) {
        ChatViewerFragment fragment = new ChatViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (ChatViewerFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatViewerFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccount = args.getString(ARGUMENT_ACCOUNT, null);
        mUser = args.getString(ARGUMENT_USER, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Theme);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        View view = localInflater.inflate(R.layout.fragment_chat_viewer, container, false);

        mContactTitleView = view.findViewById(R.id.contact_title);

        mAbstractContact = RosterManager.getInstance().getBestContact(mAccount, mUser);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_chat);
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(getActivity());
                getActivity().overridePendingTransition(R.anim.slide_in_reverse, R.anim.slide_out_reverse);
            }
        });

        setHasOptionsMenu(true);

        mSendButton = (ImageButton) view.findViewById(R.id.button_send_message);
        mSendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());

        mChatMessageAdapter = new ChatMessageAdapter(getActivity(), mAccount, mUser, this, this);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.chat_messages_recycler_view);
        mRecyclerView.setAdapter(mChatMessageAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // to avoid strange bug on some 4.x androids
        view.findViewById(R.id.input_layout).setBackgroundColor(ColorManager.getInstance().getChatInputBackgroundColor());

        mInputView = (EditText) view.findViewById(R.id.chat_input);

        view.findViewById(R.id.button_send_message).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }

                });

        mInputView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (SettingsManager.chatsSendByEnter()
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        mInputView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mSkipOnTextChanges && mStopTypingTimer != null) {
                    mStopTypingTimer.cancel();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                setUpInputViewButtons();

                if (mSkipOnTextChanges) {
                    return;
                }

                ChatStateManager.getInstance().onComposing(mAccount, mUser, text);

                mStopTypingTimer = new Timer();
                mStopTypingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Application.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ChatStateManager.getInstance().onPaused(mAccount, mUser);
                            }
                        });
                    }

                }, STOP_TYPING_DELAY);
            }

        });


        final ImageButton emojiButton = (ImageButton) view.findViewById(R.id.button_emoticon);
        final View rootView = view.findViewById(R.id.root_view);


        // Give the topmost view of your activity layout hierarchy. This will be used to measure soft keyboard height
        final EmojiconsPopup popup = new EmojiconsPopup(rootView, getActivity());

        // Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard();

        // If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_mood_black_32dp);
            }
        });

        // If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if (popup.isShowing())
                    popup.dismiss();
            }
        });

        // On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if (mInputView == null || emojicon == null) {
                    return;
                }

                int start = mInputView.getSelectionStart();
                int end = mInputView.getSelectionEnd();
                if (start < 0) {
                    mInputView.append(emojicon.getEmoji());
                } else {
                    mInputView.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });

        // On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mInputView.dispatchKeyEvent(event);
            }
        });

        // To toggle between text keyboard and emoji keyboard (Popup)
        emojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // If popup is not showing => emoji keyboard is not visible, we need to show it
                if (!popup.isShowing()) {

                    // If keyboard is visible, simply show the emoji popup
                    if (popup.isKeyBoardOpen()) {
                        popup.showAtBottom();
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_32dp);
                    }

                    // else, open the text keyboard first and immediately after that show the emoji popup
                    else {
                        mInputView.setFocusableInTouchMode(true);
                        mInputView.requestFocus();
                        popup.showAtBottomPending();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(mInputView, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_32dp);
                    }
                }

                // If popup is showing, simply dismiss it to show the undelying text keyboard
                else {
                    popup.dismiss();
                }
            }
        });

        mAttachButton = (ImageButton) view.findViewById(R.id.button_attach);
        mAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionsRequester.requestFileReadPermissionIfNeeded(ChatViewerFragment.this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
                    startFileSelection();
                }

            }
        });

        return view;
    }

    private void startFileSelection() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ATTACH_FILE :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    startFileSelection();
                } else {
                    onNoReadPermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    saveFileToDownloads();
                } else {
                    onNoWritePermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_EXPORT_CHAT :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    showExportChatDialog();
                } else {
                    onNoWritePermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE :
                if (!PermissionsRequester.isPermissionGranted(grantResults)) {
                    onNoWritePermissionError();
                }
                break;
        }
    }

    private void onNoWritePermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
    }

    private void onNoReadPermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode != FILE_SELECT_ACTIVITY_REQUEST_CODE) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        final Uri fileUri = result.getData();
        final String path = FileUtils.getPath(getActivity(), fileUri);

        LogManager.i(this, String.format("File uri: %s, path: %s", fileUri, path));

        if (path == null) {
            Toast.makeText(getActivity(), R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
            return;
        }

        uploadFile(path);
    }

    private void uploadFile(String path) {
        HttpFileUploadManager.getInstance().uploadFile(mAccount, mUser, path);
    }

    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId){
        iconToBeChanged.setImageResource(drawableResourceId);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.registerChat(this);
        updateChat();
        restoreInputState();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

//    private void showSecurityMenu() {
//        PopupMenu popup = new PopupMenu(getActivity(), mSecurityButton);
//        popup.inflate(R.menu.menu_security);
//        popup.setOnMenuItemClickListener(this);
//
//        Menu menu = popup.getMenu();
//
//        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(mAccount, mUser);
//
//        if (securityLevel == SecurityLevel.plain) {
//            menu.findItem(R.id.action_start_encryption).setVisible(true)
//                    .setEnabled(SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.disabled);
//        } else {
//            menu.findItem(R.id.action_restart_encryption).setVisible(true);
//        }
//
//        boolean isEncrypted = securityLevel != SecurityLevel.plain;
//
//        menu.findItem(R.id.action_stop_encryption).setEnabled(isEncrypted);
//        menu.findItem(R.id.action_verify_with_fingerprint).setEnabled(isEncrypted);
//        menu.findItem(R.id.action_verify_with_question).setEnabled(isEncrypted);
//        menu.findItem(R.id.action_verify_with_shared_secret).setEnabled(isEncrypted);
//
//        popup.show();
//    }

    private void setUpInputViewButtons() {
        mIsInputEmpty = mInputView.getText().toString().trim().isEmpty();

        if (mIsInputEmpty) {
            mSendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());
            mSendButton.setEnabled(false);
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(mAccount)) {
                mAttachButton.setVisibility(View.VISIBLE);
            } else {
                mAttachButton.setVisibility(View.GONE);
            }
        } else {
            mSendButton.setEnabled(true);
            mSendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(mAccount));
            mAttachButton.setVisibility(View.GONE);
        }
    }

    public void restoreInputState() {
        mSkipOnTextChanges = true;

        mInputView.setText(ChatManager.getInstance().getTypedMessage(mAccount, mUser));
        mInputView.setSelection(ChatManager.getInstance().getSelectionStart(mAccount, mUser),
                ChatManager.getInstance().getSelectionEnd(mAccount, mUser));

        mSkipOnTextChanges = false;

        if (!mInputView.getText().toString().isEmpty()) {
            mInputView.requestFocus();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ChatStateManager.getInstance().onPaused(mAccount, mUser);

        saveInputState();
        mListener.unregisterChat(this);
    }

    public void saveInputState() {
        ChatManager.getInstance().setTyped(mAccount, mUser, mInputView.getText().toString(),
                mInputView.getSelectionStart(), mInputView.getSelectionEnd());
    }

    private void sendMessage() {
        String text = mInputView.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        clearInputText();

        sendMessage(text);

        mListener.onMessageSent();

        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
                || (getActivity().getResources().getBoolean(R.bool.landscape)
                && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)) {
            ChatViewerActivity.hideKeyboard(getActivity());
        }
    }

    private void sendMessage(String text) {
        MessageManager.getInstance().sendMessage(mAccount, mUser, text);
        updateChat();
    }

    /**
     * This method used for hardware menu button
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        setUpOptionsMenu(menu);
    }

    /**
     * This method used for hardware menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onMenuItemClick(item);
    }

    private void setUpOptionsMenu(Menu menu) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(mAccount, mUser);

        if (abstractChat instanceof RoomChat) {
            RoomState chatState = ((RoomChat) abstractChat).getState();

            if (chatState == RoomState.available) {
                menu.findItem(R.id.action_list_of_occupants).setVisible(true);
            }

            if (chatState == RoomState.unavailable) {
                menu.findItem(R.id.action_join_conference).setVisible(true);

            } else {
                menu.findItem(R.id.action_invite_to_chat).setVisible(true);

                if (chatState == RoomState.error) {
                    menu.findItem(R.id.action_authorization_settings).setVisible(true);
                } else {
                    menu.findItem(R.id.action_leave_conference).setVisible(true);
                }
            }
        }

        if (abstractChat instanceof RegularChat) {
            menu.findItem(R.id.action_view_contact).setVisible(true);
            menu.findItem(R.id.action_close_chat).setVisible(true);
            menu.findItem(R.id.action_block_contact).setVisible(true);
        }
    }

    public void updateChat() {
        ContactTitleInflater.updateTitle(mContactTitleView, getActivity(), mAbstractContact);
        int itemCountBeforeUpdate = mChatMessageAdapter.getItemCount();
        mChatMessageAdapter.onChange();
        scrollChat(itemCountBeforeUpdate);
        setUpOptionsMenu(mToolbar.getMenu());
    }

    private void scrollChat(int itemCountBeforeUpdate) {
        int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
        if (lastVisibleItemPosition == -1 || lastVisibleItemPosition == (itemCountBeforeUpdate - 1)) {
            scrollDown();
        }
    }

    private void scrollDown() {
        mRecyclerView.scrollToPosition(mChatMessageAdapter.getItemCount() - 1);
    }

    public boolean isEqual(BaseEntity chat) {
        return chat != null && this.mAccount.equals(chat.getAccount()) && this.mUser.equals(chat.getUser());
    }

    public void setInputText(String additional) {
        mSkipOnTextChanges = true;
        mInputView.setText(additional);
        mInputView.setSelection(additional.length());
        mSkipOnTextChanges = false;
    }

    public String getAccount() {
        return mAccount;
    }

    public String getUser() {
        return mUser;
    }

    private void clearInputText() {
        mSkipOnTextChanges = true;
        mInputView.getText().clear();
        mSkipOnTextChanges = false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            /* security menu */

            case R.id.action_start_encryption:
                startEncryption(mAccount, mUser);
                return true;

            case R.id.action_restart_encryption:
                restartEncryption(mAccount, mUser);
                return true;

            case R.id.action_stop_encryption:
                stopEncryption(mAccount, mUser);
                return true;

            case R.id.action_verify_with_fingerprint:
//                startActivity(FingerprintViewer.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_verify_with_question:
//                startActivity(QuestionViewer.createIntent(getActivity(), account, user, true, false, null));
                return true;

            case R.id.action_verify_with_shared_secret:
//                startActivity(QuestionViewer.createIntent(getActivity(), account, user, false, false, null));
                return true;

            /* regular chat options menu */

            case R.id.action_view_contact:
                showContactInfo();
                return true;

            case R.id.action_chat_settings:
//                startActivity(ChatContactSettings.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_show_history:
                showHistory(mAccount, mUser);
                return true;

            case R.id.action_authorization_settings:
//                startActivity(ConferenceAdd.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_close_chat:
                closeChat(mAccount, mUser);
                return true;

            case R.id.action_clear_history:
                clearHistory(mAccount, mUser);
                return true;

            case R.id.action_export_chat:
                onExportChatClick();
                return true;

            case R.id.action_call_attention:
                callAttention();
                return true;

            case R.id.action_block_contact:
//                BlockContactDialog.newInstance(account, user).show(getFragmentManager(), BlockContactDialog.class.getName());
                return true;

            /* conference specific options menu */

            case R.id.action_join_conference:
                MUCManager.getInstance().joinRoom(mAccount, mUser, true);
                return true;

            case R.id.action_invite_to_chat:
//                startActivity(ContactList.createRoomInviteIntent(getActivity(), account, user));
                return true;

            case R.id.action_leave_conference:
                leaveConference(mAccount, mUser);
                return true;

            case R.id.action_list_of_occupants:
//                startActivity(OccupantList.createIntent(getActivity(), account, user));
                return true;

            /* message popup menu */

            case R.id.action_message_repeat:
                if (mClickedMessageItem.isUploadFileMessage()) {
                    uploadFile(mClickedMessageItem.getFile().getPath());
                } else {
                    sendMessage(mClickedMessageItem.getText());
                }
                return true;

            case R.id.action_message_copy:
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText(mClickedMessageItem.getSpannable(), mClickedMessageItem.getSpannable()));
                return true;

            case R.id.action_message_quote:
                setInputText("> " + mClickedMessageItem.getText() + "\n");
                return true;

            case R.id.action_message_remove:
                MessageManager.getInstance().removeMessage(mClickedMessageItem);
                updateChat();
                return true;

            case R.id.action_message_open_file:
                FileManager.openFile(getActivity(), mClickedMessageItem.getFile());
                return true;

            case R.id.action_message_save_file:
                OnSaveFileToDownloadsClick();
                return true;

            case R.id.action_message_open_muc_private_chat:
                String occupantFullJid = mUser + "/" + mClickedMessageItem.getResource();
                MessageManager.getInstance().openChat(mAccount, occupantFullJid);
                startActivity(ChatViewerActivity.createSpecificChatIntent(getActivity(), mAccount, occupantFullJid));
                return true;

            default:
                return false;
        }
    }

    private void onExportChatClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_EXPORT_CHAT)) {
            showExportChatDialog();
        }

    }

    private void showExportChatDialog() {
//        ChatExportDialogFragment.newInstance(account, user).show(getFragmentManager(), "CHAT_EXPORT");
    }

    private void OnSaveFileToDownloadsClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS)) {
            saveFileToDownloads();
        }
    }

    private void saveFileToDownloads() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    FileManager.saveFileToDownloads(mClickedMessageItem.getFile());
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.file_saved_successfully, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.could_not_save_file, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void showHistory(String account, String user) {
        MessageManager.getInstance().requestToLoadLocalHistory(account, user);
        MessageArchiveManager.getInstance().requestHistory(account, user, MINIMUM_MESSAGES_TO_LOAD, 0);
    }

    private void stopEncryption(String account, String user) {
        try {
            OTRManager.getInstance().endSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void restartEncryption(String account, String user) {
        try {
            OTRManager.getInstance().refreshSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void startEncryption(String account, String user) {
        try {
            OTRManager.getInstance().startSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar) {
            showContactInfo();
        }
    }

    private void showContactInfo() {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(mAccount, mUser)) {
            intent = ContactViewerActivity.createIntent(getActivity(), mAccount, mUser);
        } else {
            intent = ContactEditorActivity.createIntent(getActivity(), mAccount, mUser);
        }
        startActivity(intent);
    }

    private void closeChat(String account, String user) {
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        mListener.onCloseChat();
    }

    private void clearHistory(String account, String user) {
        MessageManager.getInstance().clearHistory(account, user);
    }

    private void leaveConference(String account, String user) {
        MUCManager.getInstance().leaveRoom(account, user);
        closeChat(account, user);
    }

    private void callAttention() {
        try {
            AttentionManager.getInstance().sendAttention(mAccount, mUser);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    @Override
    public void onMessageClick(View caller, int position) {
        int itemViewType = mChatMessageAdapter.getItemViewType(position);

        if (itemViewType == ChatMessageAdapter.VIEW_TYPE_INCOMING_MESSAGE
                || itemViewType == ChatMessageAdapter.VIEW_TYPE_OUTGOING_MESSAGE) {

            mClickedMessageItem = mChatMessageAdapter.getMessageItem(position);

            PopupMenu popup = new PopupMenu(getActivity(), caller);
            popup.inflate(R.menu.menu_chat_context);
            popup.setOnMenuItemClickListener(this);

            final Menu menu = popup.getMenu();

            if (mClickedMessageItem.isError()) {
                menu.findItem(R.id.action_message_repeat).setVisible(true);
            }

            if (mClickedMessageItem.isUploadFileMessage()) {
                menu.findItem(R.id.action_message_copy).setVisible(false);
                menu.findItem(R.id.action_message_quote).setVisible(false);
                menu.findItem(R.id.action_message_remove).setVisible(false);
            }

            final File file = mClickedMessageItem.getFile();

            if (file != null && file.exists()) {
                menu.findItem(R.id.action_message_open_file).setVisible(true);
                menu.findItem(R.id.action_message_save_file).setVisible(true);
            }

            if (mClickedMessageItem.isIncoming() && MUCManager.getInstance().hasRoom(mAccount, mUser)) {
                menu.findItem(R.id.action_message_open_muc_private_chat).setVisible(true);
            }

            popup.show();
        }
    }

    public void playIncomingAnimation() {
        if (mShakeAnimation == null) {
            mShakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
        }
        mToolbar.findViewById(R.id.name_holder).startAnimation(mShakeAnimation);
    }

    @Override
    public void onSuccessfullUpload(String getUrl) {
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onNoDownloadFilePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    public interface ChatViewerFragmentListener {
        void onCloseChat();

        void onMessageSent();

        void registerChat(ChatViewerFragment chat);

        void unregisterChat(ChatViewerFragment chat);
    }
}