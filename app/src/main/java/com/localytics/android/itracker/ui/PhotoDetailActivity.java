package com.localytics.android.itracker.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.WindowManager;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Photo;

import java.util.ArrayList;

import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PhotoDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = makeLogTag(PhotoDetailActivity.class);

    public static final String EXTRA_SELECTED_PHOTO = "extra_selected_photo";
    public static final String EXTRA_AVAILABLE_PHOTOS = "extra_available_photos";

    private PhotoPagerAdapter mAdapter;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        final ArrayList<Photo> availablePhotos = getIntent().getParcelableArrayListExtra(EXTRA_AVAILABLE_PHOTOS);

        mAdapter = new PhotoPagerAdapter(getFragmentManager(), availablePhotos);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
        mPager.setOffscreenPageLimit(2);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
//        final ActionBar actionBar = getSupportActionBar();

        // Hide title text and set home as up
//        actionBar.setDisplayShowTitleEnabled(false);
//        actionBar.setDisplayHomeAsUpEnabled(true);

        // Hide and show the ActionBar as the visibility changes
//        mPager.setOnSystemUiVisibilityChangeListener(
//                new View.OnSystemUiVisibilityChangeListener() {
//                    @Override
//                    public void onSystemUiVisibilityChange(int visibility) {
//                        if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
//                            actionBar.hide();
//                        } else {
//                            actionBar.show();
//                        }
//                    }
//                });

        // Start low profile mode and hide ActionBar
//        mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
//        actionBar.hide();

        // Set the selected item based on the extra passed in to this activity
        final Photo selectedPhoto = getIntent().getParcelableExtra(EXTRA_SELECTED_PHOTO);
        if (selectedPhoto != null ) {
            int itemIndex = availablePhotos.indexOf(selectedPhoto);
            if (itemIndex != -1) {
                mPager.setCurrentItem(itemIndex);
            }
        }


        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        toggleHideyBar();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PhotoPagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Photo> mPhotos;

        public PhotoPagerAdapter(FragmentManager fm, ArrayList<Photo> photos) {
            super(fm);
            mPhotos = photos;
        }

        /**
         * Return the Fragment associated with a specified position.
         *
         * @param position
         */
        @Override
        public Fragment getItem(int position) {
            return PhotoDetailFragment.newInstance(mPhotos.get(position));
        }

        /**
         * Return the number of views available.
         */
        @Override
        public int getCount() {
            return mPhotos.size();
        }
    }

    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @Override
    public void onClick(View v) {
//        toggleHideyBar();
    }

//    public void toggleHideyBar() {
//
//        // BEGIN_INCLUDE (get_current_ui_flags)
//        // The UI options currently enabled are represented by a bitfield.
//        // getSystemUiVisibility() gives us that bitfield.
//        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
//        int newUiOptions = uiOptions;
//        // END_INCLUDE (get_current_ui_flags)
//        // BEGIN_INCLUDE (toggle_ui_flags)
//        boolean isImmersiveModeEnabled =
//                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
//        if (isImmersiveModeEnabled) {
//            LOGI(TAG, "Turning immersive mode mode off. ");
//        } else {
//            LOGI(TAG, "Turning immersive mode mode on.");
//        }
//
//        // Navigation bar hiding:  Backwards compatible to ICS.
//        if (Build.VERSION.SDK_INT >= 14) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//        }
//
//        // Status bar hiding: Backwards compatible to Jellybean
//        if (Build.VERSION.SDK_INT >= 16) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
//        }
//
//        // Immersive mode: Backward compatible to KitKat.
//        // Note that this flag doesn't do anything by itself, it only augments the behavior
//        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
//        // all three flags are being toggled together.
//        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
//        // Sticky immersive mode differs in that it makes the navigation and status bars
//        // semi-transparent, and the UI flag does not get cleared when the user interacts with
//        // the screen.
//        if (Build.VERSION.SDK_INT >= 18) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        }
//
//        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
//        //END_INCLUDE (set_ui_flags)
//    }
}
