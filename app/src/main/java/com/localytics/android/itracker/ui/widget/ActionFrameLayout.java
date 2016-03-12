package com.localytics.android.itracker.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Track;
import com.localytics.android.itracker.ui.OnTrackItemSelectedListener;

import org.joda.time.DateTime;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bbo on 1/15/16.
 */
public class ActionFrameLayout extends FrameLayout {

    private static final String TAG = makeLogTag(ActionFrameLayout.class);

    private boolean mDragging;

    private RecyclerView  mTracksView;
    private TimelinesView mTimelinesView;
    private MotionsView   mMotionsView;
    private FrameLayout   mSTContainer;
    private TextView      mShowTimelines;
    private TextView      mDatePopupView;

    private FloatingActionButton mTimelinesFab;
    private FloatingActionButton mFootprintFab;

    private ValueAnimator mDateViewShowAnimator;
    private ValueAnimator mDateViewHideAnimator;
    private ValueAnimator mDataViewCancelHideAnimator;

    private AnimatorSet mShowTimelinesAnimSet;
    private AnimatorSet mHideTimelinesAnimSet;

    private static final long DATE_VIEW_ANIM_TIME = 500;
    private static final long DATE_VIEW_HIDE_DELAY_TIME = 2000;
    private static final long TIMELINES_VIEW_SHOW_ANIM_TIME = 500;
    private static final long TIMELINES_VIEW_HIDE_ANIM_TIME = 500;
    private static final long TIMELINE_FAB_DOWN_ANIM_TIME = 600;
    private static final long TIMELINE_FAB_UP_ANIM_TIME = 600;
    private static final long FOOTPRINT_FAB_SHOW_DELAY = 500;

    private GestureDetectorCompat mDetector;

    private OnFootprintFabClickedListener mFootprintFabClickedListener;

    public ActionFrameLayout(Context context) {
        this(context, null);
    }

