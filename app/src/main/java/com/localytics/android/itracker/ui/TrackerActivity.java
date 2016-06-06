package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.ui.widget.FragmentPagerAdapter;
import com.localytics.android.itracker.util.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.LOGD;


public class TrackerActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener {

    private static final String TAG = LogUtils.makeLogTag(TrackerActivity.class);

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private NavigationView mNavView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Menu mMenu;

    private Handler mHandler = new Handler();

    private boolean mReadyToFinish;

    private final static String SELECTED_TAB = "selected_tab";

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

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    static final int REQUEST_GMS_ERROR_DIALOG     = 1;
    static final int REQUEST_ACCOUNT_PICKER       = 2;
    static final int REQUEST_AUTHORIZATION        = 3;
    static final int REQUEST_PHOTO_CAPTURE        = 4;
    static final int REQUEST_VIDEO_CAPTURE        = 5;
    static final int REQUEST_DIRECT_TAG           = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                mToolbar,              /* Toolbar object */
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

        if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt(SELECTED_TAB));
        }
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

        int position = mViewPager.getCurrentItem();
        mTabLayout.getTabAt(position).select(); // Switch to the current fragment
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAB, mViewPager.getCurrentItem());
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
     * This is the same as {@link #onSaveInstanceState} but is called for activities
     * created with the attribute {@link android.R.attr#persistableMode} set to
     * <code>persistAcrossReboots</code>. The {@link PersistableBundle} passed
     * in will be saved and presented in {@link #onCreate(Bundle, PersistableBundle)}
     * the first time that this activity is restarted following the next device reboot.
     *
     * @param outState           Bundle in which to place your saved state.
     * @param outPersistentState State which will be saved across reboots.
     * @see #onSaveInstanceState(Bundle)
     * @see #onCreate
     * @see #onRestoreInstanceState(Bundle, PersistableBundle)
     * @see #onPause
     */
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
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
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mReadyToFinish = false;
                }
            }, 2000);

//            Snackbar.make(findViewById(android.R.id.content), "Press again to quit", Snackbar.LENGTH_LONG)
//                    .setActionTextColor(Color.BLACK)
//                    .show();
            Toast.makeText(this, getString(R.string.back_pressed_prompt), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    public int getSelectedTab() {
        return mViewPager.getCurrentItem();
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
        adapter.addFragment(new PhotoFragment(), getString(TAB_NAMES[1]));
        adapter.addFragment(new MediaFragment(), getString(TAB_NAMES[2]));
        adapter.addFragment(new FriendFragment(), getString(TAB_NAMES[3]));
        mViewPager.setAdapter(adapter);
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
        TrackerFragment fragment = (TrackerFragment) adapter.getItem(tab.getPosition());
        fragment.onFragmentUnselected();
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
        int colorAccent = ContextCompat.getColor(this, R.color.colorAccent);
        int colorOrigin = ContextCompat.getColor(this, R.color.tab_text_color);

        // Clear the color of the unselected tab views
        for (int i = 0; i < mTabLayout.getTabCount(); ++i) {
            if (i == tab.getPosition()) {
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
        mViewPager.setCurrentItem(tab.getPosition(), false);

        // Update the color of the selected tab view
        TextView view = (TextView) tab.getCustomView();
        view.setTextColor(colorAccent);
        Drawable[] drawables = view.getCompoundDrawables();
        if (drawables[1] != null) {
            drawables[1].setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
        }

        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        TrackerFragment fragment = (TrackerFragment) adapter.getItem(tab.getPosition());
        fragment.onFragmentSelected();
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