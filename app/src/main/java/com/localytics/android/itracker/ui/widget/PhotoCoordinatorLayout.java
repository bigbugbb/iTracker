package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

/**
 * Created by bigbug on 1/29/16.
 */
public class PhotoCoordinatorLayout extends CoordinatorLayout {
    public PhotoCoordinatorLayout(Context context) {
        this(context, null);
    }

    public PhotoCoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
