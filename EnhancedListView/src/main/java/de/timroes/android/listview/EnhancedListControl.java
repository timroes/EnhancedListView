package de.timroes.android.listview;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.SortedSet;

public interface EnhancedListControl extends EnhancedList {

    boolean isInEditMode();

    void setSlop(float dimension);

    float getSlop();

    void setMinFlingVelocity(int scaledMinimumFlingVelocity);

    void setMaxFlingVelocity(int scaledMaximumFlingVelocity);

    void setAnimationTime(int integer);

    void setUndoButton(Button undoButton);

    void incrementValidDelayedMsgId();

    void setUndoPopupTextView(TextView undoPopup);

    void setUndoPopup(PopupWindow mUndoButton);

    void setScreenDensity(float density);

    void setOnScrollListener(AbsListView.OnScrollListener onScrollListener);

    void setSwipePaused(boolean b);

    boolean hasUndoActions();

    UndoStyle getUndoStyle();

    void undoFirstAction();

    void undoAll();

    void undoLast();

    boolean hasNoUndoActions();

    boolean isUndoPopupShowing();

    void dismissUndoPopup();

    int undoActionsSize();

    void setUndoPopupText(String msg);

    String getTitleFromUndoAction(int i);

    void setUndoButtonText(String msg);

    void discardAllUndoables();

    boolean hasDismissCallback();

    int getItemsCount();

    void slideOutView(View view, View childView, int position, boolean b);

    boolean hasSwipingLayout();

    int getSwipingLayout();

    View getChild(int position);

    boolean superOnTouchEvent(MotionEvent ev);

    boolean isSwipeEnabled();

    boolean getTouchBeforeAutoHide();

    void hidePopupMessageDelayed();

    int getViewWidth();

    void updateViewWidth();

    boolean getSwipePaused();

    int getChildCount();

    void getLocationOnScreen(int[] listViewCoords);

    int getHeaderViewsCount();

    View getChildAt(int i);

    void setSwipeDownView(View swipingView);

    void setSwipeDownChild(View child);

    boolean hasSwipeDownView();

    int getPositionSwipeDownView();

    boolean hasSwipeCallback();

    boolean onShouldSwipe(int position);

    float getMinFlingVelocity();

    float getMaxFlingVelocity();

    boolean isSwipeDirectionValid(float xVelocity);

    View getSwipeDownView();

    View getSwipeDownChild();

    long getAnimationTime();

    ViewParent getParent();

    void requestDisallowInterceptTouchEvent(boolean b);

    boolean removeAnimation(View dismissView);

    SortedSet<PendingDismissData> getPendingDismisses();

    Undoable onDismiss(int position);

    void addUndoAction(Undoable undoable);

    void showUndoPopup(float yLocationOffset);

    void clearPendingDismissed();

    void addPendingDismiss(PendingDismissData pendingDismissData);
}
