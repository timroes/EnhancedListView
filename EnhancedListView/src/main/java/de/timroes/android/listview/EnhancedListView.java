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
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

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
public class EnhancedListView extends ListView implements EnhancedListControl {

    EnhancedListFlow enhancedListFlow = new EnhancedListFlow();
    private final Object[] animationLock = new Object[0];

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

    private TextView undoPopupTextView;
    private float screenDensity;

    private PopupWindow undoPopup;
    private int validDelayedMsgId;
    private Handler hideUndoHandler = new HideUndoPopupHandler();
    private Button undoButton;

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
    public void setOnScrollListener(final de.timroes.android.listview.OnScrollListener onScrollListener) {
        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                onScrollListener.onScrollStateChanged(view, scrollState);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
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
    public boolean isUndoPopupShowing() {
        return undoPopup.isShowing();
    }

    @Override
    public void dismissUndoPopup() {
        if (undoPopup.isShowing()) {
            undoPopup.dismiss();
        }
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

    @Override
    public void discardAllUndoables() {
        for (Undoable undoable : undoActions) {
            undoable.discard();
        }
        undoActions.clear();
    }

    @Override
    public boolean hasDismissCallback() {
        return dismissCallback != null;
    }

    @Override
    public int getItemsCount() {
        return getCount();
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
        enhancedListFlow.discardUndo();
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
        enhancedListFlow.delete(position);
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
    public void slideOutView(final View view, final View childView, final int position, boolean toRightSide) {
        enhancedListFlow.slideOutView(view, childView, position, toRightSide);
    }

    @Override
    public boolean hasSwipingLayout() {
        return swipingLayout > 0;
    }

    @Override
    public int getSwipingLayout() {
        return swipingLayout;
    }

    @Override
    public View getChild(int position) {
        return getChildAt(position - getFirstVisiblePosition());
    }

    @Override
    public boolean superOnTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean isSwipeEnabled() {
        return swipeEnabled;
    }

    @Override
    public boolean getTouchBeforeAutoHide() {
        return touchBeforeAutoHide;
    }

    @Override
    public void hidePopupMessageDelayed() {
        hideUndoHandler.sendMessageDelayed(hideUndoHandler.obtainMessage(validDelayedMsgId), undoHideDelay);
    }

    @Override
    public int getPositionSwipeDownView(View swipeDownView) {
        return getPositionForView(swipeDownView) - getHeaderViewsCount();
    }

    @Override
    public boolean hasSwipeCallback() {
        return shouldSwipeCallback != null;
    }

    @Override
    public boolean onShouldSwipe(int position) {
        return shouldSwipeCallback.onShouldSwipe(this, position);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return enhancedListFlow.onTouchEvent(ev);
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
        enhancedListFlow.performDismiss(dismissView, listItemView, dismissPosition);
    }

    /**
     * Checks whether the delta of a swipe indicates, that the swipe is in the
     * correct direction, regarding the direction set via
     * {@link de.timroes.android.listview.EnhancedListView#setSwipeDirection(de.timroes.android.listview.SwipeDirection)}
     *
     * @param deltaX The delta of x coordinate of the swipe.
     * @return Whether the delta of a swipe is in the right direction.
     */
    public boolean isSwipeDirectionValid(float deltaX) {

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
    public boolean removeAnimation(View dismissView) {
        synchronized (animationLock) {
            --dismissAnimationRefCount;
            animatedViews.remove(dismissView);
            boolean noAnimationLeft = dismissAnimationRefCount == 0;
            return noAnimationLeft;
        }
    }

    @Override
    public SortedSet<PendingDismissData> getPendingDismisses() {
        return pendingDismisses;
    }

    @Override
    public Undoable onDismiss(int position) {
        return dismissCallback.onDismiss(this, position);
    }

    @Override
    public void addUndoAction(Undoable undoable) {
        undoActions.add(undoable);
    }

    @Override
    public void showUndoPopup(float yLocationOffset) {
        undoPopup.setWidth((int) Math.min(screenDensity * 400, getWidth() * 0.9f));
        undoPopup.showAtLocation(this,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                0, (int) yLocationOffset);
    }

    @Override
    public void clearPendingDismissed() {
        pendingDismisses.clear();
    }

    @Override
    public void addPendingDismiss(PendingDismissData pendingDismissData) {
        pendingDismisses.add(pendingDismissData);
    }

    @Override
    public boolean shouldPrepareAnimation(View view) {
        // Only start new animation, if this view isn't already animated (too fast swiping bug)
        synchronized (animationLock) {
            if (animatedViews.contains(view)) {
                return false;
            }
            ++dismissAnimationRefCount;
            animatedViews.add(view);
            return true;
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
