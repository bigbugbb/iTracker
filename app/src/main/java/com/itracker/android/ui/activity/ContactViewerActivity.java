package com.itracker.android.ui.activity;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.account.OnAccountChangedListener;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.extension.muc.MUCManager;
import com.itracker.android.data.extension.vcard.OnVCardListener;
import com.itracker.android.data.intent.AccountIntentBuilder;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.data.roster.AbstractContact;
import com.itracker.android.data.roster.GroupManager;
import com.itracker.android.data.roster.OnContactChangedListener;
import com.itracker.android.data.roster.RosterContact;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.adapter.ContactTitleInflater;
import com.itracker.android.ui.adapter.ContactViewerHeaderInflater;
import com.itracker.android.ui.fragment.ConferenceInfoFragment;
import com.itracker.android.ui.fragment.ContactVCardViewerFragment;
import com.itracker.android.utils.BitmapUtils;
import com.itracker.android.utils.SdkVersionUtils;
import com.itracker.android.xmpp.address.Jid;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class ContactViewerActivity extends BaseActivity implements
        View.OnClickListener,
        OnVCardListener,
        OnContactChangedListener,
        OnAccountChangedListener,
        ContactVCardViewerFragment.Listener {

    private static final String TAG = makeLogTag(ContactViewerActivity.class);

    public static final int ACCOUNT_INFO_EDITOR_REQUEST_CODE = 1;
    public static final String INTENT_IS_FOR_ACCOUNT = "com.itracker.android.ui.activity.ContactViewerActivity.INTENT_IS_FOR_ACCOUNT";

    protected String mAccount;
    protected String mBareAddress;
    protected View mContactViewerHeader;
    protected View mContactTitleCollapsed;
    protected ImageView mAppBarBkgImage;
    protected AbstractContact mBestContact;
    protected AppBarLayout mAppBarLayout;
    protected CollapsingToolbarLayout mCollapsingToolbar;

    protected Button mBtnSendMessage;
    protected Button mBtnEditAccount;

    protected boolean mInitialized;
    protected boolean mExpandedMode;
    protected TransitionDrawable mToobarBackgroundColorTransition;

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewerActivity.class)
                .setAccount(account).setUser(user).build();
    }

    protected static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    protected static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // View information about contact from system contact list
            Uri data = getIntent().getData();
            if (data != null && "content".equals(data.getScheme())) {
                List<String> segments = data.getPathSegments();
                if (segments.size() == 2 && "data".equals(segments.get(0))) {
                    Long id;
                    try {
                        id = Long.valueOf(segments.get(1));
                    } catch (NumberFormatException e) {
                        id = null;
                    }
                    if (id != null)
                        // FIXME: Will be empty while application is loading
                        for (RosterContact rosterContact : RosterManager.getInstance().getContacts())
                            if (id.equals(rosterContact.getViewId())) {
                                mAccount = rosterContact.getAccount();
                                mBareAddress = rosterContact.getUser();
                                break;
                            }
                }
            }
        } else {
            mAccount = getAccount(getIntent());
            mBareAddress = getUser(getIntent());
        }

        boolean isForAccount = getIntent().getBooleanExtra(INTENT_IS_FOR_ACCOUNT, false);

        if (mBareAddress != null && mBareAddress.equalsIgnoreCase(GroupManager.IS_ACCOUNT)) {
            mBareAddress = Jid.getBareAddress(AccountManager.getInstance().getAccount(mAccount).getRealJid());
        } else if (isForAccount) {
            mBareAddress = Jid.getBareAddress(mAccount);
        }

        if (mAccount == null || mBareAddress == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        setContentView(R.layout.activity_contact_viewer);

        if (savedInstanceState == null) {
            Fragment fragment;
            if (MUCManager.getInstance().hasRoom(mAccount, mBareAddress)) {
                fragment = ConferenceInfoFragment.newInstance(mAccount, mBareAddress);
            } else {
                fragment = ContactVCardViewerFragment.newInstance(mAccount, mBareAddress);
            }

            getFragmentManager().beginTransaction().add(R.id.scrollable_container, fragment).commit();
        }

        mBestContact = RosterManager.getInstance().getBestContact(mAccount, mBareAddress);
        int colorTransparent = ContextCompat.getColor(this, android.R.color.transparent);
        int colorPrimary = ContextCompat.getColor(this, R.color.colorPrimary);

        mAppBarBkgImage = (ImageView) findViewById(R.id.contact_appbar_bkg_image);
        Bitmap bitmap = ((BitmapDrawable) mAppBarBkgImage.getDrawable()).getBitmap();
        float gradientHeight = getResources().getDimension(R.dimen.collapsing_toolbar_image_gradient_height);
        bitmap = BitmapUtils.addTransparentGradient(bitmap, gradientHeight);
        bitmap = BitmapUtils.addTransparentGradient(bitmap, gradientHeight / 2);
        mAppBarBkgImage.setImageDrawable(new BitmapDrawable(getResources(), bitmap));

        mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ContactViewerActivity.this));

        mContactViewerHeader = findViewById(R.id.contact_viewer_header);
        mContactTitleCollapsed = findViewById(R.id.contact_title_collapsed);

        mToobarBackgroundColorTransition = new TransitionDrawable(
                new ColorDrawable[]{new ColorDrawable(colorTransparent), new ColorDrawable(colorPrimary)});

        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setBackgroundColor(colorTransparent);
        mCollapsingToolbar.setContentScrimColor(colorTransparent);
        mCollapsingToolbar.setStatusBarScrimColor(colorPrimary);
        mCollapsingToolbar.setTitleEnabled(false);

        mBtnSendMessage = (Button) findViewById(R.id.send_message);
        mBtnSendMessage.setOnClickListener(this);
        mBtnSendMessage.setVisibility(isForAccount ? View.INVISIBLE : View.VISIBLE);
        mBtnEditAccount = (Button) findViewById(R.id.edit_account);
        mBtnEditAccount.setOnClickListener(this);
        mBtnEditAccount.setVisibility(isForAccount ? View.VISIBLE : View.INVISIBLE);

        findViewById(R.id.app_icon).setOnClickListener(this);

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float collapsingToolbarHeight = getResources().getDimension(R.dimen.collapsing_toolbar_height);
        float actionBarHeight = getResources().getDimension(R.dimen.default_toolbar_height);
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, metrics);
        }
        float buttonSpaceHeight = getResources().getDimension(R.dimen.collapsing_toolbar_button_space_height);
        float statusBarHeight = getStatusBarHeight(); // in pixel
        float offsetThreshold = collapsingToolbarHeight - statusBarHeight - actionBarHeight - buttonSpaceHeight;
        int animDuration = getResources().getInteger(android.R.integer.config_longAnimTime);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (verticalOffset <= -offsetThreshold) {
                if (mExpandedMode) {
                    mContactTitleCollapsed.animate()
                            .alpha(1)
                            .setDuration(animDuration)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    mToolbar.setBackground(mToobarBackgroundColorTransition);
                    mToobarBackgroundColorTransition.startTransition(animDuration);
                    updateBackArrowColor(R.color.white);
                    updateOverflowColor(R.color.white);
                    updateMenuItemsColor(R.color.white);
                }
                mExpandedMode = false;
            } else {
                if (!mExpandedMode) {
                    if (!mInitialized) {
                        mContactTitleCollapsed.setAlpha(0);
                        mToolbar.setBackgroundColor(colorTransparent);
                        mInitialized = true;
                    } else {
                        mContactTitleCollapsed.animate()
                                .alpha(0)
                                .setDuration(animDuration)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        mToolbar.setBackground(mToobarBackgroundColorTransition);
                        mToobarBackgroundColorTransition.reverseTransition(animDuration);
                    }
                    updateBackArrowColor(R.color.grey_400);
                    updateOverflowColor(R.color.grey_400);
                    updateMenuItemsColor(R.color.grey_400);
                }
                mExpandedMode = true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        ContactViewerHeaderInflater.updateHeader(mContactViewerHeader, this, mBestContact);
        ContactTitleInflater.updateTitle(mContactTitleCollapsed, this, mBestContact);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(mAccount, mBareAddress)) {
                updateCollapsedTitle();
                break;
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_message:
                onSendMessageClicked();
                break;
            case R.id.edit_account:
                onEditAccountClicked();
                break;
            case R.id.app_icon:
                Toast.makeText(this, R.string.funny_face, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    protected void onSendMessageClicked() {
        Intent intent = ChatViewerActivity.createClearTopIntent(ContactViewerActivity.this,
                mAccount, mBareAddress);
        startActivity(intent);
    }

    protected void onEditAccountClicked() {
        VCard vCard = ((ContactVCardViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container)).getVCard();
        if (vCard != null) {
            Intent intent = AccountInfoEditorActivity.createIntent(this, mAccount, vCard.getChildElementXML().toString());
            startActivityForResult(intent, ACCOUNT_INFO_EDITOR_REQUEST_CODE);
        }
    }

    private void updateCollapsedTitle() {
        mBestContact = RosterManager.getInstance().getBestContact(mAccount, mBareAddress);
        ContactTitleInflater.updateTitle(mContactTitleCollapsed, this, mBestContact);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateBackArrowColor(int colorId) {
        final Drawable upArrow = SdkVersionUtils.hasLollipop() ? getResources().getDrawable(R.drawable.abc_ic_ab_back_material, null) :
                getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(ContextCompat.getColor(this, colorId), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    // the derived classes should override this method to update their own menu item icon color
    protected void updateMenuItemsColor(int colorId) {
    }

    private void updateOverflowColor(int colorId) {
        final String overflowDescription = getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        final ArrayList<View> outViews = new ArrayList<>();
        decorView.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        if (outViews.size() > 0) {
            AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
            overflow.setColorFilter(ContextCompat.getColor(this, colorId), PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(mAccount)) {
            updateCollapsedTitle();
        }
    }

    protected String getAccount() {
        return mAccount;
    }

    protected String getBareAddress() {
        return mBareAddress;
    }

    @Override
    public void onVCardReceived() {
        ContactViewerHeaderInflater.updateHeader(mContactViewerHeader, this, mBestContact);
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        ContactViewerHeaderInflater.updateHeader(mContactViewerHeader, this, mBestContact, vCard);
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {

    }

    private float getStatusBarHeight() {
        float height = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = getResources().getDimensionPixelSize(resId);
        }
        return height;
    }
}