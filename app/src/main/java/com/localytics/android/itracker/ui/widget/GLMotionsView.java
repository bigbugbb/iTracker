package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Motion;
import com.localytics.android.itracker.ui.gles.SummaryRenderer;
import com.localytics.android.itracker.ui.gles.util.Geometry;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * Created by bbo on 12/21/15.
 */
public class GLMotionsView extends GLSurfaceView {

    private final static String TAG = makeLogTag(GLMotionsView.class);

    private static final float AXIS_X_MIN = 0f;
    private static final float AXIS_X_MAX = 1f;
    private static final float AXIS_Y_MIN = 0f;
    private static final float AXIS_Y_MAX = 1f;
    private static final float DEFAULT_GRAPH_PAGE_SIZE = 24f;

    private SummaryRenderer mRenderer;
    private OverScroller mScroller;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private float mGraphPageSize;
    private RectF mViewport;
    private RectF mPrevViewport;
    private Point mSurfaceSize;

    private float[] mData;

    private OnViewportChangedListener mListener;

    public GLMotionsView(Context context) {
        this(context, null);
    }

    public GLMotionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideBottomPanel, 0, 0);
        mGraphPageSize = a.getFloat(R.styleable.GLMotionsView_graphPageSize, DEFAULT_GRAPH_PAGE_SIZE);
        a.recycle();

        mData = new float[24 * 60 * Config.MONITORING_DURATION_IN_SECONDS];

        mScroller = new OverScroller(getContext());
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mViewport = new RectF(
                AXIS_X_MIN,
                AXIS_Y_MAX,
                AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN) / DEFAULT_GRAPH_PAGE_SIZE,
                AXIS_Y_MIN);
        mPrevViewport = new RectF(mViewport);

        mRenderer = new SummaryRenderer(getContext());
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void updateMotions(final Motion[] motions) {
        // Update the gl renderer with the summary data
        if (ViewCompat.isAttachedToWindow(GLMotionsView.this)) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    Motion.populateData(motions, mData, 0);
                    mRenderer.updateMotionGraph(mData, new Geometry.Point(0, 0, 0.011f), mGraphPageSize, 0.8f);
                    requestRender();
                }
            });
        }
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = getMeasuredWidth();
        setMeasuredDimension(measuredWidth, Math.round(measuredWidth * 0.618f));
    }

    private SimpleOnScaleGestureListener mScaleGestureListener = new SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }
    };

    private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            final float normalizedX = e.getX() / getWidth() * 2 - 1;
            final float normalizedY = -(e.getY() / getHeight() * 2 - 1);

            LOGD(TAG, String.format("normalizedX: %f, normalizedY: %f", normalizedX, normalizedY));

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.touchDown(normalizedX, normalizedY);
                    requestRender();
                }
            });

            mScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(GLMotionsView.this);
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            final float normalizedDistanceX = distanceX / getWidth() * 2;
            final float normalizedDistanceY = distanceY / getHeight() * 2;

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.scrollBy(-normalizedDistanceX, -normalizedDistanceY);
                    requestRender();
                }
            });

            float viewportStart = mViewport.left + mViewport.width() * distanceX / getWidth();
            viewportStart = Math.max(0f, Math.min(AXIS_X_MAX - mViewport.width(), viewportStart));

            mPrevViewport.set(mViewport);
            mViewport.set(viewportStart, 1f, viewportStart + mViewport.width(), 0f);

            if (mListener != null) {
                mListener.onViewportChanged(mPrevViewport, mViewport);
            }
//            LOGD(TAG, String.format("viewport: %s", mViewport.toString()));

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);
            return true;
        }
    };

    private void fling(int velocityX, int velocityY) {
        // Flings use math in pixels (as opposed to math based on the viewport).
        mSurfaceSize = surfaceSizeFromViewport();
        int startX = (int) (mSurfaceSize.x * (mViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
        int startY = (int) (mSurfaceSize.y * (mViewport.bottom - AXIS_Y_MIN) / (AXIS_Y_MAX - AXIS_Y_MIN));
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, mSurfaceSize.x - getWidth(),
                0, mSurfaceSize.y - getHeight(),
                0,
                0);

        // This call is needed for trigger computeScroll.
        ViewCompat.postInvalidateOnAnimation(this);
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
        if (mScroller.computeScrollOffset()) {
            int currX = mScroller.getCurrX();
            int lastX = Math.round(mSurfaceSize.x * (mViewport.left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN));
            final float normalizedDistanceX = (currX - lastX) * 1f / getWidth() * 2;

            float viewportStart = (AXIS_X_MAX - AXIS_X_MIN) * currX / mSurfaceSize.x;
            viewportStart = Math.max(0f, Math.min(AXIS_X_MAX - mViewport.width(), viewportStart));

            mPrevViewport.set(mViewport);
            mViewport.set(viewportStart, 1f, viewportStart + mViewport.width(), 0f);

            if (mListener != null) {
                mListener.onViewportChanged(mPrevViewport, mViewport);
            }

//            LOGD(TAG, String.format("viewport: %s currX: %d lastX: %d", mViewport.toShortString(), currX, lastX));

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.scrollBy(-normalizedDistanceX, 0f);
                    requestRender();
                }
            });

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private Point surfaceSizeFromViewport() {
        float surfaceSizeWith = getWidth() * (AXIS_X_MAX - AXIS_X_MIN) / mViewport.width();
        float surfaceSizeHeight = getHeight() * (AXIS_Y_MAX - AXIS_Y_MIN) / mViewport.height();
        return new Point((int) surfaceSizeWith, (int) surfaceSizeHeight);
    }

    public interface OnViewportChangedListener {
        void onViewportChanged(RectF prevViewport, RectF currViewport);
    }
}