package de.timroes.android.listview;

import android.view.View;

class UndoClickListener implements View.OnClickListener {

    private EnhancedListFlow enhancedListFlow;
    private EnhancedList enhancedList;

    public UndoClickListener(EnhancedList enhancedList, EnhancedListFlow enhancedListFlow) {
        this.enhancedList = enhancedList;
        this.enhancedListFlow = enhancedListFlow;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        //if (!mUndoActions.isEmpty()) {
        if (enhancedList.hasUndoActions()) {
            switch (enhancedList.getUndoStyle()) {
                case SINGLE_POPUP:
                    enhancedList.undoFirstAction();
                    //mUndoActions.get(0).undo();
                    //mUndoActions.clear();
                    break;
                case COLLAPSED_POPUP:
                    //Collections.reverse(mUndoActions);
                    //for (Undoable undo : mUndoActions) {
                    //    undo.undo();
                    //}
                    //mUndoActions.clear();
                    enhancedList.undoAll();
                    break;
                case MULTILEVEL_POPUP:
                    //mUndoActions.get(mUndoActions.size() - 1).undo();
                    //mUndoActions.remove(mUndoActions.size() - 1);
                    enhancedList.undoLast();
                    break;
            }
        }

        // Dismiss dialog or change text
        //if (mUndoActions.isEmpty()) {
        if (enhancedList.hasNoUndoActions()) {
            if (enhancedList.isUndoPopupShowing()) {
                enhancedList.dismissUndoPopup();
                //mUndoPopup.dismiss();
            }
        } else {
            enhancedListFlow.changePopupText();
            enhancedListFlow.changeButtonLabel();
        }

        //mValidDelayedMsgId++;
        enhancedList.incrementValidDelayedMsgId();
    }


}