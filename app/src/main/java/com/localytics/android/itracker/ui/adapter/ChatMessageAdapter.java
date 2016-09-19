package com.localytics.android.itracker.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.data.SettingsManager;
import com.localytics.android.itracker.data.account.AccountItem;
import com.localytics.android.itracker.data.account.AccountManager;
import com.localytics.android.itracker.data.extension.avatar.AvatarManager;
import com.localytics.android.itracker.data.extension.file.FileManager;
import com.localytics.android.itracker.data.extension.muc.MUCManager;
import com.localytics.android.itracker.data.extension.muc.RoomContact;
import com.localytics.android.itracker.data.message.ChatAction;
import com.localytics.android.itracker.data.message.MessageItem;
import com.localytics.android.itracker.data.message.MessageManager;
import com.localytics.android.itracker.data.roster.AbstractContact;
import com.localytics.android.itracker.data.roster.RosterContact;
import com.localytics.android.itracker.data.roster.RosterManager;
import com.localytics.android.itracker.ui.color.ColorManager;
import com.localytics.android.itracker.ui.helper.PermissionsRequester;
import com.localytics.android.itracker.utils.Emoticons;
import com.localytics.android.itracker.utils.StringUtils;
import com.localytics.android.xmpp.address.Jid;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_HINT = 1;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;

    private final Context mContext;
    private final Message.MessageClickListener mMessageClickListener;
    /**
     * Message font appearance.
     */
    private final int mAppearanceStyle;
    private String mAccount;
    private String mUser;
    private boolean mIsMUC;
    private String mucNickname;

    private List<MessageItem> mMessages;
    /**
     * Text with extra information.
     */
    private String mHint;
    private Listener mListener;

    public interface Listener {
        void onNoDownloadFilePermission();
    }

    public ChatMessageAdapter(Context context, String account, String user, Message.MessageClickListener messageClickListener, ChatMessageAdapter.Listener listener) {
        mContext = context;
        mMessages = Collections.emptyList();
        mAccount = account;
        mUser = user;
        mMessageClickListener = messageClickListener;
        mListener = listener;

        mIsMUC = MUCManager.getInstance().hasRoom(account, user);
        if (mIsMUC) {
            mucNickname = MUCManager.getInstance().getNickname(account, user);
        }
        mHint = null;
        mAppearanceStyle = SettingsManager.chatsAppearanceStyle();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HINT:
                return new BasicMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_info, parent, false));

            case VIEW_TYPE_ACTION_MESSAGE:
                return new BasicMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_action_message, parent, false));

            case VIEW_TYPE_INCOMING_MESSAGE:
                return new IncomingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_incoming_message, parent, false), mMessageClickListener);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessage(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_viewer_outgoing_message, parent, false), mMessageClickListener);
            default:
                return null;
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int viewType = getItemViewType(position);

        MessageItem messageItem = getMessageItem(position);

        switch (viewType) {
            case VIEW_TYPE_HINT:
                ((BasicMessage) holder).mMessageText.setText(mHint);
                break;

            case VIEW_TYPE_ACTION_MESSAGE:
                ChatAction action = messageItem.getAction();
                String time = StringUtils.getSmartTimeText(mContext, messageItem.getTimestamp());

                String name;
                if (mIsMUC) {
                    name = messageItem.getResource();
                } else {
                    name = RosterManager.getInstance().getBestContact(mAccount, messageItem.getChat().getUser()).getName();
                }
                ((BasicMessage) holder).mMessageText.setText(time + ": "
                        + action.getText(mContext, name, messageItem.getSpannable().toString()));

                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                setUpIncomingMessage((IncomingMessage) holder, messageItem);
                break;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                setUpOutgoingMessage((OutgoingMessage) holder, messageItem);
                break;
        }

    }

    private void setUpOutgoingMessage(OutgoingMessage outgoingMessage, MessageItem messageItem) {
        setUpMessage(messageItem, outgoingMessage);
        setUpAvatar(messageItem, outgoingMessage);
        setStatusIcon(messageItem, outgoingMessage);
        setUpFileMessage(outgoingMessage, messageItem);

        setUpMessageBalloonBackground(outgoingMessage.mMessageBalloon,
                mContext.getResources().getColorStateList(R.color.outgoing_message_color_state_dark), R.drawable.message_outgoing_states);
    }

    private void setUpIncomingMessage(final IncomingMessage incomingMessage, final MessageItem messageItem) {
        setUpMessage(messageItem, incomingMessage);

        setUpMessageBalloonBackground(incomingMessage.mMessageBalloon,
                ColorManager.getInstance().getChatIncomingBalloonColorsStateList(mAccount), R.drawable.message_incoming);

        setUpAvatar(messageItem, incomingMessage);
        setUpFileMessage(incomingMessage, messageItem);

        if (messageItem.getText().trim().isEmpty()) {
            incomingMessage.mMessageBalloon.setVisibility(View.GONE);
            incomingMessage.mMessageTime.setVisibility(View.GONE);
            incomingMessage.mAvatar.setVisibility(View.GONE);
            LogManager.w(this, "Empty message! Hidden, but need to correct");
        } else {
            incomingMessage.mMessageBalloon.setVisibility(View.VISIBLE);
            incomingMessage.mMessageTime.setVisibility(View.VISIBLE);
        }
    }

    private void setUpMessageBalloonBackground(View messageBalloon, ColorStateList darkColorStateList, int lightBackgroundId) {

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            final Drawable originalBackgroundDrawable = messageBalloon.getBackground();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalBackgroundDrawable.setTintList(darkColorStateList);
            } else {
                Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
                DrawableCompat.setTintList(wrapDrawable, darkColorStateList);

                int pL = messageBalloon.getPaddingLeft();
                int pT = messageBalloon.getPaddingTop();
                int pR = messageBalloon.getPaddingRight();
                int pB = messageBalloon.getPaddingBottom();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    messageBalloon.setBackground(wrapDrawable);
                } else {
                    messageBalloon.setBackgroundDrawable(wrapDrawable);
                }

                messageBalloon.setPadding(pL, pT, pR, pB);
            }
        } else {
            int pL = messageBalloon.getPaddingLeft();
            int pT = messageBalloon.getPaddingTop();
            int pR = messageBalloon.getPaddingRight();
            int pB = messageBalloon.getPaddingBottom();

            messageBalloon.setBackgroundResource(lightBackgroundId);
            messageBalloon.getBackground().setLevel(AccountManager.getInstance().getColorLevel(mAccount));
            messageBalloon.setPadding(pL, pT, pR, pB);
        }
    }


    private void setUpFileMessage(final Message messageView, final MessageItem messageItem) {
        messageView.mDownloadProgressBar.setVisibility(View.GONE);
        messageView.mAttachmentButton.setVisibility(View.GONE);
        messageView.mDownloadButton.setVisibility(View.GONE);
        messageView.mMessageImage.setVisibility(View.GONE);
        messageView.mMessageFileInfo.setVisibility(View.GONE);
        messageView.mMessageTextForFileName.setVisibility(View.GONE);

        if (messageItem.getFile() == null) {
            return;
        }

        LogManager.i(this, "processing file messageView " + messageItem.getText());

        messageView.mMessageText.setVisibility(View.GONE);
        messageView.mMessageTextForFileName.setText(FileManager.getFileName(messageItem.getFile().getPath()));
        messageView.mMessageTextForFileName.setVisibility(View.VISIBLE);

        messageView.mAttachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileManager.openFile(mContext, messageItem.getFile());
            }
        });

        final Long fileSize = messageItem.getFileSize();
        if (fileSize != null) {
            messageView.mMessageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(mContext, fileSize));
            messageView.mMessageFileInfo.setVisibility(View.VISIBLE);
        }

        if (messageItem.getFile().exists()) {
            onFileExists(messageView, messageItem.getFile());
        } else {
            if (SettingsManager.connectionLoadImages()
                    && FileManager.fileIsImage(messageItem.getFile())
                    && PermissionsRequester.hasFileWritePermission()) {
                LogManager.i(this, "Downloading file from message adapter");
                downloadFile(messageView, messageItem);
            } else {
                messageView.mDownloadButton.setVisibility(View.VISIBLE);
                messageView.mDownloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadFile(messageView, messageItem);
                    }
                });
            }
        }
    }

    private void downloadFile(final Message messageView, final MessageItem messageItem) {
        if (!PermissionsRequester.hasFileWritePermission()) {
            mListener.onNoDownloadFilePermission();
            return;
        }

        messageView.mDownloadButton.setVisibility(View.GONE);
        messageView.mDownloadProgressBar.setVisibility(View.VISIBLE);
        FileManager.getInstance().downloadFile(messageItem, new FileManager.ProgressListener() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                String progress = android.text.format.Formatter.formatShortFileSize(mContext, bytesWritten);
                // in some cases total size set to 1 (should be fixed in future version of com.loopj.android:android-async-http)
                if (bytesWritten <= totalSize) {
                    progress += " / " + android.text.format.Formatter.formatShortFileSize(mContext, totalSize);
                }

                if (!progress.equals(messageView.mMessageFileInfo.getText())) {
                    messageView.mMessageFileInfo.setText(progress);
                }

                if (messageView.mMessageFileInfo.getVisibility() != View.VISIBLE) {
                    messageView.mMessageFileInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFinish(long totalSize) {
                MessageManager.getInstance().onChatChanged(messageItem.getChat().getAccount(), messageItem.getChat().getUser(), false);
            }
        });
    }

    private void onFileExists(Message message, final File file) {
        if (FileManager.fileIsImage(file) && PermissionsRequester.hasFileReadPermission()) {
            message.mMessageTextForFileName.setVisibility(View.GONE);
            message.mMessageImage.setVisibility(View.VISIBLE);
            FileManager.loadImageFromFile(file, message.mMessageImage);
            message.mMessageImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FileManager.openFile(mContext, file);
                }
            });

        } else {
            message.mAttachmentButton.setVisibility(View.VISIBLE);
        }

        message.mMessageFileInfo.setText(android.text.format.Formatter.formatShortFileSize(mContext, file.length()));
        message.mMessageFileInfo.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        if (mHint == null) {
            return mMessages.size();
        } else {
            return mMessages.size() + 1;
        }
    }

    public MessageItem getMessageItem(int position) {
        if (position < mMessages.size()) {
            return mMessages.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mMessages.size()) {
            return VIEW_TYPE_HINT;
        }

        MessageItem messageItem = getMessageItem(position);
        if (messageItem.getAction() != null) {
            return VIEW_TYPE_ACTION_MESSAGE;
        }

        if (messageItem.isIncoming()) {
            if (mIsMUC && messageItem.getResource().equals(mucNickname)) {
                return VIEW_TYPE_OUTGOING_MESSAGE;
            }
            return VIEW_TYPE_INCOMING_MESSAGE;
        } else {
            return VIEW_TYPE_OUTGOING_MESSAGE;
        }
    }

    private void setUpMessage(MessageItem messageItem, Message message) {

        if (mIsMUC) {
            message.mMessageHeader.setText(messageItem.getResource());
            message.mMessageHeader.setVisibility(View.VISIBLE);
        } else {
            message.mMessageHeader.setVisibility(View.GONE);
        }

        if (messageItem.isUnencypted()) {
            message.mMessageUnencrypted.setVisibility(View.VISIBLE);
        } else {
            message.mMessageUnencrypted.setVisibility(View.GONE);
        }

        message.mMessageText.setTextAppearance(mContext, mAppearanceStyle);
        message.mMessageTextForFileName.setTextAppearance(mContext, mAppearanceStyle);

        final Spannable spannable = messageItem.getSpannable();
        Emoticons.getSmiledText(mContext, spannable, message.mMessageText);
        message.mMessageText.setText(spannable);
        message.mMessageText.setVisibility(View.VISIBLE);

        String time = StringUtils.getSmartTimeText(mContext, messageItem.getTimestamp());

        Date delayTimestamp = messageItem.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = mContext.getString(messageItem.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getSmartTimeText(mContext, delayTimestamp));
            time += " (" + delay + ")";
        }

        message.mMessageTime.setText(time);
    }

    private void setStatusIcon(MessageItem messageItem, OutgoingMessage message) {
        message.mStatusIcon.setVisibility(View.VISIBLE);
        message.mProgressBar.setVisibility(View.GONE);

        if (messageItem.isUploadFileMessage() && !messageItem.isError()) {
            message.mProgressBar.setVisibility(View.VISIBLE);
        }

        int messageIcon = R.drawable.ic_message_delivered_18dp;
        if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_18dp;
        } else if (!messageItem.isUploadFileMessage() && !messageItem.isSent()) {
            messageIcon = R.drawable.ic_message_not_sent_18dp;
        } else if (!messageItem.isDelivered()) {
            message.mStatusIcon.setVisibility(View.GONE);
        }

        message.mStatusIcon.setImageResource(messageIcon);
    }

    private void setUpAvatar(MessageItem messageItem, IncomingMessage message) {
        if (SettingsManager.chatsShowAvatars()) {
            final String account = messageItem.getChat().getAccount();
            final String user = messageItem.getChat().getUser();
            final String resource = messageItem.getResource();

            message.mAvatar.setVisibility(View.VISIBLE);
            if ((mIsMUC && MUCManager.getInstance().getNickname(account, user).equalsIgnoreCase(resource))) {
                message.mAvatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (mIsMUC) {
                    if ("".equals(resource)) {
                        message.mAvatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                    } else {
                        message.mAvatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user + "/" + resource));
                    }
                } else {
                    message.mAvatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user));
                }
            }
        } else {
            message.mAvatar.setVisibility(View.GONE);
        }
    }

    private void setUpAvatar(MessageItem messageItem, OutgoingMessage message) {
        if (SettingsManager.chatsShowAvatars()) {
            message.mAvatar.setVisibility(View.VISIBLE);
            message.mAvatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(mAccount));
        } else {
            message.mAvatar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onChange() {
        mMessages = new ArrayList<>(MessageManager.getInstance().getMessages(mAccount, mUser));
        mHint = getHint();
        notifyDataSetChanged();
    }

    /**
     * @return New hint.
     */
    private String getHint() {
        AccountItem accountItem = AccountManager.getInstance().getAccount(mAccount);
        boolean online = accountItem != null && accountItem.getState().isConnected();
        final AbstractContact abstractContact = RosterManager.getInstance().getBestContact(mAccount, mUser);
        if (!online) {
            if (abstractContact instanceof RoomContact) {
                return mContext.getString(R.string.muc_is_unavailable);
            } else {
                return mContext.getString(R.string.account_is_offline);
            }
        } else if (!abstractContact.getStatusMode().isOnline()) {
            if (abstractContact instanceof RoomContact) {
                return mContext.getString(R.string.muc_is_unavailable);
            } else {
                return mContext.getString(R.string.contact_is_offline, abstractContact.getName());
            }
        }
        return null;
    }

    public static class BasicMessage extends RecyclerView.ViewHolder {

        public TextView mMessageText;

        public BasicMessage(View itemView) {
            super(itemView);

            mMessageText = (TextView) itemView.findViewById(R.id.message_text);
        }
    }

    public static abstract class Message extends BasicMessage implements View.OnClickListener {

        public TextView mMessageTime;
        public TextView mMessageHeader;
        public TextView mMessageUnencrypted;
        public View mMessageBalloon;

        MessageClickListener mOnClickListener;

        public ImageButton mDownloadButton;
        public ImageButton mAttachmentButton;
        public ProgressBar mDownloadProgressBar;
        public ImageView mMessageImage;
        public TextView mMessageFileInfo;
        public TextView mMessageTextForFileName;

        public Message(View itemView, MessageClickListener onClickListener) {
            super(itemView);
            mOnClickListener = onClickListener;

            mMessageTime = (TextView) itemView.findViewById(R.id.message_time);
            mMessageHeader = (TextView) itemView.findViewById(R.id.message_header);
            mMessageUnencrypted = (TextView) itemView.findViewById(R.id.message_unencrypted);
            mMessageBalloon = itemView.findViewById(R.id.message_balloon);

            mDownloadButton = (ImageButton) itemView.findViewById(R.id.message_download_button);
            mAttachmentButton = (ImageButton) itemView.findViewById(R.id.message_attachment_button);
            mDownloadProgressBar = (ProgressBar) itemView.findViewById(R.id.message_download_progress_bar);
            mMessageImage = (ImageView) itemView.findViewById(R.id.message_image);
            mMessageFileInfo = (TextView) itemView.findViewById(R.id.message_file_info);
            mMessageTextForFileName = (TextView) itemView.findViewById(R.id.message_text_for_filenames);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mOnClickListener.onMessageClick(mMessageBalloon, getPosition());
        }

        public interface MessageClickListener {
            void onMessageClick(View caller, int position);
        }
    }

    public static class IncomingMessage extends Message {

        public ImageView mAvatar;

        public IncomingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            mAvatar = (ImageView) itemView.findViewById(R.id.incoming_avatar);
        }
    }

    public static class OutgoingMessage extends Message {

        public ImageView mAvatar;
        public ImageView mStatusIcon;
        public ProgressBar mProgressBar;

        public OutgoingMessage(View itemView, MessageClickListener listener) {
            super(itemView, listener);
            mAvatar = (ImageView) itemView.findViewById(R.id.outgoing_avatar);
            mStatusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
        }
    }
}