package com.itracker.android.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class DarkFrameLayout extends FrameLayout {
    private static final String TAG = makeLogTag(DarkFrameLayout.class);

    public static final int MAX_ALPHA = 0X9f;

    private int mAlpha = 0x00;
    private Paint mFadePaint;

    private SlideBottomPanel mSlideBottomPanel;

    public DarkFrameLayout(Context context) {
        this(context, null);
    }

    public DarkFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DarkFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFadePaint = new Paint();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawFade(canvas);
    }

    private void drawFade(Canvas canvas) {
        mFadePaint.setColor(Color.argb(mAlpha, 0, 0, 0));
        canvas.drawRect(0, 0, getMeasuredWidth(), getHeight(), mFadePaint);
    }

    public void fade(boolean fade) {
        mAlpha = fade ? 0x8f : 0x00;
        invalidate();
    }

    public void fade(int alpha) {
        this.mAlpha = alpha;
        invalidate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mSlideBottomPanel.isPanelShowing();
    }

    public int getCurrentAlpha() {
        return mAlpha;
    }

    public void setSlideBottomPanel(SlideBottomPanel mSlideBottomPanel) {
        this.mSlideBottomPanel = mSlideBottomPanel;
    }
}