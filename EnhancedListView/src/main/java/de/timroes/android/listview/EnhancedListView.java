/*
 * Copyright 2012 - 2013 Roman Nurik, Jake Wharton, Tim Roes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.timroes.android.listview;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link android.widget.ListView} offering enhanced features like Swipe To Dismiss and an
 * undo functionality. See the documentation on GitHub for more information.
 *
 * @author Tim Roes <mail@timroes.de>
 */
public class EnhancedListView extends ListView {

    private class UndoClickListener implements OnClickListener {

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            if (!mUndoActions.isEmpty()) {
                switch (mUndoStyle) {
                    case SINGLE_POPUP:
                        mUndoActions.get(0).undo();
                        mUndoActions.clear();
                        break;
                    case COLLAPSED_POPUP:
                        Collections.reverse(mUndoActions);
                        for (Undoable undo : mUndoActions) {
                            undo.undo();
                        }
                        mUndoActions.clear();
                        break;
                    case MULTILEVEL_POPUP:
                        mUndoActions.get(mUndoActions.size() - 1).undo();
                        mUndoActions.remove(mUndoActions.size() - 1);
                        break;
                }
            }

            // Dismiss dialog or change text
            if (mUndoActions.isEmpty()) {
                if (mUndoPopup.isShowing()) {
                    mUndoPopup.dismiss();
                }
            } else {
                changePopupText();
                changeButtonLabel();
            }

