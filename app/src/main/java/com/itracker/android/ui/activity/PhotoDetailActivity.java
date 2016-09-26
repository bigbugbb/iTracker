package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;

import com.itracker.android.R;
import com.itracker.android.data.model.Photo;
import com.itracker.android.ui.fragment.PhotoDetailFragment;
import com.itracker.android.ui.listener.OnContentClickListener;

import java.util.ArrayList;

import static com.itracker.android.utils.LogUtils.makeLogTag;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PhotoDetailActivity extends ManagedActivity implements OnContentClickListener {
    private final static String TAG = makeLogTag(PhotoDetailActivity.class);

    public static final String EXTRA_SELECTED_PHOTO = "extra_selected_photo";
    public static final String EXTRA_AVAILABLE_PHOTOS = "extra_available_photos";

    private PhotoPagerAdapter mAdapter;
    private ViewPager mPager;

    public static Intent createIntent(Context context, Photo selectedPhoto, ArrayList<Photo> availablePhotos) {
        Intent intent = new Intent(context, PhotoDetailActivity.class);
        intent.putExtra(PhotoDetailActivity.EXTRA_SELECTED_PHOTO, selectedPhoto);
        intent.putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_AVAILABLE_PHOTOS, availablePhotos);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        final ArrayList<Photo> availablePhotos = getIntent().getParcelableArrayListExtra(EXTRA_AVAILABLE_PHOTOS);

        mAdapter = new PhotoPagerAdapter(getFragmentManager(), availablePhotos);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(2);

        // Set the selected item based on the extra passed in to this activity
        final Photo selectedPhoto = getIntent().getParcelableExtra(EXTRA_SELECTED_PHOTO);
        if (selectedPhoto != null) {
            for (int i = 0; i < availablePhotos.size(); ++i) {
                if (availablePhotos.get(i).data.equals(selectedPhoto.data)) {
                    mPager.setCurrentItem(i);
                    break;
                }
            }
        }

        toggleHideSystemBar();
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

    @Override
    public void onContentViewClicked(View v) {
        toggleHideSystemBar();
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

    public void toggleHideSystemBar() {
        int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(newVis);
    }
}
