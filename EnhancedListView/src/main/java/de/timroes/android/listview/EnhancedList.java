package de.timroes.android.listview;

import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public interface EnhancedList {
    boolean isInEditMode();

    void setSlop(float dimension);

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
}
