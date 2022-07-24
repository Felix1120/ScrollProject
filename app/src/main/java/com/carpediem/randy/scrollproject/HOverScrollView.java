package com.carpediem.randy.scrollproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.OverScroller;

/**
 * Created by randy on 16-4-5.
 */
public class HOverScrollView extends View {

    private float mLastX = 0;
    private boolean mIsBeingDragged = false;
    private int mTouchSlop;
    private int mMinFlingSpeed;
    private int mMaxFlingSpeed;
    private int mOverFlingDistance;
    private int mOverScrollDistance;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;


    public HOverScrollView(Context context) {
        super(context);
        init(context);
    }

    public HOverScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinFlingSpeed = configuration.getScaledMinimumFlingVelocity();
        mMaxFlingSpeed = configuration.getScaledMaximumFlingVelocity();
        mOverFlingDistance = configuration.getScaledOverflingDistance();
        mOverScrollDistance = configuration.getScaledOverscrollDistance();
        mScroller = new OverScroller(context);

        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setWillNotDraw(false);
    }

    private void initVelocityTrackerIfNotExist() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    Paint paint = new Paint();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(getResources().getColor(R.color.white));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(dp2px(getContext(), 2));
        paint.setTextSize(dp2px(getContext(), 20));
        for (int index = 0; index < 100; index++) {
            canvas.drawText(String.valueOf(index), 150 * index, 150, paint);
            canvas.drawCircle(150 * index + 25, 250, 50, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScroller == null) {
            return false;
        }
        initVelocityTrackerIfNotExist();
        // ScrollView中设置了offsetLocation,这里需要设置吗？
        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = mScroller.isFinished();
                if (!mScroller.isFinished()) { //fling
                    mScroller.abortAnimation();
                }
                mLastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float deltaX = mLastX - x;

                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                    requestParentDisallowInterceptTouchEvent();
                    mIsBeingDragged = true;
                    // 减少滑动的距离
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    //直接滑动
                    overScrollBy((int) deltaX, 0, getScrollX(), 0, getScrollRange(), 0, mOverScrollDistance, 0, true);
                    mLastX = x;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                endDrag();
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingSpeed);
                    int initialVelocity = (int) mVelocityTracker.getXVelocity();
                    if (Math.abs(initialVelocity) > mMinFlingSpeed) {
                        // fling
                        doFling(-initialVelocity);
                    }
                    endDrag();
                }
                break;
            default:
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }


    private void doFling(int speed) {
        if (mScroller == null) {
            return;
        }
        mScroller.fling(getScrollX(), 0, speed, 0, 0, getScrollRange(), 0, 0);
        invalidate();
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
        mLastX = 0;
    }

    private void requestParentDisallowInterceptTouchEvent() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        Log.i("TEST", "onOverScrolled x" + scrollX + " y" + scrollY);
        if (!mScroller.isFinished()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            scrollTo(scrollX, scrollY);
            onScrollChanged(scrollX, scrollY, oldX, oldY);
            if (clampedY) {
                Log.i("TEST1", "springBack");
                mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            // TouchEvent中的overScroll调用
            super.scrollTo(scrollX, scrollY);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            int range = getScrollRange();
            if (oldX != x || oldY != y) {
                overScrollBy(x - oldX, y - oldY, oldX, oldY, range, 0, mOverFlingDistance, 0, false);
            }
        }
    }

    private int getScrollRange() {
        return dp2px(getContext(), 4800);
    }

    public static int dp2px(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

}
