package de.timroes.android.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public class EnhancedListFlow {

    private Context context;
    private EnhancedList enhancedList;

    public void init(Context ctx, final EnhancedList enhancedList) {
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
                //mValidDelayedMsgId++;
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

    private AbsListView.OnScrollListener makeScrollListener(final EnhancedList enhancedList) {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //mSwipePaused = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
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
        //if (mUndoActions.size() > 1) {
        if (enhancedList.undoActionsSize() > 1) {
            msg = context.getResources().getString(R.string.elv_n_items_deleted, enhancedList.undoActionsSize());
        } else if (enhancedList.undoActionsSize() >= 1) {
            // Set title from single undoable or when no multiple deletion string
            // is given
            //msg = mUndoActions.get(mUndoActions.size() - 1).getTitle();
            msg = enhancedList.getTitleFromUndoAction(enhancedList.undoActionsSize() - 1);

            if (msg == null) {
                msg = context.getResources().getString(R.string.elv_item_deleted);
            }
        }
        enhancedList.setUndoPopupText(msg);
        //mUndoPopupTextView.setText(msg);
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
        //mUndoButton.setText(msg);
    }
}
