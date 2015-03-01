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

    void setUndoButton(Button undoButton);

    void incrementValidDelayedMsgId();

    void setUndoPopupTextView(TextView undoPopup);

    void setUndoPopup(PopupWindow mUndoButton);

    void setScreenDensity(float density);

    void setOnScrollListener(OnScrollListener onScrollListener);

    boolean hasUndoActions();

    UndoStyle getUndoStyle();

    void undoFirstAction();

    void undoAll();

    void undoLast();

    boolean isUndoPopupShowing();

    void dismissUndoPopup();

    int undoActionsSize();

    void setUndoPopupText(String msg);

    String getTitleFromUndoAction(int position);

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

    int getChildCount();

    void getLocationOnScreen(int[] listViewCoords);

    int getHeaderViewsCount();

    View getChildAt(int i);

    int getPositionSwipeDownView(View swipeDownView);

    boolean hasSwipeCallback();

    boolean onShouldSwipe(int position);

    boolean isSwipeDirectionValid(float xVelocity);

    ViewParent getParent();

    void requestDisallowInterceptTouchEvent(boolean b);

    boolean removeAnimation(View dismissView);

    SortedSet<PendingDismissData> getPendingDismisses();

    Undoable onDismiss(int position);

    void addUndoAction(Undoable undoable);

    void showUndoPopup(float yLocationOffset);

    void clearPendingDismissed();

    void addPendingDismiss(PendingDismissData pendingDismissData);

    boolean shouldPrepareAnimation(View view);

    int getWidth();
}
