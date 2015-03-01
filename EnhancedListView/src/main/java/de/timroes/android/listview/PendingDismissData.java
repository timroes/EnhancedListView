package de.timroes.android.listview;

import android.view.View;

class PendingDismissData implements Comparable<PendingDismissData> {

    public int position;
    /**
     * The view that should get swiped out.
     */
    public View view;
    /**
     * The whole list item view.
     */
    public View childView;

    PendingDismissData(int position, View view, View childView) {
        this.position = position;
        this.view = view;
        this.childView = childView;
    }

    @Override
    public int compareTo(PendingDismissData other) {
        // Sort by descending position
        return other.position - position;
    }

}