package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.v13.view.ViewCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Motion;

import org.joda.time.DateTime;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * Created by bbo on 1/15/16.
 */
public class MotionsView extends View {
    private final static String TAG = makeLogTag(MotionsView.class);

    private static final float AXIS_X_MIN = 0f;
    private static final float AXIS_X_MAX = 1f;
    private static final float AXIS_Y_MIN = 0f;
    private static final float AXIS_Y_MAX = 1f;
    private static final float DEFAULT_GRAPH_PAGE_COUNT = 24f;

    private static final int PADDING_LEFT   = 40;
    private static final int PADDING_RIGHT  = 16;
    private static final int PADDING_TOP    = 16;
    private static final int PADDING_BOTTOM = 40;

    private static final int VIEWPORT_MOVE_DURATION = 500;

    private static final int DEFAULT_VERTICAL_CALIBRATION_COUNT = 5;
    private static final int DEFAULT_HORIZONTAL_CALIBRATION_COUNT = 7;

    private static final int DEFAULT_BASELINE_HEIGHT = 20;

    private static final String TEXT_MEASURE_TEMPLATE = "00:00";

    private OverScroller mScroller;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private float mGraphPageCount;
    private RectF mViewport;
    private RectF mPrevViewport;
    private Point mSurfaceSize;
    private Point mGraphWindowSize;
    private Rect  mPadding;
    private int   mVertCalibCount;
    private int   mHoriCalibCount;
    private float mBaseLineHeight;

    private Paint mBackgroundPaint;
    private Paint mBorderPaint;
    private Paint mGridPaint;
    private Paint mGraphPaint;
    private Paint mTextPaint;

    private float mTextWidth;
    private float mTextHeight;

    private float[] mData;

    private EdgeEffectCompat mEdgeEffectLeft;
    private EdgeEffectCompat mEdgeEffectRight;
    private boolean mEdgeEffectLeftActive;
    private boolean mEdgeEffectRightActive;

    private OnViewportChangedListener mListener;

    public interface OnMotionsUpdatedListener {
        void onMotionsUpdated();
    }

    public MotionsView(Context context) {
        this(context, null);
    }

