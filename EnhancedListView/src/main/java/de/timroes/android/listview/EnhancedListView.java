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
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
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
public class EnhancedListView extends ListView implements EnhancedList {

    EnhancedListFlow enhancedListFlow = new EnhancedListFlow();

    @Override
    public void setSlop(float slop) {
        this.slop = slop;
    }

    @Override
    public void setMinFlingVelocity(int minimumFlingVelocity) {
        this.minFlingVelocity = minimumFlingVelocity;
    }

    @Override
    public void setMaxFlingVelocity(int maximumFlingVelocity) {
        this.maxFlingVelocity = maximumFlingVelocity;
    }

    @Override
    public void setAnimationTime(int animationTime) {
        this.animationTime = animationTime;
    }

    @Override
    public void setUndoButton(Button undoButton) {
        this.undoButton = undoButton;
    }

    @Override
    public void incrementValidDelayedMsgId() {
        this.validDelayedMsgId++;
    }

    @Override
    public void setUndoPopupTextView(TextView undoPopup) {
        this.undoPopupTextView = undoPopup;
    }

    @Override
    public void setUndoPopup(PopupWindow undoPopup) {
        this.undoPopup = undoPopup;
    }

    @Override
    public void setScreenDensity(float screenDensity) {
        this.screenDensity = screenDensity;
    }

    @Override
    public void setSwipePaused(boolean swipePaused) {
        this.swipePaused = swipePaused;
    }

    @Override
    public boolean hasUndoActions() {
        return !undoActions.isEmpty();
    }

    @Override
    public UndoStyle getUndoStyle() {
        return undoStyle;
    }

    @Override
    public void undoFirstAction() {
        undoActions.get(0).undo();
        undoActions.clear();
    }

    @Override
    public void undoAll() {
        Collections.reverse(undoActions);
        for (Undoable undo : undoActions) {
            undo.undo();
        }
        undoActions.clear();
    }

    @Override
    public void undoLast() {
        undoActions.get(undoActions.size() - 1).undo();
        undoActions.remove(undoActions.size() - 1);
    }

    @Override
    public boolean hasNoUndoActions() {
        return undoActions.isEmpty();
    }

    @Override
    public boolean isUndoPopupShowing() {
        return undoPopup.isShowing();
    }

    @Override
    public void dismissUndoPopup() {
        undoPopup.dismiss();
    }

    @Override
    public int undoActionsSize() {
        return undoActions.size();
    }

    @Override
    public void setUndoPopupText(String msg) {
        this.undoPopupTextView.setText(msg);
    }

    @Override
    public String getTitleFromUndoAction(int position) {
        return undoActions.get(position).getTitle();
    }

    @Override
    public void setUndoButtonText(String msg) {
        this.undoButton.setText(msg);
    }

    private class HideUndoPopupHandler extends Handler {

