package com.laowch.dragtosort;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by lao on 14/12/20.
 */
public class DraggableImageLayout extends LinearLayout implements View.OnLongClickListener {

    private ScrollView mScrollView;

    private View mAddPictureView;


    private int mDownX = -1;
    private int mDownY = -1;

    private int mOffsetX = 0;
    private int mOffsetY = 0;


    private int mLastEventY = -1;

    private final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;

    private ImageView mHoverCell;
    private Rect mHoverCellCurrentBounds;
    private Rect mHoverCellOriginalBounds;

    private boolean mCellIsMobile = false;
    private boolean mIsMobileScrolling = false;
    private int mSmoothScrollAmountAtEdge = 0;

    private final int INVALID_POSITION = -1;

    private int mMobilePosition = INVALID_POSITION;

    final HashMap<Integer, Rect> childBounds = new HashMap<Integer, Rect>();


    public DraggableImageLayout(Context context) {
        super(context);
        init(context);
    }

    public DraggableImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DraggableImageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        setOnLongClickListener(this);

    }

    @Override
    public boolean onLongClick(View v) {

        mCellIsMobile = true;
        requestDisallowInterceptTouchEvent(true);

        // calculate scale ratio
        float viewPortHeight = DisplayUtils.getScreenHeight(getContext()) - getResources().getDimensionPixelSize(R.dimen.draggable_image_vertical_padding) * 2;
        final float ratio;
        if (viewPortHeight / this.getHeight() > 1) {
            ratio = 1;
        } else if (viewPortHeight / this.getHeight() < 0.3f) {
            ratio = 0.3f;
        } else {
            ratio = viewPortHeight / this.getHeight();
        }

        // calculate mMobilePosition and record startBounds

        int totalY = getPaddingTop();

        final int scrollY = mScrollView.getScrollY();


        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            totalY += child.getHeight();
            if (totalY > mDownY && mMobilePosition == INVALID_POSITION) {
                mMobilePosition = i;
            }

            Rect startRect = new Rect(child.getLeft(), child.getTop() - scrollY, child.getRight(),
                    child.getBottom() - scrollY);
            childBounds.put(i, startRect);
        }

        final View selectedView = getChildAt(mMobilePosition);
        selectedView.setAlpha(0.5f);

        // change layout

        mAddPictureView.setVisibility(View.GONE);
        this.setPadding(0, 0, 0, 0);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.getLayoutParams().width = (int) (child.getLayoutParams().width * ratio);
            child.getLayoutParams().height = (int) (child.getLayoutParams().height * ratio);
        }

        requestLayout();
        invalidate();

        // anim
        final ArrayList<Animator> animations = new ArrayList<Animator>();


        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                initHoverCell((ImageView) selectedView, animations, ratio, scrollY);

                int scrollY = mScrollView.getScrollY();

                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    Rect startRect = childBounds.get(i);
                    int startTop = startRect.top;


                    int delta = (int) (startTop - child.getTop() + child.getHeight() * (1 / ratio - 1) / 2) + scrollY;
                    if (delta != 0) {
                        ObjectAnimator yTransAnim = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, delta, 0);
                        ObjectAnimator xScaleAnim = ObjectAnimator.ofFloat(child, View.SCALE_X, 1 / ratio, 1);
                        ObjectAnimator yScaleAnim = ObjectAnimator.ofFloat(child, View.SCALE_Y, 1 / ratio, 1);
                        animations.add(yTransAnim);
                        animations.add(xScaleAnim);
                        animations.add(yScaleAnim);
                    }
                }

                AnimatorSet set = new AnimatorSet();

                set.setDuration(300);
                set.playTogether(animations);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        childBounds.clear();
                    }
                });
                set.start();

                return true;
            }
        });


        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                mDownY = (int) event.getY();
                mActivePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER_ID) {
                    break;
                }

                int pointerIndex = event.findPointerIndex(mActivePointerId);

                mLastEventY = (int) event.getY(pointerIndex);
                int deltaY = mLastEventY - mDownY;

                if (mCellIsMobile) {
//                    mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
//                            mHoverCellOriginalBounds.top + deltaY + mOffsetY);

                    moveHoverCell(event.getX(), event.getY());
                    invalidate();

                    handleCellSwitch();

                    mIsMobileScrolling = false;
                    handleMobileCellScroll();

                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                touchEventsEnded();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchEventsCancelled();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                /* If a multitouch event took place and the original touch dictating
                 * the movement of the hover cell has ended, then the dragging event
                 * ends and the hover cell is animated to its corresponding position
                 * in the listview. */
                pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                        MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    touchEventsEnded();
                }
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    private void handleMobileCellScroll() {

    }

    private void handleCellSwitch() {
    }

    private void touchEventsCancelled() {

    }

    private void touchEventsEnded() {

        if (mMobilePosition == INVALID_POSITION) {
            return;
        }


        final View selectedView = getChildAt(mMobilePosition);
        releaseHoverCell(selectedView);


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    /**
     * Creates the hover cell with the appropriate bitmap and of appropriate
     * size. The hover cell's BitmapDrawable is drawn on top of the bitmap every
     * single time an invalidate call is made.
     */
    private void initHoverCell(ImageView imageView, ArrayList<Animator> animations, float ratio, int scrollY) {
        int w = imageView.getWidth();
        int h = imageView.getHeight();
        int top = imageView.getTop();
        int left = imageView.getLeft();

        mHoverCell.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(w, h);
        mHoverCell.setLayoutParams(layoutParams);
        mHoverCell.setTranslationX(left);
        mHoverCell.setTranslationY(top);
        mHoverCell.setScaleType(ImageView.ScaleType.CENTER_INSIDE);


        Drawable drawable = imageView.getDrawable();

        mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
        mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

        mHoverCell.setImageDrawable(drawable);


        // animation

        ObjectAnimator transX = ObjectAnimator.ofFloat(mHoverCell, View.TRANSLATION_X, left, mDownX - w / 2);
        animations.add(transX);


        int rawY = mDownY - scrollY;
        ObjectAnimator transY = ObjectAnimator.ofFloat(mHoverCell, View.TRANSLATION_Y, top, rawY - h / 2);
        animations.add(transY);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mHoverCell, View.SCALE_X, 1 / ratio, 1);
        animations.add(scaleX);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mHoverCell, View.SCALE_Y, 1 / ratio, 1);
        animations.add(scaleY);


    }


    private void releaseHoverCell(final View selectedView) {

        mHoverCell.animate().translationX(selectedView.getLeft()).translationY(selectedView.getTop()).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mHoverCell.setVisibility(GONE);
                selectedView.setAlpha(1);
                expandImages();
            }
        }).start();

    }

    private void expandImages() {


        requestDisallowInterceptTouchEvent(false);

        final int lastMobilePosition = mMobilePosition;


        mCellIsMobile = false;
        mMobilePosition = INVALID_POSITION;

        final float ratio = DisplayUtils.getScreenWidth(getContext()) / (float) getChildAt(0).getWidth();

        // record startBounds


        final int scrollY = mScrollView.getScrollY();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            Rect startRect = new Rect(child.getLeft(), child.getTop() - scrollY, child.getRight(),
                    child.getBottom() - scrollY);
            childBounds.put(i, startRect);
        }


        // set ratio

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.getLayoutParams().width = (int) (child.getLayoutParams().width * ratio);
            child.getLayoutParams().height = (int) (child.getLayoutParams().height * ratio);
        }

        mAddPictureView.setVisibility(View.VISIBLE);
        this.setPadding(0, DisplayUtils.dpToPixel(getContext(), 90), 0, 0);


        requestLayout();
        invalidate();

        //anim

        final ArrayList<Animator> animations = new ArrayList<Animator>();

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);


                View view = getChildAt(lastMobilePosition);
                mScrollView.setScrollY(view.getTop());


                int scrollY = mScrollView.getScrollY();


                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    Rect startRect = childBounds.get(i);
                    int startTop = startRect.top;


                    int delta = (int) (startTop - child.getTop() + child.getHeight() * (1 / ratio - 1) / 2) + scrollY;

                    if (delta != 0) {
                        ObjectAnimator yTransAnim = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, delta, 0);
                        ObjectAnimator xScaleAnim = ObjectAnimator.ofFloat(child, View.SCALE_X, 1 / ratio, 1);
                        ObjectAnimator yScaleAnim = ObjectAnimator.ofFloat(child, View.SCALE_Y, 1 / ratio, 1);
                        animations.add(yTransAnim);
                        animations.add(xScaleAnim);
                        animations.add(yScaleAnim);
                    }
                }

                AnimatorSet set = new AnimatorSet();

                set.setDuration(300);
                set.playTogether(animations);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        childBounds.clear();
                    }
                });
                set.start();

                return true;
            }
        });
    }


    private void moveHoverCell(float x, float y) {
        int w = mHoverCell.getWidth();
        int h = mHoverCell.getHeight();
        int top = mHoverCell.getTop();
        int left = mHoverCell.getLeft();

        mHoverCell.setTranslationX(x - (w / 2));
        mHoverCell.setTranslationY(y - (h / 2));
    }


    public void setHoverView(ImageView hoverView) {
        this.mHoverCell = hoverView;
    }

    public void setScrollView(ScrollView mScrollView) {
        this.mScrollView = mScrollView;
    }

    public void setAddPictureView(View addPictureView) {
        this.mAddPictureView = addPictureView;
    }
}