            mValidDelayedMsgId++;
        }
    }

    private class HideUndoPopupHandler extends Handler {

        /**
         * Subclasses must implement this to receive messages.
         */
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == mValidDelayedMsgId) {
                discardUndo();
            }
        }
    }

    // Cached ViewConfiguration and system-wide constant values
    private float mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    private final Object[] mAnimationLock = new Object[0];

    // Swipe-To-Dismiss
    private boolean mSwipeEnabled;
    private OnDismissCallback mDismissCallback;
    private OnShouldSwipeCallback mShouldSwipeCallback;
    private UndoStyle mUndoStyle = UndoStyle.SINGLE_POPUP;
    private boolean mTouchBeforeAutoHide = true;
    private SwipeDirection mSwipeDirection = SwipeDirection.BOTH;
    private int mUndoHideDelay = 5000;
    private int mSwipingLayout;

    private List<Undoable> mUndoActions = new ArrayList<Undoable>();
    private SortedSet<PendingDismissData> mPendingDismisses = new TreeSet<PendingDismissData>();
    private List<View> mAnimatedViews = new LinkedList<View>();
    private int mDismissAnimationRefCount;

    private boolean mSwipePaused;
    private boolean mSwiping;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private View mSwipeDownView;
    private View mSwipeDownChild;
    private TextView mUndoPopupTextView;
    private VelocityTracker mVelocityTracker;
    private float mDownX;
    private int mDownPosition;
    private float mScreenDensity;

    private PopupWindow mUndoPopup;
    private int mValidDelayedMsgId;
    private Handler mHideUndoHandler = new HideUndoPopupHandler();
    private Button mUndoButton;
    // END Swipe-To-Dismiss

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context ctx) {

        if (isInEditMode()) {
            // Skip initializing when in edit mode (IDE preview).
            return;
        }
        ViewConfiguration vc = ViewConfiguration.get(ctx);
        mSlop = getResources().getDimension(R.dimen.elv_touch_slop);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = ctx.getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        // Initialize undo popup
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View undoView = inflater.inflate(R.layout.elv_undo_popup, null);
        mUndoButton = (Button) undoView.findViewById(R.id.undo);
        mUndoButton.setOnClickListener(new UndoClickListener());
        mUndoButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // If the user touches the screen invalidate the current running delay by incrementing
                // the valid message id. So this delay won't hide the undo popup anymore
                mValidDelayedMsgId++;
                return false;
            }
        });
        mUndoPopupTextView = (TextView) undoView.findViewById(R.id.text);

        mUndoPopup = new PopupWindow(undoView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mUndoPopup.setAnimationStyle(R.style.elv_fade_animation);

        mScreenDensity = getResources().getDisplayMetrics().density;
        // END initialize undo popup

        setOnScrollListener(makeScrollListener());

    }

    /**
     * Enables the <i>Swipe to Dismiss</i> feature for this list. This allows users to swipe out
     * an list item element to delete it from the list. Every time the user swipes out an element
     * {@link de.timroes.android.listview.OnDismissCallback#onDismiss(EnhancedListView, int)}
     * of the given {@link de.timroes.android.listview.EnhancedListView} will be called. To enable
     * <i>undo</i> of the deletion, return an {@link de.timroes.android.listview.Undoable}
     * from {@link de.timroes.android.listview.OnDismissCallback#onDismiss(EnhancedListView, int)}.
     * Return {@code null}, if you don't want the <i>undo</i> feature enabled. Read the README file
     * or the demo project for more detailed samples.
     *
     * @return The {@link de.timroes.android.listview.EnhancedListView}
     * @throws java.lang.IllegalStateException when you haven't passed an {@link de.timroes.android.listview.OnDismissCallback}
     *                                         to {@link de.timroes.android.listview.EnhancedListView#setDismissCallback(de.timroes.android.listview.OnDismissCallback)} before calling this
     *                                         method.
     */
    public EnhancedListView enableSwipeToDismiss() {

        if (mDismissCallback == null) {
            throw new IllegalStateException("You must pass an OnDismissCallback to the list before enabling Swipe to Dismiss.");
        }

        mSwipeEnabled = true;

        return this;
    }

    /**
     * Disables the <i>Swipe to Dismiss</i> feature for this list.
     *
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView disableSwipeToDismiss() {
        mSwipeEnabled = false;
        return this;
    }

    /**
     * Sets the callback to be called when the user dismissed an item from the list (either by
     * swiping it out - with <i>Swipe to Dismiss</i> enabled - or by deleting it with
     * {@link #delete(int)}). You must call this, before you call {@link #delete(int)} or
     * {@link #enableSwipeToDismiss()} otherwise you will get an {@link java.lang.IllegalStateException}.
     *
     * @param dismissCallback The callback used to handle dismisses of list items.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setDismissCallback(OnDismissCallback dismissCallback) {
        mDismissCallback = dismissCallback;
        return this;
    }

    /**
     * Sets the callback to be called when the user is swiping an item from the list.
     *
     * @param shouldSwipeCallback The callback used to handle swipes of list items.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback) {
        mShouldSwipeCallback = shouldSwipeCallback;
        return this;
    }

    /**
     * Sets the undo style of this list. See the javadoc of {@link de.timroes.android.listview.UndoStyle}
     * for a detailed explanation of the different styles. The default style (if you never call this
     * method) is {@link de.timroes.android.listview.UndoStyle#SINGLE_POPUP}.
     *
     * @param undoStyle The style of this listview.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setUndoStyle(UndoStyle undoStyle) {
        mUndoStyle = undoStyle;
        return this;
    }

    /**
     * Sets the time in milliseconds after which the undo popup automatically disappears.
     * The countdown will start when the user touches the screen. If you want to start the countdown
     * immediately when the popups appears, call {@link #setRequireTouchBeforeDismiss(boolean)} with
     * {@code false}.
     *
     * @param hideDelay The delay in milliseconds.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setUndoHideDelay(int hideDelay) {
        mUndoHideDelay = hideDelay;
        return this;
    }

    /**
     * Sets whether another touch on the view is required before the popup counts down to dismiss
     * the undo popup. By default this is set to {@code true}.
     *
     * @param touchBeforeDismiss Whether the screen needs to be touched before the countdown starts.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     * @see #setUndoHideDelay(int)
     */
    public EnhancedListView setRequireTouchBeforeDismiss(boolean touchBeforeDismiss) {
        mTouchBeforeAutoHide = touchBeforeDismiss;
        return this;
    }

    /**
     * Sets the directions in which a list item can be swiped to delete.
     * By default this is set to {@link SwipeDirection#BOTH} so that an item
     * can be swiped into both directions.
     * <p/>
     * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
     * {@link #enableSwipeToDismiss()} to enable the feature.
     *
     * @param direction The direction to which the swipe should be limited.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setSwipeDirection(SwipeDirection direction) {
        mSwipeDirection = direction;
        return this;
    }

    /**
     * Sets the id of the view, that should be moved, when the user swipes an item.
     * Only the view with the specified id will move, while all other views in the list item, will
     * stay where they are. This might be usefull to have a background behind the view that is swiped
     * out, to stay where it is (and maybe explain that the item is going to be deleted).
     * If you never call this method (or call it with 0), the whole view will be swiped. Also if there
     * is no view in a list item, with the given id, the whole view will be swiped.
     * <p/>
     * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
     * {@link #enableSwipeToDismiss()} to enable the feature.
     *
     * @param swipingLayoutId The id (from R.id) of the view, that should be swiped.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setSwipingLayout(int swipingLayoutId) {
        mSwipingLayout = swipingLayoutId;
        return this;
    }

    /**
     * Discard all stored undos and hide the undo popup dialog.
     * This method must be called in {@link android.app.Activity#onStop()}. Otherwise
     * {@link de.timroes.android.listview.Undoable#discard()} might not be called for several items, what might
     * break your data consistency.
     */
    public void discardUndo() {
        for (Undoable undoable : mUndoActions) {
            undoable.discard();
        }
        mUndoActions.clear();
        if (mUndoPopup.isShowing()) {
            mUndoPopup.dismiss();
        }
    }

    /**
     * Delete the list item at the specified position. This will animate the item sliding out of the
     * list and then collapsing until it vanished (same as if the user slides out an item).
     * <p/>
     * NOTE: If you are using list headers, be aware, that the position argument must take care of
     * them. Meaning 0 references the first list header. So if you want to delete the first list
     * item, you have to pass the number of list headers as {@code position}. Most of the times
     * that shouldn't be a problem, since you most probably will evaluate the position which should
     * be deleted in a way, that respects the list headers.
     *
     * @param position The position of the item in the list.
     * @throws java.lang.IndexOutOfBoundsException when trying to delete an item outside of the list range.
     * @throws java.lang.IllegalStateException     when this method is called before an {@link de.timroes.android.listview.OnDismissCallback}
     *                                             is set via {@link de.timroes.android.listview.EnhancedListView#setDismissCallback(de.timroes.android.listview.OnDismissCallback)}.
     */
    public void delete(int position) {
        if (mDismissCallback == null) {
            throw new IllegalStateException("You must set an OnDismissCallback, before deleting items.");
        }
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException(String.format("Tried to delete item %d. #items in list: %d", position, getCount()));
        }
        View childView = getChildAt(position - getFirstVisiblePosition());
        View view = null;
        if (mSwipingLayout > 0) {
            view = childView.findViewById(mSwipingLayout);
        }
        if (view == null) {
            view = childView;
        }
        slideOutView(view, childView, position, true);
    }

    /**
     * Slide out a view to the right or left of the list. After the animation has finished, the
     * view will be dismissed by calling {@link #performDismiss(android.view.View, android.view.View, int)}.
     *
     * @param view        The view, that should be slided out.
     * @param childView   The whole view of the list item.
     * @param position    The item position of the item.
     * @param toRightSide Whether it should slide out to the right side.
     */
    private void slideOutView(final View view, final View childView, final int position, boolean toRightSide) {

        // Only start new animation, if this view isn't already animated (too fast swiping bug)
        synchronized (mAnimationLock) {
            if (mAnimatedViews.contains(view)) {
                return;
            }
            ++mDismissAnimationRefCount;
            mAnimatedViews.add(view);
        }

        ViewPropertyAnimator.animate(view)
                .translationX(toRightSide ? mViewWidth : -mViewWidth)
                .alpha(0)
                .setDuration(mAnimationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        performDismiss(view, childView, position);
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!mSwipeEnabled) {
            return super.onTouchEvent(ev);
        }

        // Send a delayed message to hide popup
        if (mTouchBeforeAutoHide && mUndoPopup.isShowing()) {
            mHideUndoHandler.sendMessageDelayed(mHideUndoHandler.obtainMessage(mValidDelayedMsgId), mUndoHideDelay);
        }

        // Store width of this list for usage of swipe distance detection
        if (mViewWidth < 2) {
            mViewWidth = getWidth();
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mSwipePaused) {
                    return super.onTouchEvent(ev);
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = getChildCount();
                int[] listViewCoords = new int[2];
                getLocationOnScreen(listViewCoords);
                int x = (int) ev.getRawX() - listViewCoords[0];
                int y = (int) ev.getRawY() - listViewCoords[1];
                View child;
                for (int i = getHeaderViewsCount(); i < childCount; i++) {
                    child = getChildAt(i);
                    if (child != null) {
                        child.getHitRect(rect);
                        if (rect.contains(x, y)) {
                            // if a specific swiping layout has been giving, use this to swipe.
                            if (mSwipingLayout > 0) {
                                View swipingView = child.findViewById(mSwipingLayout);
                                if (swipingView != null) {
                                    mSwipeDownView = swipingView;
                                    mSwipeDownChild = child;
                                    break;
                                }
                            }
                            // If no swiping layout has been found, swipe the whole child
                            mSwipeDownView = mSwipeDownChild = child;
                            break;
                        }
                    }
                }

                if (mSwipeDownView != null) {
                    // test if the item should be swiped
                    int position = getPositionForView(mSwipeDownView) - getHeaderViewsCount();
                    if ((mShouldSwipeCallback == null) ||
                            mShouldSwipeCallback.onShouldSwipe(this, position)) {
                        mDownX = ev.getRawX();
                        mDownPosition = position;

                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(ev);
                    } else {
                        // set back to null to revert swiping
                        mSwipeDownView = mSwipeDownChild = null;
                    }
                }
                super.onTouchEvent(ev);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = ev.getRawX() - mDownX;
                mVelocityTracker.addMovement(ev);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                float velocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity
                        && velocityY < velocityX && mSwiping && isSwipeDirectionValid(mVelocityTracker.getXVelocity())
                        && deltaX >= mViewWidth * 0.2f) {
                    dismiss = true;
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    slideOutView(mSwipeDownView, mSwipeDownChild, mDownPosition, dismissRight);
                } else if (mSwiping) {
                    // Swipe back to regular position
                    ViewPropertyAnimator.animate(mSwipeDownView)
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker = null;
                mDownX = 0;
                mSwipeDownView = null;
                mSwipeDownChild = null;
                mDownPosition = AbsListView.INVALID_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {

                if (mVelocityTracker == null || mSwipePaused) {
                    break;
                }

                mVelocityTracker.addMovement(ev);
                float deltaX = ev.getRawX() - mDownX;
                // Only start swipe in correct direction
                if (isSwipeDirectionValid(deltaX)) {
                    ViewParent parent = getParent();
                    if (parent != null) {
                        // If we swipe don't allow parent to intercept touch (e.g. like NavigationDrawer does)
                        // otherwise swipe would not be working.
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(deltaX) > mSlop) {
                        mSwiping = true;
                        requestDisallowInterceptTouchEvent(true);

                        // Cancel ListView's touch (un-highlighting the item)
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL
                                | (ev.getActionIndex()
                                << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        super.onTouchEvent(cancelEvent);
                    }
                } else {
                    // If we swiped into wrong direction, act like this was the new
                    // touch down point
                    mDownX = ev.getRawX();
                    deltaX = 0;
                }

                if (mSwiping) {
                    ViewHelper.setTranslationX(mSwipeDownView, deltaX);
                    ViewHelper.setAlpha(mSwipeDownView, Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / mViewWidth)));
                    return true;
                }
                break;
            }
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Animate the dismissed list item to zero-height and fire the dismiss callback when
     * all dismissed list item animations have completed.
     *
     * @param dismissView     The view that has been slided out.
     * @param listItemView    The list item view. This is the whole view of the list item, and not just
     *                        the part, that the user swiped.
     * @param dismissPosition The position of the view inside the list.
     */
    private void performDismiss(final View dismissView, final View listItemView, final int dismissPosition) {

        final ViewGroup.LayoutParams lp = listItemView.getLayoutParams();
        final int originalLayoutHeight = lp.height;

        int originalHeight = listItemView.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                // Make sure no other animation is running. Remove animation from running list, that just finished
                boolean noAnimationLeft;
                synchronized (mAnimationLock) {
                    --mDismissAnimationRefCount;
                    mAnimatedViews.remove(dismissView);
                    noAnimationLeft = mDismissAnimationRefCount == 0;
                }

                if (noAnimationLeft) {
                    // No active animations, process all pending dismisses.

                    for (PendingDismissData dismiss : mPendingDismisses) {
                        if (mUndoStyle == UndoStyle.SINGLE_POPUP) {
                            for (Undoable undoable : mUndoActions) {
                                undoable.discard();
                            }
                            mUndoActions.clear();
                        }
                        Undoable undoable = mDismissCallback.onDismiss(EnhancedListView.this, dismiss.position);
                        if (undoable != null) {
                            mUndoActions.add(undoable);
                        }
                        mValidDelayedMsgId++;
                    }

                    if (!mUndoActions.isEmpty()) {
                        changePopupText();
                        changeButtonLabel();

                        // Show undo popup
                        float yLocationOffset = getResources().getDimension(R.dimen.elv_undo_bottom_offset);
                        mUndoPopup.setWidth((int) Math.min(mScreenDensity * 400, getWidth() * 0.9f));
                        mUndoPopup.showAtLocation(EnhancedListView.this,
                                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                                0, (int) yLocationOffset);

                        // Queue the dismiss only if required
                        if (!mTouchBeforeAutoHide) {
                            // Send a delayed message to hide popup
                            mHideUndoHandler.sendMessageDelayed(mHideUndoHandler.obtainMessage(mValidDelayedMsgId),
                                    mUndoHideDelay);
                        }
                    }

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        ViewHelper.setAlpha(pendingDismiss.view, 1f);
                        ViewHelper.setTranslationX(pendingDismiss.view, 0);
                        lp = pendingDismiss.childView.getLayoutParams();
                        lp.height = originalLayoutHeight;
                        pendingDismiss.childView.setLayoutParams(lp);
                    }

                    mPendingDismisses.clear();
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                listItemView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
        animator.start();
    }

    /**
     * Changes the text of the undo popup. If more then one item can be undone, the number of deleted
     * items will be shown. If only one deletion can be undone, the title of this deletion (or a default
     * string in case the title is {@code null}) will be shown.
     */
    private void changePopupText() {
        String msg = null;
        if (mUndoActions.size() > 1) {
            msg = getResources().getString(R.string.elv_n_items_deleted, mUndoActions.size());
        } else if (mUndoActions.size() >= 1) {
            // Set title from single undoable or when no multiple deletion string
            // is given
            msg = mUndoActions.get(mUndoActions.size() - 1).getTitle();

            if (msg == null) {
                msg = getResources().getString(R.string.elv_item_deleted);
            }
        }
        mUndoPopupTextView.setText(msg);
    }

    /**
     * Changes the label of the undo button.
     */
    private void changeButtonLabel() {
        String msg;
        if (mUndoActions.size() > 1 && mUndoStyle == UndoStyle.COLLAPSED_POPUP) {
            msg = getResources().getString(R.string.elv_undo_all);
        } else {
            msg = getResources().getString(R.string.elv_undo);
        }
        mUndoButton.setText(msg);
    }

    private OnScrollListener makeScrollListener() {
        return new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mSwipePaused = scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        };
    }

    /**
     * Checks whether the delta of a swipe indicates, that the swipe is in the
     * correct direction, regarding the direction set via
     * {@link de.timroes.android.listview.EnhancedListView#setSwipeDirection(de.timroes.android.listview.SwipeDirection)}
     *
     * @param deltaX The delta of x coordinate of the swipe.
     * @return Whether the delta of a swipe is in the right direction.
     */
    private boolean isSwipeDirectionValid(float deltaX) {

        int rtlSign = 1;
        // On API level 17 and above, check if we are in a Right-To-Left layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                rtlSign = -1;
            }
        }

        // Check if swipe has been done in the correct direction
        switch (mSwipeDirection) {
            default:
            case BOTH:
                return true;
            case START:
                return rtlSign * deltaX < 0;
            case END:
                return rtlSign * deltaX > 0;
        }

    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

		/*
		 * If the container window no longer visiable,
		 * dismiss visible undo popup window so it won't leak,
		 * cos the container window will be destroyed before dismissing the popup window.
		 */
        if (visibility != View.VISIBLE) {
            discardUndo();
        }
    }
}
