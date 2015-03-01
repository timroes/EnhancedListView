package de.timroes.android.listview;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class EnhancedRecyclerListView extends RecyclerView implements EnhancedListControl {

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

    public EnhancedRecyclerListView(Context context) {
        super(context);
        enhancedListFlow.init(context, this);

        this.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        this.setLayoutManager(linearLayoutManager);

    }

    public EnhancedRecyclerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        enhancedListFlow.init(context, this);

        this.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        this.setLayoutManager(linearLayoutManager);
    }

    public EnhancedRecyclerListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        enhancedListFlow.init(context, this);

        this.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        this.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void setUndoButton(Button undoButton) {
        this.undoButton = undoButton;
    }

    @Override
    public void incrementValidDelayedMsgId() {
        validDelayedMsgId++;
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
    public void setScreenDensity(float density) {
        this.screenDensity = density;
    }

    @Override
    public void setOnScrollListener(final de.timroes.android.listview.OnScrollListener onScrollListener) {
        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                onScrollListener.onScrollStateChanged(recyclerView, newState);
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
        return getAdapter().getItemCount();
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

    @Override
    public void slideOutView(View view, View childView, int position, boolean toRightSide) {
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
        return getChildAt(position - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition());
    }

    @Override
    public boolean superOnTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return enhancedListFlow.onTouchEvent(ev);
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
    public int getHeaderViewsCount() {
        return 0;
    }

    @Override
    public int getPositionSwipeDownView(View swipeDownView) {
        return getChildPosition(swipeDownView);
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
    public boolean isSwipeDirectionValid(float xVelocity) {

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
                return rtlSign * xVelocity < 0;
            case END:
                return rtlSign * xVelocity > 0;
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
    public void discardUndo() {
        enhancedListFlow.discardUndo();
    }

    @Override
    public EnhancedList enableSwipeToDismiss() {

        if (dismissCallback == null) {
            throw new IllegalStateException("You must pass an OnDismissCallback to the list before enabling Swipe to Dismiss.");
        }

        swipeEnabled = true;

        return this;
    }

    @Override
    public EnhancedList disableSwipeToDismiss() {
        swipeEnabled = false;
        return this;
    }

    @Override
    public EnhancedList setDismissCallback(OnDismissCallback dismissCallback) {
        this.dismissCallback = dismissCallback;
        return this;
    }

    @Override
    public EnhancedList setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback) {
        this.shouldSwipeCallback = shouldSwipeCallback;
        return this;
    }

    @Override
    public EnhancedList setUndoStyle(UndoStyle undoStyle) {
        this.undoStyle = undoStyle;
        return this;
    }

    @Override
    public EnhancedList setUndoHideDelay(int hideDelay) {
        undoHideDelay = hideDelay;
        return this;
    }

    @Override
    public EnhancedList setRequireTouchBeforeDismiss(boolean touchBeforeDismiss) {
        touchBeforeAutoHide = touchBeforeDismiss;
        return this;
    }

    @Override
    public EnhancedList setSwipeDirection(SwipeDirection direction) {
        swipeDirection = direction;
        return this;
    }

    @Override
    public EnhancedList setSwipingLayout(int swipingLayoutId) {
        swipingLayout = swipingLayoutId;
        return this;
    }

    @Override
    public void delete(int position) {
        enhancedListFlow.delete(position);
    }
}
