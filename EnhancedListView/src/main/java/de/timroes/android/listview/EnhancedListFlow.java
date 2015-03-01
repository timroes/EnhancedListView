package de.timroes.android.listview;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class EnhancedListFlow {

    private Context context;
    private EnhancedListControl enhancedList;

    public void init(Context ctx, final EnhancedListControl enhancedList) {
        this.context = ctx;
        this.enhancedList = enhancedList;

        if (enhancedList.isInEditMode()) {
            // Skip initializing when in edit mode (IDE preview).
            return;
        }
        ViewConfiguration vc = ViewConfiguration.get(ctx);
        enhancedList.setSlop(ctx.getResources().getDimension(R.dimen.elv_touch_slop));

        enhancedList.setMinFlingVelocity(vc.getScaledMinimumFlingVelocity());
        enhancedList.setMaxFlingVelocity(vc.getScaledMaximumFlingVelocity());

        enhancedList.setAnimationTime(ctx.getResources().getInteger(
                android.R.integer.config_shortAnimTime));

        // Initialize undo popup
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View undoView = inflater.inflate(R.layout.elv_undo_popup, null);

        Button mUndoButton = (Button) undoView.findViewById(R.id.undo);
        mUndoButton.setOnClickListener(new UndoClickListener(enhancedList, this));
        mUndoButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // If the user touches the screen invalidate the current running delay by incrementing
                // the valid message id. So this delay won't hide the undo popup anymore
                enhancedList.incrementValidDelayedMsgId();
                return false;
            }
        });
        enhancedList.setUndoButton(mUndoButton);

        enhancedList.setUndoPopupTextView((TextView) undoView.findViewById(R.id.text));

        PopupWindow mUndoPopup = new PopupWindow(undoView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mUndoPopup.setAnimationStyle(R.style.elv_fade_animation);
        enhancedList.setUndoPopup(mUndoPopup);

        enhancedList.setScreenDensity(ctx.getResources().getDisplayMetrics().density);
        // END initialize undo popup

        enhancedList.setOnScrollListener(makeScrollListener(enhancedList));
    }

    private AbsListView.OnScrollListener makeScrollListener(final EnhancedListControl enhancedList) {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                enhancedList.setSwipePaused(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        };
    }

    /**
     * Changes the text of the undo popup. If more then one item can be undone, the number of deleted
     * items will be shown. If only one deletion can be undone, the title of this deletion (or a default
     * string in case the title is {@code null}) will be shown.
     */
    public void changePopupText() {
        String msg = null;
        if (enhancedList.undoActionsSize() > 1) {
            msg = context.getResources().getString(R.string.elv_n_items_deleted, enhancedList.undoActionsSize());
        } else if (enhancedList.undoActionsSize() >= 1) {
            // Set title from single undoable or when no multiple deletion string
            // is given
            msg = enhancedList.getTitleFromUndoAction(enhancedList.undoActionsSize() - 1);

            if (msg == null) {
                msg = context.getResources().getString(R.string.elv_item_deleted);
            }
        }
        enhancedList.setUndoPopupText(msg);
    }

    /**
     * Changes the label of the undo button.
     */
    public void changeButtonLabel() {
        String msg;
        if (enhancedList.undoActionsSize() > 1 && enhancedList.getUndoStyle() == UndoStyle.COLLAPSED_POPUP) {
            msg = context.getResources().getString(R.string.elv_undo_all);
        } else {
            msg = context.getResources().getString(R.string.elv_undo);
        }
        enhancedList.setUndoButtonText(msg);
    }

    /**
     * Discard all stored undos and hide the undo popup dialog.
     * This method must be called in {@link android.app.Activity#onStop()}. Otherwise
     * {@link de.timroes.android.listview.Undoable#discard()} might not be called for several items, what might
     * break your data consistency.
     */
    public void discardUndo() {
        enhancedList.discardAllUndoables();
        enhancedList.dismissUndoPopup();
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
        if (!enhancedList.hasDismissCallback()) {
            throw new IllegalStateException("You must set an OnDismissCallback, before deleting items.");
        }
        if (position < 0 || position >= enhancedList.getItemsCount()) {
            throw new IndexOutOfBoundsException(String.format("Tried to delete item %d. #items in list: %d", position, enhancedList.getItemsCount()));
        }
        View childView = enhancedList.getChild(position);
        View view = null;
        if (enhancedList.hasSwipingLayout()) {
            view = childView.findViewById(enhancedList.getSwipingLayout());
        }
        if (view == null) {
            view = childView;
        }
        enhancedList.slideOutView(view, childView, position, true);
    }

    private float downX;
    private int downPosition;
    private boolean swiping;
    private VelocityTracker velocityTracker;

    public boolean onTouchEvent(MotionEvent ev) {

        if (!enhancedList.isSwipeEnabled()) {
            return enhancedList.superOnTouchEvent(ev);
        }

        // Send a delayed message to hide popup
        if (enhancedList.getTouchBeforeAutoHide() && enhancedList.isUndoPopupShowing()) {
            enhancedList.hidePopupMessageDelayed();
        }

        // Store width of this list for usage of swipe distance detection
        if (enhancedList.getViewWidth() < 2) {
            enhancedList.updateViewWidth();
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (enhancedList.getSwipePaused()) {
                    return enhancedList.superOnTouchEvent(ev);
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = enhancedList.getChildCount();
                int[] listViewCoords = new int[2];
                enhancedList.getLocationOnScreen(listViewCoords);
                int x = (int) ev.getRawX() - listViewCoords[0];
                int y = (int) ev.getRawY() - listViewCoords[1];
                View child;
                for (int i = enhancedList.getHeaderViewsCount(); i < childCount; i++) {
                    child = enhancedList.getChildAt(i);
                    if (child != null) {
                        child.getHitRect(rect);
                        if (rect.contains(x, y)) {
                            // if a specific swiping layout has been giving, use this to swipe.
                            if (enhancedList.hasSwipingLayout()) {
                                View swipingView = child.findViewById(enhancedList.getSwipingLayout());
                                if (swipingView != null) {
                                    enhancedList.setSwipeDownView(swipingView);
                                    enhancedList.setSwipeDownChild(child);
                                    break;
                                }
                            }
                            // If no swiping layout has been found, swipe the whole child
                            enhancedList.setSwipeDownView(child);
                            enhancedList.setSwipeDownChild(child);
                            break;
                        }
                    }
                }

                if (enhancedList.hasSwipeDownView()) {
                    // test if the item should be swiped
                    int position = enhancedList.getPositionSwipeDownView();
                    if ((!enhancedList.hasSwipeCallback()) ||
                            enhancedList.onShouldSwipe(position)) {
                        downX = ev.getRawX();
                        downPosition = position;
                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(ev);
                    } else {
                        // set back to null to revert swiping
                        enhancedList.setSwipeDownView(null);
                        enhancedList.setSwipeDownChild(null);
                    }
                }
                enhancedList.superOnTouchEvent(ev);
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
                if (Math.abs(deltaX) > enhancedList.getViewWidth() / 2 && swiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (enhancedList.getMinFlingVelocity() <= velocityX && velocityX <= enhancedList.getMaxFlingVelocity()
                        && velocityY < velocityX && swiping && enhancedList.isSwipeDirectionValid(velocityTracker.getXVelocity())
                        && deltaX >= enhancedList.getViewWidth() * 0.2f) {
                    dismiss = true;
                    dismissRight = velocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    enhancedList.slideOutView(enhancedList.getSwipeDownView(), enhancedList.getSwipeDownChild(), downPosition, dismissRight);
                } else if (swiping) {
                    // Swipe back to regular position
                    ViewPropertyAnimator.animate(enhancedList.getSwipeDownView())
                            .translationX(0)
                            .alpha(1)
                            .setDuration(enhancedList.getAnimationTime())
                            .setListener(null);
                }
                velocityTracker = null;
                downX = 0;
                enhancedList.setSwipeDownView(null);
                enhancedList.setSwipeDownChild(null);
                downPosition = AbsListView.INVALID_POSITION;
                swiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {

                if (velocityTracker == null || enhancedList.getSwipePaused()) {
                    break;
                }

                velocityTracker.addMovement(ev);
                float deltaX = ev.getRawX() - downX;
                // Only start swipe in correct direction
                if (enhancedList.isSwipeDirectionValid(deltaX)) {
                    ViewParent parent = enhancedList.getParent();
                    if (parent != null) {
                        // If we swipe don't allow parent to intercept touch (e.g. like NavigationDrawer does)
                        // otherwise swipe would not be working.
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(deltaX) > enhancedList.getSlop()) {
                        swiping = true;
                        enhancedList.requestDisallowInterceptTouchEvent(true);

                        // Cancel ListView's touch (un-highlighting the item)
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL
                                | (ev.getActionIndex()
                                << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        enhancedList.superOnTouchEvent(cancelEvent);
                    }
                } else {
                    // If we swiped into wrong direction, act like this was the new
                    // touch down point
                    downX = ev.getRawX();
                    deltaX = 0;
                }

                if (swiping) {
                    ViewHelper.setTranslationX(enhancedList.getSwipeDownView(), deltaX);
                    ViewHelper.setAlpha(enhancedList.getSwipeDownView(), Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / enhancedList.getViewWidth())));
                    return true;
                }
                break;
            }
        }
        return enhancedList.superOnTouchEvent(ev);
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
    public void performDismiss(final View dismissView, final View listItemView, final int dismissPosition) {

        final ViewGroup.LayoutParams lp = listItemView.getLayoutParams();
        final int originalLayoutHeight = lp.height;

        int originalHeight = listItemView.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(enhancedList.getAnimationTime());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                // Make sure no other animation is running. Remove animation from running list, that just finished
                boolean noAnimationLeft = enhancedList.removeAnimation(dismissView);

                if (noAnimationLeft) {
                    // No active animations, process all pending dismisses.

                    for (PendingDismissData dismiss : enhancedList.getPendingDismisses()) {
                        if (enhancedList.getUndoStyle() == UndoStyle.SINGLE_POPUP) {
                            enhancedList.discardAllUndoables();
                        }
                        Undoable undoable = enhancedList.onDismiss(dismiss.position);
                        if (undoable != null) {
                            enhancedList.addUndoAction(undoable);
                        }
                        enhancedList.incrementValidDelayedMsgId();
                    }

                    if (enhancedList.hasUndoActions()) {
                        changePopupText();
                        changeButtonLabel();

                        // Show undo popup
                        float yLocationOffset = context.getResources().getDimension(R.dimen.elv_undo_bottom_offset);
                        enhancedList.showUndoPopup(yLocationOffset);

                        // Queue the dismiss only if required
                        if (!enhancedList.getTouchBeforeAutoHide()) {
                            // Send a delayed message to hide popup
                            enhancedList.hidePopupMessageDelayed();
                        }
                    }

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : enhancedList.getPendingDismisses()) {
                        ViewHelper.setAlpha(pendingDismiss.view, 1f);
                        ViewHelper.setTranslationX(pendingDismiss.view, 0);
                        lp = pendingDismiss.childView.getLayoutParams();
                        lp.height = originalLayoutHeight;
                        pendingDismiss.childView.setLayoutParams(lp);
                    }

                    enhancedList.clearPendingDismissed();
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

        enhancedList.addPendingDismiss(new PendingDismissData(dismissPosition, dismissView, listItemView));
        animator.start();
    }
}
