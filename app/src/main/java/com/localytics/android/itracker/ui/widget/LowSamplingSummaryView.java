package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by bigbug on 10/24/15.
 */
/**
 * Sub-class of ImageView which automatically notifies the drawable when it is
 * being displayed.
 */
public class LowSamplingSummaryView extends ImageView {

    public LowSamplingSummaryView(Context context) {
        super(context);
    }

    public LowSamplingSummaryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LowSamplingSummaryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        final TypedArray a = context.obtainStyledAttributes(
//                attrs, R.styleable.MotionSummaryView, defStyleAttr, 0);
//
//        mImagePaddingTop = a.getInteger(R.styleable.MotionSummaryView_imagePaddingTop, 0);
//        mImagePaddingBottom = a.getInteger(R.styleable.MotionSummaryView_imagePaddingBottom, 0);
//
//        a.recycle();
    }

    /**
     * @see android.widget.ImageView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        setImageDrawable(null);

        super.onDetachedFromWindow();
    }
}
