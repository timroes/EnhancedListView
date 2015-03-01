package de.timroes.android.listview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public class EnhancedRecyclerListView extends RecyclerView implements EnhancedList {

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
    public void setSlop(float dimension) {

    }

    @Override
    public void setMinFlingVelocity(int scaledMinimumFlingVelocity) {

    }

    @Override
    public void setMaxFlingVelocity(int scaledMaximumFlingVelocity) {

    }

    @Override
    public void setAnimationTime(int integer) {

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
    public void setSwipePaused(boolean b) {

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
    public boolean hasNoUndoActions() {
        return false;
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
    public String getTitleFromUndoAction(int i) {
        return null;
    }

    @Override
    public void setUndoButtonText(String msg) {

    }
}
