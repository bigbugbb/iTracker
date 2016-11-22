package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.common.io.Files;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.NetworkException;
import com.itracker.android.data.SettingsManager;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.extension.vcard.OnVCardListener;
import com.itracker.android.data.extension.vcard.OnVCardSaveListener;
import com.itracker.android.data.extension.vcard.VCardManager;
import com.itracker.android.data.intent.EntityIntentBuilder;
import com.itracker.android.data.message.MessageManager;
import com.itracker.android.data.model.Photo;
import com.itracker.android.data.model.Track;
import com.itracker.android.data.roster.PresenceManager;
import com.itracker.android.data.roster.RosterManager;
import com.itracker.android.ui.adapter.FragmentPagerAdapter;
import com.itracker.android.ui.dialog.ContactSubscriptionDialog;
import com.itracker.android.ui.fragment.ActionFragment;
import com.itracker.android.ui.fragment.FriendFragment;
import com.itracker.android.ui.fragment.MediaFragment;
import com.itracker.android.ui.fragment.PhotoFragment;
import com.itracker.android.ui.listener.OnPhotoInventoryUpdatedListener;
import com.itracker.android.ui.listener.OnSelectedStateChangedListener;
import com.itracker.android.ui.listener.OnSelectedTrackChangedListener;
import com.itracker.android.ui.listener.OnTrackItemSelectedListener;
import com.itracker.android.ui.widget.CollectionView;
import com.itracker.android.utils.AccountUtils;
import com.itracker.android.utils.LogUtils;
import com.itracker.android.utils.PrefUtils;
import com.itracker.android.utils.ServerUtils;
import com.itracker.android.xmpp.address.Jid;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.itracker.android.utils.LogUtils.LOGD;