    public ActionFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(true);
    }

    public void setOnFootprintFabClickedListener(OnFootprintFabClickedListener listener) {
        mFootprintFabClickedListener = listener;
    }

    private OnTrackItemSelectedListener mOnTrackItemSelectedListener = new OnTrackItemSelectedListener() {
        @Override
        public void onTrackItemSelected(View itemView, int position) {
            TrackItemAdapter adapter = (TrackItemAdapter) mTracksView.getAdapter();
            Track selectedTrack = adapter.getItem(position);
            DateTime dateTime = new DateTime(selectedTrack.date);
            String date = new StringBuilder()
                    .append(dateTime.year().getAsText())
                    .append(", ")
                    .append(dateTime.monthOfYear().getAsText())
                    .append(" ")
                    .append(dateTime.dayOfMonth().getAsText())
                    .append(", ")
                    .append(dateTime.dayOfWeek().getAsShortText())
                    .toString();
            SpannableString text = new SpannableString(date);
            text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);

            if (mDatePopupView.getText().equals(text)) {
                showDateViewWithAnimation();
            } else {
                mDatePopupView.setText(text);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showDateViewWithAnimation();
                    }
                }, 50);
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mTracksView    = (RecyclerView) findViewById(R.id.tracks_view);
        mTimelinesView = (TimelinesView) findViewById(R.id.activities_view);
        mMotionsView   = (MotionsView) findViewById(R.id.motions_view);
        mSTContainer   = (FrameLayout) findViewById(R.id.show_timeline_container);
        mShowTimelines = (TextView) findViewById(R.id.show_timeline);
        mDatePopupView = (TextView) findViewById(R.id.date_popup_view);
        mTimelinesFab  = (FloatingActionButton) findViewById(R.id.fab_timeline);
        mFootprintFab  = (FloatingActionButton) findViewById(R.id.fab_footprint);

        TrackItemAdapter adapter = (TrackItemAdapter) mTracksView.getAdapter();
        adapter.addOnItemSelectedListener(mOnTrackItemSelectedListener);

        mShowTimelines.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrHideTimelinesWithAnimation();
            }
        });

        mFootprintFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFootprintFabClickedListener != null) {
                    mFootprintFabClickedListener.onFootprintFabClicked();
                }
            }
        });

        mDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent ev) {
                Rect hitRect = new Rect();
                mMotionsView.getGlobalVisibleRect(hitRect);
                if (hitRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    mFootprintFab.removeCallbacks(mShowFootprintFab);
                    mFootprintFab.hide();
                    showDateViewWithAnimation();
                }
                mTimelinesFab.getGlobalVisibleRect(hitRect);
                if (hitRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    mDragging = true;
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mDragging) {
                    LOGD(TAG, "onScroll - dx: " + distanceX + "\tdy: " + distanceY);
                    requestLayout();
                }
                return false;
            }
        });
    }

    private void showOrHideTimelinesWithAnimation() {
        if (mShowTimelinesAnimSet == null) {
            if (mHideTimelinesAnimSet != null) {
                mHideTimelinesAnimSet.cancel();
                mHideTimelinesAnimSet = null;
            }
            mShowTimelinesAnimSet = createShowTimelinesAnimatorSet();
            mShowTimelinesAnimSet.start();
        } else {
            mShowTimelinesAnimSet.cancel();
            mShowTimelinesAnimSet = null;
            mHideTimelinesAnimSet = createHideTimelinesAnimatorSet();
            mHideTimelinesAnimSet.start();
        }
    }

    private void showDateViewWithAnimation() {
        if (mDateViewHideAnimator != null && mDateViewHideAnimator.isStarted()) {
            mDateViewHideAnimator.cancel();
            mDataViewCancelHideAnimator = createShowDateViewAnimator(); // Start popping up from the current position.
            mDataViewCancelHideAnimator.start();
        } else if (mDataViewCancelHideAnimator != null && mDataViewCancelHideAnimator.isStarted()) {
            // Just wait
        } else {
            if (mDateViewShowAnimator == null) {
                mDateViewShowAnimator = createShowDateViewAnimator();
            }
            if (!mDateViewShowAnimator.isStarted()) {
                mDateViewShowAnimator.start();
            }
        }
    }

    private ValueAnimator createShowDateViewAnimator() {
        final ValueAnimator animator = ObjectAnimator.ofFloat(mDatePopupView, "y", dpToPx(8))
                .setDuration(DATE_VIEW_ANIM_TIME);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mDateViewHideAnimator = createHideDateViewAnimator();
                mDateViewHideAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animator;
    }

    private ValueAnimator createHideDateViewAnimator() {
        final ValueAnimator animator = ObjectAnimator.ofFloat(mDatePopupView, "y", -mDatePopupView.getHeight())
                .setDuration(DATE_VIEW_ANIM_TIME);
        animator.setStartDelay(DATE_VIEW_HIDE_DELAY_TIME);
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    private AnimatorSet createShowTimelinesAnimatorSet() {
        final float timelinesViewTranslationY = mSTContainer.getBottom() - mTimelinesView.getOverScrollView().getHeight();
        ObjectAnimator animTimelinesView = ObjectAnimator.ofFloat(mTimelinesView, "y", timelinesViewTranslationY)
                .setDuration(TIMELINES_VIEW_SHOW_ANIM_TIME);

        final float timelinesFabTranslationY = mSTContainer.getBottom() + mTimelinesView.getContentView().getHeight() - mTimelinesFab.getHeight() / 2;
        ObjectAnimator animTimelinesFab = ObjectAnimator.ofFloat(mTimelinesFab, "y", timelinesFabTranslationY)
                .setDuration(TIMELINE_FAB_DOWN_ANIM_TIME);

        mShowTimelines.setText(getContext().getString(R.string.hide_timeline));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(animTimelinesView, animTimelinesFab);

        return animatorSet;
    }

    private AnimatorSet createHideTimelinesAnimatorSet() {
        final float timelinesViewTranslationY = mSTContainer.getBottom() - mTimelinesView.getHeight();
        ObjectAnimator animTimelinesView = ObjectAnimator.ofFloat(mTimelinesView, "y", timelinesViewTranslationY)
                .setDuration(TIMELINES_VIEW_HIDE_ANIM_TIME);

        final float timelinesFabTranslationY = mSTContainer.getTop() + (mSTContainer.getHeight() - mTimelinesFab.getHeight()) * 0.5f;
        ObjectAnimator animTimelinesFab = ObjectAnimator.ofFloat(mTimelinesFab, "y", timelinesFabTranslationY)
                .setDuration(TIMELINE_FAB_UP_ANIM_TIME);

        mShowTimelines.setText(getContext().getString(R.string.show_timeline));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(animTimelinesView, animTimelinesFab);

        return animatorSet;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFootprintFab.removeCallbacks(mShowFootprintFab);  // Remove all message in the message queue to prevent memory leak
        mDatePopupView.removeCallbacks(null);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragging || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final boolean handled = mDetector.onTouchEvent(ev);
        if (handled) {
            return true;
        } else {
            if (MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_UP) {
                mFootprintFab.postDelayed(mShowFootprintFab, FOOTPRINT_FAB_SHOW_DELAY);
                mDragging = false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int parentWidth  = getMeasuredWidth();
        int parentHeight = getMeasuredHeight();

        /**
         * This code is simplified by assuming the child view sequence in the xml file
         * and there is no horizontal padding.
         */
        // Layout tracks view
        mTracksView.measure(
                MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Math.round(parentHeight * 0.5f), MeasureSpec.EXACTLY));
        int measuredHeight = mTracksView.getMeasuredHeight();
        mTracksView.layout(
                0,
                parentHeight - measuredHeight,
                parentWidth,
                parentHeight);

        // Layout timeline view
        mTimelinesView.measure(
                MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Math.round(parentHeight * 0.425f), MeasureSpec.EXACTLY));
        measuredHeight = mTimelinesView.getMeasuredHeight();
        mTimelinesView.layout(
                0,
                mTracksView.getTop() - measuredHeight,
                parentWidth,
                mTracksView.getTop());

        // Layout show timeline button
        mSTContainer.layout(
                0,
                mTracksView.getTop() - mSTContainer.getMeasuredHeight(),
                parentWidth,
                mTracksView.getTop());

        // Layout motions view
        mMotionsView.measure(
                MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(mTracksView.getTop() - mSTContainer.getMeasuredHeight(), MeasureSpec.EXACTLY));
        mMotionsView.layout(
                0,
                0,
                parentWidth,
                mMotionsView.getMeasuredHeight());

        // Layout timeline fab
        int fabHeightOffset = Math.round((mSTContainer.getHeight() - mTimelinesFab.getHeight()) * 0.5f);
        mTimelinesFab.layout(
                mTimelinesView.getLeft() + mTimelinesFab.getLeft(),
                mSTContainer.getTop() + fabHeightOffset,
                mTimelinesView.getLeft() + mTimelinesFab.getLeft() + mTimelinesFab.getWidth(),
                mSTContainer.getTop() + fabHeightOffset + mTimelinesFab.getHeight());

        // Layout footprint fab
        LayoutParams params = (LayoutParams) mFootprintFab.getLayoutParams();
        mFootprintFab.layout(
                parentWidth - mFootprintFab.getWidth() - params.getMarginEnd(),
                mMotionsView.getBottom() - mFootprintFab.getHeight() / 2,
                parentWidth - params.getMarginEnd(),
                mMotionsView.getBottom() + mFootprintFab.getHeight() / 2);

        // Layout date popup view
        mDatePopupView.layout(
                (parentWidth - mDatePopupView.getWidth()) / 2,
                -mDatePopupView.getHeight(),
                (parentWidth + mDatePopupView.getWidth()) / 2,
                0);
    }

    private Runnable mShowFootprintFab = new Runnable() {
        @Override
        public void run() {
            mFootprintFab.show();
        }
    };

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    public interface OnFootprintFabClickedListener {
        void onFootprintFabClicked();
    }
}
