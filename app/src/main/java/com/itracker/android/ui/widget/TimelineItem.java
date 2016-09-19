package com.itracker.android.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itracker.android.R;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.itracker.android.data.model.Activity.activitiesFromCursor;
import static com.itracker.android.utils.LogUtils.makeLogTag;

public class TimelineItem extends LinearLayout {
    private static final String TAG = makeLogTag(TimelineItem.class);

    private int mDrawableWidth;
    private int mDrawableHeight;
    private int mLineWidth;
    private int mCircleRadius;
    private int mDrawablePosition;

    private Paint mNormalPaint;
    private Paint mSelectedTopLinePaint;
    private Paint mSelectedBottomLinePaint;
    private Paint mSelectedCirclePaint;

    private float mLineLeft;
    private float mLineRight;
    private float mTopLineBottom;
    private float mBottomLineTop;

    private boolean mLastItem;

    private static final int DEFAULT_DRAWABLE_WIDTH_IN_DP  = 12;
    private static final int DEFAULT_DRAWABLE_HEIGHT_IN_DP = 56;
    private static final int DEFAULT_LINE_WIDTH_IN_DP = 1;
    private static final int DEFAULT_CIRCLE_NODE_RADIUS_IN_DP = 4;

    private static final int DRAWABLE_POSITION_LEFT  = 0;
    private static final int DRAWABLE_POSITION_RIGHT = 1;

    private TextView mTimeRange;
    private TextView mActivity;

    private Drawable mNormalDrawable;
    private Drawable mSelectedDrawable;
    private Drawable mEmptyDrawable;

    public TimelineItem(Context context) {
        this(context, null);
    }

