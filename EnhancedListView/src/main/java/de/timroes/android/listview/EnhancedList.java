package de.timroes.android.listview;

import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public interface EnhancedList {

    void discardUndo();

    EnhancedListView enableSwipeToDismiss();

    EnhancedListView disableSwipeToDismiss();

    EnhancedListView setDismissCallback(OnDismissCallback dismissCallback);

    EnhancedListView setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback);

    EnhancedListView setUndoStyle(UndoStyle undoStyle);

    EnhancedListView setUndoHideDelay(int hideDelay);

    EnhancedListView setRequireTouchBeforeDismiss(boolean touchBeforeDismiss);

    EnhancedListView setSwipeDirection(SwipeDirection direction);

    EnhancedListView setSwipingLayout(int swipingLayoutId);
}
