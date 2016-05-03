package com.localytics.android.itracker.ui.widget.player;

import android.view.SurfaceHolder;
import android.view.View;

public class VideoSizeCalculator {

    private Dimens mDimens;

    private int mVideoWidth;
    private int mVideoHeight;

    public VideoSizeCalculator() {
        mDimens = new Dimens();
    }

    public void setVideoSize(int mVideoWidth, int mVideoHeight) {
        this.mVideoWidth = mVideoWidth;
        this.mVideoHeight = mVideoHeight;
    }

    public boolean hasValidSize() {
        return mVideoWidth > 0 && mVideoHeight > 0;
    }

    protected Dimens measure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = View.getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (hasValidSize()) {

            int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if (mVideoWidth * height < width * mVideoHeight) {
                    width = Math.round(height * mVideoWidth / (float) mVideoHeight);
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = Math.round(width * mVideoHeight / (float) mVideoWidth);
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = Math.round(width * mVideoHeight / (float) mVideoWidth);
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = Math.round(height * mVideoWidth / (float) mVideoHeight);
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = Math.round(height * mVideoWidth / (float) mVideoHeight);
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = Math.round(width * mVideoHeight / (float) mVideoWidth);
                }
            }
        }
        mDimens.width = width;
        mDimens.height = height;
        return mDimens;
    }

    public boolean isEqualToCurrentSize(int w, int h) {
        return mVideoWidth == w && mVideoHeight == h;
    }

    public void updateHolder(SurfaceHolder holder) {
        holder.setFixedSize(mVideoWidth, mVideoHeight);
    }

    public static class Dimens {
        int width;
        int height;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

}