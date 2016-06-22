package com.localytics.android.itracker.download;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by bigbug on 6/16/16.
 */
class FileDownloadRequest implements Parcelable {

    public enum RequestAction {
        START  ("start"),
        PAUSE  ("pause"),
        CANCEL ("cancel");

        private final String mAction;

        RequestAction(String action) {
            mAction = action;
        }

        public String value() {
            return mAction;
        }
    }

    String mId;
    RequestAction mAction;
    Uri mSrcUri;
    Uri mDestUri;

    private FileDownloadRequest(Builder builder) {
        mId = builder.mId;
        mAction = builder.mAction;
        mSrcUri = builder.mSrcUri;
        mDestUri = builder.mDestUri;
    }

    public static class Builder {
        String mId;
        RequestAction mAction;
        Uri mSrcUri;
        Uri mDestUri;

        public Builder() {}

        public Builder setRequestId(final String id) {
            mId = id;
            return this;
        }

        public Builder setRequestAction(final RequestAction action) {
            mAction = action;
            return this;
        }

        public Builder setSourceUri(final Uri srcUri) {
            mSrcUri = srcUri;
            return this;
        }

        public Builder setDestinationUri(final Uri destUri) {
            mDestUri = destUri;
            return this;
        }

        public FileDownloadRequest build() {
            if (TextUtils.isEmpty(mId)) {
                throw new IllegalArgumentException("Request id cannot be empty");
            }

            if (mAction == null) {
                throw new IllegalArgumentException("Request action must be set");
            }

            if (mAction != RequestAction.START && mAction != RequestAction.PAUSE && mAction != RequestAction.CANCEL) {
                throw new IllegalArgumentException("Request action cannot be recognized");
            }

            if (mDestUri == null) {
                throw new IllegalArgumentException("Destination file location must be specified");
            }

            return new FileDownloadRequest(this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mAction.value());
        dest.writeParcelable(mSrcUri, 0);
        dest.writeParcelable(mDestUri, 0);
    }

    private FileDownloadRequest(Parcel in) {
        mId = in.readString();
        mAction = RequestAction.valueOf(in.readString());
        mSrcUri = in.readParcelable(Uri.class.getClassLoader());
        mDestUri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Parcelable.Creator<FileDownloadRequest> CREATOR = new Parcelable.Creator<FileDownloadRequest>() {

        public FileDownloadRequest createFromParcel(Parcel source) {
            return new FileDownloadRequest(source);
        }

        public FileDownloadRequest[] newArray(int size) {
            return new FileDownloadRequest[size];
        }
    };
}
