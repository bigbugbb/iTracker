package com.localytics.android.itracker.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Photo;
import com.localytics.android.itracker.ui.widget.CollectionView;
import com.localytics.android.itracker.ui.widget.CollectionViewCallbacks;
import com.localytics.android.itracker.ui.widget.PhotoCoordinatorLayout;
import com.localytics.android.itracker.util.ThrottledContentObserver;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class PhotoFragment extends TrackerFragment implements
        OnTimeRangeChangedListener {

    private static final String TAG = makeLogTag(PhotoFragment.class);

    private CollectionView mPhotoCollectionView;
    private PhotoCollectionAdapter mPhotoCollectionAdapter;

    private String mCurrentPhotoPath;
    private FloatingActionButton mFabTakePhoto;

    private ThrottledContentObserver mPhotosObserver;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final long TAKE_PHOTO_FAB_SHOW_DELAY = 500;

    public PhotoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

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
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        PhotoCoordinatorLayout root = (PhotoCoordinatorLayout) inflater.inflate(R.layout.fragment_photo, container, false);
        mPhotoCollectionView = (CollectionView) root.findViewById(R.id.photos_view);
        mPhotoCollectionAdapter = new PhotoCollectionAdapter();
        mPhotoCollectionView.setCollectionAdapter(mPhotoCollectionAdapter);

        mFabTakePhoto = (FloatingActionButton) root.findViewById(R.id.fab_take_photo);
        mFabTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }
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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");
        reloadPhotosWithRequiredPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPhotosObserver.cancelPendingCallback();
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
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            galleryAddPhoto();
            reloadPhotosWithRequiredPermission();
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
        getActivity().sendBroadcast(mediaScanIntent);
    }

    @Override
    public void trackTimeRange(long beginTime, long endTime) {
        super.trackTimeRange(beginTime, endTime);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                CollectionView.Inventory photoInventory = Photo.photoInventoryFromCursor(data);
                if (photoInventory != null) {
                    updateInventoryDisplayColumns(photoInventory);
                    mPhotoCollectionView.updateInventory(photoInventory, true);
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

    @Override
    public void onBeginTimeChanged(long begin) {
        mBeginTime = begin;
        reloadPhotosWithRequiredPermission();
    }

    @Override
    public void onEndTimeChanged(long end) {
        mEndTime = end;
        reloadPhotosWithRequiredPermission();
    }

    private void reloadPhotosWithRequiredPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            reloadPhotos(getLoaderManager(), mBeginTime, mEndTime, this);
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

    private static class PhotoCollectionAdapter implements CollectionViewCallbacks {

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
        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object itemTag) {
            final Object tag = view.getTag();
            if (tag instanceof ItemViewHolder) {
                final ItemViewHolder holder = (ItemViewHolder) tag;
                final Photo photo = (Photo) itemTag;
                if (holder.photoImage != null) {
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

        private static class ItemViewHolder {
            ImageView photoImage;
        }
    }
}
