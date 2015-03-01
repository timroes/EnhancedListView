package de.timroes.android.listview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.SortedSet;

public class EnhancedRecyclerListView extends RecyclerView implements EnhancedListControl {

    EnhancedListFlow enhancedListFlow = new EnhancedListFlow();

    public EnhancedRecyclerListView(Context context) {
        super(context);
        enhancedListFlow.init(context, this);
    }

    public EnhancedRecyclerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        enhancedListFlow.init(context, this);
    }

    public EnhancedRecyclerListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        enhancedListFlow.init(context, this);
    }

    @Override
    public void setUndoButton(Button undoButton) {

    }

    @Override
    public void incrementValidDelayedMsgId() {

    }

    @Override
    public void setUndoPopupTextView(TextView undoPopup) {

    }

    @Override
    public void setUndoPopup(PopupWindow mUndoButton) {

    }

    @Override
    public void setScreenDensity(float density) {

    }

    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {

    }

    @Override
    public boolean hasUndoActions() {
        return false;
    }

    @Override
    public UndoStyle getUndoStyle() {
        return null;
    }

    @Override
    public void undoFirstAction() {

    }

    @Override
    public void undoAll() {

    }

    @Override
    public void undoLast() {

    }

    @Override
    public boolean isUndoPopupShowing() {
        return false;
    }

    @Override
    public void dismissUndoPopup() {

    }

    @Override
    public int undoActionsSize() {
        return 0;
    }

    @Override
    public void setUndoPopupText(String msg) {

    }

    @Override
    public String getTitleFromUndoAction(int position) {
        return null;
    }

    @Override
    public void setUndoButtonText(String msg) {

    }

    @Override
    public void discardAllUndoables() {

    }

    @Override
    public boolean hasDismissCallback() {
        return false;
    }

    @Override
    public int getItemsCount() {
        return 0;
    }

    @Override
    public void slideOutView(View view, View childView, int position, boolean b) {

    }

    @Override
    public boolean hasSwipingLayout() {
        return false;
    }

    @Override
    public int getSwipingLayout() {
        return 0;
    }

    @Override
    public View getChild(int position) {
        return null;
    }

    @Override
    public boolean superOnTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean isSwipeEnabled() {
        return false;
    }

    @Override
    public boolean getTouchBeforeAutoHide() {
        return false;
    }

    @Override
    public void hidePopupMessageDelayed() {

    }

    @Override
    public int getHeaderViewsCount() {
        return 0;
    }

    @Override
    public int getPositionSwipeDownView(View swipeDownView) {
        return 0;
    }

    @Override
    public boolean hasSwipeCallback() {
        return false;
    }

    @Override
    public boolean onShouldSwipe(int position) {
        return false;
    }

    @Override
    public boolean isSwipeDirectionValid(float xVelocity) {
        return false;
    }

    @Override
    public boolean removeAnimation(View dismissView) {
        return false;
    }

    @Override
    public SortedSet<PendingDismissData> getPendingDismisses() {
        return null;
    }

    @Override
    public Undoable onDismiss(int position) {
        return null;
    }

    @Override
    public void addUndoAction(Undoable undoable) {

    }

    @Override
    public void showUndoPopup(float yLocationOffset) {

    }

    @Override
    public void clearPendingDismissed() {

    }

    @Override
    public void addPendingDismiss(PendingDismissData pendingDismissData) {

    }

    @Override
    public boolean shouldPrepareAnimation(View view) {
        return false;
    }

    @Override
    public void discardUndo() {

    }

    @Override
    public EnhancedListView enableSwipeToDismiss() {
        return null;
    }

    @Override
    public EnhancedListView disableSwipeToDismiss() {
        return null;
    }

    @Override
    public EnhancedListView setDismissCallback(OnDismissCallback dismissCallback) {
        return null;
    }

    @Override
    public EnhancedListView setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback) {
        return null;
    }

    @Override
    public EnhancedListView setUndoStyle(UndoStyle undoStyle) {
        return null;
    }

    @Override
    public EnhancedListView setUndoHideDelay(int hideDelay) {
        return null;
    }

    @Override
    public EnhancedListView setRequireTouchBeforeDismiss(boolean touchBeforeDismiss) {
        return null;
    }

    @Override
    public EnhancedListView setSwipeDirection(SwipeDirection direction) {
        return null;
    }

    @Override
    public EnhancedListView setSwipingLayout(int swipingLayoutId) {
        return null;
    }
}
