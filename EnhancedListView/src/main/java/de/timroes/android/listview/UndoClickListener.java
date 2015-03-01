package de.timroes.android.listview;

import android.view.View;

class UndoClickListener implements View.OnClickListener {

    private EnhancedListFlow enhancedListFlow;
    private EnhancedListControl enhancedList;

    public UndoClickListener(EnhancedListControl enhancedList, EnhancedListFlow enhancedListFlow) {
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
        if (enhancedList.hasUndoActions()) {
            switch (enhancedList.getUndoStyle()) {
                case SINGLE_POPUP:
                    enhancedList.undoFirstAction();
                    break;
                case COLLAPSED_POPUP:
                    enhancedList.undoAll();
                    break;
                case MULTILEVEL_POPUP:
                    enhancedList.undoLast();
                    break;
            }
        }

        // Dismiss dialog or change text
        if (enhancedList.hasNoUndoActions()) {
            if (enhancedList.isUndoPopupShowing()) {
                enhancedList.dismissUndoPopup();
            }
        } else {
            enhancedListFlow.changePopupText();
            enhancedListFlow.changeButtonLabel();
        }

        enhancedList.incrementValidDelayedMsgId();
    }


}