        /**
         * Subclasses must implement this to receive messages.
         */
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == validDelayedMsgId) {
                discardUndo();
            }
        }
    }

    // Cached ViewConfiguration and system-wide constant values
    private float slop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private long animationTime;

    private final Object[] animationLock = new Object[0];

    // Swipe-To-Dismiss
    private boolean swipeEnabled;
    private OnDismissCallback dismissCallback;
    private OnShouldSwipeCallback shouldSwipeCallback;
    private UndoStyle undoStyle = UndoStyle.SINGLE_POPUP;
    private boolean touchBeforeAutoHide = true;
    private SwipeDirection swipeDirection = SwipeDirection.BOTH;
    private int undoHideDelay = 5000;
    private int swipingLayout;

    private List<Undoable> undoActions = new ArrayList<Undoable>();
    private SortedSet<PendingDismissData> pendingDismisses = new TreeSet<PendingDismissData>();
    private List<View> animatedViews = new LinkedList<View>();
    private int dismissAnimationRefCount;

    private boolean swipePaused;
    private boolean swiping;
    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private View swipeDownView;
    private View swipeDownChild;
    private TextView undoPopupTextView;
    private VelocityTracker velocityTracker;
    private float downX;
    private int downPosition;
    private float screenDensity;

    private PopupWindow undoPopup;
    private int validDelayedMsgId;
    private Handler hideUndoHandler = new HideUndoPopupHandler();
    private Button undoButton;
    // END Swipe-To-Dismiss

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context) {
        super(context);
        enhancedListFlow.init(context, this);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        enhancedListFlow.init(context, this);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        enhancedListFlow.init(context, this);
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

        if (dismissCallback == null) {
            throw new IllegalStateException("You must pass an OnDismissCallback to the list before enabling Swipe to Dismiss.");
        }

        swipeEnabled = true;

        return this;
    }

    /**
     * Disables the <i>Swipe to Dismiss</i> feature for this list.
     *
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView disableSwipeToDismiss() {
        swipeEnabled = false;
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
        this.dismissCallback = dismissCallback;
        return this;
    }

    /**
     * Sets the callback to be called when the user is swiping an item from the list.
     *
     * @param shouldSwipeCallback The callback used to handle swipes of list items.
     * @return This {@link de.timroes.android.listview.EnhancedListView}
     */
    public EnhancedListView setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback) {
        this.shouldSwipeCallback = shouldSwipeCallback;
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
        this.undoStyle = undoStyle;
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
        undoHideDelay = hideDelay;
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
        touchBeforeAutoHide = touchBeforeDismiss;
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
        swipeDirection = direction;
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
        swipingLayout = swipingLayoutId;
        return this;
    }

    /**
     * Discard all stored undos and hide the undo popup dialog.
     * This method must be called in {@link android.app.Activity#onStop()}. Otherwise
     * {@link de.timroes.android.listview.Undoable#discard()} might not be called for several items, what might
     * break your data consistency.
     */
    public void discardUndo() {
        for (Undoable undoable : undoActions) {
            undoable.discard();
        }
        undoActions.clear();
        if (undoPopup.isShowing()) {
            undoPopup.dismiss();
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
        if (dismissCallback == null) {
            throw new IllegalStateException("You must set an OnDismissCallback, before deleting items.");
        }
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException(String.format("Tried to delete item %d. #items in list: %d", position, getCount()));
        }
        View childView = getChildAt(position - getFirstVisiblePosition());
        View view = null;
        if (swipingLayout > 0) {
            view = childView.findViewById(swipingLayout);
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
        synchronized (animationLock) {
            if (animatedViews.contains(view)) {
                return;
            }
            ++dismissAnimationRefCount;
            animatedViews.add(view);
        }

        ViewPropertyAnimator.animate(view)
                .translationX(toRightSide ? viewWidth : -viewWidth)
                .alpha(0)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        performDismiss(view, childView, position);
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!swipeEnabled) {
            return super.onTouchEvent(ev);
        }

        // Send a delayed message to hide popup
        if (touchBeforeAutoHide && undoPopup.isShowing()) {
            hideUndoHandler.sendMessageDelayed(hideUndoHandler.obtainMessage(validDelayedMsgId), undoHideDelay);
        }

        // Store width of this list for usage of swipe distance detection
        if (viewWidth < 2) {
            viewWidth = getWidth();
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (swipePaused) {
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
                            if (swipingLayout > 0) {
                                View swipingView = child.findViewById(swipingLayout);
                                if (swipingView != null) {
                                    swipeDownView = swipingView;
                                    swipeDownChild = child;
                                    break;
                                }
                            }
                            // If no swiping layout has been found, swipe the whole child
                            swipeDownView = swipeDownChild = child;
                            break;
                        }
                    }
                }

                if (swipeDownView != null) {
                    // test if the item should be swiped
                    int position = getPositionForView(swipeDownView) - getHeaderViewsCount();
                    if ((shouldSwipeCallback == null) ||
                            shouldSwipeCallback.onShouldSwipe(this, position)) {
                        downX = ev.getRawX();
                        downPosition = position;

                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(ev);
                    } else {
                        // set back to null to revert swiping
                        swipeDownView = swipeDownChild = null;
                    }
                }
                super.onTouchEvent(ev);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (velocityTracker == null) {
                    break;
                }

                float deltaX = ev.getRawX() - downX;
                velocityTracker.addMovement(ev);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                float velocityY = Math.abs(velocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > viewWidth / 2 && swiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity
                        && velocityY < velocityX && swiping && isSwipeDirectionValid(velocityTracker.getXVelocity())
                        && deltaX >= viewWidth * 0.2f) {
                    dismiss = true;
                    dismissRight = velocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    slideOutView(swipeDownView, swipeDownChild, downPosition, dismissRight);
                } else if (swiping) {
                    // Swipe back to regular position
                    ViewPropertyAnimator.animate(swipeDownView)
                            .translationX(0)
                            .alpha(1)
                            .setDuration(animationTime)
                            .setListener(null);
                }
                velocityTracker = null;
                downX = 0;
                swipeDownView = null;
                swipeDownChild = null;
                downPosition = AbsListView.INVALID_POSITION;
                swiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {

                if (velocityTracker == null || swipePaused) {
                    break;
                }

                velocityTracker.addMovement(ev);
                float deltaX = ev.getRawX() - downX;
                // Only start swipe in correct direction
                if (isSwipeDirectionValid(deltaX)) {
                    ViewParent parent = getParent();
                    if (parent != null) {
                        // If we swipe don't allow parent to intercept touch (e.g. like NavigationDrawer does)
                        // otherwise swipe would not be working.
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(deltaX) > slop) {
                        swiping = true;
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
                    downX = ev.getRawX();
                    deltaX = 0;
                }

                if (swiping) {
                    ViewHelper.setTranslationX(swipeDownView, deltaX);
                    ViewHelper.setAlpha(swipeDownView, Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / viewWidth)));
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
        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                // Make sure no other animation is running. Remove animation from running list, that just finished
                boolean noAnimationLeft;
                synchronized (animationLock) {
                    --dismissAnimationRefCount;
                    animatedViews.remove(dismissView);
                    noAnimationLeft = dismissAnimationRefCount == 0;
                }

                if (noAnimationLeft) {
                    // No active animations, process all pending dismisses.

                    for (PendingDismissData dismiss : pendingDismisses) {
                        if (undoStyle == UndoStyle.SINGLE_POPUP) {
                            for (Undoable undoable : undoActions) {
                                undoable.discard();
                            }
                            undoActions.clear();
                        }
                        Undoable undoable = dismissCallback.onDismiss(EnhancedListView.this, dismiss.position);
                        if (undoable != null) {
                            undoActions.add(undoable);
                        }
                        validDelayedMsgId++;
                    }

                    if (!undoActions.isEmpty()) {
                        enhancedListFlow.changePopupText();
                        enhancedListFlow.changeButtonLabel();

                        // Show undo popup
                        float yLocationOffset = getResources().getDimension(R.dimen.elv_undo_bottom_offset);
                        undoPopup.setWidth((int) Math.min(screenDensity * 400, getWidth() * 0.9f));
                        undoPopup.showAtLocation(EnhancedListView.this,
                                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                                0, (int) yLocationOffset);

                        // Queue the dismiss only if required
                        if (!touchBeforeAutoHide) {
                            // Send a delayed message to hide popup
                            hideUndoHandler.sendMessageDelayed(hideUndoHandler.obtainMessage(validDelayedMsgId),
                                    undoHideDelay);
                        }
                    }

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : pendingDismisses) {
                        ViewHelper.setAlpha(pendingDismiss.view, 1f);
                        ViewHelper.setTranslationX(pendingDismiss.view, 0);
                        lp = pendingDismiss.childView.getLayoutParams();
                        lp.height = originalLayoutHeight;
                        pendingDismiss.childView.setLayoutParams(lp);
                    }

                    pendingDismisses.clear();
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

        pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
        animator.start();
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
        switch (swipeDirection) {
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