    public TimelineItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimelineItem, defStyleAttr, 0);

        mDrawableWidth  = a.getDimensionPixelSize(R.styleable.TimelineItem_drawableWidth, dpToPx(DEFAULT_DRAWABLE_WIDTH_IN_DP));
        mDrawableHeight = a.getDimensionPixelSize(R.styleable.TimelineItem_drawableHeight, dpToPx(DEFAULT_DRAWABLE_HEIGHT_IN_DP));
        mLineWidth = a.getDimensionPixelSize(R.styleable.TimelineItem_lineWidth, dpToPx(DEFAULT_LINE_WIDTH_IN_DP));
        mCircleRadius = a.getDimensionPixelSize(R.styleable.TimelineItem_circleNodeRadius, dpToPx(DEFAULT_CIRCLE_NODE_RADIUS_IN_DP));
        mDrawablePosition = a.getInt(R.styleable.TimelineItem_drawablePosition, DRAWABLE_POSITION_LEFT);

        a.recycle();

        mLineLeft = (mDrawableWidth - mLineWidth) * 0.5f;
        mLineRight = (mDrawableWidth + mLineWidth) * 0.5f;
        mTopLineBottom = mDrawableHeight * 0.5f - mCircleRadius;
        mBottomLineTop = mDrawableHeight * 0.5f + mCircleRadius;

        mNormalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNormalPaint.setColor(Color.GRAY);
        mNormalPaint.setStrokeWidth(dpToPx(1));

        int colorAccent = ContextCompat.getColor(context, R.color.colorAccent);
        mSelectedTopLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader shaderTop = new LinearGradient(mLineLeft, 0, mLineRight, mTopLineBottom, Color.GRAY, colorAccent, Shader.TileMode.CLAMP);
        mSelectedTopLinePaint.setShader(shaderTop);
        mSelectedBottomLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader shaderBottom = new LinearGradient(mLineLeft, mBottomLineTop, mLineRight, mDrawableHeight, colorAccent, Color.GRAY, Shader.TileMode.CLAMP);
        mSelectedBottomLinePaint.setShader(shaderBottom);

        mSelectedCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectedCirclePaint.setColor(colorAccent);
        mSelectedCirclePaint.setStyle(Paint.Style.FILL);
    }

    public void init() {
        mTimeRange = (TextView) findViewById(R.id.time_span);
        mActivity = (TextView) findViewById(R.id.detected_activity);
        mNormalDrawable = createTimelineNormalDrawable();
        mSelectedDrawable = createTimelineSelectedDrawable();
        mEmptyDrawable = new PaintDrawable(Color.TRANSPARENT);
    }

    public void populateView(Timeline timeline, boolean lastItem) {
        // Update the drawables because the last item doesn't have the bottom line.
        if (mLastItem != lastItem) {
            mLastItem = lastItem;
            mSelectedDrawable = createTimelineSelectedDrawable();
            mNormalDrawable = createTimelineNormalDrawable();
        }

        setTimeRange(timeline.getTimeRangeAsText());
        setActivityType(timeline.getActivityType());
        setTimelineDrawable(timeline.isSelected());
    }

    public void setTimeRange(String timeRange) {
        mTimeRange.setText(timeRange);
    }

    public void setActivityType(String activityType) {
        mActivity.setText(activityType);
    }

    public void setTimelineDrawable(boolean selected) {
        Drawable[] drawables = mTimeRange.getCompoundDrawables();
        Drawable target = (mDrawablePosition == DRAWABLE_POSITION_LEFT ? drawables[0] : drawables[2]);
        if (target == null) {
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{ mNormalDrawable, mEmptyDrawable });
            layerDrawable.setId(0, 0);
            layerDrawable.setId(1, 1);
            if (selected) {
                layerDrawable.setDrawableByLayerId(1, mSelectedDrawable);
            }
            mTimeRange.setCompoundDrawablesWithIntrinsicBounds(
                    mDrawablePosition == DRAWABLE_POSITION_LEFT ? layerDrawable : null,
                    null,
                    mDrawablePosition == DRAWABLE_POSITION_RIGHT ? layerDrawable : null,
                    null);
            layerDrawable.invalidateSelf();
        } else {
            LayerDrawable layerDrawable = (LayerDrawable) target;
            // When the item is the last, don't forget to change the normal drawable
            if (selected) {
                if (layerDrawable.getDrawable(0) != mNormalDrawable) {
                    layerDrawable.setDrawableByLayerId(0, mNormalDrawable);
                }
                if (layerDrawable.getDrawable(1) != mSelectedDrawable) {
                    layerDrawable.setDrawableByLayerId(1, mSelectedDrawable);
                }
            } else {
                if (layerDrawable.getDrawable(0) != mNormalDrawable) {
                    layerDrawable.setDrawableByLayerId(0, mNormalDrawable);
                }
                if (layerDrawable.getDrawable(1) != mEmptyDrawable) {
                    layerDrawable.setDrawableByLayerId(1, mEmptyDrawable);
                }
            }
            layerDrawable.invalidateSelf();
        }
    }

    private Drawable createTimelineNormalDrawable() {
        Context context = getContext();
        Bitmap bitmap = Bitmap.createBitmap(mDrawableWidth, mDrawableHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mNormalPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(mLineLeft, 0, mLineRight, mTopLineBottom, mNormalPaint);
        mNormalPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mDrawableWidth * 0.5f, mDrawableHeight * 0.5f, mCircleRadius, mNormalPaint);
        if (!mLastItem) {
            mNormalPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mLineLeft, mBottomLineTop, mLineRight, mDrawableHeight, mNormalPaint);
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private Drawable createTimelineSelectedDrawable() {
        Bitmap bitmap = Bitmap.createBitmap(mDrawableWidth, mDrawableHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(mLineLeft, 0, mLineRight, mTopLineBottom, mSelectedTopLinePaint);
        canvas.drawCircle(mDrawableWidth * 0.5f, mDrawableHeight * 0.5f, mCircleRadius + dpToPx(0.5f), mSelectedCirclePaint);
        if (!mLastItem) {
            canvas.drawRect(mLineLeft, mBottomLineTop, mLineRight, mDrawableHeight, mSelectedBottomLinePaint);
        }
        return new BitmapDrawable(getContext().getResources(), bitmap);
    }

    private int dpToPx(float dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    public static class Timeline {
        private DateTime mStartTime;
        private DateTime mStopTime;
        private com.itracker.android.data.model.Activity mActivity;
        private boolean mSelected;

        public Timeline(DateTime startTime, DateTime stopTime, com.itracker.android.data.model.Activity activity) {
            mStartTime = startTime;
            mStopTime = stopTime;
            mActivity = activity;
            mSelected = false;
        }

        public DateTime getStartTime() {
            return mStartTime;
        }

        public DateTime getStopTime() {
            return mStopTime;
        }

        public String getTimeRangeAsText() {
            return String.format("%02d:%02d %02d:%02d",
                    mStartTime.getHourOfDay(), mStartTime.getMinuteOfHour(), mStopTime.getHourOfDay(), mStopTime.getMinuteOfHour());
        }

        public String getActivityType() {
            return mActivity.type;
        }

        public void select(boolean selected) {
            mSelected = selected;
        }

        public boolean isSelected() {
            return mSelected;
        }

        public static List<Timeline> fromActivities(Cursor data) {
            List<Timeline> timelines = new ArrayList<>(32);
            com.itracker.android.data.model.Activity[] activities = activitiesFromCursor(data);

            if (activities != null) {
                boolean sameToAll = true;
                final long timeGapThreshold = DateUtils.MINUTE_IN_MILLIS * 10;
                for (int i = 0, start = 0; i < activities.length; ++i) {
                    String prevType = activities[start].type;
                    String currType = activities[i].type;

                    if ((i > 0 && Math.abs(activities[i - 1].time - activities[i].time) > timeGapThreshold) ||
                            !currType.equals(prevType)) {
                        if (i - start >= 2) {
                            DateTime startTime = new DateTime(activities[start].time);
                            DateTime stopTime = new DateTime(activities[i - 1].time);
                            Timeline timeline = new Timeline(startTime, stopTime, activities[i - 1]);
                            timelines.add(timeline);
                        }
                        start = i;
                    }

                    if (!currType.equals(prevType)) {
                        sameToAll = false;
                    }
                }

                if (sameToAll && activities.length >= 3) {
                    DateTime firstTime = new DateTime(activities[0].time);
                    DateTime lastTime = new DateTime(activities[activities.length - 1].time);
                    Timeline timeline = new Timeline(firstTime, lastTime, activities[activities.length - 1]);
                    timelines.add(timeline);
                }
            }

            return timelines;
        }
    }
}