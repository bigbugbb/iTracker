package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by bigbug on 11/7/15.
 */
public class ScrollHideFABBehavior extends FloatingActionButton.Behavior {

    private Runnable mAutoShow;

    public ScrollHideFABBehavior(Context context, @Nullable AttributeSet attrs) {
        super();
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, final FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        if (nestedScrollAxes == ViewCompat.SCROLL_AXIS_HORIZONTAL) {
            if (mAutoShow == null) {
                mAutoShow = new Runnable() {
                    @Override
                    public void run() {
                        child.show();
                    }
                };
            }
            child.removeCallbacks(mAutoShow);
            child.hide();
            return true;
        }
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, final FloatingActionButton child, View target) {
        super.onStopNestedScroll(coordinatorLayout, child, target);
        child.postDelayed(mAutoShow, 500);
    }
}
