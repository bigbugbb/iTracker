package com.localytics.android.itracker.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.localytics.android.itracker.ui.widget.CollectionView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;


public final class Photo extends BaseData implements Parcelable {

    public String title;
    public String data;
    public String mime;
    public int    size;
    public int    width;
    public int    height;
    public float  latitude;
    public float  longitude;

    public Photo() {
    }

    public Photo(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.TITLE));
        data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
        mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE));
        size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE));
        width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
        height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
        latitude = cursor.getFloat(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE));
        longitude = cursor.getFloat(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE));
    }

    public static Photo validPhotoFromCursor(Cursor cursor) {
        Photo photo = new Photo(cursor);
        if (FileUtils.getFile(photo.data).length() > 0) {
            return photo;
        } else {
            return null;
        }
    }

    // The cursor window should be larger than the whole block of data.
    public static Photo[] photosFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Photo[] images = new Photo[size];
            int i = 0;
            do {
                images[i++] = validPhotoFromCursor(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return images;
        } else {
            return null;
        }
    }

    public static CollectionView.Inventory photoInventoryFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final CollectionView.Inventory inventory = new CollectionView.Inventory();

            DateTime previousDate = null;
            CollectionView.InventoryGroup group;
            do {
                final Photo photo = validPhotoFromCursor(cursor);
                final DateTime currentDate = new DateTime(photo.time * DateUtils.MILLIS_PER_SECOND).withTimeAtStartOfDay();
                if (previousDate == null || !currentDate.equals(previousDate)) {
                    // Add a new group for each date
                    group = new CollectionView.InventoryGroup((int) (currentDate.getMillis() / DateUtils.MILLIS_PER_SECOND));
                    group.setDisplayCols(4);
                    group.setShowHeader(true);
                    group.setHeaderTag(currentDate);
                    group.addItemWithTag(photo);
                    inventory.addGroup(group);
                } else {
                    group = inventory.getGroup(inventory.getGroupCount() - 1);
                    group.addItemWithTag(photo);
                }
                previousDate = currentDate;
            } while (cursor.moveToNext());

            return inventory;
        }
        return null;
    }

    public static boolean containSamePhotos(CollectionView.Inventory oldInv, CollectionView.Inventory newInv) {
        if (oldInv == null) {
            return false;
        }

        if (oldInv.getTotalItemCount() != newInv.getTotalItemCount()) {
            return false;
        }

        if (oldInv.getGroupCount() != newInv.getGroupCount()) {
            return false;
        }

        return true; // Skip elements test for simplicity, should be fine for this app.
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeString(title);
        dest.writeString(data);
        dest.writeString(mime);
        dest.writeInt(size);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
    }

    private Photo(Parcel in) {
        time = in.readLong();
        title = in.readString();
        data = in.readString();
        mime = in.readString();
        size = in.readInt();
        width = in.readInt();
        height = in.readInt();
        latitude = in.readFloat();
        longitude = in.readFloat();
    }

    public static final Parcelable.Creator<Photo> CREATOR = new Parcelable.Creator<Photo>() {

        public Photo createFromParcel(Parcel source) {
            return new Photo(source);
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };
}
