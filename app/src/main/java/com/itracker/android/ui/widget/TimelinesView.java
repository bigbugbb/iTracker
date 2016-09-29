package com.itracker.android.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.itracker.android.R;
import com.itracker.android.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by bbo on 1/15/16.
 */
public class TimelinesView extends RelativeLayout {
    public static final String TAG = LogUtils.makeLogTag(TimelinesView.class);

    private final int DEFAULT_OVERSCROLL_SIZE = 32;

    private View     mOverScrollView;
    private TextView mEmptyView;
    private ListView mListView;
    private View     mShadowOverlay;

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

    public ListView getListView() {
        return mListView;
    }

    public View getContentView() {
        ListAdapter adapter = mListView.getAdapter();
        if (adapter != null && adapter.getCount() > 0) {
            return mListView;
        }
        return mEmptyView;
    }

    public View getOverScrollView() {
        return mOverScrollView;
    }

    private void initViews() {
        mOverScrollView = new View(getContext());
        mOverScrollView.setId(generateViewId());

        int padding = dpToPx(getContext(), 16);
        mEmptyView = new TextView(getContext());
        mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mEmptyView.setId(android.R.id.empty);
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setPadding(padding, padding, padding, padding);
        mEmptyView.setText(R.string.need_time_for_timelines);

        mListView = new ListView(getContext());
        mListView.setId(generateViewId());
        mListView.setDivider(null);
        mListView.setEmptyView(mEmptyView);

        mShadowOverlay = new View(getContext());
        mShadowOverlay.setId(generateViewId());

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mOverScrollSize);
        params.addRule(ALIGN_PARENT_TOP);
        mOverScrollView.setLayoutParams(params);
        mOverScrollView.setBackground(mOverScrollBackground);

        params = new LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(getContext(), 16));
        params.addRule(ALIGN_PARENT_BOTTOM);
        mShadowOverlay.setLayoutParams(params);
        mShadowOverlay.setBackground(getResources().getDrawable(R.drawable.timelinesview_bottom_shadow));

        params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.addRule(BELOW, mOverScrollView.getId());
        params.addRule(ALIGN_PARENT_BOTTOM);
        mListView.setLayoutParams(params);
        mEmptyView.setLayoutParams(params);

        addView(mOverScrollView);
        addView(mListView);
        addView(mEmptyView);
        addView(mShadowOverlay);
    }

    public static class TimelineItemAdapter extends ArrayAdapter<TimelineItem.Timeline> {

        public TimelineItemAdapter(Context context) {
            super(context, 0, new ArrayList<>());
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
