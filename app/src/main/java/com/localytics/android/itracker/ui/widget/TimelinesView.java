package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.util.LogUtils;

import java.util.ArrayList;

/**
 * Created by bbo on 1/15/16.
 */
public class TimelinesView extends RelativeLayout {
    public static final String TAG = LogUtils.makeLogTag(TimelinesView.class);

    private final int DEFAULT_OVERSCROLL_SIZE = 32;

    private View     mOverScrollView;
    private ListView mContentView;

    private int mOverScrollSize;
    private Drawable mOverScrollBackground;

    public TimelinesView(Context context) {
        this(context, null);
    }

    public TimelinesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelinesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimelinesView, defStyleAttr, 0);

        mOverScrollSize = a.getDimensionPixelSize(R.styleable.TimelinesView_overScrollSize, dpToPx(context, DEFAULT_OVERSCROLL_SIZE));
        mOverScrollBackground = a.getDrawable(R.styleable.TimelinesView_overScrollBackground);

        a.recycle();

        initViews();
    }

    public ListView getContentView() {
        return mContentView;
    }

    public View getOverScrollView() {
        return mOverScrollView;
    }

    private void initViews() {
        mOverScrollView = new View(getContext());
        mOverScrollView.setId(generateViewId());

        mContentView = new ListView(getContext());
        mContentView.setId(generateViewId());
        mContentView.setDivider(null);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mOverScrollSize);
        params.addRule(ALIGN_PARENT_TOP);
        mOverScrollView.setLayoutParams(params);
        mOverScrollView.setBackground(mOverScrollBackground);

        params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.addRule(BELOW, mOverScrollView.getId());
        params.addRule(ALIGN_PARENT_BOTTOM);
        mContentView.setLayoutParams(params);

        addView(mOverScrollView);
        addView(mContentView);
    }

    public static class TimelineItemAdapter extends ArrayAdapter<TimelineItem.Timeline> {

        public TimelineItemAdapter(Context context) {
            super(context, 0, new ArrayList<TimelineItem.Timeline>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimelineItem itemView = (TimelineItem) convertView;
            if (itemView == null) {
                itemView = (TimelineItem) LayoutInflater.from(getContext()).inflate(R.layout.item_timeline, parent, false);
                itemView.init();
            }
            itemView.populateView(getItem(position), position == getCount() - 1);
            return itemView;
        }

        public void selectItem(int position) {
            for (int i = 0; i < getCount(); ++i) {
                TimelineItem.Timeline timeline = getItem(i);
                timeline.select(i == position);
            }
        }
    }

    private static int dpToPx(Context context, int dps) {
        return Math.round(context.getResources().getDisplayMetrics().density * dps);
    }
}