public class TrackerActivity extends SingleActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        TabLayout.OnTabSelectedListener,
        OnSelectedTrackChangedListener,
        OnPhotoInventoryUpdatedListener,
        OnVCardListener,
        OnVCardSaveListener {

    private static final String TAG = LogUtils.makeLogTag(TrackerActivity.class);

    private static final String ACTION_CONTACT_SUBSCRIPTION = "com.itracker.android.ui.activity.TrackerActivity.ACTION_CONTACT_SUBSCRIPTION";

    private static int VCARD_REQUEST_RETRY_COUNT = 5;
    private static int VCARD_UPDATE_RETRY_COUNT = 3;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private NavigationView mNavView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Menu mMenu;

    private VCard mVCard;
    private int mVCardRequestRetries = VCARD_REQUEST_RETRY_COUNT;
    private int mVCardUpdateRetries = VCARD_UPDATE_RETRY_COUNT;

    private Track mSelectedTrack;
    private CollectionView.Inventory mPhotoInventory;

    private String mAction;

    private Handler mHandler = new Handler();

    private boolean mReadyToFinish;

    final int[] TAB_NAMES = new int[] {
            R.string.tab_name_action,
            R.string.tab_name_photo,
            R.string.tab_name_media,
            R.string.tab_name_friends,
    };
    final int[] TAB_ICONS = new int[] {
            R.drawable.ic_tab_action,
            R.drawable.ic_tab_photo,
            R.drawable.ic_tab_media,
            R.drawable.ic_tab_friends
    };

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    public static final int REQUEST_GMS_ERROR_DIALOG     = 1;
    public static final int REQUEST_ACCOUNT_PICKER       = 2;
    public static final int REQUEST_AUTHORIZATION        = 3;
    public static final int REQUEST_PHOTO_CAPTURE        = 4;
    public static final int REQUEST_VIDEO_CAPTURE        = 5;
    public static final int REQUEST_DIRECT_TAG           = 6;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, TrackerActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent createContactSubscriptionIntent(Context context, String account, String user) {
        Intent intent = new EntityIntentBuilder(context, TrackerActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_CONTACT_SUBSCRIPTION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                      /* host Activity */
                mDrawerLayout,             /* DrawerLayout object */
                mToolbar,                  /* Toolbar object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateNavigationHeader();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // Setup navigation view
        mNavView = (NavigationView) findViewById(R.id.nav_view);
        mNavView.setNavigationItemSelectedListener(this);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        populateViewPager();
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        populateTabViews(mViewPager);

        // Build the binding between view pager and tab layout.
        mTabLayout.setOnTabSelectedListener(this);
        mViewPager.addOnPageChangeListener(new TrackerOnPageChangeListener(mTabLayout));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAction = getIntent().getAction();
        getIntent().setAction(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewPager.setCurrentItem(PrefUtils.getLastSelectedTab(this));
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnVCardSaveListener.class, this);
        Application.getInstance().addUIListener(OnSelectedTrackChangedListener.class, this);
        Application.getInstance().addUIListener(OnPhotoInventoryUpdatedListener.class, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!PrefUtils.isVCardUpdated(this)) {
            requestVCard(SettingsManager.getInstance().contactsSelectedAccount());
        }

        if (mAction != null) {
            switch (mAction) {
//                case ContactList.ACTION_ROOM_INVITE:
//                case Intent.ACTION_SEND:
//                case Intent.ACTION_CREATE_SHORTCUT:
//                    if (Intent.ACTION_SEND.equals(action)) {
//                        sendText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
//                    }
//                    Toast.makeText(this, getString(R.string.select_contact), Toast.LENGTH_LONG).show();
//                    break;
//                case Intent.ACTION_VIEW: {
//                    action = null;
//                    Uri data = getIntent().getData();
//                    if (data != null && "xmpp".equals(data.getScheme())) {
//                        XMPPUri xmppUri;
//                        try {
//                            xmppUri = XMPPUri.parse(data);
//                        } catch (IllegalArgumentException e) {
//                            xmppUri = null;
//                        }
//                        if (xmppUri != null && "message".equals(xmppUri.getQueryType())) {
//                            ArrayList<String> texts = xmppUri.getValues("body");
//                            String text = null;
//                            if (texts != null && !texts.isEmpty()) {
//                                text = texts.get(0);
//                            }
//                            openChat(xmppUri.getPath(), text);
//                        }
//                    }
//                    break;
//                }
//                case Intent.ACTION_SENDTO: {
//                    action = null;
//                    Uri data = getIntent().getData();
//                    if (data != null) {
//                        String path = data.getPath();
//                        if (path != null && path.startsWith("/")) {
//                            openChat(path.substring(1), null);
//                        }
//                    }
//                    break;
//                }

                case TrackerActivity.ACTION_CONTACT_SUBSCRIPTION:
                    mAction = null;
                    showContactSubscriptionDialog();
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PrefUtils.setLastSelectedTab(this, mViewPager.getCurrentItem());
        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnVCardSaveListener.class, this);
        Application.getInstance().removeUIListener(OnSelectedTrackChangedListener.class, this);
        Application.getInstance().removeUIListener(OnPhotoInventoryUpdatedListener.class, this);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracker, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_footprint) {
            if (mSelectedTrack != null) {
                startActivity(FootprintActivity.createIntent(this, mSelectedTrack));
            }
        } else if (id == R.id.nav_gallery) {
            if (mPhotoInventory != null) {
                startActivity(PhotoDetailActivity.createIntent(this, null, Photo.inventoryToList(mPhotoInventory)));
            }
        } else if (id == R.id.nav_downloaded) {
            startActivity(MediaDownloadActivity.createIntent(this));
        } else if (id == R.id.nav_messages) {
            selectTab(3, null);
            ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
            FriendFragment fragment = (FriendFragment) adapter.mFragments.get(3);
            fragment.switchToMessagesPage();
        } else if (id == R.id.nav_contacts) {
            selectTab(3, null);
            ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
            FriendFragment fragment = (FriendFragment) adapter.mFragments.get(3);
            fragment.switchToContactsPage();
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_exit) {
            finish();
        }

        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        for (Fragment fragment : adapter.mFragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(mNavView)) {
            mDrawerLayout.closeDrawers();
            return;
        } else if (!mReadyToFinish) {
            mReadyToFinish = true;
            mHandler.postDelayed(() -> mReadyToFinish = false, 2000);
            Toast.makeText(this, getString(R.string.back_pressed_prompt), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    private void showContactSubscriptionDialog() {
        Intent intent = getIntent();
        String account = EntityIntentBuilder.getAccount(intent);
        String user = EntityIntentBuilder.getUser(intent);
        if (account != null && user != null) {
            ContactSubscriptionDialog.newInstance(account, user).show(getFragmentManager(), ContactSubscriptionDialog.class.getName());
        }
    }

    private void updateNavigationHeader() {
        ImageView avatar = (ImageView) findViewById(R.id.avatar);
        TextView tvUsername = (TextView) findViewById(R.id.username);
        TextView tvEmail = (TextView) findViewById(R.id.email);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userName = user.getDisplayName();
            userName = TextUtils.isEmpty(userName) ? SettingsManager.contactsDefaultUsername() : userName;
            String email = user.getEmail();
            email = TextUtils.isEmpty(email) ? AccountUtils.getActiveAccountName(this) : email;
            tvUsername.setText(userName);
            tvEmail.setText(email);
            Uri photoUri = user.getPhotoUrl();
            photoUri = photoUri == null ? Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.baozou_avatar) : photoUri;
            // stackoverflow.com/question/36384789/glide-not-loading-real-image-and-stuck-with-placeholder
            Glide.with(this).load(photoUri).placeholder(R.drawable.baozou_avatar).dontAnimate().into(avatar);
        } else {
            tvUsername.setText(getString(R.string.username_placeholder));
            tvEmail.setText(getString(R.string.email_placeholder));
            Glide.with(this).load(R.drawable.ic_avatar_1).crossFade().into(avatar);
        }

        avatar.setOnClickListener(v -> {
            mDrawerLayout.closeDrawers();
            Intent intent = AccountViewerActivity.createAccountInfoIntent(TrackerActivity.this,
                    AccountManager.getInstance().getSelectedAccount());
            startActivity(intent);
        });
    }

    /**
     * Adding custom view to tab
     */
    private void populateTabViews(ViewPager viewPager) {
        mTabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < mTabLayout.getTabCount(); ++i) {
            TextView view = (TextView) LayoutInflater.from(this).inflate(R.layout.tracker_tab, null);
            view.setText(TAB_NAMES[i]);
            view.setCompoundDrawablesWithIntrinsicBounds(0, TAB_ICONS[i], 0, 0);
            mTabLayout.getTabAt(i).setCustomView(view);
        }
    }

    /**
     * Adding fragments to ViewPager
     */
    private void populateViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
        adapter.addFragment(new ActionFragment(), getString(TAB_NAMES[0]));
        adapter.addFragment(new PhotoFragment(),  getString(TAB_NAMES[1]));
        adapter.addFragment(new MediaFragment(),  getString(TAB_NAMES[2]));
        adapter.addFragment(new FriendFragment(), getString(TAB_NAMES[3]));
        mViewPager.setAdapter(adapter);
    }

    private void requestVCard(@NonNull String account) {
        if (mVCard == null) {
            VCardManager.getInstance().request(account, Jid.getBareAddress(account));
        }
    }

    // Update the vcard with the user profile from the firebase user.
    private void updateVCard(String account, String bareAddress, VCard vCard) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (!account.startsWith(bareAddress)) {
            LOGD(TAG, "Can't update with the vcard from another account...");
            return;
        }

        if (user == null) {
            LOGD(TAG, "Waiting for firebase user...");
            Application.getInstance().runOnUiThread(() -> onVCardSaveFailed(account));
            return;
        }

        String name = user.getDisplayName();
        String email = user.getEmail();
        name = TextUtils.isEmpty(name) ? SettingsManager.contactsDefaultUsername() : name;
        email = TextUtils.isEmpty(email) ? AccountUtils.getActiveAccountName(this) : email;

        vCard.setNickName(name);
        vCard.setEmailHome(email);

        try {
            Uri photoUri = user.getPhotoUrl();
            if (user.getPhotoUrl() == null) {
                photoUri = photoUri == null ? Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.baozou_avatar) : photoUri;
            }
            FutureTarget<File> target = Glide.with(this).load(photoUri).downloadOnly(-1, -1);
            File avatorImageFile = target.get(10, TimeUnit.SECONDS);
            vCard.setAvatar(avatorImageFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        VCardManager.getInstance().saveVCard(account, vCard);
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        LOGD(TAG, "onVCardReceived: " + account);
        mVCard = vCard;
        Application.getInstance().runInBackground(() -> updateVCard(account, bareAddress, vCard));
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        LOGD(TAG, "onVCardFailed: " + account);
        mVCard = null;
        if (mVCardRequestRetries-- > 0) {
            mHandler.postDelayed(() -> requestVCard(account), DateUtils.SECOND_IN_MILLIS * 3);
        }
    }

    @Override
    public void onVCardSaveSuccess(String account) {
        LOGD(TAG, "onVCardSaveSuccess: " + account);
        PrefUtils.markVCardUpdated(this);
    }

    @Override
    public void onVCardSaveFailed(String account) {
        LOGD(TAG, "onVCardSaveFailed: " + account);
        if (mVCardUpdateRetries-- > 0) {
            mHandler.postDelayed(() -> requestVCard(account), DateUtils.SECOND_IN_MILLIS * 3);
        }
    }

    /**
     * Called when a tab enters the selected state.
     *
     * @param tab The tab that was selected
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is selected", tab.getPosition()));
        selectTab(tab);
    }

   /**
     * Called when a tab exits the selected state.
     *
     * @param tab The tab that was unselected
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is unselected", tab.getPosition()));
        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        OnSelectedStateChangedListener listener = (OnSelectedStateChangedListener) adapter.getItem(tab.getPosition());
        listener.onUnselected();
    }

    /**
     * Called when a tab that is already selected is chosen again by the user. Some applications
     * may use this action to return to the top level of a category.
     *
     * @param tab The tab that was reselected.
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is reselected", tab.getPosition()));
        selectTab(tab);
    }

    private void selectTab(TabLayout.Tab tab) {
        selectTab(tab.getPosition(), tab.getCustomView());
    }

    private void selectTab(int position, View customViewFromTab) {
        int colorAccent = ContextCompat.getColor(this, R.color.colorAccent);
        int colorOrigin = ContextCompat.getColor(this, R.color.tab_text_color);

        // Clear the color of the unselected tab views
        for (int i = 0; i < mTabLayout.getTabCount(); ++i) {
            if (i == position) {
                continue;
            }
            TextView view = (TextView) mTabLayout.getTabAt(i).getCustomView();
            if (view != null) {
                view.setTextColor(colorOrigin);
                Drawable[] drawables = view.getCompoundDrawables();
                if (drawables[1] != null) {
                    drawables[1].setColorFilter(null);
                }
            }
        }

        // We can either select the page by scrolling the view pager or by clicking the tab view.
        // For the later case the event generates from the tab layout, so view pager must be notified.
        // Stop using smooth scroll is necessary otherwise onPageChanged will still be called during
        // the settling state even after the unselected tabs are cleared their color.
        mViewPager.setCurrentItem(position, false);

        // Update the color of the selected tab view
        if (customViewFromTab != null) {
            TextView view = (TextView) customViewFromTab;
            view.setTextColor(colorAccent);
            Drawable[] drawables = view.getCompoundDrawables();
            if (drawables[1] != null) {
                drawables[1].setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
            }
        }

        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        OnSelectedStateChangedListener listener = (OnSelectedStateChangedListener) adapter.getItem(position);
        listener.onSelected();
    }

    @Override
    public void onSelectedTrackChanged(Track track) {
        mSelectedTrack = track;
    }

    @Override
    public void onPhotoInventoryUpdated(CollectionView.Inventory photoInventory) {
        mPhotoInventory = photoInventory;
    }

    private static class TrackerOnPageChangeListener extends SimpleOnPageChangeListener {
        private WeakReference<TabLayout> mTabLayoutRef;
        private Bitmap mCachedBitmap;
        private Canvas mCachedCanvas;

        public TrackerOnPageChangeListener(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
            mCachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            mCachedCanvas = new Canvas(mCachedBitmap);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final TextView selectedView = (TextView) tabLayout.getTabAt(position).getCustomView();
                int colorNormal = ContextCompat.getColor(tabLayout.getContext(), R.color.tab_text_color);
                int colorAccent = ContextCompat.getColor(tabLayout.getContext(), R.color.colorAccent);

                if (selectedView != null && selectedView.getWidth() > 0) {

                    if (positionOffset > 0f && position < tabLayout.getTabCount() - 1) {
                        TextView nextView = (TextView) tabLayout.getTabAt(position + 1).getCustomView();

                        int selectedColor = ColorUtils.setAlphaComponent(colorAccent, Math.round(255 * (1 - positionOffset)));
                        int nextColor = ColorUtils.setAlphaComponent(colorAccent, Math.round(255 * positionOffset));

                        // Update text and color of the selected tab
                        selectedView.setTextColor(getFilteredTextColor(colorNormal, selectedColor));
                        nextView.setTextColor(getFilteredTextColor(colorNormal, nextColor));
                        Drawable[] drawables = selectedView.getCompoundDrawables();
                        drawables[1].setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
                        drawables = nextView.getCompoundDrawables();
                        drawables[1].setColorFilter(nextColor, PorterDuff.Mode.SRC_ATOP);
                    }
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            LOGD(TAG, "state = " + state);
        }

        // The crazy hack to apply color filter to the text color.
        private int getFilteredTextColor(int textColor, int filterColor) {
            mCachedCanvas.drawColor(textColor);
            mCachedCanvas.drawColor(filterColor, PorterDuff.Mode.SRC_ATOP);
            return mCachedBitmap.getPixel(0, 0);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}