    public MotionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MotionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);

        float density = getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideBottomPanel, 0, 0);
        mGraphPageCount = a.getFloat(R.styleable.MotionsView_graphPageCount, DEFAULT_GRAPH_PAGE_COUNT);
        mVertCalibCount = a.getInteger(R.styleable.MotionsView_vertCalibrationCount, DEFAULT_VERTICAL_CALIBRATION_COUNT);
        mHoriCalibCount = a.getInteger(R.styleable.MotionsView_horiCalibrationCount, DEFAULT_HORIZONTAL_CALIBRATION_COUNT);
        mBaseLineHeight = a.getInteger(R.styleable.MotionsView_baselineHeight, DEFAULT_BASELINE_HEIGHT);
        a.recycle();

        mScroller = new OverScroller(getContext());
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mPadding = new Rect(dpToPx(PADDING_LEFT), dpToPx(PADDING_TOP), dpToPx(PADDING_RIGHT), dpToPx(PADDING_BOTTOM));
        mViewport = new RectF(
                AXIS_X_MIN,
                AXIS_Y_MAX,
                AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN) / DEFAULT_GRAPH_PAGE_COUNT,
                AXIS_Y_MIN);
        mPrevViewport = new RectF(mViewport);

        mData = new float[24 * 60 * Config.MONITORING_DURATION_IN_SECONDS];

        mEdgeEffectLeft  = new EdgeEffectCompat(context);
        mEdgeEffectRight = new EdgeEffectCompat(context);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.motions_graph_background));

        mGridPaint = new Paint();
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setColor(ContextCompat.getColor(context, R.color.motions_graph_grid));
        mGridPaint.setStrokeWidth(0);  // hairline

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStrokeWidth(density * 2);
        mBorderPaint.setShadowLayer(density, density / 2, density / 2, Color.GRAY);

        mGraphPaint = new Paint();
        mGraphPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        mGraphPaint.setAntiAlias(true);
        mGraphPaint.setStrokeWidth(density);

        mTextPaint = new Paint();
        mTextPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        mTextPaint.setShadowLayer(density, density / 2, density / 2, Color.GRAY);

        Rect rect = new Rect();
        mTextPaint.getTextBounds(TEXT_MEASURE_TEMPLATE, 0, 1, rect);
        mTextWidth  = rect.width();
        mTextHeight = rect.height();
    }

    public void updateMotions(final Motion[] motions, final OnMotionsUpdatedListener listener) {
        new AsyncTask<Motion, Void, Void>() {

            @Override
            protected Void doInBackground(Motion... motions) {
                Motion.populateData(motions, mData, mBaseLineHeight);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (ViewCompat.isAttachedToWindow(MotionsView.this)) {
                    invalidate();
                    if (listener != null) {
                        listener.onMotionsUpdated();
                    }
                }
            }

        }.execute(motions);
    }

    public void moveViewport(DateTime startTime, DateTime stopTime) {
        float viewportStart = (startTime.getMinuteOfDay() - 1) * Config.MONITORING_DURATION_IN_SECONDS * 1f / mData.length;
        viewportStart = Math.max(0f, Math.min(AXIS_X_MAX - mViewport.width(), viewportStart));

        // Get start position and stop position in pixels
        int startX = (int) (mSurfaceSize.x * (mViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
        int startY = (int) (mSurfaceSize.y * (mViewport.bottom - AXIS_Y_MIN) / (AXIS_Y_MAX - AXIS_Y_MIN));
        int stopX = (int) (mSurfaceSize.x * (viewportStart - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
        int stopY = startY;

        releaseEdgeEffects();
        mScroller.startScroll(startX, startY, stopX - startX, stopY - startY, VIEWPORT_MOVE_DURATION);

        // This call is needed for trigger computeScroll.
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public RectF getCurrentViewport() {
        return mViewport;
    }

    public RectF getPreviousViewport() {
        return mPrevViewport;
    }

    public void setOnViewportChangedListener(OnViewportChangedListener listener) {
        mListener = listener;
    }

    private ScaleGestureDetector.SimpleOnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }
    };

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            releaseEdgeEffects();
            mScroller.forceFinished(true);
            getParent().requestDisallowInterceptTouchEvent(true);
            invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float viewportOffset = mViewport.width() * distanceX / mGraphWindowSize.x;
            float viewportStart = mViewport.left + viewportOffset;
            viewportStart = Math.max(0f, Math.min(AXIS_X_MAX - mViewport.width(), viewportStart));

            mPrevViewport.set(mViewport);
            mViewport.set(viewportStart, 1f, viewportStart + mViewport.width(), 0f); // Vertical scale is unsupported.

            if (mListener != null) {
                mListener.onViewportChanged(mPrevViewport, mViewport);
            }

            boolean canScrollX = mViewport.left > AXIS_X_MIN || mViewport.right < AXIS_X_MAX;
            int scrolledX = (int) (mSurfaceSize.x * (mPrevViewport.left + viewportOffset));
            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / (float) mGraphWindowSize.x);
                mEdgeEffectLeftActive = true;
            }
            if (canScrollX && scrolledX > mSurfaceSize.x - mGraphWindowSize.x) {
                mEdgeEffectRight.onPull((scrolledX - (mSurfaceSize.x - mGraphWindowSize.x)) / (float) mGraphWindowSize.x);
                mEdgeEffectRightActive = true;
            }

            invalidate();

//            LOGD(TAG, String.format("viewport: %s", mViewport.toString()));

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);
            return true;
        }
    };

    private void releaseEdgeEffects() {
        mEdgeEffectLeftActive = mEdgeEffectRightActive = false;
        mEdgeEffectLeft.onRelease();
        mEdgeEffectRight.onRelease();
    }

    private void fling(int velocityX, int velocityY) {
        releaseEdgeEffects();
        // Flings use math in pixels (as opposed to math based on the viewport).
        int startX = (int) (mSurfaceSize.x * (mViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
        int startY = (int) (mSurfaceSize.y * (mViewport.bottom - AXIS_Y_MIN) / (AXIS_Y_MAX - AXIS_Y_MIN));
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, mSurfaceSize.x - mGraphWindowSize.x,
                0, mSurfaceSize.y - mGraphWindowSize.y,
                mGraphWindowSize.x / 2,
                0);

        // This call is needed for trigger computeScroll.
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mGraphWindowSize = getGraphWindowSize();
        mSurfaceSize = surfaceSizeFromViewport();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }

    /**
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a {@link Scroller Scroller}
     * object.
     */
    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            int currX = mScroller.getCurrX();
            int lastX = Math.round(mSurfaceSize.x * (mViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
            float viewportStart = (AXIS_X_MAX - AXIS_X_MIN) * currX / mSurfaceSize.x;
            viewportStart = Math.max(0f, Math.min(AXIS_X_MAX - mViewport.width(), viewportStart));

            mPrevViewport.set(mViewport);
            mViewport.set(viewportStart, 1f, viewportStart + mViewport.width(), 0f);

            if (mListener != null) {
                mListener.onViewportChanged(mPrevViewport, mViewport);
            }
//            LOGD(TAG, String.format("viewport: %s currX: %d lastX: %d", mViewport.toShortString(), currX, lastX));

            boolean canScrollX = (mViewport.left > AXIS_X_MIN || mViewport.right < AXIS_X_MAX);

            if (canScrollX
                    && currX < 0
                    && mEdgeEffectLeft.isFinished()
                    && !mEdgeEffectLeftActive) {
                mEdgeEffectLeft.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectLeftActive = true;
            } else if (canScrollX
                    && currX > (mSurfaceSize.x - mGraphWindowSize.x)
                    && mEdgeEffectRight.isFinished()
                    && !mEdgeEffectRightActive) {
                mEdgeEffectRight.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectRightActive = true;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void onDraw(Canvas c) {
        // Draw graph background
        c.drawRect(mPadding.left, mPadding.top, mPadding.left + mGraphWindowSize.x, mPadding.top + mGraphWindowSize.y, mBackgroundPaint);

        c.save();
            int begin = Math.round(mViewport.left * mData.length);
            int end = Math.round(mViewport.right * mData.length);
            float scaleX = mViewport.width() * mSurfaceSize.x / (end - begin);
            float scaleY = -mGraphWindowSize.y * 1f / Config.ACCELEROMETER_DATA_MAX_MAGNITUDE;
            c.translate(mPadding.left, getHeight() - mPadding.bottom);

            // Draw vertical grid lines and calibration texts
            for (int i = 0; i <= mVertCalibCount; ++i) {
                float x = -mPadding.left / 2;
                float y = -mGraphWindowSize.y / (float) mVertCalibCount * i;
                c.drawLine(0, y, mGraphWindowSize.x, y, mGridPaint);
                c.drawText(Integer.toString(
                        (int) (100f / mVertCalibCount * i)),
                        x,
                        y + mTextHeight / 2,
                        mTextPaint);
            }

            // Draw horizontal grid lines and calibration texts
            int stepInMinute = Math.round((end - begin) / 60f);
            for (int i = begin; i < end; ++i) {
                int minute = Math.round(i * 1f / Config.MONITORING_DURATION_IN_SECONDS);
                if (minute % stepInMinute == 0) {
                    final String time = String.format("%02d:%02d", minute / 60, minute % 60);
                    float x = (i - begin) * scaleX;
                    float y = dpToPx(20);
                    c.drawLine(x, 0, x, -mGraphWindowSize.y, mGridPaint);
                    c.drawText(time, x, y, mTextPaint);
                    i += (stepInMinute - 1) * Config.MONITORING_DURATION_IN_SECONDS;  // minus 1 in case we are jumping so much
                }
            }

            c.save();
                c.scale(scaleX, scaleY);
                for (int i = 0, j = begin; j < end - 1; ++i, ++j) {
                    // Draw each data point
                    c.drawLine(i, mData[j], i + 1, mData[j + 1], mGraphPaint);
                }
            c.restore();

            // Draw edge effect
            boolean needsInvalidate = false;
            if (!mEdgeEffectLeft.isFinished()) {
                final int restoreCount = c.save();
                c.rotate(-90, 0, 0);
                mEdgeEffectLeft.setSize(mGraphWindowSize.y, mGraphWindowSize.x);
                if (mEdgeEffectLeft.draw(c)) {
                    needsInvalidate = true;
                }
                c.restoreToCount(restoreCount);
            }

            if (!mEdgeEffectRight.isFinished()) {
                final int restoreCount = c.save();
                c.translate(mGraphWindowSize.x, -mGraphWindowSize.y);
                c.rotate(90, 0, 0);
                mEdgeEffectRight.setSize(mGraphWindowSize.y, mGraphWindowSize.x);
                if (mEdgeEffectRight.draw(c)) {
                    needsInvalidate = true;
                }
                c.restoreToCount(restoreCount);
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        c.restore();

        // Draw graph border
        c.drawRect(mPadding.left, mPadding.top, mPadding.left + mGraphWindowSize.x, mPadding.top + mGraphWindowSize.y, mBorderPaint);
    }

    private Point getGraphWindowSize() {
        int horzPadding = PADDING_LEFT + PADDING_RIGHT;
        int vertPadding = PADDING_TOP + PADDING_BOTTOM;
        return new Point(getWidth() - dpToPx(horzPadding), getHeight() - dpToPx(vertPadding));
    }

    private Point surfaceSizeFromViewport() {
        Point graphWindowSize = getGraphWindowSize();
        float surfaceSizeWith   = graphWindowSize.x * (AXIS_X_MAX - AXIS_X_MIN) / mViewport.width();
        float surfaceSizeHeight = graphWindowSize.y * (AXIS_Y_MAX - AXIS_Y_MIN) / mViewport.height();
        return new Point(Math.round(surfaceSizeWith), Math.round(surfaceSizeHeight));
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    public interface OnViewportChangedListener {
        void onViewportChanged(RectF prevViewport, RectF currViewport);
    }
}
