package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bbo on 1/29/16.
 */
public class PhotoFrameLayout extends CoordinatorLayout {
    public final static String TAG = makeLogTag(PhotoFrameLayout.class);

    public PhotoFrameLayout(Context context) {
        this(context, null);
    }

    public PhotoFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
