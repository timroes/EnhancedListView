package de.timroes.android.listview;

/**
 * Defines the direction in which list items can be swiped out to delete them.
 * Use {@link de.timroes.android.listview.EnhancedListView#setSwipeDirection(SwipeDirection)}
 * to change the default behavior.
 * <p>
 * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
 * {@link de.timroes.android.listview.EnhancedListView#enableSwipeToDismiss()}
 * to enable the feature.
 */
public enum SwipeDirection {

    /**
     * The user can swipe each item into both directions (left and right) to delete it.
     */
    BOTH,

    /**
     * The user can only swipe the items to the beginning of the item to
     * delete it. The start of an item is in Left-To-Right languages the left
     * side and in Right-To-Left languages the right side. Before API level
     * 17 this is always the left side.
     */
    START,

    /**
     * The user can only swipe the items to the end of the item to delete it.
     * This is in Left-To-Right languages the right side in Right-To-Left
     * languages the left side. Before API level 17 this will always be the
     * right side.
     */
    END

}