package com.localytics.android.itracker.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Photo;
import com.localytics.android.itracker.ui.widget.CollectionView;
import com.localytics.android.itracker.ui.widget.CollectionViewCallbacks;
import com.localytics.android.itracker.ui.widget.PhotoCoordinatorLayout;
import com.localytics.android.itracker.utils.ThrottledContentObserver;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class PhotoFragment extends TrackerFragment {

    private static final String TAG = makeLogTag(PhotoFragment.class);

    private CollectionView mPhotoCollectionView;
    private PhotoCollectionAdapter mPhotoCollectionAdapter;

    private CollectionView.Inventory mPhotoInventory;

    private String mCurrentPhotoPath;
    private FloatingActionButton mFabTakePhoto;

    private TimeRangeController mTimeRangeController;

    private ThrottledContentObserver mPhotosObserver;

    private boolean mSearchEnabled;

    private boolean mPermissionRequested = true;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final long TAKE_PHOTO_FAB_SHOW_DELAY = 500;

    private static final int REQUEST_PERMISSIONS_TO_ACCESS_PHOTOS = 100;
    private static final int REQUEST_PERMISSIONS_TO_TAKE_PHOTO    = 101;

    public PhotoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mTimeRangeController = new TimeRangeController(this);

        // Should be triggered after we taking a new photo
        mPhotosObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (photos). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting photos cursor reload as a result of ContentObserver firing.");
                    reloadPhotosWithRequiredPermission();
                }
            }
        });
        activity.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mPhotosObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPhotosObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = 1;
        mTimeRangeController.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        PhotoCoordinatorLayout root = (PhotoCoordinatorLayout) inflater.inflate(R.layout.fragment_photo, container, false);

        mPhotoCollectionView = (CollectionView) root.findViewById(R.id.photos_view);
        mPhotoCollectionAdapter = new PhotoCollectionAdapter();
        mPhotoCollectionView.setCollectionAdapter(mPhotoCollectionAdapter);
        if (mPhotoInventory != null) {
            mPhotoCollectionView.updateInventory(mPhotoInventory, true);
        }

        mFabTakePhoto = (FloatingActionButton) root.findViewById(R.id.fab_take_photo);
        mFabTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!requestPhotoTakingPermissions()) {
                    takePhoto();
                }
            }
        });

        mPhotoCollectionView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mFabTakePhoto == null) {
                    return false;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mFabTakePhoto.removeCallbacks(mShowTakePhotoFab);
                        mFabTakePhoto.hide();
                        break;
                    case MotionEvent.ACTION_UP:
                        mFabTakePhoto.postDelayed(mShowTakePhotoFab, TAKE_PHOTO_FAB_SHOW_DELAY);
                        break;
                }
                return false;
            }
        });

        return root;
    }

    private Runnable mShowTakePhotoFab = new Runnable() {
        @Override
        public void run() {
            if (mFabTakePhoto != null) {
                mFabTakePhoto.show();
            }
        }
    };

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                // Error occurred while creating the File
                LOGE(TAG, "IOException while creating the file: " + e.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, TrackerActivity.REQUEST_PHOTO_CAPTURE);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_action, menu);

        if (mSearchEnabled) {
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            toolbar.addView(mTimeRangeController.getTimeRange(),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            menu.findItem(R.id.action_clear).setVisible(true);
            menu.findItem(R.id.action_search).setVisible(false);
        }

        mMenu = menu;
    }

    @Override
    public void onDestroyOptionsMenu() {
        if (mSearchEnabled) {
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            toolbar.removeView(mTimeRangeController.getTimeRange());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        switch (item.getItemId()) {
            case R.id.action_search: {
                mSearchEnabled = true;
                item.setVisible(false);
                toolbar.addView(mTimeRangeController.getTimeRange(),
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                mMenu.findItem(R.id.action_clear).setVisible(true);
                return true;
            }
            case R.id.action_clear: {
                mSearchEnabled = false;
                item.setVisible(false);
                toolbar.removeView(mTimeRangeController.getTimeRange());
                mMenu.findItem(R.id.action_search).setVisible(true);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mTimeRangeController.updateTimeRange();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadPhotosWithRequiredPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPhotosObserver.cancelPendingCallback();
        mTimeRangeController.saveState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFabTakePhoto != null) {
            mFabTakePhoto.removeCallbacks(mShowTakePhotoFab);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TrackerActivity.REQUEST_PHOTO_CAPTURE && resultCode == Activity.RESULT_OK) {
            galleryAddPhoto();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestPhotoTakingPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ArrayList<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.CAMERA);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // No explanation needed, we always have to request these permissions.
            requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS_TO_TAKE_PHOTO);

            return true;
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestPhotosAccessPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ArrayList<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (mPermissionRequested) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS_TO_ACCESS_PHOTOS);
                mPermissionRequested = false; // so the next onResume won't trigger another request
            }

            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // It is possible that the permissions request interaction with the user is interrupted.
        // In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
        if (permissions.length == 0) {
            if (requestCode == REQUEST_PERMISSIONS_TO_TAKE_PHOTO) {
                requestPhotoTakingPermissions();
            } else if (requestCode == REQUEST_PERMISSIONS_TO_ACCESS_PHOTOS) {
                requestPhotosAccessPermission();
            }
            return;
        }

        switch (requestCode) {
            case REQUEST_PERMISSIONS_TO_TAKE_PHOTO: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Toast.makeText(getActivity(), R.string.require_take_photo_permissions, Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_PERMISSIONS_TO_ACCESS_PHOTOS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    reloadPhotosWithRequiredPermission();
                } else {
                    Toast.makeText(getActivity(), R.string.require_photos_access_permission, Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        if (isAdded()) {
            mPermissionRequested = true;
            reloadPhotosWithRequiredPermission();
        }
    }

    @Override
    public void onFragmentUnselected() {
        super.onFragmentUnselected();
        if (isAdded()) {
            mFabTakePhoto.show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPhoto() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File image = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(image);
        mediaScanIntent.setData(contentUri);
        getActivity().sendOrderedBroadcast(mediaScanIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reloadPhotosWithRequiredPermission();
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                CollectionView.Inventory newInventory = Photo.photoInventoryFromCursor(data);
                if (newInventory != null && !Photo.containSamePhotos(mPhotoInventory, newInventory)) {
                    mPhotoInventory = newInventory;
                    updateInventoryDisplayColumns(mPhotoInventory);
                    mPhotoCollectionView.updateInventory(mPhotoInventory, true);
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    private void reloadPhotosWithRequiredPermission() {
        if (!requestPhotosAccessPermission()) {
            long beginTime = mTimeRangeController.getBeginDate().getMillis();
            long endtime = mTimeRangeController.getEndDate().getMillis();
            reloadPhotos(getLoaderManager(), beginTime, endtime, this);
        }
    }

    private void updateInventoryDisplayColumns(CollectionView.Inventory inventory) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        for (CollectionView.InventoryGroup group : inventory) {
            if (dpWidth < 400) {
                group.setDisplayCols(3);
            } else if (dpWidth < 600) {
                group.setDisplayCols(4);
            } else {
                group.setDisplayCols(5);
            }
        }
    }

    private ArrayList<Photo> inventoryToList(CollectionView.Inventory inventory) {
        ArrayList<Photo> photos = new ArrayList<>();
        for (CollectionView.InventoryGroup group : inventory) {
            for (int i = 0; i < group.getItemCount(); ++i) {
                Object itemTag = group.getItemTag(i);
                if (itemTag instanceof Photo) {
                    photos.add((Photo) itemTag);
                }
            }
        }
        return photos;
    }

    private class PhotoCollectionAdapter implements CollectionViewCallbacks {

        @Override
        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.photo_collection_header, parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel, Object headerTag) {
            TextView photoDate = (TextView) view.findViewById(R.id.photo_date);
            photoDate.setText(getDateLabel((DateTime) headerTag));
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.photo_collection_item, parent, false);
            final ItemViewHolder holder = new ItemViewHolder();
            holder.photoImage = (ImageView) view.findViewById(R.id.photo_image);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindCollectionItemView(final Context context, View view, int groupId, int indexInGroup, int dataIndex, Object itemTag) {
            final Object tag = view.getTag();
            if (tag instanceof ItemViewHolder) {
                final ItemViewHolder holder = (ItemViewHolder) tag;
                final Photo photo = (Photo) itemTag;
                if (holder.photoImage != null) {
                    holder.registerListener(context, view, photo);
                    Glide.with(context)
                            .load(photo.data)
                            .centerCrop()
                            .crossFade()
                            .into(holder.photoImage);
                }
            }
        }

        private String getDateLabel(final DateTime date) {
            if (date == null) {
                return "";
            }

            DateTime today = DateTime.now().withTimeAtStartOfDay();
            if (date.equals(today)) {
                return "Today";
            }

            DateTime yesterday = today.minusDays(1).withTimeAtStartOfDay(); // For daytime saving adjustment
            if (date.equals(yesterday)) {
                return "Yesterday";
            }

            StringBuilder sb = new StringBuilder()
                    .append(date.dayOfWeek().getAsText())
                    .append(", ")
                    .append(date.monthOfYear().getAsShortText())
                    .append(" ")
                    .append(date.dayOfMonth().getAsText());

            if (date.year().get() != today.year().get()) {
                sb.append(", ").append(date.year().getAsText());
            }

            return sb.toString();
        }

        private class ItemViewHolder {
            ImageView photoImage;

            void registerListener(final Context context, final View view, final Photo photo) {
                photoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent i = new Intent(context, PhotoDetailActivity.class);
                        i.putExtra(PhotoDetailActivity.EXTRA_SELECTED_PHOTO, photo);
                        i.putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_AVAILABLE_PHOTOS, inventoryToList(mPhotoInventory));
                        ActivityOptions options =
                                ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
                        context.startActivity(i, options.toBundle());
                    }
                });
            }
        }
    }
}
